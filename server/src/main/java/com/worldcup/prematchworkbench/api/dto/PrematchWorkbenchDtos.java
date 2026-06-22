package com.worldcup.prematchworkbench.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PrematchWorkbenchDtos {
    private PrematchWorkbenchDtos() {
    }

    public record WorkbenchMatchSummaryResponse(
            Long matchId,
            String matchKey,
            String matchName,
            LocalDate matchday,
            String jcCode,
            String competition,
            String stage,
            String venue,
            LocalDateTime kickoffTime,
            String status,
            String resultStatus,
            Long homeTeamId,
            String homeTeamName,
            Long awayTeamId,
            String awayTeamName,
            int integrityScore,
            long missingCount,
            long staleCount,
            long conflictCount,
            long teamProfileCount,
            long playerProfileCount,
            long lineupCount,
            long oddsMarketCount,
            long sentimentFactorCount,
            long analysisReportCount,
            long betPlanCount,
            long betCount
    ) {
    }

    public record PrematchWorkbenchDetailResponse(
            WorkbenchMatchSummaryResponse summary,
            List<WorkbenchTeamResponse> teams,
            List<WorkbenchLineupResponse> lineups,
            List<WorkbenchPlayerResponse> players,
            List<WorkbenchOddsMarketResponse> oddsMarkets,
            List<WorkbenchSentimentFactorResponse> sentimentFactors,
            List<WorkbenchEvidenceResponse> evidence,
            List<WorkbenchConflictResponse> conflicts,
            List<WorkbenchAnalysisReportResponse> analysisReports,
            List<WorkbenchBetPlanResponse> betPlans,
            List<WorkbenchBetRecordResponse> bets,
            List<IntegrityCheckResponse> integrityChecks
    ) {
    }

    public record WorkbenchTeamResponse(
            Long teamId,
            String teamKey,
            String teamName,
            String fifaCode,
            String countryRegion,
            String styleTags,
            String attackProfile,
            String defenseProfile,
            String publicSentiment,
            List<WorkbenchTeamFactResponse> facts
    ) {
    }

    public record WorkbenchTeamFactResponse(
            Long factId,
            String factType,
            String periodKey,
            String title,
            String summary,
            String sentimentLabel,
            BigDecimal confidenceScore,
            BigDecimal reliabilityScore,
            String sourceName,
            String sourceUrl,
            String sourceRef,
            LocalDateTime capturedAt
    ) {
    }

    public record WorkbenchLineupResponse(
            Long id,
            Long matchId,
            Long teamId,
            String teamName,
            Long playerId,
            String playerName,
            String role,
            String position,
            boolean starter
    ) {
    }

    public record WorkbenchPlayerResponse(
            Long playerId,
            String playerKey,
            Long teamId,
            String teamName,
            String playerName,
            Integer shirtNumber,
            String position,
            String status,
            String injuryStatus,
            String cardStatus,
            String lockerRoomStatus,
            List<WorkbenchPlayerFactResponse> facts
    ) {
    }

    public record WorkbenchPlayerFactResponse(
            Long factId,
            String factType,
            String periodKey,
            String title,
            String summary,
            String sentimentLabel,
            BigDecimal confidenceScore,
            BigDecimal reliabilityScore,
            String sourceName,
            String sourceUrl,
            String sourceRef,
            LocalDateTime capturedAt
    ) {
    }

    public record WorkbenchOddsMarketResponse(
            Long marketId,
            String bookmaker,
            String marketCode,
            String marketName,
            String snapshotType,
            BigDecimal handicapLine,
            String lineValue,
            LocalDateTime capturedAt,
            String sourceRef,
            List<WorkbenchOddsSelectionResponse> selections
    ) {
    }

    public record WorkbenchOddsSelectionResponse(
            Long selectionId,
            String selectionCode,
            String selectionName,
            BigDecimal oddsValue,
            BigDecimal impliedProbability,
            String selectionStatus
    ) {
    }

    public record WorkbenchSentimentFactorResponse(
            Long factorId,
            Long matchId,
            String factorCategory,
            String factorType,
            String title,
            String summary,
            String impactDirection,
            String entityType,
            String entityKey,
            String evidenceLevel,
            String sourceName,
            String sourceUrl,
            String sourceRef,
            LocalDateTime observedAt,
            LocalDateTime expiresAt,
            BigDecimal confidenceScore,
            BigDecimal reliabilityScore,
            List<WorkbenchSentimentRiskResponse> risks
    ) {
    }

    public record WorkbenchSentimentRiskResponse(
            Long riskId,
            String riskType,
            String riskLevel,
            BigDecimal riskScore,
            String title,
            String rationale,
            String suggestedAction,
            String sourceName,
            String sourceRef
    ) {
    }

    public record WorkbenchEvidenceResponse(
            Long evidenceId,
            String sourceType,
            String sourceName,
            String sourceRef,
            String sourceUrl,
            LocalDateTime evidenceTime,
            String summary,
            BigDecimal reliabilityScore
    ) {
    }

    public record WorkbenchConflictResponse(
            Long conflictId,
            String conflictType,
            String entityKey,
            String fieldName,
            String currentValue,
            String incomingValue,
            String resolutionStatus,
            String rawPayload
    ) {
    }

    public record WorkbenchAnalysisReportResponse(
            Long reportId,
            String analysisId,
            String conclusionType,
            String confidence,
            String riskSummary,
            String recommendedMarkets,
            String dimensions,
            String narrativeMd,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record WorkbenchBetPlanResponse(
            Long planId,
            Long analysisReportId,
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
            List<WorkbenchBetPlanItemResponse> items
    ) {
    }

    public record WorkbenchBetPlanItemResponse(
            Long itemId,
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
            int itemOrder
    ) {
    }

    public record WorkbenchBetRecordResponse(
            Long betRecordId,
            String betId,
            String ticketNo,
            LocalDate betDate,
            LocalDate matchday,
            String matchName,
            String marketType,
            String selectionText,
            BigDecimal stake,
            BigDecimal odds,
            BigDecimal closingOdds,
            BigDecimal clv,
            BigDecimal returnAmount,
            String hitStatus,
            BigDecimal profitLoss,
            String reviewStatus
    ) {
    }

    public record IntegrityCheckResponse(
            String code,
            String label,
            String status,
            String severity,
            String message,
            long evidenceCount,
            LocalDateTime lastUpdatedAt
    ) {
    }
}
