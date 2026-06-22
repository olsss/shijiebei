package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.oddscenter.service.OddsCenterQueryService;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMarketDictionary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMarketSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMatchDetail;
import com.worldcup.publicapi.service.PublicApiMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/odds")
public class PublicOddsController {
    private final OddsCenterQueryService queryService;
    private final PublicApiMapper mapper;

    public PublicOddsController(OddsCenterQueryService queryService, PublicApiMapper mapper) {
        this.queryService = queryService;
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<List<PublicOddsMarketSummary>> overview() {
        return ApiResponse.ok(queryService.overview().stream().map(mapper::toPublicOddsMarketSummary).toList());
    }

    @GetMapping("/matches/{matchId}")
    public ApiResponse<PublicOddsMatchDetail> matchOdds(@PathVariable long matchId) {
        return ApiResponse.ok(mapper.toPublicOddsMatchDetail(queryService.matchOdds(matchId)));
    }

    @GetMapping("/bookmakers")
    public ApiResponse<List<String>> bookmakers() {
        return ApiResponse.ok(queryService.bookmakers().stream().map(mapper::sanitizeText).toList());
    }

    @GetMapping("/markets")
    public ApiResponse<List<PublicOddsMarketDictionary>> markets() {
        return ApiResponse.ok(queryService.markets().stream().map(mapper::toPublicOddsMarketDictionary).toList());
    }
}
