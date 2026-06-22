package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.matchcenter.service.MatchCenterQueryService;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchEvent;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchLineup;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchPlayerStats;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchTeamStats;
import com.worldcup.publicapi.service.PublicApiMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/matches")
public class PublicMatchesController {
    private final MatchCenterQueryService queryService;
    private final PublicApiMapper mapper;

    public PublicMatchesController(MatchCenterQueryService queryService, PublicApiMapper mapper) {
        this.queryService = queryService;
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<List<PublicMatchSummary>> matches() {
        return ApiResponse.ok(queryService.matches().stream().map(mapper::toPublicMatchSummary).toList());
    }

    @GetMapping("/{matchId}")
    public ApiResponse<PublicMatchDetail> match(@PathVariable long matchId) {
        return ApiResponse.ok(mapper.toPublicMatchDetail(queryService.match(matchId)));
    }

    @GetMapping("/{matchId}/lineups")
    public ApiResponse<List<PublicMatchLineup>> lineups(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.lineups(matchId).stream().map(mapper::toPublicMatchLineup).toList());
    }

    @GetMapping("/{matchId}/events")
    public ApiResponse<List<PublicMatchEvent>> events(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.events(matchId).stream().map(mapper::toPublicMatchEvent).toList());
    }

    @GetMapping("/{matchId}/team-stats")
    public ApiResponse<List<PublicMatchTeamStats>> teamStats(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.teamStats(matchId).stream().map(mapper::toPublicMatchTeamStats).toList());
    }

    @GetMapping("/{matchId}/player-stats")
    public ApiResponse<List<PublicMatchPlayerStats>> playerStats(@PathVariable long matchId) {
        return ApiResponse.ok(queryService.playerStats(matchId).stream().map(mapper::toPublicMatchPlayerStats).toList());
    }
}
