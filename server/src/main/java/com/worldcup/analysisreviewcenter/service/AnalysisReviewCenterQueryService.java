package com.worldcup.analysisreviewcenter.service;

import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.AnalysisReportDetailResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.AnalysisReportSummaryResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.BetPlanDetailResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.BetPlanItemResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.BetPlanSummaryResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.BetRecordResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.OverviewResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.PostMatchReviewResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.ReviewLessonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnalysisReviewCenterQueryService {
    private final JdbcTemplate jdbcTemplate;

    public AnalysisReviewCenterQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public OverviewResponse overview() {
        return jdbcTemplate.queryForObject(
                "SELECT " +
                        "(SELECT COUNT(*) FROM analysis_reports) AS report_count, " +
                        "(SELECT COUNT(*) FROM bet_plans) AS bet_plan_count, " +
                        "(SELECT COUNT(*) FROM bets) AS bet_count, " +
                        "(SELECT COUNT(*) FROM post_match_reviews) AS review_count, " +
                        "COALESCE((SELECT SUM(stake) FROM bets), 0) AS total_stake, " +
                        "COALESCE((SELECT SUM(return_amount) FROM bets), 0) AS total_return, " +
                        "COALESCE((SELECT SUM(profit_loss) FROM bets), 0) AS net_profit, " +
                        "COALESCE((SELECT AVG(clv) FROM bets WHERE clv IS NOT NULL), 0) AS average_clv",
                (rs, rowNum) -> {
                    BigDecimal totalStake = valueOrZero(rs.getBigDecimal("total_stake"));
                    BigDecimal netProfit = valueOrZero(rs.getBigDecimal("net_profit"));
                    return new OverviewResponse(
                            rs.getLong("report_count"),
                            rs.getLong("bet_plan_count"),
                            rs.getLong("bet_count"),
                            rs.getLong("review_count"),
                            totalStake,
                            valueOrZero(rs.getBigDecimal("total_return")),
                            netProfit,
                            roi(netProfit, totalStake),
                            valueOrZero(rs.getBigDecimal("average_clv"))
                    );
                }
        );
    }

    @Transactional(readOnly = true)
    public List<AnalysisReportSummaryResponse> reports() {
        return jdbcTemplate.query(reportSelect() + " ORDER BY ar.created_at DESC, ar.id DESC", reportMapper());
    }

    @Transactional(readOnly = true)
    public AnalysisReportDetailResponse report(long reportId) {
        AnalysisReportSummaryResponse report = jdbcTemplate.query(
                        reportSelect() + " WHERE ar.id=?",
                        reportMapper(),
                        reportId
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "分析报告不存在"));
        return new AnalysisReportDetailResponse(
                report,
                betPlans(" WHERE bp.analysis_report_id=? ORDER BY bp.id DESC", reportId),
                reviews(" WHERE pmr.analysis_report_id=? ORDER BY pmr.id DESC", reportId)
        );
    }

    @Transactional(readOnly = true)
    public List<BetPlanSummaryResponse> betPlans() {
        return betPlans(" ORDER BY bp.created_at DESC, bp.id DESC");
    }

    @Transactional(readOnly = true)
    public BetPlanDetailResponse betPlan(long planId) {
        BetPlanSummaryResponse plan = betPlans(" WHERE bp.id=?", planId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "下注方案不存在"));
        return new BetPlanDetailResponse(plan, betPlanItems(planId));
    }

    @Transactional(readOnly = true)
    public List<BetRecordResponse> bets() {
        return jdbcTemplate.query(
                "SELECT b.id, b.import_item_id, b.match_id, COALESCE(b.match_name, m.match_name) AS match_name, b.matchday, " +
                        "b.bet_id, b.ticket_no, b.bet_date, b.market_type, b.selection_text, b.stake, b.odds, b.closing_odds, b.clv, " +
                        "b.return_amount, b.hit_status, b.profit_loss, b.settled_at, b.review_status, b.raw_payload " +
                        "FROM bets b LEFT JOIN matches m ON m.id=b.match_id ORDER BY b.bet_date DESC, b.id DESC",
                betMapper()
        );
    }

    @Transactional(readOnly = true)
    public List<PostMatchReviewResponse> reviews() {
        return reviews(" ORDER BY pmr.created_at DESC, pmr.id DESC");
    }

    private List<BetPlanSummaryResponse> betPlans(String suffix, Object... args) {
        return jdbcTemplate.query(betPlanSelect() + suffix, betPlanMapper(), args);
    }

    private List<PostMatchReviewResponse> reviews(String suffix, Object... args) {
        return jdbcTemplate.query(reviewSelect() + suffix, reviewMapper(), args);
    }

    private List<BetPlanItemResponse> betPlanItems(long planId) {
        return jdbcTemplate.query(
                "SELECT id, bet_plan_id, match_id, market_type, selection_text, stake_suggestion, odds, line_value, logic_type, risk_level, play_type, pass_type, item_order, raw_payload " +
                        "FROM bet_plan_items WHERE bet_plan_id=? ORDER BY item_order, id",
                (rs, rowNum) -> new BetPlanItemResponse(
                        rs.getLong("id"),
                        rs.getLong("bet_plan_id"),
                        nullableLong(rs, "match_id"),
                        rs.getString("market_type"),
                        rs.getString("selection_text"),
                        rs.getBigDecimal("stake_suggestion"),
                        rs.getBigDecimal("odds"),
                        rs.getString("line_value"),
                        rs.getString("logic_type"),
                        rs.getString("risk_level"),
                        rs.getString("play_type"),
                        rs.getString("pass_type"),
                        rs.getInt("item_order"),
                        rs.getString("raw_payload")
                ),
                planId
        );
    }

    private List<ReviewLessonResponse> lessons(long reviewId) {
        return jdbcTemplate.query(
                "SELECT id, review_id, lesson_type, lesson_text, severity, raw_payload FROM review_lessons WHERE review_id=? ORDER BY id",
                (rs, rowNum) -> new ReviewLessonResponse(
                        rs.getLong("id"),
                        rs.getLong("review_id"),
                        rs.getString("lesson_type"),
                        rs.getString("lesson_text"),
                        rs.getString("severity"),
                        rs.getString("raw_payload")
                ),
                reviewId
        );
    }

    private String reportSelect() {
        return "SELECT ar.id, ar.match_id, m.match_name, m.matchday, m.jc_code, ar.analysis_id, ar.conclusion_type, ar.confidence, " +
                "ar.risk_summary, ar.narrative_md, ar.raw_payload, " +
                "(SELECT COUNT(*) FROM bet_plans bp WHERE bp.analysis_report_id=ar.id) AS bet_plan_count, " +
                "(SELECT COUNT(*) FROM post_match_reviews pmr WHERE pmr.analysis_report_id=ar.id) AS review_count " +
                "FROM analysis_reports ar LEFT JOIN matches m ON m.id=ar.match_id";
    }

    private String betPlanSelect() {
        return "SELECT bp.id, bp.analysis_report_id, bp.match_id, m.match_name, m.matchday, m.jc_code, bp.plan_key, bp.plan_title, " +
                "bp.conclusion_type, bp.confidence, bp.budget_amount, bp.risk_summary, bp.betting_method, bp.strategy_type, bp.status, " +
                "bp.generated_by, bp.generated_at, bp.raw_payload, " +
                "(SELECT COUNT(*) FROM bet_plan_items bpi WHERE bpi.bet_plan_id=bp.id) AS item_count " +
                "FROM bet_plans bp LEFT JOIN matches m ON m.id=bp.match_id";
    }

    private String reviewSelect() {
        return "SELECT pmr.id, pmr.import_item_id, pmr.match_id, m.match_name, m.matchday, pmr.analysis_report_id, pmr.review_key, pmr.review_title, " +
                "pmr.math_review, pmr.football_review, pmr.handicap_review, pmr.tournament_temperament_review, pmr.odds_value_review, " +
                "pmr.overall_summary, pmr.raw_payload " +
                "FROM post_match_reviews pmr LEFT JOIN matches m ON m.id=pmr.match_id";
    }

    private RowMapper<AnalysisReportSummaryResponse> reportMapper() {
        return (rs, rowNum) -> new AnalysisReportSummaryResponse(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                rs.getString("match_name"),
                localDate(rs, "matchday"),
                rs.getString("jc_code"),
                rs.getString("analysis_id"),
                rs.getString("conclusion_type"),
                rs.getString("confidence"),
                rs.getString("risk_summary"),
                rs.getString("narrative_md"),
                rs.getLong("bet_plan_count"),
                rs.getLong("review_count"),
                rs.getString("raw_payload")
        );
    }

    private RowMapper<BetPlanSummaryResponse> betPlanMapper() {
        return (rs, rowNum) -> new BetPlanSummaryResponse(
                rs.getLong("id"),
                nullableLong(rs, "analysis_report_id"),
                nullableLong(rs, "match_id"),
                rs.getString("match_name"),
                localDate(rs, "matchday"),
                rs.getString("jc_code"),
                rs.getString("plan_key"),
                rs.getString("plan_title"),
                rs.getString("conclusion_type"),
                rs.getString("confidence"),
                rs.getBigDecimal("budget_amount"),
                rs.getString("risk_summary"),
                rs.getString("betting_method"),
                rs.getString("strategy_type"),
                rs.getString("status"),
                rs.getString("generated_by"),
                localDateTime(rs, "generated_at"),
                rs.getLong("item_count"),
                rs.getString("raw_payload")
        );
    }

    private RowMapper<BetRecordResponse> betMapper() {
        return (rs, rowNum) -> new BetRecordResponse(
                rs.getLong("id"),
                rs.getLong("import_item_id"),
                nullableLong(rs, "match_id"),
                rs.getString("match_name"),
                localDate(rs, "matchday"),
                rs.getString("bet_id"),
                rs.getString("ticket_no"),
                localDate(rs, "bet_date"),
                rs.getString("market_type"),
                rs.getString("selection_text"),
                rs.getBigDecimal("stake"),
                rs.getBigDecimal("odds"),
                rs.getBigDecimal("closing_odds"),
                rs.getBigDecimal("clv"),
                rs.getBigDecimal("return_amount"),
                rs.getString("hit_status"),
                rs.getBigDecimal("profit_loss"),
                localDateTime(rs, "settled_at"),
                rs.getString("review_status"),
                rs.getString("raw_payload")
        );
    }

    private RowMapper<PostMatchReviewResponse> reviewMapper() {
        return (rs, rowNum) -> {
            long reviewId = rs.getLong("id");
            return new PostMatchReviewResponse(
                    reviewId,
                    rs.getLong("import_item_id"),
                    nullableLong(rs, "match_id"),
                    rs.getString("match_name"),
                    localDate(rs, "matchday"),
                    nullableLong(rs, "analysis_report_id"),
                    rs.getString("review_key"),
                    rs.getString("review_title"),
                    rs.getString("math_review"),
                    rs.getString("football_review"),
                    rs.getString("handicap_review"),
                    rs.getString("tournament_temperament_review"),
                    rs.getString("odds_value_review"),
                    rs.getString("overall_summary"),
                    rs.getString("raw_payload"),
                    lessons(reviewId)
            );
        };
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal roi(BigDecimal netProfit, BigDecimal totalStake) {
        if (totalStake == null || totalStake.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return netProfit.divide(totalStake, 6, RoundingMode.HALF_UP);
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
}
