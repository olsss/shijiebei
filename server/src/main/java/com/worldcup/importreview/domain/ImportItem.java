package com.worldcup.importreview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_items")
public class ImportItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJob job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ImportItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ImportItemStatus status;

    @Column(nullable = false, length = 1000)
    private String relativePath;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false, length = 500)
    private String summaryTitle;

    @Column(nullable = false)
    private boolean validJson;

    @Column(nullable = false, length = 1000)
    private String validationMessage;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String rawJson;

    @Column(length = 1000)
    private String rejectionReason;

    @Column(length = 120)
    private String reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public ImportJob getJob() { return job; }
    public void setJob(ImportJob job) { this.job = job; }
    public ImportItemType getItemType() { return itemType; }
    public void setItemType(ImportItemType itemType) { this.itemType = itemType; }
    public ImportItemStatus getStatus() { return status; }
    public void setStatus(ImportItemStatus status) { this.status = status; }
    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getSummaryTitle() { return summaryTitle; }
    public void setSummaryTitle(String summaryTitle) { this.summaryTitle = summaryTitle; }
    public boolean isValidJson() { return validJson; }
    public void setValidJson(boolean validJson) { this.validJson = validJson; }
    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}


