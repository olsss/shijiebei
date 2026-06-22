package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicAdminTodoCounters;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicIntegrityCounters;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsFreshness;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOverviewMatch;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOverviewResponse;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicRiskCounters;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PublicOverviewService {
    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;

    public PublicOverviewService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PublicOverviewResponse overview() {
        return new PublicOverviewResponse(
                LocalDateTime.now(),
                upcomingMatches(),
                riskCounters(),
                integrityCounters(),
                oddsFreshness(),
                decisionSummary(),
                adminTodoCounters()
        );
    }

    private List<PublicOverviewMatch> upcomingMatches() {
        return jdbcTemplate.query("""
                SELECT m.id, m.match_name, m.matchday, m.jc_code, m.competition, m.stage,
                       m.kickoff_time, m.status,
                       (SELECT COUNT(*) FROM sentiment_risk_assessments sra
                        WHERE sra.match_id=m.id AND UPPER(sra.risk_level) IN ('HIGH', 'CRITICAL')) AS risk_count,
                       CASE
                           WHEN EXISTS (SELECT 1 FROM source_evidence se WHERE se.match_id=m.id)
                            AND EXISTS (SELECT 1 FROM odds_market_snapshots oms WHERE oms.match_id=m.id)
                            AND EXISTS (SELECT 1 FROM match_lineups ml WHERE ml.match_id=m.id)
                           THEN 100 ELSE 0
                       END AS integrity_score
                FROM matches m
                ORDER BY CASE WHEN m.kickoff_time IS NULL THEN 1 ELSE 0 END, m.kickoff_time ASC, m.id ASC
                LIMIT 5
                """, (rs, rowNum) -> new PublicOverviewMatch(
                rs.getLong("id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeText(rs.getString("jc_code")),
                mapper.sanitizeText(rs.getString("competition")),
                mapper.sanitizeText(rs.getString("stage")),
                localDateTime(rs, "kickoff_time"),
                mapper.sanitizeToken(rs.getString("status")),
                rs.getInt("integrity_score"),
                rs.getLong("risk_count")
        ));
    }

    private PublicRiskCounters riskCounters() {
        LocalDateTime now = LocalDateTime.now();
        return new PublicRiskCounters(
                count("SELECT COUNT(*) FROM sentiment_risk_assessments WHERE UPPER(risk_level) IN ('HIGH', 'CRITICAL')"),
                count("SELECT COUNT(*) FROM sentiment_risk_assessments WHERE UPPER(risk_level) = 'MEDIUM'"),
                count("SELECT COUNT(*) FROM match_context_factors WHERE expires_at IS NOT NULL AND expires_at < ?", now),
                count("SELECT COUNT(*) FROM data_conflicts WHERE resolution_status IS NULL OR resolution_status <> 'RESOLVED'")
        );
    }

    private PublicIntegrityCounters integrityCounters() {
        long total = count("SELECT COUNT(*) FROM matches");
        long complete = count("""
                SELECT COUNT(*) FROM matches m
                WHERE EXISTS (SELECT 1 FROM source_evidence se WHERE se.match_id=m.id)
                  AND EXISTS (SELECT 1 FROM odds_market_snapshots oms WHERE oms.match_id=m.id)
                  AND EXISTS (SELECT 1 FROM match_lineups ml WHERE ml.match_id=m.id)
                """);
        long blocked = count("""
                SELECT COUNT(*) FROM matches m
                WHERE NOT EXISTS (SELECT 1 FROM source_evidence se WHERE se.match_id=m.id)
                """);
        long partial = Math.max(0, total - complete - blocked);
        return new PublicIntegrityCounters(complete, partial, blocked);
    }

    private PublicOddsFreshness oddsFreshness() {
        LocalDateTime staleCutoff = LocalDateTime.now().minusHours(3);
        return new PublicOddsFreshness(
                count("SELECT COUNT(*) FROM odds_market_snapshots"),
                count("SELECT COUNT(*) FROM odds_market_snapshots WHERE UPPER(snapshot_type) = 'LIVE'"),
                count("SELECT COUNT(*) FROM odds_market_snapshots WHERE UPPER(snapshot_type) = 'LIVE' AND (captured_at IS NULL OR captured_at < ?)", staleCutoff)
        );
    }

    private PublicDecisionSummary decisionSummary() {
        LocalDateTime latestReport = latestTimestamp("SELECT MAX(updated_at) FROM analysis_reports");
        LocalDateTime latestReview = latestTimestamp("SELECT MAX(updated_at) FROM post_match_reviews");
        return new PublicDecisionSummary(
                count("SELECT COUNT(*) FROM analysis_reports"),
                count("SELECT COUNT(*) FROM post_match_reviews"),
                max(latestReport, latestReview)
        );
    }

    private PublicAdminTodoCounters adminTodoCounters() {
        return new PublicAdminTodoCounters(
                count("SELECT COUNT(*) FROM import_items WHERE UPPER(status) IN ('PENDING', 'PENDING_REVIEW')"),
                count("SELECT COUNT(*) FROM collection_items WHERE UPPER(status) IN ('PENDING', 'PENDING_REVIEW')")
        );
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private LocalDateTime latestTimestamp(String sql) {
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return localDateTime(rs, 1);
        });
    }

    private LocalDate localDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private LocalDateTime localDateTime(ResultSet rs, int column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }
}
