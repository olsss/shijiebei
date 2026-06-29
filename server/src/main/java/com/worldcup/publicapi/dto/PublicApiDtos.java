package com.worldcup.publicapi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PublicApiDtos {
    private PublicApiDtos() {}

    public record PublicDecisionReport(
            Long id,
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            String conclusionType,
            String confidence,
            String riskSummary,
            String reviewSummary,
            String lessonSummary
    ) {}

    public record PublicMatchSummary(
            Long id,
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
            PublicTeamVisual homeTeam,
            PublicTeamVisual awayTeam,
            PublicScoreboard scoreboard,
            long eventCount,
            long lineupCount,
            long evidenceCount,
            long conflictCount
    ) {}

    public record PublicTeamVisual(
            Long teamId,
            String teamName,
            String fifaCode,
            String countryIso2,
            String flagUrl,
            String countryRegion
    ) {}

    public record PublicScoreboard(
            Integer homeScore,
            Integer awayScore,
            String scoreDisplay,
            String winnerSide,
            String resultText,
            String scoreSource
    ) {}

    public record PublicVisualMetric(
            String key,
            String label,
            BigDecimal value,
            String unit,
            String tone,
            String explanation
    ) {}

    public record PublicMatchDetail(
            PublicMatchSummary summary,
            String externalFactors,
            List<PublicMatchLineup> lineups,
            List<PublicMatchEvent> events,
            List<PublicMatchTeamStats> teamStats,
            List<PublicMatchPlayerStats> playerStats,
            List<PublicMatchEvidence> evidence,
            List<PublicMatchConflict> conflicts
    ) {}

    public record PublicMatchLineup(
            Long id,
            Long matchId,
            Long teamId,
            String teamName,
            Long playerId,
            String playerName,
            String role,
            String position,
            boolean starter
    ) {}

    public record PublicMatchEvent(
            Long id,
            Long matchId,
            Integer eventMinute,
            String eventType,
            Long teamId,
            String teamName,
            Long playerId,
            String playerName
    ) {}

    public record PublicMatchTeamStats(
            Long id,
            Long matchId,
            Long teamId,
            String teamName,
            String statsType,
            Integer goalsFor,
            Integer goalsAgainst,
            Integer firstGoalMinute,
            String scoringMinutes
    ) {}

    public record PublicMatchPlayerStats(
            Long id,
            Long matchId,
            Long playerId,
            String playerName,
            Long teamId,
            String teamName,
            Integer minutesPlayed,
            Integer goals,
            Integer assists,
            Integer yellowCards,
            Integer redCards
    ) {}

    public record PublicMatchEvidence(
            Long id,
            String sourceType,
            String sourceName,
            String sourceRef,
            String sourceUrl,
            LocalDateTime evidenceTime,
            String summary,
            BigDecimal reliabilityScore,
            String qualityLevel,
            String freshnessStatus,
            String supportsConclusion,
            String suggestedAction
    ) {}

    public record PublicMatchConflict(
            Long id,
            String conflictType,
            String entityKey,
            String fieldName,
            String resolutionStatus
    ) {}

    public record PublicOddsMarketSummary(
            Long id,
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            PublicTeamVisual homeTeam,
            PublicTeamVisual awayTeam,
            PublicScoreboard scoreboard,
            String bookmaker,
            String marketCode,
            String marketName,
            String snapshotType,
            BigDecimal handicapLine,
            String lineValue,
            LocalDateTime capturedAt,
            long selectionCount
    ) {}

    public record PublicOddsMatchDetail(
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            PublicTeamVisual homeTeam,
            PublicTeamVisual awayTeam,
            PublicScoreboard scoreboard,
            List<PublicOddsMarketDetail> markets
    ) {}

    public record PublicOddsMarketDetail(
            Long id,
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            String bookmaker,
            String marketCode,
            String marketName,
            String snapshotType,
            BigDecimal handicapLine,
            String lineValue,
            LocalDateTime capturedAt,
            long selectionCount,
            String sourceRef,
            List<PublicOddsSelection> selections
    ) {}

    public record PublicOddsSelection(
            Long id,
            Long marketSnapshotId,
            String selectionCode,
            String selectionName,
            BigDecimal oddsValue,
            BigDecimal impliedProbability,
            String selectionStatus
    ) {}

    public record PublicOddsMarketDictionary(
            String marketCode,
            String marketName
    ) {}

    public record PublicSentimentFactorSummary(
            Long id,
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            PublicTeamVisual homeTeam,
            PublicTeamVisual awayTeam,
            PublicScoreboard scoreboard,
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
            boolean stale,
            long riskCount,
            String highestRiskLevel
    ) {}

    public record PublicSentimentMatchDetail(
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            PublicTeamVisual homeTeam,
            PublicTeamVisual awayTeam,
            PublicScoreboard scoreboard,
            List<PublicSentimentFactorDetail> factors,
            List<PublicSentimentRisk> risks
    ) {}

    public record PublicSentimentFactorDetail(
            Long id,
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
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
            boolean stale
    ) {}

    public record PublicSentimentRisk(
            Long id,
            Long matchId,
            Long factorId,
            String riskType,
            String riskLevel,
            BigDecimal riskScore,
            String title,
            String rationale,
            String suggestedAction,
            String sourceName,
            String sourceRef
    ) {}

    public record PublicProfileFact(
            Long id,
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
    ) {}

    public record PublicProfileReadiness(
            int score,
            String level,
            String summary,
            List<String> strengths,
            List<String> missingDimensions,
            List<String> nextActions
    ) {}

    public record PublicTeamMetricSummary(
            BigDecimal xg,
            BigDecimal xga,
            BigDecimal npxg,
            BigDecimal ppda,
            BigDecimal xpts,
            Integer shots,
            Integer shotsOnTarget,
            BigDecimal possessionPct,
            Integer progressivePasses,
            BigDecimal setPieceXg,
            BigDecimal formScore,
            String sourceName,
            String sourceRef,
            LocalDateTime capturedAt
    ) {}

    public record PublicPlayerMetricSummary(
            Integer minutesPlayed,
            BigDecimal goals,
            BigDecimal assists,
            BigDecimal xg,
            BigDecimal xa,
            BigDecimal npxg,
            Integer shots,
            Integer shotsOnTarget,
            Integer keyPasses,
            Integer progressivePasses,
            BigDecimal trainingLoad,
            BigDecimal availabilityScore,
            BigDecimal expectedStartingProbability,
            String sourceName,
            String sourceRef,
            LocalDateTime capturedAt
    ) {}

    public record PublicTeamProfileSummary(
            Long id,
            String teamKey,
            String displayName,
            String fifaCode,
            String countryRegion,
            String countryIso2,
            String flagAssetKey,
            String confederation,
            String groupName,
            String metadataSourceRef,
            String styleTags,
            String attackProfile,
            String defenseProfile,
            String publicSentiment,
            long playerCount,
            long factCount,
            long technicalMetricCount,
            long advancedMetricCount,
            Integer groupStandingRank,
            Integer groupStandingPoints,
            String groupStandingRecord,
            Integer groupGoalDifference,
            String groupStandingSummary,
            LocalDateTime latestProfileUpdate
    ) {}

    public record PublicTeamPlayer(
            Long id,
            String playerKey,
            String displayName,
            Integer shirtNumber,
            String position,
            String status,
            String injuryStatus,
            String cardStatus,
            String lockerRoomStatus
    ) {}

    public record PublicTeamLineup(
            Long matchId,
            String matchName,
            LocalDate matchday,
            Long playerId,
            String playerName,
            String role,
            String position,
            boolean starter
    ) {}

    public record PublicTeamScoringPattern(
            Long matchId,
            String matchName,
            LocalDate matchday,
            Integer goalsFor,
            Integer goalsAgainst,
            Integer firstGoalMinute,
            String scoringMinutes
    ) {}

    public record PublicTeamExternalFactor(
            Long matchId,
            String matchName,
            LocalDate matchday,
            String externalFactors
    ) {}

    public record PublicTeamMatchHistory(
            Long matchId,
            String matchName,
            LocalDate matchday,
            String competition,
            String stage,
            String venue,
            String resultStatus,
            Integer goalsFor,
            Integer goalsAgainst,
            String scoringMinutes
    ) {}

    public record PublicTeamProfileDetail(
            PublicTeamProfileSummary team,
            List<PublicProfileFact> facts,
            List<PublicTeamPlayer> players,
            List<PublicTeamLineup> lineups,
            List<PublicTeamScoringPattern> scoringPatterns,
            List<PublicTeamExternalFactor> externalFactors,
            List<PublicTeamMatchHistory> matchHistory,
            long evidenceCount,
            long conflictCount,
            PublicProfileReadiness readiness,
            PublicTeamMetricSummary latestMetric
    ) {}

    public record PublicPlayerProfileSummary(
            Long id,
            String playerKey,
            Long teamId,
            String teamName,
            PublicTeamVisual team,
            String displayName,
            Integer shirtNumber,
            String position,
            String status,
            String injuryStatus,
            String cardStatus,
            String lockerRoomStatus,
            long factCount,
            long performanceMetricCount,
            long advancedMetricCount,
            LocalDateTime latestProfileUpdate
    ) {}

    public record PublicPlayerProfileDetail(
            PublicPlayerProfileSummary player,
            List<PublicProfileFact> facts,
            PublicProfileReadiness readiness,
            PublicPlayerMetricSummary latestMetric
    ) {}

    public record PublicOverviewResponse(
            LocalDateTime generatedAt,
            List<PublicOverviewMatch> upcomingMatches,
            PublicRiskCounters riskCounters,
            PublicIntegrityCounters integrityCounters,
            PublicOddsFreshness oddsFreshness,
            PublicDecisionSummary decisionSummary
    ) {}

    public record PublicOverviewMatch(
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            String competition,
            String stage,
            LocalDateTime kickoffTime,
            String status,
            String resultStatus,
            PublicTeamVisual homeTeam,
            PublicTeamVisual awayTeam,
            PublicScoreboard scoreboard,
            int integrityScore,
            long riskCount
    ) {}

    public record PublicRiskCounters(
            long highRiskCount,
            long mediumRiskCount,
            long staleFactorCount,
            long unresolvedConflictCount
    ) {}

    public record PublicIntegrityCounters(
            long completeCount,
            long partialCount,
            long blockedCount
    ) {}

    public record PublicOddsFreshness(
            long marketCount,
            long liveMarketCount,
            long staleLiveMarketCount
    ) {}

    public record PublicDecisionSummary(
            long reportCount,
            long reviewCount,
            LocalDateTime latestDecisionAt
    ) {}

    public record PublicDecisionReview(
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
            String overallSummary,
            List<PublicDecisionLesson> lessons
    ) {}

    public record PublicDecisionLesson(
            Long id,
            String lessonType,
            String lessonText,
            String severity
    ) {}

    public record PublicPrematchMatchSummary(
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
            PublicTeamVisual homeTeam,
            PublicTeamVisual awayTeam,
            PublicScoreboard scoreboard,
            int integrityScore,
            long missingCount,
            long staleCount,
            long conflictCount,
            long teamProfileCount,
            long playerProfileCount,
            long lineupCount,
            long oddsMarketCount,
            long sentimentFactorCount,
            long analysisReportCount
    ) {}

    public record PublicPrematchDetail(
            PublicPrematchMatchSummary summary,
            PublicPrematchVisualSummary visualSummary,
            List<PublicPrematchTeamComparison> teamComparison,
            List<PublicPrematchTeam> teams,
            List<PublicPrematchLineup> lineups,
            List<PublicPrematchPlayer> players,
            List<PublicPrematchOddsMarket> oddsMarkets,
            List<PublicPrematchSentimentFactor> sentimentFactors,
            List<PublicPrematchEvidence> evidence,
            List<PublicPrematchConflict> conflicts,
            List<PublicPrematchAnalysisReport> analysisReports,
            List<PublicPrematchIntegrityCheck> integrityChecks
    ) {}

    public record PublicPrematchVisualSummary(
            String statusText,
            String readinessText,
            String riskText,
            String nextCheckText,
            List<PublicVisualMetric> metrics
    ) {}

    public record PublicPrematchTeamComparison(
            PublicTeamVisual team,
            List<PublicVisualMetric> metrics
    ) {}

    public record PublicPrematchTeam(
            Long teamId,
            String teamKey,
            String teamName,
            String fifaCode,
            String countryRegion,
            String styleTags,
            String attackProfile,
            String defenseProfile,
            String publicSentiment,
            List<PublicPrematchFact> facts
    ) {}

    public record PublicPrematchFact(
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
    ) {}

    public record PublicPrematchLineup(
            Long id,
            Long matchId,
            Long teamId,
            String teamName,
            Long playerId,
            String playerName,
            String role,
            String position,
            boolean starter
    ) {}

    public record PublicPrematchPlayer(
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
            List<PublicPrematchFact> facts
    ) {}

    public record PublicPrematchOddsMarket(
            Long marketId,
            String bookmaker,
            String marketCode,
            String marketName,
            String snapshotType,
            BigDecimal handicapLine,
            String lineValue,
            LocalDateTime capturedAt,
            String sourceRef,
            List<PublicPrematchOddsSelection> selections
    ) {}

    public record PublicPrematchOddsSelection(
            Long selectionId,
            String selectionCode,
            String selectionName,
            BigDecimal oddsValue,
            BigDecimal impliedProbability,
            String selectionStatus
    ) {}

    public record PublicPrematchSentimentFactor(
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
            List<PublicPrematchSentimentRisk> risks
    ) {}

    public record PublicPrematchSentimentRisk(
            Long riskId,
            String riskType,
            String riskLevel,
            BigDecimal riskScore,
            String title,
            String rationale,
            String suggestedAction,
            String sourceName,
            String sourceRef
    ) {}

    public record PublicPrematchEvidence(
            Long evidenceId,
            String sourceType,
            String sourceName,
            String sourceRef,
            String sourceUrl,
            LocalDateTime evidenceTime,
            String summary,
            BigDecimal reliabilityScore
    ) {}

    public record PublicPrematchConflict(
            Long conflictId,
            String conflictType,
            String entityKey,
            String fieldName,
            String resolutionStatus
    ) {}

    public record PublicPrematchAnalysisReport(
            Long reportId,
            String analysisId,
            String conclusionType,
            String confidence,
            String riskSummary,
            String recommendedMarkets,
            String dimensions,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record PublicPrematchIntegrityCheck(
            String code,
            String label,
            String status,
            String severity,
            String message,
            long evidenceCount,
            LocalDateTime lastUpdatedAt
    ) {}
}
