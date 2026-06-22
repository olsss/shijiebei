package com.worldcup.analysisreviewcenter.api;

import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.AnalysisReportDetailResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.AnalysisReportSummaryResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.BetPlanDetailResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.BetPlanSummaryResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.BetRecordResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.OverviewResponse;
import com.worldcup.analysisreviewcenter.api.dto.AnalysisReviewCenterDtos.PostMatchReviewResponse;
import com.worldcup.analysisreviewcenter.service.AnalysisReviewCenterQueryService;
import com.worldcup.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analysis-review")
public class AnalysisReviewCenterController {
    private final AnalysisReviewCenterQueryService queryService;

    public AnalysisReviewCenterController(AnalysisReviewCenterQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewResponse> overview() {
        return ApiResponse.ok(queryService.overview());
    }

    @GetMapping("/reports")
    public ApiResponse<List<AnalysisReportSummaryResponse>> reports() {
        return ApiResponse.ok(queryService.reports());
    }

    @GetMapping("/reports/{reportId}")
    public ApiResponse<AnalysisReportDetailResponse> report(@PathVariable long reportId) {
        return ApiResponse.ok(queryService.report(reportId));
    }

    @GetMapping("/bet-plans")
    public ApiResponse<List<BetPlanSummaryResponse>> betPlans() {
        return ApiResponse.ok(queryService.betPlans());
    }

    @GetMapping("/bet-plans/{planId}")
    public ApiResponse<BetPlanDetailResponse> betPlan(@PathVariable long planId) {
        return ApiResponse.ok(queryService.betPlan(planId));
    }

    @GetMapping("/bets")
    public ApiResponse<List<BetRecordResponse>> bets() {
        return ApiResponse.ok(queryService.bets());
    }

    @GetMapping("/reviews")
    public ApiResponse<List<PostMatchReviewResponse>> reviews() {
        return ApiResponse.ok(queryService.reviews());
    }
}
