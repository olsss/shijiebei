package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentFactorSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentMatchDetail;
import com.worldcup.publicapi.service.PublicSentimentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/sentiment")
public class PublicSentimentController {
    private final PublicSentimentService publicSentimentService;

    public PublicSentimentController(PublicSentimentService publicSentimentService) {
        this.publicSentimentService = publicSentimentService;
    }

    @GetMapping
    public ApiResponse<List<PublicSentimentFactorSummary>> overview() {
        return ApiResponse.ok(publicSentimentService.overview());
    }

    @GetMapping("/matches/{matchId}")
    public ApiResponse<PublicSentimentMatchDetail> matchSentiment(@PathVariable long matchId) {
        return ApiResponse.ok(publicSentimentService.matchSentiment(matchId));
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(publicSentimentService.categories());
    }

    @GetMapping("/risk-types")
    public ApiResponse<List<String>> riskTypes() {
        return ApiResponse.ok(publicSentimentService.riskTypes());
    }
}
