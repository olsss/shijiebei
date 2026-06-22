package com.worldcup.importreview.api;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.importreview.api.dto.BatchApproveRequest;
import com.worldcup.importreview.api.dto.ImportItemDetailResponse;
import com.worldcup.importreview.api.dto.ImportItemResponse;
import com.worldcup.importreview.api.dto.ImportJobResponse;
import com.worldcup.importreview.api.dto.RejectImportItemRequest;
import com.worldcup.importreview.api.dto.ScanArchiveRequest;
import com.worldcup.importreview.domain.ImportItemStatus;
import com.worldcup.importreview.domain.ImportItemType;
import com.worldcup.importreview.service.JsonImportReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
public class ImportReviewController {
    private final JsonImportReviewService service;

    public ImportReviewController(JsonImportReviewService service) {
        this.service = service;
    }

    @PostMapping("/import-jobs/scan")
    public ApiResponse<ImportJobResponse> scan(@RequestBody(required = false) ScanArchiveRequest request) {
        Path archivePath = request == null || request.archivePath() == null || request.archivePath().isBlank()
                ? null
                : Path.of(request.archivePath());
        return ApiResponse.ok(service.scanAndPersist(archivePath));
    }

    @GetMapping("/import-jobs/{jobId}")
    public ApiResponse<ImportJobResponse> getJob(@PathVariable long jobId) {
        return ApiResponse.ok(service.getJob(jobId));
    }

    @GetMapping("/import-items")
    public ApiResponse<List<ImportItemResponse>> listItems(@RequestParam(required = false) ImportItemStatus status,
                                                           @RequestParam(required = false) ImportItemType type) {
        return ApiResponse.ok(service.listItems(status, type));
    }

    @GetMapping("/import-items/{itemId}")
    public ApiResponse<ImportItemDetailResponse> getItem(@PathVariable long itemId) {
        return ApiResponse.ok(service.getItem(itemId));
    }

    @PostMapping("/import-items/{itemId}/approve")
    public ApiResponse<ImportItemResponse> approve(@PathVariable long itemId, Principal principal) {
        return ApiResponse.ok(service.approve(itemId, actor(principal)));
    }

    @PostMapping("/import-items/batch-approve")
    public ApiResponse<List<ImportItemResponse>> batchApprove(@RequestBody BatchApproveRequest request, Principal principal) {
        return ApiResponse.ok(service.batchApprove(request.itemIds(), actor(principal)));
    }

    @PostMapping("/import-items/{itemId}/reject")
    public ApiResponse<ImportItemResponse> reject(@PathVariable long itemId,
                                                  @RequestBody RejectImportItemRequest request,
                                                  Principal principal) {
        return ApiResponse.ok(service.reject(itemId, request.reason(), actor(principal)));
    }

    private String actor(Principal principal) {
        return principal == null ? "admin" : principal.getName();
    }
}
