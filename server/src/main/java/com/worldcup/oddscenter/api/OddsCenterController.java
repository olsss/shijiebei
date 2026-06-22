package com.worldcup.oddscenter.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.oddscenter.api.dto.OddsCenterDtos.OddsMarketDictionaryResponse;
import com.worldcup.oddscenter.api.dto.OddsCenterDtos.OddsMarketSummaryResponse;
import com.worldcup.oddscenter.api.dto.OddsCenterDtos.OddsMatchDetailResponse;
import com.worldcup.oddscenter.service.OddsCenterQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/odds")
@PreAuthorize("hasRole('ADMIN')")
public class OddsCenterController {
    private final OddsCenterQueryService queryService;

    public OddsCenterController(OddsCenterQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ApiResponse<List<OddsMarketSummaryResponse>> overview() {
        return ApiResponse.ok(queryService.overview());
    }

    @GetMapping("/matches/{matchId}")
    public ApiResponse<OddsMatchDetailResponse> matchOdds(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.matchOdds(matchId));
    }

    @GetMapping("/bookmakers")
    public ApiResponse<List<String>> bookmakers() {
        return ApiResponse.ok(queryService.bookmakers());
    }

    @GetMapping("/markets")
    public ApiResponse<List<OddsMarketDictionaryResponse>> markets() {
        return ApiResponse.ok(queryService.markets());
    }
}
