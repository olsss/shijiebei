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
            long eventCount,
            long lineupCount,
            long evidenceCount,
            long conflictCount
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
            BigDecimal reliabilityScore
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

    public record PublicTeamProfileSummary(
            Long id,
            String teamKey,
            String displayName,
            String fifaCode,
            String countryRegion,
            String styleTags,
            String attackProfile,
            String defenseProfile,
            String publicSentiment,
            long playerCount,
            long factCount,
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
            long conflictCount
    ) {}

    public record PublicPlayerProfileSummary(
            Long id,
            String playerKey,
            Long teamId,
            String teamName,
            String displayName,
            Integer shirtNumber,
            String position,
            String status,
            String injuryStatus,
            String cardStatus,
            String lockerRoomStatus,
            long factCount,
            LocalDateTime latestProfileUpdate
    ) {}

    public record PublicPlayerProfileDetail(
            PublicPlayerProfileSummary player,
            List<PublicProfileFact> facts
    ) {}
}
