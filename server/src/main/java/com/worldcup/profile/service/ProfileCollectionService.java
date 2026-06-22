package com.worldcup.profile.service;

import com.worldcup.profile.api.dto.ProfileDtos.CollectionItemResponse;
import com.worldcup.profile.api.dto.ProfileDtos.CollectionItemReviewResponse;
import com.worldcup.profile.api.dto.ProfileDtos.CollectionJobResponse;
import com.worldcup.profile.api.dto.ProfileDtos.CreateCollectionJobRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Locale;

@Service
public class ProfileCollectionService {
    private final JdbcTemplate jdbcTemplate;

    public ProfileCollectionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public CollectionJobResponse createJob(CreateCollectionJobRequest request, String actor) {
        String sourceType = fallback(request.sourceType(), "MANUAL");
        String sourceName = fallback(request.sourceName(), "AI/Codex");
        Long jobId = insertAndReturnId(
                "INSERT INTO collection_jobs(source_type, source_name, keyword, status, triggered_by, message, total_items, pending_items) VALUES (?,?,?,?,?,?,?,?)",
                sourceType,
                sourceName,
                request.keyword(),
                "COMPLETED",
                actorOrDefault(actor),
                "手动创建采集任务",
                1,
                1
        );
        insertAndReturnId(
                "INSERT INTO collection_items(job_id, entity_type, entity_key, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score, source_name, source_url, source_ref, raw_payload, status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                jobId,
                upperOrDefault(request.entityType(), "TEAM"),
                required(request.entityKey(), "entityKey 不能为空"),
                upperOrDefault(request.factType(), "RAW"),
                request.periodKey(),
                required(request.title(), "title 不能为空"),
                required(request.summary(), "summary 不能为空"),
                request.sentimentLabel(),
                request.confidenceScore(),
                request.reliabilityScore(),
                sourceName,
                request.sourceUrl(),
                request.sourceRef(),
                request.rawPayload(),
                "PENDING_REVIEW"
        );
        return jdbcTemplate.queryForObject(
                "SELECT id, source_type, source_name, keyword, status, total_items, pending_items, approved_items, rejected_items FROM collection_jobs WHERE id = ?",
                jobMapper(),
                jobId
        );
    }

    @Transactional(readOnly = true)
    public List<CollectionItemResponse> listItems(String status) {
        if (status == null || status.isBlank()) {
            return jdbcTemplate.query(collectionItemSelect() + " ORDER BY id DESC", itemMapper());
        }
        return jdbcTemplate.query(collectionItemSelect() + " WHERE status = ? ORDER BY id DESC", itemMapper(), status);
    }

    @Transactional
    public CollectionItemReviewResponse approveItem(long itemId, String actor) {
        CollectionItemResponse item = findItem(itemId);
        if ("APPROVED".equals(item.status()) && item.targetType() != null && item.targetId() != null) {
            return new CollectionItemReviewResponse(item.id(), item.status(), item.targetType(), item.targetId(), "采集项已批准，返回既有画像事实");
        }
        if (!"PENDING_REVIEW".equals(item.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有待审核采集项可以批准");
        }

        CollectionItemReviewResponse response = switch (item.entityType()) {
            case "TEAM" -> approveTeamFact(item, actorOrDefault(actor));
            case "PLAYER" -> approvePlayerFact(item, actorOrDefault(actor));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的画像实体类型: " + item.entityType());
        };
        updateJobCounts(item.jobId());
        writeAudit(actor, "APPROVE_PROFILE_COLLECTION_ITEM", "collection_items", String.valueOf(itemId), response.targetType());
        return response;
    }

    @Transactional
    public CollectionItemReviewResponse rejectItem(long itemId, String reason, String actor) {
        CollectionItemResponse item = findItem(itemId);
        if (!"PENDING_REVIEW".equals(item.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有待审核采集项可以驳回");
        }
        jdbcTemplate.update(
                "UPDATE collection_items SET status='REJECTED', review_note=?, reviewed_by=?, reviewed_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                reason == null || reason.isBlank() ? "未填写原因" : reason,
                actorOrDefault(actor),
                itemId
        );
        updateJobCounts(item.jobId());
        writeAudit(actor, "REJECT_PROFILE_COLLECTION_ITEM", "collection_items", String.valueOf(itemId), reason);
        return new CollectionItemReviewResponse(itemId, "REJECTED", null, null, "采集项已驳回");
    }

    private CollectionItemReviewResponse approveTeamFact(CollectionItemResponse item, String actor) {
        Long teamId = findEntityId("SELECT id FROM teams WHERE team_key = ?", item.entityKey(), "球队不存在: " + item.entityKey());
        Long factId = insertAndReturnId(
                "INSERT INTO team_profile_facts(team_id, collection_item_id, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score, source_name, source_url, source_ref, captured_at, approved_by, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,(SELECT raw_payload FROM collection_items WHERE id=?))",
                teamId,
                item.id(),
                item.factType(),
                item.periodKey(),
                item.title(),
                item.summary(),
                item.sentimentLabel(),
                item.confidenceScore(),
                item.reliabilityScore(),
                item.sourceName(),
                item.sourceUrl(),
                item.sourceRef(),
                item.capturedAt(),
                actor,
                item.id()
        );
        markApproved(item.id(), "TEAM_PROFILE_FACT", factId, actor);
        return new CollectionItemReviewResponse(item.id(), "APPROVED", "TEAM_PROFILE_FACT", factId, "球队画像事实已写入正式库");
    }

    private CollectionItemReviewResponse approvePlayerFact(CollectionItemResponse item, String actor) {
        Long playerId = findEntityId("SELECT id FROM players WHERE player_key = ?", item.entityKey(), "球员不存在: " + item.entityKey());
        Long factId = insertAndReturnId(
                "INSERT INTO player_profile_facts(player_id, collection_item_id, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score, source_name, source_url, source_ref, captured_at, approved_by, raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,(SELECT raw_payload FROM collection_items WHERE id=?))",
                playerId,
                item.id(),
                item.factType(),
                item.periodKey(),
                item.title(),
                item.summary(),
                item.sentimentLabel(),
                item.confidenceScore(),
                item.reliabilityScore(),
                item.sourceName(),
                item.sourceUrl(),
                item.sourceRef(),
                item.capturedAt(),
                actor,
                item.id()
        );
        markApproved(item.id(), "PLAYER_PROFILE_FACT", factId, actor);
        return new CollectionItemReviewResponse(item.id(), "APPROVED", "PLAYER_PROFILE_FACT", factId, "球员画像事实已写入正式库");
    }

    private CollectionItemResponse findItem(long itemId) {
        List<CollectionItemResponse> items = jdbcTemplate.query(collectionItemSelect() + " WHERE id = ?", itemMapper(), itemId);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "采集项不存在");
        }
        return items.get(0);
    }

