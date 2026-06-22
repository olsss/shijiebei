package com.worldcup.profile.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.profile.api.dto.ProfileDtos.CollectionItemResponse;
import com.worldcup.profile.api.dto.ProfileDtos.CollectionItemReviewResponse;
import com.worldcup.profile.api.dto.ProfileDtos.CollectionJobResponse;
import com.worldcup.profile.api.dto.ProfileDtos.CreateCollectionJobRequest;
import com.worldcup.profile.api.dto.ProfileDtos.PlayerProfileDetail;
import com.worldcup.profile.api.dto.ProfileDtos.PlayerProfileSummary;
import com.worldcup.profile.api.dto.ProfileDtos.RejectCollectionItemRequest;
import com.worldcup.profile.api.dto.ProfileDtos.TeamPlayerResponse;
import com.worldcup.profile.api.dto.ProfileDtos.TeamProfileDetail;
import com.worldcup.profile.api.dto.ProfileDtos.TeamProfileSummary;
import com.worldcup.profile.service.ProfileCollectionService;
import com.worldcup.profile.service.ProfileQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private final ProfileCollectionService collectionService;
    private final ProfileQueryService queryService;

    public ProfileController(ProfileCollectionService collectionService, ProfileQueryService queryService) {
        this.collectionService = collectionService;
        this.queryService = queryService;
    }

    @PostMapping("/collections/jobs")
    public ApiResponse<CollectionJobResponse> createCollectionJob(@RequestBody CreateCollectionJobRequest request,
                                                                  Principal principal) {
        return ApiResponse.ok(collectionService.createJob(request, actor(principal)));
    }

    @GetMapping("/collections/items")
    public ApiResponse<List<CollectionItemResponse>> collectionItems(@RequestParam(required = false) String status) {
        return ApiResponse.ok(collectionService.listItems(status));
    }

    @PostMapping("/collections/items/{itemId}/approve")
    public ApiResponse<CollectionItemReviewResponse> approveCollectionItem(@PathVariable long itemId,
                                                                           Principal principal) {
        return ApiResponse.ok(collectionService.approveItem(itemId, actor(principal)));
    }

    @PostMapping("/collections/items/{itemId}/reject")
    public ApiResponse<CollectionItemReviewResponse> rejectCollectionItem(@PathVariable long itemId,
                                                                          @RequestBody(required = false) RejectCollectionItemRequest request,
                                                                          Principal principal) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(collectionService.rejectItem(itemId, reason, actor(principal)));
    }

    @GetMapping("/teams")
    public ApiResponse<List<TeamProfileSummary>> teams() {
        return ApiResponse.ok(queryService.teams());
    }

    @GetMapping("/teams/{teamId}")
    public ApiResponse<TeamProfileDetail> team(@PathVariable long teamId) {
        return ApiResponse.ok(queryService.team(teamId));
    }

    @GetMapping("/teams/{teamId}/players")
    public ApiResponse<List<TeamPlayerResponse>> teamPlayers(@PathVariable long teamId) {
        return ApiResponse.ok(queryService.teamPlayers(teamId));
    }

    @GetMapping("/players")
    public ApiResponse<List<PlayerProfileSummary>> players() {
        return ApiResponse.ok(queryService.players());
    }

    @GetMapping("/players/{playerId}")
    public ApiResponse<PlayerProfileDetail> player(@PathVariable long playerId) {
        return ApiResponse.ok(queryService.player(playerId));
    }

    private String actor(Principal principal) {
        return principal == null ? "admin" : principal.getName();
    }
}
