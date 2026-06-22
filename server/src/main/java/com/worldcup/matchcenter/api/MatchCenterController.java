package com.worldcup.matchcenter.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchDetailResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchEventResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchLineupResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchPlayerStatsResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchSummaryResponse;
import com.worldcup.matchcenter.api.dto.MatchCenterDtos.MatchTeamStatsResponse;
import com.worldcup.matchcenter.service.MatchCenterQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchCenterController {
    private final MatchCenterQueryService queryService;

    public MatchCenterController(MatchCenterQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ApiResponse<List<MatchSummaryResponse>> matches() {
        return ApiResponse.ok(queryService.matches());
    }

    @GetMapping("/{matchId}")
    public ApiResponse<MatchDetailResponse> match(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.match(matchId));
    }

    @GetMapping("/{matchId}/lineups")
    public ApiResponse<List<MatchLineupResponse>> lineups(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.lineups(matchId));
    }

    @GetMapping("/{matchId}/events")
    public ApiResponse<List<MatchEventResponse>> events(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.events(matchId));
    }

    @GetMapping("/{matchId}/team-stats")
    public ApiResponse<List<MatchTeamStatsResponse>> teamStats(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.teamStats(matchId));
    }

    @GetMapping("/{matchId}/player-stats")
    public ApiResponse<List<MatchPlayerStatsResponse>> playerStats(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.playerStats(matchId));
    }
}
