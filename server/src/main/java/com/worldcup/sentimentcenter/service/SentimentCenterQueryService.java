package com.worldcup.sentimentcenter.service;

import com.worldcup.sentimentcenter.api.dto.SentimentCenterDtos.SentimentFactorDetailResponse;
import com.worldcup.sentimentcenter.api.dto.SentimentCenterDtos.SentimentFactorSummaryResponse;
import com.worldcup.sentimentcenter.api.dto.SentimentCenterDtos.SentimentMatchDetailResponse;
import com.worldcup.sentimentcenter.api.dto.SentimentCenterDtos.SentimentRiskResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SentimentCenterQueryService {
    private final JdbcTemplate jdbcTemplate;

    public SentimentCenterQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<SentimentFactorSummaryResponse> overview() {
        return jdbcTemplate.query(
                factorSummarySelect() + " ORDER BY m.matchday DESC, mcf.id DESC",
                summaryMapper()
        );
    }

    @Transactional(readOnly = true)
    public SentimentMatchDetailResponse matchSentiment(long matchId) {
        MatchRow match = findMatch(matchId);
        List<SentimentFactorDetailResponse> factors = jdbcTemplate.query(
                factorDetailSelect() + " WHERE mcf.match_id=? ORDER BY mcf.factor_category, mcf.id",
                detailMapper(),
                matchId
        );
        List<SentimentRiskResponse> risks = jdbcTemplate.query(
                riskSelect() + " WHERE sar.match_id=? ORDER BY sar.id",
                riskMapper(),
                matchId
        );
        return new SentimentMatchDetailResponse(match.id(), match.matchName(), match.matchday(), match.jcCode(), factors, risks);
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return jdbcTemplate.query(
                "SELECT DISTINCT factor_category FROM match_context_factors ORDER BY factor_category",
                (rs, rowNum) -> rs.getString("factor_category")
        );
    }

    @Transactional(readOnly = true)
    public List<String> riskTypes() {
        return jdbcTemplate.query(
                "SELECT DISTINCT risk_type FROM sentiment_risk_assessments ORDER BY risk_type",
                (rs, rowNum) -> rs.getString("risk_type")
        );
    }

    private MatchRow findMatch(long matchId) {
        return jdbcTemplate.query(
                        "SELECT id, match_name, matchday, jc_code FROM matches WHERE id=?",
                        (rs, rowNum) -> new MatchRow(
                                rs.getLong("id"),
                                rs.getString("match_name"),
                                localDate(rs, "matchday"),
                                rs.getString("jc_code")
                        ),
                        matchId
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "比赛不存在"));
    }

    private String factorSummarySelect() {
        return "SELECT mcf.id, mcf.match_id, m.match_name, m.matchday, m.jc_code, mcf.factor_category, mcf.factor_type, " +
                "mcf.title, mcf.summary, mcf.impact_direction, mcf.entity_type, mcf.entity_key, mcf.evidence_level, " +
                "mcf.source_name, mcf.source_url, mcf.source_ref, mcf.observed_at, mcf.expires_at, " +
                "mcf.confidence_score, mcf.reliability_score, " +
                "(SELECT COUNT(*) FROM sentiment_risk_assessments sar WHERE sar.factor_id=mcf.id) AS risk_count, " +
                "(SELECT MAX(CASE sar.risk_level WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END) FROM sentiment_risk_assessments sar WHERE sar.factor_id=mcf.id) AS risk_rank " +
                "FROM match_context_factors mcf LEFT JOIN matches m ON m.id=mcf.match_id";
    }

    private String factorDetailSelect() {
        return "SELECT mcf.id, mcf.match_id, m.match_name, m.matchday, m.jc_code, mcf.factor_category, mcf.factor_type, " +
                "mcf.title, mcf.summary, mcf.impact_direction, mcf.entity_type, mcf.entity_key, mcf.evidence_level, " +
                "mcf.source_name, mcf.source_url, mcf.source_ref, mcf.observed_at, mcf.expires_at, " +
                "mcf.confidence_score, mcf.reliability_score, mcf.raw_payload " +
                "FROM match_context_factors mcf LEFT JOIN matches m ON m.id=mcf.match_id";
    }

    private String riskSelect() {
        return "SELECT id, match_id, factor_id, risk_type, risk_level, risk_score, title, rationale, suggested_action, source_name, source_ref, raw_payload " +
                "FROM sentiment_risk_assessments sar";
    }

    private RowMapper<SentimentFactorSummaryResponse> summaryMapper() {
        return (rs, rowNum) -> new SentimentFactorSummaryResponse(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                rs.getString("match_name"),
                localDate(rs, "matchday"),
                rs.getString("jc_code"),
                rs.getString("factor_category"),
                rs.getString("factor_type"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("impact_direction"),
                rs.getString("entity_type"),
                rs.getString("entity_key"),
                rs.getString("evidence_level"),
                rs.getString("source_name"),
                rs.getString("source_url"),
                rs.getString("source_ref"),
                localDateTime(rs, "observed_at"),
                localDateTime(rs, "expires_at"),
                rs.getBigDecimal("confidence_score"),
                rs.getBigDecimal("reliability_score"),
                stale(localDateTime(rs, "expires_at")),
                rs.getLong("risk_count"),
                riskLevel(rs.getInt("risk_rank"))
        );
    }

    private RowMapper<SentimentFactorDetailResponse> detailMapper() {
        return (rs, rowNum) -> new SentimentFactorDetailResponse(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                rs.getString("match_name"),
                localDate(rs, "matchday"),
                rs.getString("jc_code"),
                rs.getString("factor_category"),
                rs.getString("factor_type"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("impact_direction"),
                rs.getString("entity_type"),
                rs.getString("entity_key"),
                rs.getString("evidence_level"),
                rs.getString("source_name"),
                rs.getString("source_url"),
                rs.getString("source_ref"),
                localDateTime(rs, "observed_at"),
                localDateTime(rs, "expires_at"),
                rs.getBigDecimal("confidence_score"),
                rs.getBigDecimal("reliability_score"),
                stale(localDateTime(rs, "expires_at")),
                rs.getString("raw_payload")
        );
    }

    private RowMapper<SentimentRiskResponse> riskMapper() {
        return (rs, rowNum) -> new SentimentRiskResponse(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                nullableLong(rs, "factor_id"),
                rs.getString("risk_type"),
                rs.getString("risk_level"),
                rs.getBigDecimal("risk_score"),
                rs.getString("title"),
                rs.getString("rationale"),
                rs.getString("suggested_action"),
                rs.getString("source_name"),
                rs.getString("source_ref"),
                rs.getString("raw_payload")
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
