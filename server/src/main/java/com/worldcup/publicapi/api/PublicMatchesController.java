package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchEvent;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchLineup;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchPlayerStats;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicMatchTeamStats;
import com.worldcup.publicapi.service.PublicMatchesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/matches")
public class PublicMatchesController {
    private final PublicMatchesService publicMatchesService;

    public PublicMatchesController(PublicMatchesService publicMatchesService) {
        this.publicMatchesService = publicMatchesService;
    }

    @GetMapping
    public ApiResponse<List<PublicMatchSummary>> matches() {
        return ApiResponse.ok(publicMatchesService.matches());
    }

    @GetMapping("/{matchId}")
    public ApiResponse<PublicMatchDetail> match(@PathVariable long matchId) {
        return ApiResponse.ok(publicMatchesService.match(matchId));
    }

    @GetMapping("/{matchId}/lineups")
    public ApiResponse<List<PublicMatchLineup>> lineups(@PathVariable long matchId) {
        return ApiResponse.ok(publicMatchesService.lineups(matchId));
    }

    @GetMapping("/{matchId}/events")
    public ApiResponse<List<PublicMatchEvent>> events(@PathVariable long matchId) {
        return ApiResponse.ok(publicMatchesService.events(matchId));
    }

    @GetMapping("/{matchId}/team-stats")
    public ApiResponse<List<PublicMatchTeamStats>> teamStats(@PathVariable long matchId) {
        return ApiResponse.ok(publicMatchesService.teamStats(matchId));
    }

    @GetMapping("/{matchId}/player-stats")
    public ApiResponse<List<PublicMatchPlayerStats>> playerStats(@PathVariable long matchId) {
        return ApiResponse.ok(publicMatchesService.playerStats(matchId));
    }
}
