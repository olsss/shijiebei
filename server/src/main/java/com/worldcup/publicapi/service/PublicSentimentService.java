package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentFactorDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentFactorSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentMatchDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentRisk;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PublicSentimentService {
    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;

    public PublicSentimentService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PublicSentimentFactorSummary> overview() {
        return jdbcTemplate.query(
                factorSummarySelect() + " ORDER BY m.matchday DESC, mcf.id DESC",
                (rs, rowNum) -> factorSummary(rs)
        );
    }

    @Transactional(readOnly = true)
    public PublicSentimentMatchDetail matchSentiment(long matchId) {
        MatchRow match = findMatch(matchId);
        List<PublicSentimentFactorDetail> factors = jdbcTemplate.query(
                factorDetailSelect() + " WHERE mcf.match_id=? ORDER BY mcf.factor_category, mcf.id",
                (rs, rowNum) -> factorDetail(rs),
                matchId
        );
        List<PublicSentimentRisk> risks = jdbcTemplate.query(
                riskSelect() + " WHERE sar.match_id=? ORDER BY sar.id",
                (rs, rowNum) -> risk(rs),
                matchId
        );
        return new PublicSentimentMatchDetail(match.id(), match.matchName(), match.matchday(), match.jcCode(), factors, risks);
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return jdbcTemplate.query(
                "SELECT DISTINCT factor_category FROM match_context_factors ORDER BY factor_category",
                (rs, rowNum) -> mapper.sanitizeToken(rs.getString("factor_category"))
        );
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
                        "SELECT id, match_name, matchday, jc_code FROM matches WHERE id=?",
                        (rs, rowNum) -> new MatchRow(
                                rs.getLong("id"),
                                mapper.sanitizeText(rs.getString("match_name")),
                                localDate(rs, "matchday"),
                                mapper.sanitizeText(rs.getString("jc_code"))
                        ),
                        matchId
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found"));
    }

    private String factorSummarySelect() {
        return "SELECT mcf.id, mcf.match_id, m.match_name, m.matchday, m.jc_code, mcf.factor_category, mcf.factor_type, "
                + "mcf.title, mcf.summary, mcf.impact_direction, mcf.entity_type, mcf.entity_key, mcf.evidence_level, "
                + "mcf.source_name, mcf.source_url, mcf.source_ref, mcf.observed_at, mcf.expires_at, "
                + "mcf.confidence_score, mcf.reliability_score, "
                + "(SELECT COUNT(*) FROM sentiment_risk_assessments sar WHERE sar.factor_id=mcf.id) AS risk_count, "
                + "(SELECT MAX(CASE sar.risk_level WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END) FROM sentiment_risk_assessments sar WHERE sar.factor_id=mcf.id) AS risk_rank "
                + "FROM match_context_factors mcf LEFT JOIN matches m ON m.id=mcf.match_id";
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

    private PublicSentimentFactorSummary factorSummary(ResultSet rs) throws SQLException {
        LocalDateTime expiresAt = localDateTime(rs, "expires_at");
        return new PublicSentimentFactorSummary(
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
                stale(expiresAt),
                rs.getLong("risk_count"),
                riskLevel(rs.getInt("risk_rank"))
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

    private LocalDate localDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private record MatchRow(Long id, String matchName, LocalDate matchday, String jcCode) {
    }
}
