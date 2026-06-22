package com.worldcup.oddscenter.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class OddsCenterDtos {
    private OddsCenterDtos() {
    }

    public record OddsMarketSummaryResponse(
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
    ) {
    }

    public record OddsMatchDetailResponse(
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            List<OddsMarketDetailResponse> markets
    ) {
    }

    public record OddsMarketDetailResponse(
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
            String sourceRef,
            String rawPayload,
            List<OddsSelectionResponse> selections
    ) {
    }

    public record OddsSelectionResponse(
            Long id,
            Long marketSnapshotId,
            String selectionCode,
            String selectionName,
            BigDecimal oddsValue,
            BigDecimal impliedProbability,
            String selectionStatus,
            String rawPayload
    ) {
    }

    public record OddsMarketDictionaryResponse(
            String marketCode,
            String marketName
    ) {
    }
}
