package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionReport;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionLesson;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionReview;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        List<ReviewRow> rows = jdbcTemplate.query("""
                SELECT pmr.id, pmr.match_id, m.match_name, m.matchday, pmr.analysis_report_id,
                       pmr.review_key, pmr.review_title, pmr.math_review, pmr.football_review,
                       pmr.handicap_review, pmr.tournament_temperament_review, pmr.odds_value_review,
                       pmr.overall_summary
                FROM post_match_reviews pmr
                LEFT JOIN matches m ON m.id=pmr.match_id
                ORDER BY pmr.created_at DESC, pmr.id DESC
                LIMIT ?
                """, (rs, rowNum) -> new ReviewRow(
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
                mapper.sanitizeText(rs.getString("overall_summary"))
        ), DEFAULT_LIMIT);
        Map<Long, List<PublicDecisionLesson>> lessonsByReviewId = lessonsByReviewId(rows.stream().map(ReviewRow::id).toList());
        return rows.stream()
                .map(row -> new PublicDecisionReview(
                        row.id(),
                        row.matchId(),
                        row.matchName(),
                        row.matchday(),
                        row.analysisReportId(),
                        row.reviewKey(),
                        row.title(),
                        row.mathSummary(),
                        row.footballSummary(),
                        row.handicapSummary(),
                        row.tournamentTemperamentSummary(),
                        row.oddsValueSummary(),
                        row.overallSummary(),
                        lessonsByReviewId.getOrDefault(row.id(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<PublicDecisionLesson>> lessonsByReviewId(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PublicDecisionLesson>> grouped = new LinkedHashMap<>();
        String placeholders = String.join(",", java.util.Collections.nCopies(reviewIds.size(), "?"));
        jdbcTemplate.query("""
                SELECT review_id, id, lesson_type, lesson_text, severity
                FROM review_lessons
                WHERE review_id IN (%s)
                ORDER BY review_id, id
                """.formatted(placeholders), rs -> {
            long reviewId = rs.getLong("review_id");
            grouped.computeIfAbsent(reviewId, ignored -> new ArrayList<>()).add(new PublicDecisionLesson(
                    rs.getLong("id"),
                    mapper.sanitizeToken(rs.getString("lesson_type")),
                    mapper.sanitizeText(rs.getString("lesson_text")),
                    mapper.sanitizeToken(rs.getString("severity"))
            ));
        }, reviewIds.toArray());
        return grouped;
    }

    private LocalDate localDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private record ReviewRow(
            Long id,
            Long matchId,
            String matchName,
            LocalDate matchday,
            Long analysisReportId,
            String reviewKey,
            String title,
            String mathSummary,
            String footballSummary,
            String handicapSummary,
            String tournamentTemperamentSummary,
            String oddsValueSummary,
            String overallSummary
    ) {
    }
}
