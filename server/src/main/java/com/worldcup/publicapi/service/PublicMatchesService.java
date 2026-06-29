package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchConflict;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchEvent;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchEvidence;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchLineup;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchPlayerStats;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicScoreboard;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchTeamStats;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PublicMatchesService {
    private static final Pattern SCORE_PATTERN = Pattern.compile("(?:比分\\s*[=：:]?\\s*)?(\\d{1,2})\\s*[-:：比]\\s*(\\d{1,2})");

    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;

    public PublicMatchesService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PublicMatchSummary> matches() {
        return jdbcTemplate.query(summarySelect() + " ORDER BY m.matchday DESC, m.kickoff_time DESC, m.id DESC",
                (rs, rowNum) -> summary(rs));
    }

    @Transactional(readOnly = true)
    public PublicMatchDetail match(long matchId) {
        PublicMatchSummary summary = findSummary(matchId);
        return new PublicMatchDetail(
                summary,
                mapper.sanitizeText(externalFactors(matchId)),
                lineups(matchId),
                events(matchId),
                teamStats(matchId),
                playerStats(matchId),
                evidence(matchId),
                conflicts(matchId)
        );
    }

    @Transactional(readOnly = true)
    public List<PublicMatchLineup> lineups(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query("""
                SELECT l.id, l.match_id, l.team_id, COALESCE(t.display_name, 'Unknown team') AS team_name,
                       l.player_id, COALESCE(p.display_name, 'Unknown player') AS player_name, l.role, l.position, l.is_starter
                FROM match_lineups l
                JOIN matches m ON m.id=l.match_id
                LEFT JOIN teams t ON t.id=l.team_id
                LEFT JOIN players p ON p.id=l.player_id
                WHERE l.match_id=?
                ORDER BY CASE WHEN l.team_id=m.home_team_id THEN 0 ELSE 1 END, l.is_starter DESC, p.shirt_number, p.display_name, l.id
                """, (rs, rowNum) -> new PublicMatchLineup(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableLong(rs, "team_id"),
                mapper.sanitizeText(rs.getString("team_name")),
                nullableLong(rs, "player_id"),
                mapper.sanitizeText(rs.getString("player_name")),
                mapper.sanitizeToken(rs.getString("role")),
                mapper.sanitizeText(rs.getString("position")),
                rs.getBoolean("is_starter")
        ), matchId);
    }

    @Transactional(readOnly = true)
    public List<PublicMatchEvent> events(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query("""
                SELECT e.id, e.match_id, e.event_minute, e.event_type, e.team_id,
                       COALESCE(t.display_name, 'Unknown team') AS team_name,
                       e.player_id, COALESCE(p.display_name, 'Unknown player') AS player_name
                FROM match_events e
                LEFT JOIN teams t ON t.id=e.team_id
                LEFT JOIN players p ON p.id=e.player_id
                WHERE e.match_id=?
                ORDER BY CASE WHEN e.event_minute IS NULL THEN 9999 ELSE e.event_minute END, e.id
                """, (rs, rowNum) -> new PublicMatchEvent(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableInt(rs, "event_minute"),
                mapper.sanitizeToken(rs.getString("event_type")),
                nullableLong(rs, "team_id"),
                mapper.sanitizeText(rs.getString("team_name")),
                nullableLong(rs, "player_id"),
                mapper.sanitizeText(rs.getString("player_name"))
        ), matchId);
    }

    @Transactional(readOnly = true)
    public List<PublicMatchTeamStats> teamStats(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query("""
                SELECT s.id, s.match_id, s.team_id, COALESCE(t.display_name, 'Unknown team') AS team_name, s.stats_type,
                       s.goals_for, s.goals_against, s.first_goal_minute, s.scoring_minutes
                FROM match_team_stats s
                JOIN matches m ON m.id=s.match_id
                LEFT JOIN teams t ON t.id=s.team_id
                WHERE s.match_id=?
                ORDER BY CASE WHEN s.team_id=m.home_team_id THEN 0 ELSE 1 END, s.id
                """, (rs, rowNum) -> new PublicMatchTeamStats(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableLong(rs, "team_id"),
                mapper.sanitizeText(rs.getString("team_name")),
                mapper.sanitizeToken(rs.getString("stats_type")),
                nullableInt(rs, "goals_for"),
                nullableInt(rs, "goals_against"),
                nullableInt(rs, "first_goal_minute"),
                mapper.sanitizeText(rs.getString("scoring_minutes"))
        ), matchId);
    }

    @Transactional(readOnly = true)
    public List<PublicMatchPlayerStats> playerStats(long matchId) {
        assertMatchExists(matchId);
        return jdbcTemplate.query("""
                SELECT s.id, s.match_id, s.player_id, COALESCE(p.display_name, 'Unknown player') AS player_name, p.team_id,
                       COALESCE(t.display_name, 'Unknown team') AS team_name,
                       s.minutes_played, s.goals, s.assists, s.yellow_cards, s.red_cards
                FROM match_player_stats s
                JOIN matches m ON m.id=s.match_id
                LEFT JOIN players p ON p.id=s.player_id
                LEFT JOIN teams t ON t.id=p.team_id
                WHERE s.match_id=?
                ORDER BY CASE WHEN p.team_id=m.home_team_id THEN 0 ELSE 1 END, p.shirt_number, p.display_name, s.id
                """, (rs, rowNum) -> new PublicMatchPlayerStats(
                rs.getLong("id"),
                rs.getLong("match_id"),
                nullableLong(rs, "player_id"),
                mapper.sanitizeText(rs.getString("player_name")),
                nullableLong(rs, "team_id"),
                mapper.sanitizeText(rs.getString("team_name")),
                nullableInt(rs, "minutes_played"),
                nullableInt(rs, "goals"),
                nullableInt(rs, "assists"),
                nullableInt(rs, "yellow_cards"),
                nullableInt(rs, "red_cards")
        ), matchId);
    }

    private PublicMatchSummary findSummary(long matchId) {
        return jdbcTemplate.query(summarySelect() + " WHERE m.id=?", (rs, rowNum) -> summary(rs), matchId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found"));
    }

    private void assertMatchExists(long matchId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matches WHERE id=?", Long.class, matchId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found");
        }
    }

    private String externalFactors(long matchId) {
        return jdbcTemplate.queryForObject("SELECT external_factors FROM matches WHERE id=?", String.class, matchId);
    }

    private List<PublicMatchEvidence> evidence(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, source_type, source_name, source_ref, source_url, evidence_time, summary, reliability_score
                FROM source_evidence
                WHERE match_id=?
                ORDER BY evidence_time DESC, id DESC
                """, (rs, rowNum) -> {
            String sourceType = mapper.sanitizeToken(rs.getString("source_type"));
            String sourceName = mapper.sanitizeText(rs.getString("source_name"));
            String sourceRef = mapper.sanitizeText(rs.getString("source_ref"));
            LocalDateTime evidenceTime = localDateTime(rs, "evidence_time");
            String summary = mapper.sanitizeText(rs.getString("summary"));
            BigDecimal reliabilityScore = rs.getBigDecimal("reliability_score");
            return new PublicMatchEvidence(
                    rs.getLong("id"),
                    sourceType,
                    sourceName,
                    sourceRef,
                    mapper.sanitizeText(rs.getString("source_url")),
                    evidenceTime,
                    summary,
                    reliabilityScore,
                    evidenceQualityLevel(reliabilityScore),
                    evidenceFreshnessStatus(evidenceTime),
                    evidenceSupportsConclusion(sourceType, sourceRef, summary),
                    evidenceSuggestedAction(reliabilityScore, evidenceTime)
            );
        }, matchId);
    }

    private String evidenceQualityLevel(BigDecimal reliabilityScore) {
        if (reliabilityScore == null) {
            return "UNRATED";
        }
        BigDecimal normalized = normalizeReliability(reliabilityScore);
        if (normalized.compareTo(BigDecimal.valueOf(8)) >= 0) {
            return "HIGH";
        }
        if (normalized.compareTo(BigDecimal.valueOf(5)) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private BigDecimal normalizeReliability(BigDecimal reliabilityScore) {
        if (reliabilityScore.compareTo(BigDecimal.ONE) <= 0) {
            return reliabilityScore.multiply(BigDecimal.TEN);
        }
        return reliabilityScore;
    }

    private String evidenceFreshnessStatus(LocalDateTime evidenceTime) {
        if (evidenceTime == null) {
            return "UNKNOWN";
        }
        long ageHours = ChronoUnit.HOURS.between(evidenceTime, LocalDateTime.now());
        if (ageHours < 0) {
            return "FUTURE";
        }
        if (ageHours <= 24) {
            return "FRESH";
        }
        if (ageHours <= 72) {
            return "AGING";
        }
        return "STALE";
    }

    private String evidenceSupportsConclusion(String sourceType, String sourceRef, String summary) {
        String text = ((sourceType == null ? "" : sourceType) + " "
                + (sourceRef == null ? "" : sourceRef) + " "
                + (summary == null ? "" : summary)).toLowerCase(Locale.ROOT);
        if (text.contains("venue") || text.contains("stadium") || text.contains("attendance")
                || text.contains("crowd") || text.contains("broadcast") || text.contains("场地")
                || text.contains("球场") || text.contains("上座") || text.contains("观众") || text.contains("转播")) {
            return "场馆 / 上座 / 转播";
        }
        if (text.contains("qualification") || text.contains("knockout") || text.contains("advancement")
                || text.contains("elimination") || text.contains("tournament format") || text.contains("round of 32") || text.contains("bracket")
                || text.contains("出线") || text.contains("淘汰赛") || text.contains("晋级")
                || text.contains("出局") || text.contains("赛制") || text.contains("32强")) {
            return "出线 / 淘汰赛路径";
        }
        if (text.contains("form") || text.contains("recent results") || text.contains("tournament form")
                || text.contains("result trend") || text.contains("近期赛果") || text.contains("本届赛果")
                || text.contains("赛果走势") || text.contains("胜平负走势") || text.contains("连续不败")
                || text.contains("连续未胜") || text.contains("零封走势")) {
            return "球队状态 / 近期赛果";
        }
        if (text.contains("key player") || text.contains("goal contribution") || text.contains("assist contribution")
                || text.contains("top scorer") || text.contains("contribution distribution") || text.contains("关键球员")
                || text.contains("进球参与") || text.contains("进攻贡献") || text.contains("助攻贡献")
                || text.contains("贡献分布") || text.contains("主要得分")) {
            return "关键球员 / 进球参与";
        }
        if (text.contains("infraction_profile") || text.contains("foul_offside_penalty")
                || text.contains("foulscommitted") || text.contains("fouls committed")
                || text.contains("offsides") || text.contains("penaltykickshots")
                || text.contains("penalty kick shots") || text.contains("犯规/越位")
                || text.contains("犯规") || text.contains("越位") || text.contains("点球尝试")) {
            return "犯规 / 越位样本";
        }
        if (text.contains("goalkeeping") || text.contains("goalkeeper") || text.contains("save pressure")
                || text.contains("shot stopping") || text.contains("save_profile") || text.contains("扑救")
                || text.contains("门将") || text.contains("守门") || text.contains("防守承压")) {
            return "门将 / 扑救画像";
        }
        if (text.contains("lineup structure") || text.contains("lineup_structure")
                || text.contains("confirmed starting") || text.contains("starting xi")
                || text.contains("formation profile") || text.contains("confirmed_lineup")
                || text.contains("首发位置") || text.contains("已确认首发")
                || text.contains("确认阵容") || text.contains("位置结构画像")) {
            return "阵容 / 首发位置结构";
        }
        if (text.contains("squad") || text.contains("roster") || text.contains("age experience")
                || text.contains("team-side-roster") || text.contains("team_side_roster")
                || text.contains("club distribution") || text.contains("height profile")
                || text.contains("position profile") || text.contains("官方名单")
                || text.contains("阵容结构") || text.contains("名单结构") || text.contains("年龄经验")
                || text.contains("俱乐部分布") || text.contains("位置分布")) {
            return "阵容 / 名单结构画像";
        }
        if (text.contains("比分") || text.contains("score") || text.contains("result") || text.contains("赛果")) {
            return "比分 / 赛果";
        }
        if (text.contains("lineup") || text.contains("阵容") || text.contains("首发")) {
            return "阵容 / 首发";
        }
        if (text.contains("referee") || text.contains("裁判")) {
            return "裁判 / 判罚背景";
        }
        if (text.contains("weather") || text.contains("天气") || text.contains("rain") || text.contains("降雨")) {
            return "天气 / 外部因素";
        }
        if (text.contains("odds") || text.contains("盘口") || text.contains("赔率")) {
            return "市场 / 赔率信号";
        }
        if (text.contains("official") || text.contains("schedule") || text.contains("官方")) {
            return "赛程 / 官方确认";
        }
        return "背景证据";
    }

    private String evidenceSuggestedAction(BigDecimal reliabilityScore, LocalDateTime evidenceTime) {
        String freshness = evidenceFreshnessStatus(evidenceTime);
        if ("STALE".equals(freshness)) {
            return "证据已超过 72 小时，建议重新核查来源";
        }
        String quality = evidenceQualityLevel(reliabilityScore);
        return switch (quality) {
            case "HIGH" -> "可作为核心证据，但仍需与比分/阵容交叉核对";
            case "MEDIUM" -> "建议找第二来源交叉验证后再用于判断";
            case "LOW" -> "仅作弱参考，不能单独支撑结论";
            default -> "缺少可信度评分，需人工补评来源质量";
        };
    }

    private List<PublicMatchConflict> conflicts(long matchId) {
        return jdbcTemplate.query("""
                SELECT id, conflict_type, entity_key, field_name, resolution_status
                FROM data_conflicts
                WHERE match_id=?
                ORDER BY id DESC
                """, (rs, rowNum) -> new PublicMatchConflict(
                rs.getLong("id"),
                mapper.sanitizeToken(rs.getString("conflict_type")),
                mapper.sanitizeText(rs.getString("entity_key")),
                mapper.sanitizeToken(rs.getString("field_name")),
                mapper.sanitizeToken(rs.getString("resolution_status"))
        ), matchId);
    }

    private String summarySelect() {
        return """
                SELECT m.id, m.match_key, m.match_name, m.matchday, m.jc_code, m.competition, m.stage, m.venue, m.kickoff_time,
                       m.status, m.result_status, m.raw_payload AS match_raw_payload, m.home_team_id,
                       COALESCE(ht.display_name, '主队待定') AS home_team_name,
                       ht.fifa_code AS home_fifa_code, ht.country_iso2 AS home_country_iso2, ht.flag_asset_key AS home_flag_asset_key, ht.country_region AS home_country_region,
                       m.away_team_id,
                       COALESCE(at.display_name, '客队待定') AS away_team_name,
                       at.fifa_code AS away_fifa_code, at.country_iso2 AS away_country_iso2, at.flag_asset_key AS away_flag_asset_key, at.country_region AS away_country_region,
                       (SELECT s.goals_for FROM match_team_stats s
                        WHERE s.match_id=m.id AND s.team_id=m.home_team_id
                        ORDER BY s.id DESC LIMIT 1) AS home_score,
                       (SELECT s.goals_for FROM match_team_stats s
                        WHERE s.match_id=m.id AND s.team_id=m.away_team_id
                        ORDER BY s.id DESC LIMIT 1) AS away_score,
                       (SELECT COUNT(*) FROM match_events e
                        WHERE e.match_id=m.id AND e.team_id=m.home_team_id
                          AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS home_event_score,
                       (SELECT COUNT(*) FROM match_events e
                        WHERE e.match_id=m.id AND e.team_id=m.away_team_id
                          AND (UPPER(e.event_type) = 'GOAL' OR UPPER(e.event_type) LIKE 'GOAL_%' OR UPPER(e.event_type) IN ('PENALTY_GOAL', 'PENALTY_SCORED'))) AS away_event_score,
                       (SELECT ev.summary FROM source_evidence ev
                        WHERE ev.match_id=m.id AND ev.summary IS NOT NULL
                        ORDER BY ev.reliability_score DESC, ev.id DESC LIMIT 1) AS score_evidence_summary,
                       (SELECT COUNT(*) FROM match_events e WHERE e.match_id=m.id) AS event_count,
                       (SELECT COUNT(*) FROM match_lineups l WHERE l.match_id=m.id) AS lineup_count,
                       (SELECT COUNT(*) FROM source_evidence ev WHERE ev.match_id=m.id) AS evidence_count,
                       (SELECT COUNT(*) FROM data_conflicts c WHERE c.match_id=m.id) AS conflict_count
                FROM matches m
                LEFT JOIN teams ht ON ht.id=m.home_team_id
                LEFT JOIN teams at ON at.id=m.away_team_id
                """;
    }

    private PublicMatchSummary summary(ResultSet rs) throws SQLException {
        return new PublicMatchSummary(
                rs.getLong("id"),
                mapper.sanitizeText(rs.getString("match_key")),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeText(rs.getString("jc_code")),
                mapper.sanitizeText(rs.getString("competition")),
                mapper.sanitizeText(rs.getString("stage")),
                mapper.sanitizeText(rs.getString("venue")),
                localDateTime(rs, "kickoff_time"),
                mapper.sanitizeToken(rs.getString("status")),
                mapper.sanitizeToken(rs.getString("result_status")),
                nullableLong(rs, "home_team_id"),
                teamName(rs, "home_team_name", "home_team_name", "主队待定"),
                nullableLong(rs, "away_team_id"),
                teamName(rs, "away_team_name", "away_team_name", "客队待定"),
                teamVisual(
                        nullableLong(rs, "home_team_id"),
                        teamName(rs, "home_team_name", "home_team_name", "主队待定"),
                        rs.getString("home_fifa_code"),
                        rs.getString("home_country_iso2"),
                        rs.getString("home_flag_asset_key"),
                        rs.getString("home_country_region")
                ),
                teamVisual(
                        nullableLong(rs, "away_team_id"),
                        teamName(rs, "away_team_name", "away_team_name", "客队待定"),
                        rs.getString("away_fifa_code"),
                        rs.getString("away_country_iso2"),
                        rs.getString("away_flag_asset_key"),
                        rs.getString("away_country_region")
                ),
                scoreboard(
                        nullableInt(rs, "home_score"),
                        nullableInt(rs, "away_score"),
                        nullableInt(rs, "home_event_score"),
                        nullableInt(rs, "away_event_score"),
                        mapper.sanitizeText(rs.getString("score_evidence_summary")),
                        rs.getString("status"),
                        rs.getString("result_status"),
                        localDateTime(rs, "kickoff_time")
                ),
                rs.getLong("event_count"),
                rs.getLong("lineup_count"),
                rs.getLong("evidence_count"),
                rs.getLong("conflict_count")
        );
    }

    private PublicTeamVisual teamVisual(Long teamId, String teamName, String fifaCode, String countryIso2, String flagAssetKey, String countryRegion) {
        String sanitizedFifaCode = mapper.sanitizeToken(fifaCode);
        return new PublicTeamVisual(
                teamId,
                mapper.sanitizeText(teamName),
                sanitizedFifaCode,
                mapper.sanitizeToken(countryIso2),
                mapper.sanitizeText(flagAssetKey),
                mapper.sanitizeText(countryRegion)
        );
    }

    private String teamName(ResultSet rs, String joinedColumn, String payloadFieldName, String fallback) throws SQLException {
        return mapper.publicTeamName(rs.getString(joinedColumn), rs.getString("match_raw_payload"), payloadFieldName, fallback);
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
        String scoreSource = null;
        Integer resolvedHome = homeScore;
        Integer resolvedAway = awayScore;
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
            String winnerSide;
            String resultText;
            if (resolvedHome > resolvedAway) {
                winnerSide = "HOME";
                resultText = "主队胜";
            } else if (resolvedAway > resolvedHome) {
                winnerSide = "AWAY";
                resultText = "客队胜";
            } else {
                winnerSide = "DRAW";
                resultText = "平局";
            }
            return new PublicScoreboard(
                    resolvedHome,
                    resolvedAway,
                    resolvedHome + " - " + resolvedAway,
                    winnerSide,
                    resultText,
                    scoreSource
            );
        }

        String normalizedStatus = status == null ? "" : status.toUpperCase(java.util.Locale.ROOT);
        String normalizedResultStatus = resultStatus == null ? "" : resultStatus.toUpperCase(java.util.Locale.ROOT);
        boolean finished = normalizedStatus.contains("FINISHED")
                || normalizedResultStatus.contains("FINAL")
                || normalizedResultStatus.contains("FINISHED");
        if (finished) {
            return new PublicScoreboard(null, null, "比分待核对", "UNKNOWN", "完赛 · 比分待核对", null);
        }
        if (kickoffTime != null) {
            return new PublicScoreboard(null, null, "待开球", "UNKNOWN", "未开赛", null);
        }
        return new PublicScoreboard(null, null, "待同步", "UNKNOWN", "赛程待同步", null);
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
}
