package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionReport;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicDecisionReview;
import com.worldcup.publicapi.service.PublicDecisionsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/decisions")
public class PublicDecisionsController {
    private final PublicDecisionsService decisionsService;

    public PublicDecisionsController(PublicDecisionsService decisionsService) {
        this.decisionsService = decisionsService;
    }

    @GetMapping("/reports")
    public ApiResponse<List<PublicDecisionReport>> reports() {
        return ApiResponse.ok(decisionsService.reports());
    }

    @GetMapping("/reviews")
    public ApiResponse<List<PublicDecisionReview>> reviews() {
        return ApiResponse.ok(decisionsService.reviews());
    }
}
