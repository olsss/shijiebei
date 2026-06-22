package com.worldcup.publicapi.dto;

import java.time.LocalDate;

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
}