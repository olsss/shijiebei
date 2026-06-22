package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.analysisreviewcenter.service.AnalysisReviewCenterQueryService;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionReport;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionReview;
import com.worldcup.publicapi.service.PublicApiMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/decisions")
public class PublicDecisionsController {
    private final AnalysisReviewCenterQueryService queryService;
    private final PublicApiMapper mapper;

    public PublicDecisionsController(AnalysisReviewCenterQueryService queryService, PublicApiMapper mapper) {
        this.queryService = queryService;
        this.mapper = mapper;
    }

    @GetMapping("/reports")
    public ApiResponse<List<PublicDecisionReport>> reports() {
        return ApiResponse.ok(queryService.reports().stream().map(mapper::toPublicDecisionReport).toList());
    }

    @GetMapping("/reviews")
    public ApiResponse<List<PublicDecisionReview>> reviews() {
        return ApiResponse.ok(queryService.reviews().stream().map(mapper::toPublicDecisionReview).toList());
    }
}
