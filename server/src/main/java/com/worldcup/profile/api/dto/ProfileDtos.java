package com.worldcup.profile.api.dto;

import java.math.BigDecimal;
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

    public record TeamProfileDetail(
            TeamProfileSummary team,
            List<ProfileFactResponse> facts,
            List<TeamPlayerResponse> players,
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
