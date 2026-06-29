package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentFactorDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentFactorSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentMatchDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentRisk;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicScoreboard;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamVisual;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PublicSentimentService {
    private static final Pattern SCORE_PATTERN = Pattern.compile("(\\d{1,2})\\s*[-:：比]\\s*(\\d{1,2})");
    private static final BigDecimal DERIVED_FACTOR_SCORE = BigDecimal.valueOf(7.0);

    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;

    public PublicSentimentService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PublicSentimentFactorSummary> overview() {
        ArrayList<PublicSentimentFactorSummary> factors = new ArrayList<>(jdbcTemplate.query(
                factorSummarySelect() + " ORDER BY m.matchday DESC, mcf.id DESC",
                (rs, rowNum) -> factorSummary(rs)
        ));
        factors.addAll(scheduleFactorSummaries());
        factors.addAll(oddsMarketFactorSummaries());
        factors.sort(sentimentSummaryComparator());
        return factors;
    }

    @Transactional(readOnly = true)
    public PublicSentimentMatchDetail matchSentiment(long matchId) {
        MatchRow match = findMatch(matchId);
        ArrayList<PublicSentimentFactorDetail> factors = new ArrayList<>(jdbcTemplate.query(
                factorDetailSelect() + " WHERE mcf.match_id=? ORDER BY mcf.factor_category, mcf.id",
                (rs, rowNum) -> factorDetail(rs),
                matchId
        ));
        factors.addAll(scheduleFactorDetails(matchId));
        factors.addAll(oddsMarketFactorDetails(matchId));
        factors.sort(sentimentDetailComparator());
        List<PublicSentimentRisk> risks = jdbcTemplate.query(
                riskSelect() + " WHERE sar.match_id=? ORDER BY sar.id",
                (rs, rowNum) -> risk(rs),
                matchId
        );
        return new PublicSentimentMatchDetail(
                match.id(),
                match.matchName(),
                match.matchday(),
                match.jcCode(),
                match.homeTeam(),
                match.awayTeam(),
                match.scoreboard(),
                factors,
                risks
        );
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        ArrayList<String> categories = new ArrayList<>(jdbcTemplate.query(
                "SELECT DISTINCT factor_category FROM match_context_factors ORDER BY factor_category",
                (rs, rowNum) -> mapper.sanitizeToken(rs.getString("factor_category"))
        ));
        if (hasRows("matches", "matchday IS NOT NULL OR kickoff_time IS NOT NULL OR stage IS NOT NULL OR venue IS NOT NULL")) {
            addCategory(categories, "SCHEDULE");
        }
        if (hasRows("odds_market_snapshots", "match_id IS NOT NULL")) {
            addCategory(categories, "MARKET_SIGNAL");
        }
        categories.sort(String::compareTo);
        return categories;
    }

    @Transactional(readOnly = true)
    public List<String> riskTypes() {
        return jdbcTemplate.query(
                "SELECT DISTINCT risk_type FROM sentiment_risk_assessments ORDER BY risk_type",
                (rs, rowNum) -> mapper.sanitizeToken(rs.getString("risk_type"))
        );
    }

    private MatchRow findMatch(long matchId) {
        return jdbcTemplate.query(
                        """
                        SELECT m.id, m.match_name, m.matchday, m.jc_code, m.status, m.result_status, m.kickoff_time,
                               ht.id AS home_team_id, ht.display_name AS home_team_name, ht.fifa_code AS home_fifa_code,
                               ht.country_iso2 AS home_country_iso2, ht.flag_asset_key AS home_flag_asset_key,
                               ht.country_region AS home_country_region,
                               at.id AS away_team_id, at.display_name AS away_team_name, at.fifa_code AS away_fifa_code,
                               at.country_iso2 AS away_country_iso2, at.flag_asset_key AS away_flag_asset_key,
                               at.country_region AS away_country_region,
                               (SELECT MAX(CASE WHEN mts.team_id=m.home_team_id THEN mts.goals_for END)
                                  FROM match_team_stats mts WHERE mts.match_id=m.id) AS home_score,
                               (SELECT MAX(CASE WHEN mts.team_id=m.away_team_id THEN mts.goals_for END)
                                  FROM match_team_stats mts WHERE mts.match_id=m.id) AS away_score,
                               (SELECT COUNT(*) FROM match_events e
                                  WHERE e.match_id=m.id AND e.team_id=m.home_team_id
                                    AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS home_event_score,
                               (SELECT COUNT(*) FROM match_events e
                                  WHERE e.match_id=m.id AND e.team_id=m.away_team_id
                                    AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS away_event_score,
                               (SELECT se.summary FROM source_evidence se
                                  WHERE se.match_id=m.id AND se.summary IS NOT NULL
                                  ORDER BY se.reliability_score DESC, se.id DESC LIMIT 1) AS evidence_summary
                        FROM matches m
                        LEFT JOIN teams ht ON ht.id=m.home_team_id
                        LEFT JOIN teams at ON at.id=m.away_team_id
                        WHERE m.id=?
                        """,
                        (rs, rowNum) -> new MatchRow(
                                rs.getLong("id"),
                                mapper.sanitizeText(rs.getString("match_name")),
                                localDate(rs, "matchday"),
                                mapper.sanitizeText(rs.getString("jc_code")),
                                teamVisual(rs, "home"),
                                teamVisual(rs, "away"),
                                scoreboard(
                                        nullableInt(rs, "home_score"),
                                        nullableInt(rs, "away_score"),
                                        nullableInt(rs, "home_event_score"),
                                        nullableInt(rs, "away_event_score"),
                                        rs.getString("evidence_summary"),
                                        rs.getString("status"),
                                        rs.getString("result_status"),
                                        localDateTime(rs, "kickoff_time")
                                )
                        ),
                        matchId
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found"));
    }

    private String factorSummarySelect() {
        return "SELECT mcf.id, mcf.match_id, m.match_name, m.matchday, m.jc_code, m.status, m.result_status, m.kickoff_time, "
                + "ht.id AS home_team_id, ht.display_name AS home_team_name, ht.fifa_code AS home_fifa_code, "
                + "ht.country_iso2 AS home_country_iso2, ht.flag_asset_key AS home_flag_asset_key, ht.country_region AS home_country_region, "
                + "at.id AS away_team_id, at.display_name AS away_team_name, at.fifa_code AS away_fifa_code, "
                + "at.country_iso2 AS away_country_iso2, at.flag_asset_key AS away_flag_asset_key, at.country_region AS away_country_region, "
                + "(SELECT MAX(CASE WHEN mts.team_id=m.home_team_id THEN mts.goals_for END) FROM match_team_stats mts WHERE mts.match_id=m.id) AS home_score, "
                + "(SELECT MAX(CASE WHEN mts.team_id=m.away_team_id THEN mts.goals_for END) FROM match_team_stats mts WHERE mts.match_id=m.id) AS away_score, "
                + "(SELECT COUNT(*) FROM match_events e WHERE e.match_id=m.id AND e.team_id=m.home_team_id AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS home_event_score, "
                + "(SELECT COUNT(*) FROM match_events e WHERE e.match_id=m.id AND e.team_id=m.away_team_id AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS away_event_score, "
                + "(SELECT se.summary FROM source_evidence se WHERE se.match_id=m.id AND se.summary IS NOT NULL ORDER BY se.reliability_score DESC, se.id DESC LIMIT 1) AS evidence_summary, "
                + "mcf.factor_category, mcf.factor_type, "
                + "mcf.title, mcf.summary, mcf.impact_direction, mcf.entity_type, mcf.entity_key, mcf.evidence_level, "
                + "mcf.source_name, mcf.source_url, mcf.source_ref, mcf.observed_at, mcf.expires_at, "
                + "mcf.confidence_score, mcf.reliability_score, "
                + "(SELECT COUNT(*) FROM sentiment_risk_assessments sar WHERE sar.factor_id=mcf.id) AS risk_count, "
                + "(SELECT MAX(CASE sar.risk_level WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END) FROM sentiment_risk_assessments sar WHERE sar.factor_id=mcf.id) AS risk_rank "
                + "FROM match_context_factors mcf "
                + "LEFT JOIN matches m ON m.id=mcf.match_id "
                + "LEFT JOIN teams ht ON ht.id=m.home_team_id "
                + "LEFT JOIN teams at ON at.id=m.away_team_id";
    }

    private String factorDetailSelect() {
        return "SELECT mcf.id, mcf.match_id, m.match_name, m.matchday, m.jc_code, mcf.factor_category, mcf.factor_type, "
                + "mcf.title, mcf.summary, mcf.impact_direction, mcf.entity_type, mcf.entity_key, mcf.evidence_level, "
                + "mcf.source_name, mcf.source_url, mcf.source_ref, mcf.observed_at, mcf.expires_at, "
                + "mcf.confidence_score, mcf.reliability_score "
                + "FROM match_context_factors mcf LEFT JOIN matches m ON m.id=mcf.match_id";
    }

    private String riskSelect() {
        return "SELECT id, match_id, factor_id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, source_ref "
                + "FROM sentiment_risk_assessments sar";
    }

    private String matchVisualSelect() {
        return "m.match_name, m.matchday, m.jc_code, m.status, m.result_status, m.kickoff_time, m.competition, m.stage, m.venue, "
                + "ht.id AS home_team_id, ht.display_name AS home_team_name, ht.fifa_code AS home_fifa_code, "
                + "ht.country_iso2 AS home_country_iso2, ht.flag_asset_key AS home_flag_asset_key, ht.country_region AS home_country_region, "
                + "at.id AS away_team_id, at.display_name AS away_team_name, at.fifa_code AS away_fifa_code, "
                + "at.country_iso2 AS away_country_iso2, at.flag_asset_key AS away_flag_asset_key, at.country_region AS away_country_region, "
                + "(SELECT MAX(CASE WHEN mts.team_id=m.home_team_id THEN mts.goals_for END) FROM match_team_stats mts WHERE mts.match_id=m.id) AS home_score, "
                + "(SELECT MAX(CASE WHEN mts.team_id=m.away_team_id THEN mts.goals_for END) FROM match_team_stats mts WHERE mts.match_id=m.id) AS away_score, "
                + "(SELECT COUNT(*) FROM match_events e WHERE e.match_id=m.id AND e.team_id=m.home_team_id AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS home_event_score, "
                + "(SELECT COUNT(*) FROM match_events e WHERE e.match_id=m.id AND e.team_id=m.away_team_id AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS away_event_score, "
                + "(SELECT se.summary FROM source_evidence se WHERE se.match_id=m.id AND se.summary IS NOT NULL ORDER BY se.reliability_score DESC, se.id DESC LIMIT 1) AS evidence_summary ";
    }

    private String matchVisualJoin() {
        return " LEFT JOIN teams ht ON ht.id=m.home_team_id "
                + "LEFT JOIN teams at ON at.id=m.away_team_id ";
    }

    private String scheduleFactorSelect(String whereClause) {
        return "SELECT m.id AS match_id, " + matchVisualSelect()
                + "FROM matches m " + matchVisualJoin()
                + whereClause
                + " ORDER BY m.matchday DESC, m.id DESC";
    }

    private String oddsMarketFactorSelect(String whereClause) {
        return "SELECT oms.id AS market_id, oms.match_id, oms.bookmaker, oms.market_code, oms.market_name, oms.snapshot_type, "
                + "oms.handicap_line, oms.line_value, oms.captured_at, oms.source_ref, "
                + "(SELECT COUNT(*) FROM odds_selection_snapshots oss WHERE oss.market_snapshot_id=oms.id) AS selection_count, "
                + matchVisualSelect()
                + "FROM odds_market_snapshots oms "
                + "LEFT JOIN matches m ON m.id=oms.match_id "
                + matchVisualJoin()
                + whereClause
                + " ORDER BY m.matchday DESC, oms.id DESC";
    }

    private List<PublicSentimentFactorSummary> scheduleFactorSummaries() {
        return jdbcTemplate.query(
                scheduleFactorSelect(" WHERE m.matchday IS NOT NULL OR m.kickoff_time IS NOT NULL OR m.stage IS NOT NULL OR m.venue IS NOT NULL"),
                (rs, rowNum) -> scheduleFactorSummary(rs)
        );
    }

    private List<PublicSentimentFactorDetail> scheduleFactorDetails(long matchId) {
        return jdbcTemplate.query(
                scheduleFactorSelect(" WHERE m.id=? AND (m.matchday IS NOT NULL OR m.kickoff_time IS NOT NULL OR m.stage IS NOT NULL OR m.venue IS NOT NULL)"),
                (rs, rowNum) -> detailFromSummary(scheduleFactorSummary(rs)),
                matchId
        );
    }

    private List<PublicSentimentFactorSummary> oddsMarketFactorSummaries() {
        return jdbcTemplate.query(
                oddsMarketFactorSelect(" WHERE oms.match_id IS NOT NULL" + formalMarketSnapshotAbsentCondition()),
                (rs, rowNum) -> oddsMarketFactorSummary(rs)
        );
    }

    private List<PublicSentimentFactorDetail> oddsMarketFactorDetails(long matchId) {
        return jdbcTemplate.query(
                oddsMarketFactorSelect(" WHERE oms.match_id=?" + formalMarketSnapshotAbsentCondition()),
                (rs, rowNum) -> detailFromSummary(oddsMarketFactorSummary(rs)),
                matchId
        );
    }

    private String formalMarketSnapshotAbsentCondition() {
        return " AND NOT EXISTS (SELECT 1 FROM match_context_factors formal_mcf "
                + "WHERE formal_mcf.match_id=oms.match_id "
                + "AND formal_mcf.factor_type='MARKET_PRICE_SNAPSHOT' "
                + "AND formal_mcf.factor_category IN ('MARKET_SIGNAL','MARKET'))";
    }

    private PublicSentimentFactorSummary factorSummary(ResultSet rs) throws SQLException {
        LocalDateTime expiresAt = localDateTime(rs, "expires_at");
        return new PublicSentimentFactorSummary(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeText(rs.getString("jc_code")),
                teamVisual(rs, "home"),
                teamVisual(rs, "away"),
                scoreboard(
                        nullableInt(rs, "home_score"),
                        nullableInt(rs, "away_score"),
                        nullableInt(rs, "home_event_score"),
                        nullableInt(rs, "away_event_score"),
                        rs.getString("evidence_summary"),
                        rs.getString("status"),
                        rs.getString("result_status"),
                        localDateTime(rs, "kickoff_time")
                ),
                mapper.sanitizeToken(rs.getString("factor_category")),
                mapper.sanitizeToken(rs.getString("factor_type")),
                mapper.sanitizeText(rs.getString("title")),
                mapper.sanitizeText(rs.getString("summary")),
                mapper.sanitizeToken(rs.getString("impact_direction")),
                mapper.sanitizeToken(rs.getString("entity_type")),
                mapper.sanitizeText(rs.getString("entity_key")),
                mapper.sanitizeText(rs.getString("evidence_level")),
                mapper.sanitizeText(rs.getString("source_name")),
                mapper.sanitizeText(rs.getString("source_url")),
                mapper.sanitizeText(rs.getString("source_ref")),
                localDateTime(rs, "observed_at"),
                expiresAt,
                rs.getBigDecimal("confidence_score"),
                rs.getBigDecimal("reliability_score"),
                stale(expiresAt),
                rs.getLong("risk_count"),
                riskLevel(rs.getInt("risk_rank"))
        );
    }

    private PublicSentimentFactorSummary scheduleFactorSummary(ResultSet rs) throws SQLException {
        return new PublicSentimentFactorSummary(
                -400_000L - rs.getLong("match_id"),
                nullableLong(rs, "match_id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeText(rs.getString("jc_code")),
                teamVisual(rs, "home"),
                teamVisual(rs, "away"),
                scoreboard(
                        nullableInt(rs, "home_score"),
                        nullableInt(rs, "away_score"),
                        nullableInt(rs, "home_event_score"),
                        nullableInt(rs, "away_event_score"),
                        rs.getString("evidence_summary"),
                        rs.getString("status"),
                        rs.getString("result_status"),
                        localDateTime(rs, "kickoff_time")
                ),
                "SCHEDULE",
                "MATCHDAY",
                "赛程时间字段",
                mapper.sanitizeText(scheduleSummary(rs)),
                "NEUTRAL",
                "MATCH",
                mapper.sanitizeText(rs.getString("match_name")),
                "STRUCTURED_API",
                "正式赛程表",
                null,
                "matches",
                localDateTime(rs, "kickoff_time"),
                null,
                DERIVED_FACTOR_SCORE,
                DERIVED_FACTOR_SCORE,
                false,
                0,
                "UNKNOWN"
        );
    }

    private PublicSentimentFactorSummary oddsMarketFactorSummary(ResultSet rs) throws SQLException {
        String bookmaker = rs.getString("bookmaker");
        String marketName = rs.getString("market_name");
        String marketCode = rs.getString("market_code");
        long marketId = rs.getLong("market_id");
        return new PublicSentimentFactorSummary(
                -300_000_000L - marketId,
                nullableLong(rs, "match_id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeText(rs.getString("jc_code")),
                teamVisual(rs, "home"),
                teamVisual(rs, "away"),
                scoreboard(
                        nullableInt(rs, "home_score"),
                        nullableInt(rs, "away_score"),
                        nullableInt(rs, "home_event_score"),
                        nullableInt(rs, "away_event_score"),
                        rs.getString("evidence_summary"),
                        rs.getString("status"),
                        rs.getString("result_status"),
                        localDateTime(rs, "kickoff_time")
                ),
                "MARKET_SIGNAL",
                mapper.sanitizeToken(marketCode),
                mapper.sanitizeText("市场快照：" + firstText(marketName, marketCode, "公开市场")),
                mapper.sanitizeText(oddsMarketSummary(rs)),
                "UNKNOWN",
                "MATCH",
                mapper.sanitizeText(rs.getString("match_name")),
                "STRUCTURED_API",
                mapper.sanitizeText(firstText(bookmaker, "公开赔率表")),
                null,
                mapper.sanitizeText(firstText(rs.getString("source_ref"), "odds_market_snapshots:" + marketId)),
                localDateTime(rs, "captured_at"),
                null,
                DERIVED_FACTOR_SCORE,
                DERIVED_FACTOR_SCORE,
                false,
                0,
                "UNKNOWN"
        );
    }

    private PublicSentimentFactorDetail detailFromSummary(PublicSentimentFactorSummary summary) {
        return new PublicSentimentFactorDetail(
                summary.id(),
                summary.matchId(),
                summary.matchName(),
                summary.matchday(),
                summary.jcCode(),
                summary.factorCategory(),
                summary.factorType(),
                summary.title(),
                summary.summary(),
                summary.impactDirection(),
                summary.entityType(),
                summary.entityKey(),
                summary.evidenceLevel(),
                summary.sourceName(),
                summary.sourceUrl(),
                summary.sourceRef(),
                summary.observedAt(),
                summary.expiresAt(),
                summary.confidenceScore(),
                summary.reliabilityScore(),
                summary.stale()
        );
    }

    private PublicSentimentFactorDetail factorDetail(ResultSet rs) throws SQLException {
        LocalDateTime expiresAt = localDateTime(rs, "expires_at");
        return new PublicSentimentFactorDetail(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeText(rs.getString("jc_code")),
                mapper.sanitizeToken(rs.getString("factor_category")),
                mapper.sanitizeToken(rs.getString("factor_type")),
                mapper.sanitizeText(rs.getString("title")),
                mapper.sanitizeText(rs.getString("summary")),
                mapper.sanitizeToken(rs.getString("impact_direction")),
                mapper.sanitizeToken(rs.getString("entity_type")),
                mapper.sanitizeText(rs.getString("entity_key")),
                mapper.sanitizeText(rs.getString("evidence_level")),
                mapper.sanitizeText(rs.getString("source_name")),
                mapper.sanitizeText(rs.getString("source_url")),
                mapper.sanitizeText(rs.getString("source_ref")),
                localDateTime(rs, "observed_at"),
                expiresAt,
                rs.getBigDecimal("confidence_score"),
                rs.getBigDecimal("reliability_score"),
                stale(expiresAt)
        );
    }

    private PublicSentimentRisk risk(ResultSet rs) throws SQLException {
        return new PublicSentimentRisk(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                nullableLong(rs, "factor_id"),
                mapper.sanitizeToken(rs.getString("risk_type")),
                mapper.sanitizeToken(rs.getString("risk_level")),
                rs.getBigDecimal("risk_score"),
                mapper.sanitizeText(rs.getString("title")),
                mapper.sanitizeText(rs.getString("rationale")),
                mapper.sanitizeText(rs.getString("suggested_action")),
                mapper.sanitizeText(rs.getString("source_name")),
                mapper.sanitizeText(rs.getString("source_ref"))
        );
    }

    private String riskLevel(int rank) {
        return switch (rank) {
            case 4 -> "CRITICAL";
            case 3 -> "HIGH";
            case 2 -> "MEDIUM";
            case 1 -> "LOW";
            default -> "UNKNOWN";
        };
    }

    private boolean stale(LocalDateTime expiresAt) {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    private String scheduleSummary(ResultSet rs) throws SQLException {
        ArrayList<String> parts = new ArrayList<>();
        LocalDate matchday = localDate(rs, "matchday");
        LocalDateTime kickoffTime = localDateTime(rs, "kickoff_time");
        if (matchday != null) {
            parts.add("比赛日 " + matchday);
        }
        if (kickoffTime != null) {
            parts.add("开球时间 " + kickoffTime.toLocalDate() + " " + kickoffTime.toLocalTime());
        }
        addPart(parts, "赛事", rs.getString("competition"));
        addPart(parts, "阶段", rs.getString("stage"));
        addPart(parts, "场地", rs.getString("venue"));
        return parts.isEmpty() ? "赛程字段来自正式比赛表。" : String.join("，", parts) + "。";
    }

    private String oddsMarketSummary(ResultSet rs) throws SQLException {
        ArrayList<String> parts = new ArrayList<>();
        addPart(parts, "公司", rs.getString("bookmaker"));
        addPart(parts, "市场", firstText(rs.getString("market_name"), rs.getString("market_code")));
        addPart(parts, "快照", rs.getString("snapshot_type"));
        addPart(parts, "市场线", firstText(rs.getString("line_value"), decimalText(rs, "handicap_line")));
        Integer selectionCount = nullableInt(rs, "selection_count");
        if (selectionCount != null) {
            parts.add("选项 " + selectionCount + " 个");
        }
        LocalDateTime capturedAt = localDateTime(rs, "captured_at");
        if (capturedAt != null) {
            parts.add("采集时间 " + capturedAt.toLocalDate() + " " + capturedAt.toLocalTime());
        }
        return parts.isEmpty() ? "公开市场快照来自正式赔率表。" : String.join("，", parts) + "。";
    }

    private String decimalText(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private Comparator<PublicSentimentFactorSummary> sentimentSummaryComparator() {
        return Comparator
                .comparing(PublicSentimentFactorSummary::matchday, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PublicSentimentFactorSummary::id, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<PublicSentimentFactorDetail> sentimentDetailComparator() {
        return Comparator
                .comparing((PublicSentimentFactorDetail factor) -> factor.id() != null && factor.id() < 0)
                .thenComparing(PublicSentimentFactorDetail::factorCategory, Comparator.nullsLast(String::compareTo))
                .thenComparing(PublicSentimentFactorDetail::id, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private void addPart(List<String> parts, String label, String value) {
        if (hasText(value)) {
            parts.add(label + " " + value);
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasRows(String table, String condition) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + condition + " LIMIT 1", Long.class);
        return value != null && value > 0;
    }

    private void addCategory(List<String> categories, String category) {
        if (!categories.contains(category)) {
            categories.add(category);
        }
    }

    private LocalDate localDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private PublicTeamVisual teamVisual(ResultSet rs, String prefix) throws SQLException {
        return new PublicTeamVisual(
                nullableLong(rs, prefix + "_team_id"),
                mapper.sanitizeText(rs.getString(prefix + "_team_name")),
                mapper.sanitizeText(rs.getString(prefix + "_fifa_code")),
                mapper.sanitizeToken(rs.getString(prefix + "_country_iso2")),
                mapper.sanitizeText(rs.getString(prefix + "_flag_asset_key")),
                mapper.sanitizeText(rs.getString(prefix + "_country_region"))
        );
    }

    private PublicScoreboard scoreboard(
            Integer homeScore,
            Integer awayScore,
            Integer homeEventScore,
            Integer awayEventScore,
            String evidenceSummary,
            String status,
            String resultStatus,
            LocalDateTime kickoffTime
    ) {
        Integer resolvedHome = homeScore;
        Integer resolvedAway = awayScore;
        String scoreSource = null;
        if (resolvedHome != null && resolvedAway != null) {
            scoreSource = "TEAM_STATS";
        } else if (homeEventScore != null && awayEventScore != null && homeEventScore + awayEventScore > 0) {
            resolvedHome = homeEventScore;
            resolvedAway = awayEventScore;
            scoreSource = "MATCH_EVENTS";
        } else {
            int[] parsed = parseScore(evidenceSummary);
            if (parsed != null) {
                resolvedHome = parsed[0];
                resolvedAway = parsed[1];
                scoreSource = "EVIDENCE_TEXT";
            }
        }

        if (resolvedHome != null && resolvedAway != null) {
            return scoreboardFromNumbers(resolvedHome, resolvedAway, scoreSource);
        }

        String normalizedStatus = status == null ? "" : status.toUpperCase(Locale.ROOT);
        String normalizedResultStatus = resultStatus == null ? "" : resultStatus.toUpperCase(Locale.ROOT);
        boolean finished = normalizedStatus.contains("FINISHED")
                || normalizedResultStatus.contains("FINAL")
                || normalizedResultStatus.contains("FINISHED");
        if (finished) {
            return new PublicScoreboard(null, null, "比分待核对", "UNKNOWN", "完赛 · 比分待核对", "PENDING");
        }
        if (kickoffTime != null || normalizedStatus.contains("SCHEDULED") || normalizedStatus.contains("PRE_MATCH")) {
            return new PublicScoreboard(null, null, "待开球", "UNKNOWN", "赛前", "PENDING");
        }
        return new PublicScoreboard(null, null, "待同步", "UNKNOWN", "赛程待同步", "PENDING");
    }

    private PublicScoreboard scoreboardFromNumbers(int homeScore, int awayScore, String source) {
        String winnerSide;
        String resultText;
        if (homeScore > awayScore) {
            winnerSide = "HOME";
            resultText = "主队胜";
        } else if (awayScore > homeScore) {
            winnerSide = "AWAY";
            resultText = "客队胜";
        } else {
            winnerSide = "DRAW";
            resultText = "平局";
        }
        return new PublicScoreboard(homeScore, awayScore, homeScore + " - " + awayScore, winnerSide, resultText, source);
    }

    private int[] parseScore(String evidenceSummary) {
        if (evidenceSummary == null || evidenceSummary.isBlank()) {
            return null;
        }
        Matcher matcher = SCORE_PATTERN.matcher(evidenceSummary);
        if (!matcher.find()) {
            return null;
        }
        return new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }

    private record MatchRow(
            Long id,
            String matchName,
            LocalDate matchday,
            String jcCode,
            PublicTeamVisual homeTeam,
            PublicTeamVisual awayTeam,
            PublicScoreboard scoreboard
    ) {
    }
}
