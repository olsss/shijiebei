package com.worldcup.coredata.api.dto;

public record CoreDataOverviewResponse(
        long teams,
        long players,
        long matches,
        long analysisReports,
        long bets,
        long oddsSnapshots,
        long evidence,
        long mappings
) {
}
