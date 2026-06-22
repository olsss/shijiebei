package com.worldcup.publicapi.service;

import com.worldcup.matchcenter.api.dto.MatchCenterDtos.*;
import com.worldcup.oddscenter.api.dto.OddsCenterDtos.*;
import com.worldcup.profile.api.dto.ProfileDtos.*;
import com.worldcup.publicapi.dto.PublicApiDtos.*;
import com.worldcup.sentimentcenter.api.dto.SentimentCenterDtos.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

@Component
public class PublicApiMapper {
    private static final String FORBIDDEN_KEY_ALIASES = String.join("|",
            "rawJson", "raw_json", "rawPayload", "raw_payload", "raw", "payload",
            "archivePath", "archive_path", "sourcePath", "source_path",
            "ticketNo", "ticket_no",
            "stake", "stakeSuggestion", "stake_suggestion",
            "budgetAmount", "budget_amount", "returnAmount", "return_amount", "profitLoss", "profit_loss",
            "approvedBy", "approved_by", "reviewedBy", "reviewed_by", "reviewNote", "review_note",
            "mappings", "importItemId", "import_item_id"
    );
    private static final Set<String> FORBIDDEN_TOKEN_ALIASES = Pattern.compile("\\|")
            .splitAsStream(FORBIDDEN_KEY_ALIASES)
            .map(alias -> alias.toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private static final Pattern SENSITIVE_KEY_VALUE = Pattern.compile(
            "(?<![A-Za-z0-9_])\"?(?:" + FORBIDDEN_KEY_ALIASES + ")\"?\\s*[:=]\\s*"
                    + "(?:\"(?:\\\\.|[^\"])*\"|\\{[^{}]*}|[^\\s,;]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WINDOWS_PATH = Pattern.compile("[A-Za-z]:[/\\\\][^\\s,;]+");

    public String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = SENSITIVE_KEY_VALUE.matcher(value).replaceAll("[REDACTED]");
        sanitized = WINDOWS_PATH.matcher(sanitized).replaceAll("[REDACTED]");
        return sanitized;
    }

    public String sanitizeToken(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (FORBIDDEN_TOKEN_ALIASES.contains(trimmed.toLowerCase(Locale.ROOT))) {
            return "[REDACTED]";
        }
        return sanitizeText(value);
    }

    public PublicMatchSummary toPublicMatchSummary(MatchSummaryResponse value) {
        return new PublicMatchSummary(
                value.id(),
                sanitizeText(value.matchKey()),
                sanitizeText(value.matchName()),
                value.matchday(),
                sanitizeText(value.jcCode()),
                sanitizeText(value.competition()),
                sanitizeText(value.stage()),
                sanitizeText(value.venue()),
                value.kickoffTime(),
                sanitizeText(value.status()),
                sanitizeText(value.resultStatus()),
                value.homeTeamId(),
                sanitizeText(value.homeTeamName()),
                value.awayTeamId(),
                sanitizeText(value.awayTeamName()),
                value.eventCount(),
                value.lineupCount(),
                value.evidenceCount(),
                value.conflictCount()
        );
    }

    public PublicMatchDetail toPublicMatchDetail(MatchDetailResponse value) {
        return new PublicMatchDetail(
                toPublicMatchSummary(value.summary()),
                sanitizeText(value.externalFactors()),
                mapList(value.lineups(), this::toPublicMatchLineup),
                mapList(value.events(), this::toPublicMatchEvent),
                mapList(value.teamStats(), this::toPublicMatchTeamStats),
                mapList(value.playerStats(), this::toPublicMatchPlayerStats),
                mapList(value.evidence(), this::toPublicMatchEvidence),
                mapList(value.conflicts(), this::toPublicMatchConflict)
        );
    }

    public PublicMatchLineup toPublicMatchLineup(MatchLineupResponse value) {
        return new PublicMatchLineup(
                value.id(),
                value.matchId(),
                value.teamId(),
                sanitizeText(value.teamName()),
                value.playerId(),
                sanitizeText(value.playerName()),
                sanitizeText(value.role()),
                sanitizeText(value.position()),
                value.starter()
        );
    }

    public PublicMatchEvent toPublicMatchEvent(MatchEventResponse value) {
        return new PublicMatchEvent(
                value.id(),
                value.matchId(),
                value.eventMinute(),
                sanitizeText(value.eventType()),
                value.teamId(),
                sanitizeText(value.teamName()),
                value.playerId(),
                sanitizeText(value.playerName())
        );
    }

    public PublicMatchTeamStats toPublicMatchTeamStats(MatchTeamStatsResponse value) {
        return new PublicMatchTeamStats(
                value.id(),
                value.matchId(),
                value.teamId(),
                sanitizeText(value.teamName()),
                sanitizeText(value.statsType()),
                value.goalsFor(),
                value.goalsAgainst(),
                value.firstGoalMinute(),
                sanitizeText(value.scoringMinutes())
        );
    }

    public PublicMatchPlayerStats toPublicMatchPlayerStats(MatchPlayerStatsResponse value) {
        return new PublicMatchPlayerStats(
                value.id(),
                value.matchId(),
                value.playerId(),
                sanitizeText(value.playerName()),
                value.teamId(),
                sanitizeText(value.teamName()),
                value.minutesPlayed(),
                value.goals(),
                value.assists(),
                value.yellowCards(),
                value.redCards()
        );
    }

    public PublicMatchEvidence toPublicMatchEvidence(MatchEvidenceResponse value) {
        return new PublicMatchEvidence(
                value.id(),
                sanitizeText(value.sourceType()),
                sanitizeText(value.sourceName()),
                sanitizeText(value.sourceRef()),
                sanitizeText(value.sourceUrl()),
                value.evidenceTime(),
                sanitizeText(value.summary()),
                value.reliabilityScore()
        );
    }

    public PublicMatchConflict toPublicMatchConflict(MatchConflictResponse value) {
        return new PublicMatchConflict(
                value.id(),
                sanitizeText(value.conflictType()),
                sanitizeText(value.entityKey()),
                sanitizeToken(value.fieldName()),
                sanitizeText(value.resolutionStatus())
        );
    }

    public PublicOddsMarketSummary toPublicOddsMarketSummary(OddsMarketSummaryResponse value) {
        return new PublicOddsMarketSummary(
                value.id(),
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                sanitizeText(value.jcCode()),
                sanitizeText(value.bookmaker()),
                sanitizeText(value.marketCode()),
                sanitizeText(value.marketName()),
                sanitizeText(value.snapshotType()),
                value.handicapLine(),
                sanitizeText(value.lineValue()),
                value.capturedAt(),
                value.selectionCount()
        );
    }

    public PublicOddsMatchDetail toPublicOddsMatchDetail(OddsMatchDetailResponse value) {
        return new PublicOddsMatchDetail(
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                sanitizeText(value.jcCode()),
                mapList(value.markets(), this::toPublicOddsMarketDetail)
        );
    }

    public PublicOddsMarketDetail toPublicOddsMarketDetail(OddsMarketDetailResponse value) {
        return new PublicOddsMarketDetail(
                value.id(),
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                sanitizeText(value.jcCode()),
                sanitizeText(value.bookmaker()),
                sanitizeText(value.marketCode()),
                sanitizeText(value.marketName()),
                sanitizeText(value.snapshotType()),
                value.handicapLine(),
                sanitizeText(value.lineValue()),
                value.capturedAt(),
                value.selectionCount(),
                sanitizeText(value.sourceRef()),
                mapList(value.selections(), this::toPublicOddsSelection)
        );
    }

    public PublicOddsSelection toPublicOddsSelection(OddsSelectionResponse value) {
        return new PublicOddsSelection(
                value.id(),
                value.marketSnapshotId(),
                sanitizeText(value.selectionCode()),
                sanitizeText(value.selectionName()),
                value.oddsValue(),
                value.impliedProbability(),
                sanitizeText(value.selectionStatus())
        );
    }

    public PublicOddsMarketDictionary toPublicOddsMarketDictionary(OddsMarketDictionaryResponse value) {
        return new PublicOddsMarketDictionary(
                sanitizeText(value.marketCode()),
                sanitizeText(value.marketName())
        );
    }

    public PublicSentimentFactorSummary toPublicSentimentFactorSummary(SentimentFactorSummaryResponse value) {
        return new PublicSentimentFactorSummary(
                value.id(),
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                sanitizeText(value.jcCode()),
                sanitizeText(value.factorCategory()),
                sanitizeText(value.factorType()),
                sanitizeText(value.title()),
                sanitizeText(value.summary()),
                sanitizeText(value.impactDirection()),
                sanitizeText(value.entityType()),
                sanitizeText(value.entityKey()),
                sanitizeText(value.evidenceLevel()),
                sanitizeText(value.sourceName()),
                sanitizeText(value.sourceUrl()),
                sanitizeText(value.sourceRef()),
                value.observedAt(),
                value.expiresAt(),
                value.confidenceScore(),
                value.reliabilityScore(),
                value.stale(),
                value.riskCount(),
                sanitizeText(value.highestRiskLevel())
        );
    }

    public PublicSentimentMatchDetail toPublicSentimentMatchDetail(SentimentMatchDetailResponse value) {
        return new PublicSentimentMatchDetail(
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                sanitizeText(value.jcCode()),
                mapList(value.factors(), this::toPublicSentimentFactorDetail),
                mapList(value.risks(), this::toPublicSentimentRisk)
        );
    }

    public PublicSentimentFactorDetail toPublicSentimentFactorDetail(SentimentFactorDetailResponse value) {
        return new PublicSentimentFactorDetail(
                value.id(),
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                sanitizeText(value.jcCode()),
                sanitizeText(value.factorCategory()),
                sanitizeText(value.factorType()),
                sanitizeText(value.title()),
                sanitizeText(value.summary()),
                sanitizeText(value.impactDirection()),
                sanitizeText(value.entityType()),
                sanitizeText(value.entityKey()),
                sanitizeText(value.evidenceLevel()),
                sanitizeText(value.sourceName()),
                sanitizeText(value.sourceUrl()),
                sanitizeText(value.sourceRef()),
                value.observedAt(),
                value.expiresAt(),
                value.confidenceScore(),
                value.reliabilityScore(),
                value.stale()
        );
    }

    public PublicSentimentRisk toPublicSentimentRisk(SentimentRiskResponse value) {
        return new PublicSentimentRisk(
                value.id(),
                value.matchId(),
                value.factorId(),
                sanitizeText(value.riskType()),
                sanitizeText(value.riskLevel()),
                value.riskScore(),
                sanitizeText(value.title()),
                sanitizeText(value.rationale()),
                sanitizeText(value.suggestedAction()),
                sanitizeText(value.sourceName()),
                sanitizeText(value.sourceRef())
        );
    }

    public PublicProfileFact toPublicProfileFact(ProfileFactResponse value) {
        return new PublicProfileFact(
                value.id(),
                sanitizeText(value.factType()),
                sanitizeText(value.periodKey()),
                sanitizeText(value.title()),
                sanitizeText(value.summary()),
                sanitizeText(value.sentimentLabel()),
                value.confidenceScore(),
                value.reliabilityScore(),
                sanitizeText(value.sourceName()),
                sanitizeText(value.sourceUrl()),
                sanitizeText(value.sourceRef()),
                value.capturedAt()
        );
    }

    public PublicTeamProfileSummary toPublicTeamProfileSummary(TeamProfileSummary value) {
        return new PublicTeamProfileSummary(
                value.id(),
                sanitizeText(value.teamKey()),
                sanitizeText(value.displayName()),
                sanitizeText(value.fifaCode()),
                sanitizeText(value.countryRegion()),
                sanitizeText(value.styleTags()),
                sanitizeText(value.attackProfile()),
                sanitizeText(value.defenseProfile()),
                sanitizeText(value.publicSentiment()),
                value.playerCount(),
                value.factCount(),
                value.latestProfileUpdate()
        );
    }

    public PublicTeamPlayer toPublicTeamPlayer(TeamPlayerResponse value) {
        return new PublicTeamPlayer(
                value.id(),
                sanitizeText(value.playerKey()),
                sanitizeText(value.displayName()),
                value.shirtNumber(),
                sanitizeText(value.position()),
                sanitizeText(value.status()),
                sanitizeText(value.injuryStatus()),
                sanitizeText(value.cardStatus()),
                sanitizeText(value.lockerRoomStatus())
        );
    }

    public PublicTeamLineup toPublicTeamLineup(TeamLineupResponse value) {
        return new PublicTeamLineup(
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                value.playerId(),
                sanitizeText(value.playerName()),
                sanitizeText(value.role()),
                sanitizeText(value.position()),
                value.starter()
        );
    }

    public PublicTeamScoringPattern toPublicTeamScoringPattern(TeamScoringPatternResponse value) {
        return new PublicTeamScoringPattern(
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                value.goalsFor(),
                value.goalsAgainst(),
                value.firstGoalMinute(),
                sanitizeText(value.scoringMinutes())
        );
    }

    public PublicTeamExternalFactor toPublicTeamExternalFactor(TeamExternalFactorResponse value) {
        return new PublicTeamExternalFactor(
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                sanitizeText(value.externalFactors())
        );
    }

    public PublicTeamMatchHistory toPublicTeamMatchHistory(TeamMatchHistoryResponse value) {
        return new PublicTeamMatchHistory(
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                sanitizeText(value.competition()),
                sanitizeText(value.stage()),
                sanitizeText(value.venue()),
                sanitizeText(value.resultStatus()),
                value.goalsFor(),
                value.goalsAgainst(),
                sanitizeText(value.scoringMinutes())
        );
    }

    public PublicTeamProfileDetail toPublicTeamProfileDetail(TeamProfileDetail value) {
        return new PublicTeamProfileDetail(
                toPublicTeamProfileSummary(value.team()),
                mapList(value.facts(), this::toPublicProfileFact),
                mapList(value.players(), this::toPublicTeamPlayer),
                mapList(value.lineups(), this::toPublicTeamLineup),
                mapList(value.scoringPatterns(), this::toPublicTeamScoringPattern),
                mapList(value.externalFactors(), this::toPublicTeamExternalFactor),
                mapList(value.matchHistory(), this::toPublicTeamMatchHistory),
                value.evidenceCount(),
                value.conflictCount()
        );
    }

    public PublicPlayerProfileSummary toPublicPlayerProfileSummary(PlayerProfileSummary value) {
        return new PublicPlayerProfileSummary(
                value.id(),
                sanitizeText(value.playerKey()),
                value.teamId(),
                sanitizeText(value.teamName()),
                sanitizeText(value.displayName()),
                value.shirtNumber(),
                sanitizeText(value.position()),
                sanitizeText(value.status()),
                sanitizeText(value.injuryStatus()),
                sanitizeText(value.cardStatus()),
                sanitizeText(value.lockerRoomStatus()),
                value.factCount(),
                value.latestProfileUpdate()
        );
    }

    public PublicPlayerProfileDetail toPublicPlayerProfileDetail(PlayerProfileDetail value) {
        return new PublicPlayerProfileDetail(
                toPublicPlayerProfileSummary(value.player()),
                mapList(value.facts(), this::toPublicProfileFact)
        );
    }

    private <T, R> List<R> mapList(List<T> values, Function<T, R> mapper) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().map(mapper).toList();
    }
}
