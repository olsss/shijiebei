package com.worldcup.sentimentcenter.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SentimentCenterDtos {
    private SentimentCenterDtos() {
    }

    public record SentimentFactorSummaryResponse(
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
    ) {
    }

    public record SentimentMatchDetailResponse(
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            List<SentimentFactorDetailResponse> factors,
            List<SentimentRiskResponse> risks
    ) {
    }

    public record SentimentFactorDetailResponse(
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
            String rawPayload
    ) {
    }

    public record SentimentRiskResponse(
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
            String sourceRef,
            String rawPayload
    ) {
    }
}
