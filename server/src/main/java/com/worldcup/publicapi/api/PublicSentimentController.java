package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentFactorSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicSentimentMatchDetail;
import com.worldcup.publicapi.service.PublicApiMapper;
import com.worldcup.sentimentcenter.service.SentimentCenterQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/sentiment")
public class PublicSentimentController {
    private final SentimentCenterQueryService queryService;
    private final PublicApiMapper mapper;

    public PublicSentimentController(SentimentCenterQueryService queryService, PublicApiMapper mapper) {
        this.queryService = queryService;
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<List<PublicSentimentFactorSummary>> overview() {
        return ApiResponse.ok(queryService.overview().stream().map(mapper::toPublicSentimentFactorSummary).toList());
    }

    @GetMapping("/matches/{matchId}")
    public ApiResponse<PublicSentimentMatchDetail> matchSentiment(@PathVariable long matchId) {
        return ApiResponse.ok(mapper.toPublicSentimentMatchDetail(queryService.matchSentiment(matchId)));
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(queryService.categories().stream().map(mapper::sanitizeText).toList());
    }

    @GetMapping("/risk-types")
    public ApiResponse<List<String>> riskTypes() {
        return ApiResponse.ok(queryService.riskTypes().stream().map(mapper::sanitizeText).toList());
    }
}