    private Long findEntityId(String sql, String key, String message) {
        List<Long> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), key);
        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return ids.get(0);
    }

    private void markApproved(long itemId, String targetType, long targetId, String actor) {
        jdbcTemplate.update(
                "UPDATE collection_items SET status='APPROVED', reviewed_by=?, reviewed_at=CURRENT_TIMESTAMP, target_type=?, target_id=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                actor,
                targetType,
                targetId,
                itemId
        );
    }

    private void updateJobCounts(long jobId) {
        jdbcTemplate.update(
                "UPDATE collection_jobs SET pending_items=(SELECT COUNT(*) FROM collection_items WHERE job_id=? AND status='PENDING_REVIEW'), approved_items=(SELECT COUNT(*) FROM collection_items WHERE job_id=? AND status='APPROVED'), rejected_items=(SELECT COUNT(*) FROM collection_items WHERE job_id=? AND status='REJECTED'), updated_at=CURRENT_TIMESTAMP WHERE id=?",
                jobId,
                jobId,
                jobId,
                jobId
        );
    }

    private String collectionItemSelect() {
        return "SELECT id, job_id, entity_type, entity_key, fact_type, period_key, title, summary, sentiment_label, confidence_score, reliability_score, source_name, source_url, source_ref, captured_at, status, review_note, reviewed_by, reviewed_at, target_type, target_id FROM collection_items";
    }

    private RowMapper<CollectionItemResponse> itemMapper() {
        return (rs, rowNum) -> new CollectionItemResponse(
                rs.getLong("id"),
                rs.getLong("job_id"),
                rs.getString("entity_type"),
                rs.getString("entity_key"),
                rs.getString("fact_type"),
                rs.getString("period_key"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("sentiment_label"),
                rs.getBigDecimal("confidence_score"),
                rs.getBigDecimal("reliability_score"),
                rs.getString("source_name"),
                rs.getString("source_url"),
                rs.getString("source_ref"),
                rs.getTimestamp("captured_at") == null ? null : rs.getTimestamp("captured_at").toLocalDateTime(),
                rs.getString("status"),
                rs.getString("review_note"),
                rs.getString("reviewed_by"),
                rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toLocalDateTime(),
                rs.getString("target_type"),
                rs.getObject("target_id") == null ? null : rs.getLong("target_id")
        );
    }

    private RowMapper<CollectionJobResponse> jobMapper() {
        return (rs, rowNum) -> new CollectionJobResponse(
                rs.getLong("id"),
                rs.getString("source_type"),
                rs.getString("source_name"),
                rs.getString("keyword"),
                rs.getString("status"),
                rs.getInt("total_items"),
                rs.getInt("pending_items"),
                rs.getInt("approved_items"),
                rs.getInt("rejected_items")
        );
    }

    private Long insertAndReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("数据库未返回新增记录 ID");
        }
        return key.longValue();
    }

    private void writeAudit(String actor, String action, String targetType, String targetId, String detail) {
        jdbcTemplate.update(
                "INSERT INTO audit_logs(actor, action, target_type, target_id, detail) VALUES (?,?,?,?,?)",
                actorOrDefault(actor),
                action,
                targetType,
                targetId,
                detail == null ? "" : detail
        );
    }

    private String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "admin" : actor;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String upperOrDefault(String value, String fallback) {
        return fallback(value, fallback).trim().toUpperCase(Locale.ROOT);
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }
}
