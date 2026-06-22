package com.worldcup.prematchworkbench.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.IntegrityCheckResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.PrematchWorkbenchDetailResponse;
import com.worldcup.prematchworkbench.api.dto.PrematchWorkbenchDtos.WorkbenchMatchSummaryResponse;
import com.worldcup.prematchworkbench.service.PrematchWorkbenchQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prematch-workbench")
public class PrematchWorkbenchController {
    private final PrematchWorkbenchQueryService queryService;

    public PrematchWorkbenchController(PrematchWorkbenchQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/matches")
    public ApiResponse<List<WorkbenchMatchSummaryResponse>> matches() {
        return ApiResponse.ok(queryService.matches());
    }

    @GetMapping("/matches/{matchId}")
    public ApiResponse<PrematchWorkbenchDetailResponse> match(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.match(matchId));
    }

    @GetMapping("/matches/{matchId}/integrity")
    public ApiResponse<List<IntegrityCheckResponse>> integrity(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.integrity(matchId));
    }
}
