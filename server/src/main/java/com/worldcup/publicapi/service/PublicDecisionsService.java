package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionReport;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionReview;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Service
public class PublicDecisionsService {
    private static final int DEFAULT_LIMIT = 50;

    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;

    public PublicDecisionsService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PublicDecisionReport> reports() {
        return jdbcTemplate.query("""
                SELECT ar.id, ar.match_id, m.match_name, m.matchday, m.jc_code,
                       ar.conclusion_type, ar.confidence, ar.risk_summary
                FROM analysis_reports ar
                LEFT JOIN matches m ON m.id=ar.match_id
                ORDER BY ar.created_at DESC, ar.id DESC
                LIMIT ?
                """, (rs, rowNum) -> new PublicDecisionReport(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                mapper.sanitizeText(rs.getString("jc_code")),
                mapper.sanitizeToken(rs.getString("conclusion_type")),
                mapper.sanitizeToken(rs.getString("confidence")),
                mapper.sanitizeText(rs.getString("risk_summary")),
                null,
                null
        ), DEFAULT_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<PublicDecisionReview> reviews() {
        return jdbcTemplate.query("""
                SELECT pmr.id, pmr.match_id, m.match_name, m.matchday, pmr.analysis_report_id,
                       pmr.review_key, pmr.review_title, pmr.math_review, pmr.football_review,
                       pmr.handicap_review, pmr.tournament_temperament_review, pmr.odds_value_review,
                       pmr.overall_summary
                FROM post_match_reviews pmr
                LEFT JOIN matches m ON m.id=pmr.match_id
                ORDER BY pmr.created_at DESC, pmr.id DESC
                LIMIT ?
                """, (rs, rowNum) -> new PublicDecisionReview(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                mapper.sanitizeText(rs.getString("match_name")),
                localDate(rs, "matchday"),
                nullableLong(rs, "analysis_report_id"),
                mapper.sanitizeToken(rs.getString("review_key")),
                mapper.sanitizeText(rs.getString("review_title")),
                mapper.sanitizeText(rs.getString("math_review")),
                mapper.sanitizeText(rs.getString("football_review")),
                mapper.sanitizeText(rs.getString("handicap_review")),
                mapper.sanitizeText(rs.getString("tournament_temperament_review")),
                mapper.sanitizeText(rs.getString("odds_value_review")),
                mapper.sanitizeText(rs.getString("overall_summary")),
                List.of()
        ), DEFAULT_LIMIT);
    }

    private LocalDate localDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
