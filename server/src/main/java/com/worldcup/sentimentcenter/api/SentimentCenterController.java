package com.worldcup.sentimentcenter.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.sentimentcenter.api.dto.SentimentCenterDtos.SentimentFactorSummaryResponse;
import com.worldcup.sentimentcenter.api.dto.SentimentCenterDtos.SentimentMatchDetailResponse;
import com.worldcup.sentimentcenter.service.SentimentCenterQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sentiment")
@PreAuthorize("hasRole('ADMIN')")
public class SentimentCenterController {
    private final SentimentCenterQueryService queryService;

    public SentimentCenterController(SentimentCenterQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ApiResponse<List<SentimentFactorSummaryResponse>> overview() {
        return ApiResponse.ok(queryService.overview());
    }

    @GetMapping("/matches/{matchId}")
    public ApiResponse<SentimentMatchDetailResponse> matchSentiment(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.matchSentiment(matchId));
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(queryService.categories());
    }

    @GetMapping("/risk-types")
    public ApiResponse<List<String>> riskTypes() {
        return ApiResponse.ok(queryService.riskTypes());
    }
}
