package com.worldcup.coredata.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.coredata.api.dto.CoreDataImportResponse;
import com.worldcup.coredata.api.dto.CoreDataMappingResponse;
import com.worldcup.coredata.api.dto.CoreDataOverviewResponse;
import com.worldcup.coredata.service.CoreDataImportService;
import com.worldcup.coredata.service.CoreDataOverviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/core-data")
@PreAuthorize("hasRole('ADMIN')")
public class CoreDataController {
    private final CoreDataOverviewService overviewService;
    private final CoreDataImportService importService;

    public CoreDataController(CoreDataOverviewService overviewService, CoreDataImportService importService) {
        this.overviewService = overviewService;
        this.importService = importService;
    }

    @GetMapping("/overview")
    public ApiResponse<CoreDataOverviewResponse> overview() {
        return ApiResponse.ok(overviewService.overview());
    }

    @PostMapping("/import-items/{itemId}/import")
    public ApiResponse<CoreDataImportResponse> importItem(@PathVariable long itemId, Principal principal) {
        return ApiResponse.ok(importService.importItem(itemId, actor(principal)));
    }

    @GetMapping("/import-items/{itemId}/mappings")
    public ApiResponse<List<CoreDataMappingResponse>> mappings(@PathVariable long itemId) {
        return ApiResponse.ok(importService.mappings(itemId));
    }

    private String actor(Principal principal) {
        return principal == null ? "admin" : principal.getName();
    }
}
