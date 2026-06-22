package com.worldcup.analysisreviewcenter.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AnalysisReviewCenterDtos {
    private AnalysisReviewCenterDtos() {
    }

    public record OverviewResponse(
            long reportCount,
            long betPlanCount,
            long betCount,
            long reviewCount,
            BigDecimal totalStake,
            BigDecimal totalReturn,
            BigDecimal netProfit,
            BigDecimal roi,
            BigDecimal averageClv
    ) {
    }

    public record AnalysisReportSummaryResponse(
            Long id,
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            String analysisId,
            String conclusionType,
            String confidence,
            String riskSummary,
            String narrativeMd,
            long betPlanCount,
            long reviewCount,
            String rawPayload
    ) {
    }

    public record AnalysisReportDetailResponse(
            AnalysisReportSummaryResponse report,
            List<BetPlanSummaryResponse> betPlans,
            List<PostMatchReviewResponse> reviews
    ) {
    }

    public record BetPlanSummaryResponse(
            Long id,
            Long analysisReportId,
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            String planKey,
            String planTitle,
            String conclusionType,
            String confidence,
            BigDecimal budgetAmount,
            String riskSummary,
            String bettingMethod,
            String strategyType,
            String status,
            String generatedBy,
            LocalDateTime generatedAt,
            long itemCount,
            String rawPayload
    ) {
    }

    public record BetPlanDetailResponse(
            BetPlanSummaryResponse plan,
            List<BetPlanItemResponse> items
    ) {
    }

    public record BetPlanItemResponse(
            Long id,
            Long betPlanId,
            Long matchId,
            String marketType,
            String selectionText,
            BigDecimal stakeSuggestion,
            BigDecimal odds,
            String lineValue,
            String logicType,
            String riskLevel,
            String playType,
            String passType,
            int itemOrder,
            String rawPayload
    ) {
    }

    public record BetRecordResponse(
            Long id,
            Long importItemId,
            Long matchId,
            String matchName,
            LocalDate matchday,
            String betId,
            String ticketNo,
            LocalDate betDate,
            String marketType,
            String selectionText,
            BigDecimal stake,
            BigDecimal odds,
            BigDecimal closingOdds,
            BigDecimal clv,
            BigDecimal returnAmount,
            String hitStatus,
            BigDecimal profitLoss,
            LocalDateTime settledAt,
            String reviewStatus,
            String rawPayload
    ) {
    }

    public record PostMatchReviewResponse(
            Long id,
            Long importItemId,
            Long matchId,
            String matchName,
            LocalDate matchday,
            Long analysisReportId,
            String reviewKey,
            String reviewTitle,
            String mathReview,
            String footballReview,
            String handicapReview,
            String tournamentTemperamentReview,
            String oddsValueReview,
            String overallSummary,
            String rawPayload,
            List<ReviewLessonResponse> lessons
    ) {
    }

    public record ReviewLessonResponse(
            Long id,
            Long reviewId,
            String lessonType,
            String lessonText,
            String severity,
            String rawPayload
    ) {
    }
}
