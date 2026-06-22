package com.worldcup.publicapi.service;

import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.*;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.*;
import com.worldcup.oddscenter.api.dto.OddsCenterDtos.*;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.*;
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
    private static final Pattern WINDOWS_PATH = Pattern.compile("(?<![A-Za-z0-9])[A-Za-z]:[/\\\\][^\\s,;]+");
    private static final Pattern FILE_URI_PATH = Pattern.compile("(?i)file:/{2,3}(?:tmp|var|home|users|etc|mnt|opt|srv|data|secret)(?:/[^\\s,;]+)*");
    private static final Pattern ARCHIVE_RELATIVE_PATH = Pattern.compile("(?i)(?<![A-Za-z0-9_./-])(?:\\./)?(?:skill/)?archive/[^\\s,;]+");
    private static final Pattern UNIX_PATH = Pattern.compile("(?i)(?<![A-Za-z0-9_./-])/(?:tmp|var|home|users|etc|mnt|opt|srv|data|secret)(?:/[^\\s,;]+)*");
    private static final Pattern SECRET_SENTINEL = Pattern.compile("(?i)SECRET[^\\s,;]*");

    public String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = SENSITIVE_KEY_VALUE.matcher(value).replaceAll("[REDACTED]");
        sanitized = FILE_URI_PATH.matcher(sanitized).replaceAll("[REDACTED]");
        sanitized = WINDOWS_PATH.matcher(sanitized).replaceAll("[REDACTED]");
        sanitized = ARCHIVE_RELATIVE_PATH.matcher(sanitized).replaceAll("[REDACTED]");
        sanitized = UNIX_PATH.matcher(sanitized).replaceAll("[REDACTED]");
        sanitized = SECRET_SENTINEL.matcher(sanitized).replaceAll("[REDACTED]");
        return sanitized;
    }

    public String sanitizeToken(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (hasForbiddenTokenSegment(trimmed)) {
            return "[REDACTED]";
        }
        return sanitizeText(value);
    }

    private boolean hasForbiddenTokenSegment(String value) {
        for (String segment : value.split("[^A-Za-z0-9_]+")) {
            if (FORBIDDEN_TOKEN_ALIASES.contains(segment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
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

    public PublicDecisionReport toPublicDecisionReport(AnalysisReportSummaryResponse value) {
        return new PublicDecisionReport(
                value.id(),
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                sanitizeText(value.jcCode()),
                sanitizeToken(value.conclusionType()),
                sanitizeToken(value.confidence()),
                sanitizeText(value.riskSummary()),
                null,
                null
        );
    }

    public PublicDecisionReview toPublicDecisionReview(PostMatchReviewResponse value) {
        return new PublicDecisionReview(
                value.id(),
                value.matchId(),
                sanitizeText(value.matchName()),
                value.matchday(),
                value.analysisReportId(),
                sanitizeToken(value.reviewKey()),
                sanitizeText(value.reviewTitle()),
                sanitizeText(value.mathReview()),
                sanitizeText(value.footballReview()),
                sanitizeText(value.handicapReview()),
                sanitizeText(value.tournamentTemperamentReview()),
                sanitizeText(value.oddsValueReview()),
                sanitizeText(value.overallSummary()),
                mapList(value.lessons(), this::toPublicDecisionLesson)
        );
    }

    public PublicDecisionLesson toPublicDecisionLesson(ReviewLessonResponse value) {
        return new PublicDecisionLesson(
                value.id(),
                sanitizeToken(value.lessonType()),
                sanitizeText(value.lessonText()),
                sanitizeToken(value.severity())
        );
    }

    public PublicPrematchMatchSummary toPublicPrematchMatchSummary(WorkbenchMatchSummaryResponse value) {
        return new PublicPrematchMatchSummary(
                value.matchId(),
                sanitizeText(value.matchKey()),
                sanitizeText(value.matchName()),
                value.matchday(),
                sanitizeText(value.jcCode()),
                sanitizeText(value.competition()),
                sanitizeText(value.stage()),
                sanitizeText(value.venue()),
                value.kickoffTime(),
                sanitizeToken(value.status()),
                sanitizeToken(value.resultStatus()),
                value.homeTeamId(),
                sanitizeText(value.homeTeamName()),
                value.awayTeamId(),
                sanitizeText(value.awayTeamName()),
                value.integrityScore(),
                value.missingCount(),
                value.staleCount(),
                value.conflictCount(),
                value.teamProfileCount(),
                value.playerProfileCount(),
                value.lineupCount(),
                value.oddsMarketCount(),
                value.sentimentFactorCount(),
                value.analysisReportCount()
        );
    }

    public PublicPrematchDetail toPublicPrematchDetail(PrematchWorkbenchDetailResponse value) {
        return new PublicPrematchDetail(
                toPublicPrematchMatchSummary(value.summary()),
                mapList(value.teams(), this::toPublicPrematchTeam),
                mapList(value.lineups(), this::toPublicPrematchLineup),
                mapList(value.players(), this::toPublicPrematchPlayer),
                mapList(value.oddsMarkets(), this::toPublicPrematchOddsMarket),
                mapList(value.sentimentFactors(), this::toPublicPrematchSentimentFactor),
                mapList(value.evidence(), this::toPublicPrematchEvidence),
                mapList(value.conflicts(), this::toPublicPrematchConflict),
                mapList(value.analysisReports(), this::toPublicPrematchAnalysisReport),
                mapList(value.integrityChecks(), this::toPublicPrematchIntegrityCheck)
        );
    }

    public PublicPrematchTeam toPublicPrematchTeam(WorkbenchTeamResponse value) {
        return new PublicPrematchTeam(
                value.teamId(),
                sanitizeText(value.teamKey()),
                sanitizeText(value.teamName()),
                sanitizeText(value.fifaCode()),
                sanitizeText(value.countryRegion()),
                sanitizeText(value.styleTags()),
                sanitizeText(value.attackProfile()),
                sanitizeText(value.defenseProfile()),
                sanitizeText(value.publicSentiment()),
                mapList(value.facts(), this::toPublicPrematchFact)
        );
    }

    public PublicPrematchFact toPublicPrematchFact(WorkbenchTeamFactResponse value) {
        return new PublicPrematchFact(
                value.factId(),
                sanitizeToken(value.factType()),
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

    public PublicPrematchLineup toPublicPrematchLineup(WorkbenchLineupResponse value) {
        return new PublicPrematchLineup(
                value.id(),
                value.matchId(),
                value.teamId(),
                sanitizeText(value.teamName()),
                value.playerId(),
                sanitizeText(value.playerName()),
                sanitizeToken(value.role()),
                sanitizeText(value.position()),
                value.starter()
        );
    }

    public PublicPrematchPlayer toPublicPrematchPlayer(WorkbenchPlayerResponse value) {
        return new PublicPrematchPlayer(
                value.playerId(),
                sanitizeText(value.playerKey()),
                value.teamId(),
                sanitizeText(value.teamName()),
                sanitizeText(value.playerName()),
                value.shirtNumber(),
                sanitizeText(value.position()),
                sanitizeToken(value.status()),
                sanitizeText(value.injuryStatus()),
                sanitizeText(value.cardStatus()),
                sanitizeText(value.lockerRoomStatus()),
                mapList(value.facts(), this::toPublicPrematchFact)
        );
    }

    public PublicPrematchFact toPublicPrematchFact(WorkbenchPlayerFactResponse value) {
        return new PublicPrematchFact(
                value.factId(),
                sanitizeToken(value.factType()),
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

    public PublicPrematchOddsMarket toPublicPrematchOddsMarket(WorkbenchOddsMarketResponse value) {
        return new PublicPrematchOddsMarket(
                value.marketId(),
                sanitizeText(value.bookmaker()),
                sanitizeToken(value.marketCode()),
                sanitizeText(value.marketName()),
                sanitizeToken(value.snapshotType()),
                value.handicapLine(),
                sanitizeText(value.lineValue()),
                value.capturedAt(),
                sanitizeText(value.sourceRef()),
                mapList(value.selections(), this::toPublicPrematchOddsSelection)
        );
    }

    public PublicPrematchOddsSelection toPublicPrematchOddsSelection(WorkbenchOddsSelectionResponse value) {
        return new PublicPrematchOddsSelection(
                value.selectionId(),
                sanitizeToken(value.selectionCode()),
                sanitizeText(value.selectionName()),
                value.oddsValue(),
                value.impliedProbability(),
                sanitizeToken(value.selectionStatus())
        );
    }

    public PublicPrematchSentimentFactor toPublicPrematchSentimentFactor(WorkbenchSentimentFactorResponse value) {
        return new PublicPrematchSentimentFactor(
                value.factorId(),
                value.matchId(),
                sanitizeToken(value.factorCategory()),
                sanitizeToken(value.factorType()),
                sanitizeText(value.title()),
                sanitizeText(value.summary()),
                sanitizeToken(value.impactDirection()),
                sanitizeToken(value.entityType()),
                sanitizeText(value.entityKey()),
                sanitizeText(value.evidenceLevel()),
                sanitizeText(value.sourceName()),
                sanitizeText(value.sourceUrl()),
                sanitizeText(value.sourceRef()),
                value.observedAt(),
                value.expiresAt(),
                value.confidenceScore(),
                value.reliabilityScore(),
                mapList(value.risks(), this::toPublicPrematchSentimentRisk)
        );
    }

    public PublicPrematchSentimentRisk toPublicPrematchSentimentRisk(WorkbenchSentimentRiskResponse value) {
        return new PublicPrematchSentimentRisk(
                value.riskId(),
                sanitizeToken(value.riskType()),
                sanitizeToken(value.riskLevel()),
                value.riskScore(),
                sanitizeText(value.title()),
                sanitizeText(value.rationale()),
                sanitizeText(value.suggestedAction()),
                sanitizeText(value.sourceName()),
                sanitizeText(value.sourceRef())
        );
    }

    public PublicPrematchEvidence toPublicPrematchEvidence(WorkbenchEvidenceResponse value) {
        return new PublicPrematchEvidence(
                value.evidenceId(),
                sanitizeToken(value.sourceType()),
                sanitizeText(value.sourceName()),
                sanitizeText(value.sourceRef()),
                sanitizeText(value.sourceUrl()),
                value.evidenceTime(),
                sanitizeText(value.summary()),
                value.reliabilityScore()
        );
    }

    public PublicPrematchConflict toPublicPrematchConflict(WorkbenchConflictResponse value) {
        return new PublicPrematchConflict(
                value.conflictId(),
                sanitizeToken(value.conflictType()),
                sanitizeText(value.entityKey()),
                sanitizeToken(value.fieldName()),
                sanitizeToken(value.resolutionStatus())
        );
    }

    public PublicPrematchAnalysisReport toPublicPrematchAnalysisReport(WorkbenchAnalysisReportResponse value) {
        return new PublicPrematchAnalysisReport(
                value.reportId(),
                sanitizeText(value.analysisId()),
                sanitizeToken(value.conclusionType()),
                sanitizeToken(value.confidence()),
                sanitizeText(value.riskSummary()),
                sanitizeText(value.recommendedMarkets()),
                sanitizeText(value.dimensions()),
                value.createdAt(),
                value.updatedAt()
        );
    }

    public PublicPrematchIntegrityCheck toPublicPrematchIntegrityCheck(IntegrityCheckResponse value) {
        return new PublicPrematchIntegrityCheck(
                sanitizeToken(value.code()),
                sanitizeText(value.label()),
                sanitizeToken(value.status()),
                sanitizeToken(value.severity()),
                sanitizeText(value.message()),
                value.evidenceCount(),
                value.lastUpdatedAt()
        );
    }

    private <T, R> List<R> mapList(List<T> values, Function<T, R> mapper) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().map(mapper).toList();
    }
}
