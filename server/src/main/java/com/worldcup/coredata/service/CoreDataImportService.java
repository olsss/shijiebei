package com.worldcup.coredata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.coredata.api.dto.CoreDataImportResponse;
import com.worldcup.coredata.api.dto.CoreDataMappingResponse;
import com.worldcup.importreview.domain.ImportItemStatus;
import com.worldcup.importreview.repo.ImportItemRepository;
import com.worldcup.importreview.service.ImportFileArchiveResult;
import com.worldcup.importreview.service.ImportFileArchiveService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CoreDataImportService {
    private final ImportItemRepository itemRepository;
    private final ObjectMapper objectMapper;
    private final BusinessJsonMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    private final ImportFileArchiveService archiveService;
    private final TransactionTemplate transactionTemplate;

    public CoreDataImportService(ImportItemRepository itemRepository,
                                 ObjectMapper objectMapper,
                                 BusinessJsonMapper mapper,
                                 JdbcTemplate jdbcTemplate,
                                 ImportFileArchiveService archiveService,
                                 PlatformTransactionManager transactionManager) {
        this.itemRepository = itemRepository;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
        this.archiveService = archiveService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public CoreDataImportResponse importItem(long itemId, String actor) {
        CoreImportTransactionResult result = transactionTemplate.execute(status -> importItemInTransaction(itemId, actor));
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "导入事务未返回结果");
        }
        if (!result.archiveAfterCommit()) {
            return new CoreDataImportResponse(result.itemId(), result.status(), result.message(), result.mappings());
        }

        ImportFileArchiveResult archive = archiveService.archiveImported(result.archivePath(), result.relativePath());
        transactionTemplate.executeWithoutResult(status ->
                writeAudit(actor, itemId, "createdMappings=" + result.mappings().size() + "; archive=" + archive.message())
        );
        return new CoreDataImportResponse(
                result.itemId(),
                result.status(),
                "正式业务数据导入完成；" + archive.message(),
                result.mappings()
        );
    }

    private CoreImportTransactionResult importItemInTransaction(long itemId, String actor) {
        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "导入项不存在"));
        if (item.getStatus() != ImportItemStatus.APPROVED || !item.isValidJson()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有已批准且有效的 JSON 可以导入正式库");
        }

        List<CoreDataMappingResponse> existing = mappings(itemId);
        if (!existing.isEmpty()) {
            return new CoreImportTransactionResult(itemId, "IMPORTED", "已导入，返回现有映射", existing, null, null, false);
        }

        try {
            var json = objectMapper.readTree(item.getRawJson());
            List<CoreDataMappingResponse> created = mapper.map(item, json, actor);
            writeAudit(actor, itemId, "createdMappings=" + created.size());
            return new CoreImportTransactionResult(
                    itemId,
                    "IMPORTED",
                    "正式业务数据导入完成",
                    created,
                    item.getJob().getArchivePath(),
                    item.getRelativePath(),
                    true
            );
        } catch (JsonProcessingException cause) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON 原文无法解析", cause);
        }
    }

    @Transactional(readOnly = true)
    public List<CoreDataMappingResponse> mappings(long itemId) {
        return jdbcTemplate.query(
                "SELECT id, import_item_id, target_type, target_id, mapping_status, message FROM import_item_mappings WHERE import_item_id = ? ORDER BY id",
                (rs, rowNum) -> new CoreDataMappingResponse(
                        rs.getLong("id"),
                        rs.getLong("import_item_id"),
                        rs.getString("target_type"),
                        rs.getLong("target_id"),
                        rs.getString("mapping_status"),
                        rs.getString("message")
                ),
                itemId
        );
    }

    private void writeAudit(String actor, long itemId, String detail) {
        jdbcTemplate.update(
                "INSERT INTO audit_logs(actor, action, target_type, target_id, detail) VALUES (?,?,?,?,?)",
                actor == null || actor.isBlank() ? "admin" : actor,
                "IMPORT_CORE_DATA",
                "import_items",
                String.valueOf(itemId),
                detail
        );
    }

    private record CoreImportTransactionResult(
            long itemId,
            String status,
            String message,
            List<CoreDataMappingResponse> mappings,
            String archivePath,
            String relativePath,
            boolean archiveAfterCommit
    ) {
    }
}
