package com.worldcup.importreview.service;

import com.worldcup.config.AppProperties;
import com.worldcup.importreview.api.dto.ImportItemDetailResponse;
import com.worldcup.importreview.api.dto.ImportItemResponse;
import com.worldcup.importreview.api.dto.ImportJobResponse;
import com.worldcup.importreview.domain.ImportItem;
import com.worldcup.importreview.domain.ImportItemStatus;
import com.worldcup.importreview.domain.ImportItemType;
import com.worldcup.importreview.domain.ImportJob;
import com.worldcup.importreview.repo.ImportItemRepository;
import com.worldcup.importreview.repo.ImportJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class JsonImportReviewService {
    private final JsonArchiveScanner scanner;
    private final ImportJobRepository jobRepository;
    private final ImportItemRepository itemRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AppProperties appProperties;
    private final ImportFileArchiveService archiveService;

    public JsonImportReviewService(JsonArchiveScanner scanner,
                                   ImportJobRepository jobRepository,
                                   ImportItemRepository itemRepository,
                                   JdbcTemplate jdbcTemplate,
                                   AppProperties appProperties,
                                   ImportFileArchiveService archiveService) {
        this.scanner = scanner;
        this.jobRepository = jobRepository;
        this.itemRepository = itemRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.appProperties = appProperties;
        this.archiveService = archiveService;
    }

    @Transactional
    public ImportJobResponse scanAndPersist(Path archivePath) {
        ArchiveScanResult result = scanner.scan(archivePath == null ? Path.of(appProperties.getArchivePath()) : archivePath);
        return persistScan(result, false, "admin");
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getJob(long jobId) {
        return toJobResponse(jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "导入任务不存在")));
    }

    @Transactional(readOnly = true)
    public List<ImportItemResponse> listItems(ImportItemStatus status, ImportItemType type) {
        List<ImportItem> items;
        if (status != null && type != null) {
            items = itemRepository.findByStatusAndItemType(status, type);
        } else if (status != null) {
            items = itemRepository.findByStatus(status);
        } else if (type != null) {
            items = itemRepository.findByItemType(type);
        } else {
            items = itemRepository.findAll();
        }
        return items.stream().map(this::toItemResponse).toList();
    }

    @Transactional(readOnly = true)
    public ImportItemDetailResponse getItem(long itemId) {
        ImportItem item = findItem(itemId);
        return new ImportItemDetailResponse(toItemResponse(item), item.getRawJson(), item.getRejectionReason());
    }

    @Transactional
    public ImportItemResponse approve(long itemId, String actor) {
        ImportItem item = findItem(itemId);
        if (!item.isValidJson()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效 JSON 不能批准入库");
        }
        item.setStatus(ImportItemStatus.APPROVED);
        item.setReviewedBy(actor);
        item.setReviewedAt(LocalDateTime.now());
        item.setRejectionReason(null);
        writeAudit(actor, "APPROVE_IMPORT_ITEM", "import_item", String.valueOf(itemId), item.getRelativePath());
        return toItemResponse(itemRepository.save(item));
    }

    @Transactional
    public List<ImportItemResponse> batchApprove(List<Long> itemIds, String actor) {
        return itemIds.stream().map(id -> approve(id, actor)).toList();
    }

    @Transactional
    public ImportItemResponse reject(long itemId, String reason, String actor) {
        ImportItem item = findItem(itemId);
        item.setStatus(ImportItemStatus.REJECTED);
        item.setReviewedBy(actor);
        item.setReviewedAt(LocalDateTime.now());
        item.setRejectionReason(reason == null || reason.isBlank() ? "未填写原因" : reason);
        writeAudit(actor, "REJECT_IMPORT_ITEM", "import_item", String.valueOf(itemId), item.getRejectionReason());
        ImportItem saved = itemRepository.save(item);
        String archivePath = saved.getJob().getArchivePath();
        String relativePath = saved.getRelativePath();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ImportFileArchiveResult archive = archiveService.archiveRejected(archivePath, relativePath);
                writeAudit(actor, "ARCHIVE_REJECTED_IMPORT_FILE", "import_item", String.valueOf(itemId), archive.message());
            }
        });
        return toItemResponse(saved);
    }

    @Transactional(readOnly = true)
    public ArchiveScanResult dryRun(Path archivePath) {
        return scanner.scan(archivePath);
    }

    @Transactional
    public ImportJobResponse approveRun(Path archivePath, String actor) {
        ArchiveScanResult result = scanner.scan(archivePath);
        return persistScan(result, true, actor);
    }

    private ImportJobResponse persistScan(ArchiveScanResult result, boolean autoApprove, String actor) {
        ImportJob job = new ImportJob();
        job.setArchivePath(result.archivePath());
        job.setStatus("SCANNED");
        job.setTotalItems(result.totalItems());
        job.setValidItems(result.validItems());
        job.setInvalidItems(result.invalidItems());
        job.setMessage("扫描完成");

        for (ArchiveScanCandidate candidate : result.candidates()) {
            ImportItem item = new ImportItem();
            item.setItemType(candidate.type());
            item.setStatus(autoApprove && candidate.validJson() ? ImportItemStatus.APPROVED : ImportItemStatus.PENDING_REVIEW);
            item.setRelativePath(candidate.relativePath());
            item.setSha256(candidate.sha256());
            item.setSummaryTitle(candidate.summaryTitle());
            item.setValidJson(candidate.validJson());
            item.setValidationMessage(candidate.validationMessage());
            item.setRawJson(candidate.rawJson());
            if (autoApprove && candidate.validJson()) {
                item.setReviewedBy(actor);
                item.setReviewedAt(LocalDateTime.now());
            }
            job.addItem(item);
        }

        ImportJob saved = jobRepository.save(job);
        writeAudit(actor, autoApprove ? "APPROVE_IMPORT_JOB" : "SCAN_IMPORT_JOB", "import_job", String.valueOf(saved.getId()), saved.getArchivePath());
        return toJobResponse(saved);
    }

    private ImportItem findItem(long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "导入条目不存在"));
    }

    private ImportJobResponse toJobResponse(ImportJob job) {
        return new ImportJobResponse(job.getId(), job.getArchivePath(), job.getStatus(), job.getTotalItems(), job.getValidItems(), job.getInvalidItems(), job.getMessage());
    }

    private ImportItemResponse toItemResponse(ImportItem item) {
        return new ImportItemResponse(
                item.getId(),
                item.getJob().getId(),
                item.getItemType().name(),
                item.getStatus().name(),
                item.getRelativePath(),
                item.getSha256(),
                item.getSummaryTitle(),
                item.isValidJson(),
                item.getValidationMessage()
        );
    }

    private void writeAudit(String actor, String action, String targetType, String targetId, String detail) {
        jdbcTemplate.update(
                "INSERT INTO audit_logs(actor, action, target_type, target_id, detail) VALUES (?, ?, ?, ?, ?)",
                actor == null || actor.isBlank() ? "admin" : actor,
                action,
                targetType,
                targetId,
                detail == null ? "" : detail
        );
    }
}
