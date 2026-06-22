package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPlayerProfileDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPlayerProfileSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamPlayer;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamProfileDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamProfileSummary;
import com.worldcup.publicapi.service.PublicProfilesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/profiles")
public class PublicProfilesController {
    private final PublicProfilesService publicProfilesService;

    public PublicProfilesController(PublicProfilesService publicProfilesService) {
        this.publicProfilesService = publicProfilesService;
    }

    @GetMapping("/teams")
    public ApiResponse<List<PublicTeamProfileSummary>> teams() {
        return ApiResponse.ok(publicProfilesService.teams());
    }

    @GetMapping("/teams/{teamId}")
    public ApiResponse<PublicTeamProfileDetail> team(@PathVariable long teamId) {
        return ApiResponse.ok(publicProfilesService.team(teamId));
    }

    @GetMapping("/teams/{teamId}/players")
    public ApiResponse<List<PublicTeamPlayer>> teamPlayers(@PathVariable long teamId) {
        return ApiResponse.ok(publicProfilesService.teamPlayers(teamId));
    }

    @GetMapping("/players")
    public ApiResponse<List<PublicPlayerProfileSummary>> players() {
        return ApiResponse.ok(publicProfilesService.players());
    }

    @GetMapping("/players/{playerId}")
    public ApiResponse<PublicPlayerProfileDetail> player(@PathVariable long playerId) {
        return ApiResponse.ok(publicProfilesService.player(playerId));
    }
}
