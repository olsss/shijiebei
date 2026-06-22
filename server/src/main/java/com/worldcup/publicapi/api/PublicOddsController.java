package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMarketDictionary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMarketSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMatchDetail;
import com.worldcup.publicapi.service.PublicOddsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/odds")
public class PublicOddsController {
    private final PublicOddsService publicOddsService;

    public PublicOddsController(PublicOddsService publicOddsService) {
        this.publicOddsService = publicOddsService;
    }

    @GetMapping
    public ApiResponse<List<PublicOddsMarketSummary>> overview() {
        return ApiResponse.ok(publicOddsService.overview());
    }

    @GetMapping("/matches/{matchId}")
    public ApiResponse<PublicOddsMatchDetail> matchOdds(@PathVariable long matchId) {
        return ApiResponse.ok(publicOddsService.matchOdds(matchId));
    }

    @GetMapping("/bookmakers")
    public ApiResponse<List<String>> bookmakers() {
        return ApiResponse.ok(publicOddsService.bookmakers());
    }

    @GetMapping("/markets")
    public ApiResponse<List<PublicOddsMarketDictionary>> markets() {
        return ApiResponse.ok(publicOddsService.markets());
    }
}
