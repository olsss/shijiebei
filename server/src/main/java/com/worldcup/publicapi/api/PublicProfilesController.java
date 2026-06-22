package com.worldcup.publicapi.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.profile.service.ProfileQueryService;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPlayerProfileDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicPlayerProfileSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamPlayer;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamProfileDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicTeamProfileSummary;
import com.worldcup.publicapi.service.PublicApiMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/profiles")
public class PublicProfilesController {
    private final ProfileQueryService queryService;
    private final PublicApiMapper mapper;

    public PublicProfilesController(ProfileQueryService queryService, PublicApiMapper mapper) {
        this.queryService = queryService;
        this.mapper = mapper;
    }

    @GetMapping("/teams")
    public ApiResponse<List<PublicTeamProfileSummary>> teams() {
        return ApiResponse.ok(queryService.teams().stream().map(mapper::toPublicTeamProfileSummary).toList());
    }

    @GetMapping("/teams/{teamId}")
    public ApiResponse<PublicTeamProfileDetail> team(@PathVariable long teamId) {
        return ApiResponse.ok(mapper.toPublicTeamProfileDetail(queryService.team(teamId)));
    }

    @GetMapping("/teams/{teamId}/players")
    public ApiResponse<List<PublicTeamPlayer>> teamPlayers(@PathVariable long teamId) {
        return ApiResponse.ok(queryService.teamPlayers(teamId).stream().map(mapper::toPublicTeamPlayer).toList());
    }

    @GetMapping("/players")
    public ApiResponse<List<PublicPlayerProfileSummary>> players() {
        return ApiResponse.ok(queryService.players().stream().map(mapper::toPublicPlayerProfileSummary).toList());
    }

    @GetMapping("/players/{playerId}")
    public ApiResponse<PublicPlayerProfileDetail> player(@PathVariable long playerId) {
        return ApiResponse.ok(mapper.toPublicPlayerProfileDetail(queryService.player(playerId)));
    }
}
