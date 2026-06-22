package com.worldcup.matchcenter.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class MatchCenterDtos {
    private MatchCenterDtos() {
    }

    public record MatchSummaryResponse(
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
    ) {
    }

    public record MatchDetailResponse(
            MatchSummaryResponse summary,
            String externalFactors,
            List<MatchLineupResponse> lineups,
            List<MatchEventResponse> events,
            List<MatchTeamStatsResponse> teamStats,
            List<MatchPlayerStatsResponse> playerStats,
            List<MatchEvidenceResponse> evidence,
            List<MatchConflictResponse> conflicts
    ) {
    }

    public record MatchLineupResponse(
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

    public record MatchEventResponse(
            Long id,
            Long matchId,
            Integer eventMinute,
            String eventType,
            Long teamId,
            String teamName,
            Long playerId,
            String playerName,
            String payload
    ) {
    }

    public record MatchTeamStatsResponse(
            Long id,
            Long matchId,
            Long teamId,
            String teamName,
            String statsType,
            Integer goalsFor,
            Integer goalsAgainst,
            Integer firstGoalMinute,
            String scoringMinutes,
            String payload
    ) {
    }

    public record MatchPlayerStatsResponse(
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
            Integer redCards,
            String payload
    ) {
    }

    public record MatchEvidenceResponse(
            Long id,
            String sourceType,
            String sourceName,
            String sourceRef,
            String sourceUrl,
            LocalDateTime evidenceTime,
            String summary,
            BigDecimal reliabilityScore
    ) {
    }

    public record MatchConflictResponse(
            Long id,
            String conflictType,
            String entityKey,
            String fieldName,
            String currentValue,
            String incomingValue,
            String resolutionStatus,
            String rawPayload
    ) {
    }
}
