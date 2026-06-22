package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.prematchworkbench.service.PrematchWorkbenchQueryService;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchIntegrityCheck;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPrematchMatchSummary;
import com.worldcup.publicapi.service.PublicApiMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/prematch-workbench")
public class PublicPrematchWorkbenchController {
    private final PrematchWorkbenchQueryService queryService;
    private final PublicApiMapper mapper;

    public PublicPrematchWorkbenchController(PrematchWorkbenchQueryService queryService, PublicApiMapper mapper) {
        this.queryService = queryService;
        this.mapper = mapper;
    }

    @GetMapping("/matches")
    public ApiResponse<List<PublicPrematchMatchSummary>> matches() {
        return ApiResponse.ok(queryService.matches().stream().map(mapper::toPublicPrematchMatchSummary).toList());
    }

    @GetMapping("/matches/{matchId}")
    public ApiResponse<PublicPrematchDetail> match(@PathVariable long matchId) {
        return ApiResponse.ok(mapper.toPublicPrematchDetail(queryService.match(matchId)));
    }

    @GetMapping("/matches/{matchId}/integrity")
    public ApiResponse<List<PublicPrematchIntegrityCheck>> integrity(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.integrity(matchId).stream().map(mapper::toPublicPrematchIntegrityCheck).toList());
    }
}
