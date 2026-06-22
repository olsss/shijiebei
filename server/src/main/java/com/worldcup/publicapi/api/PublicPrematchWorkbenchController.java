package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchIntegrityCheck;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchMatchSummary;
import com.worldcup.publicapi.service.PublicPrematchWorkbenchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/prematch-workbench")
public class PublicPrematchWorkbenchController {
    private final PublicPrematchWorkbenchService workbenchService;

    public PublicPrematchWorkbenchController(PublicPrematchWorkbenchService workbenchService) {
        this.workbenchService = workbenchService;
    }

    @GetMapping("/matches")
    public ApiResponse<List<PublicPrematchMatchSummary>> matches() {
        return ApiResponse.ok(workbenchService.matches());
    }

    @GetMapping("/matches/{matchId}")
    public ApiResponse<PublicPrematchDetail> match(@PathVariable long matchId) {
        return ApiResponse.ok(workbenchService.match(matchId));
    }

    @GetMapping("/matches/{matchId}/integrity")
    public ApiResponse<List<PublicPrematchIntegrityCheck>> integrity(@PathVariable long matchId) {
        return ApiResponse.ok(workbenchService.integrity(matchId));
    }
}
