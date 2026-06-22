package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOverviewResponse;
import com.worldcup.publicapi.service.PublicOverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/overview")
public class PublicOverviewController {
    private final PublicOverviewService overviewService;

    public PublicOverviewController(PublicOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping
    public ApiResponse<PublicOverviewResponse> overview() {
        return ApiResponse.ok(overviewService.overview());
    }
}
