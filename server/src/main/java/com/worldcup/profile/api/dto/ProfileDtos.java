package com.worldcup.profile.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ProfileDtos {
    private ProfileDtos() {
    }

    public record CreateCollectionJobRequest(
            String sourceType,
            String sourceName,
            String keyword,
            String entityType,
            String entityKey,
            String factType,
            String periodKey,
            String title,
            String summary,
            String sentimentLabel,
            BigDecimal confidenceScore,
            BigDecimal reliabilityScore,
            String sourceUrl,
            String sourceRef,
            String rawPayload
    ) {
    }

    public record RejectCollectionItemRequest(String reason) {
    }

    public record CollectionItemResponse(
            Long id,
            Long jobId,
            String entityType,
            String entityKey,
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
            LocalDateTime capturedAt,
            String status,
            String reviewNote,
            String reviewedBy,
            LocalDateTime reviewedAt,
            String targetType,
            Long targetId
    ) {
    }

    public record CollectionItemReviewResponse(
            Long id,
            String status,
            String targetType,
            Long targetId,
            String message
    ) {
    }

    public record CollectionJobResponse(
            Long id,
            String sourceType,
            String sourceName,
            String keyword,
            String status,
            int totalItems,
            int pendingItems,
            int approvedItems,
            int rejectedItems
    ) {
    }

    public record ProfileFactResponse(
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
            LocalDateTime capturedAt,
            String approvedBy
    ) {
    }

    public record TeamProfileSummary(
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
            LocalDateTime latestProfileUpdate
    ) {
    }

    public record TeamPlayerResponse(
            Long id,
            String playerKey,
            String displayName,
            Integer shirtNumber,
            String position,
            String status,
            String injuryStatus,
            String cardStatus,
            String lockerRoomStatus
    ) {
    }

    public record TeamLineupResponse(
            Long matchId,
            String matchName,
            LocalDate matchday,
            Long playerId,
            String playerName,
            String role,
            String position,
            boolean starter
    ) {
    }

    public record TeamScoringPatternResponse(
            Long matchId,
            String matchName,
            LocalDate matchday,
            Integer goalsFor,
            Integer goalsAgainst,
            Integer firstGoalMinute,
            String scoringMinutes
    ) {
    }

    public record TeamExternalFactorResponse(
            Long matchId,
            String matchName,
            LocalDate matchday,
            String externalFactors
    ) {
    }

    public record TeamMatchHistoryResponse(
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
    ) {
    }

    public record TeamProfileDetail(
            TeamProfileSummary team,
            List<ProfileFactResponse> facts,
            List<TeamPlayerResponse> players,
            List<TeamLineupResponse> lineups,
            List<TeamScoringPatternResponse> scoringPatterns,
            List<TeamExternalFactorResponse> externalFactors,
            List<TeamMatchHistoryResponse> matchHistory,
            long evidenceCount,
            long conflictCount
    ) {
    }

    public record PlayerProfileSummary(
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
    ) {
    }

    public record PlayerProfileDetail(
            PlayerProfileSummary player,
            List<ProfileFactResponse> facts
    ) {
    }
}
