package com.worldcup.importreview.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "import_jobs")
public class ImportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String archivePath;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(nullable = false)
    private int totalItems;

    @Column(nullable = false)
    private int validItems;

    @Column(nullable = false)
    private int invalidItems;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportItem> items = new ArrayList<>();

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
    public String getArchivePath() { return archivePath; }
    public void setArchivePath(String archivePath) { this.archivePath = archivePath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public int getValidItems() { return validItems; }
    public void setValidItems(int validItems) { this.validItems = validItems; }
    public int getInvalidItems() { return invalidItems; }
    public void setInvalidItems(int invalidItems) { this.invalidItems = invalidItems; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<ImportItem> getItems() { return items; }
    public void addItem(ImportItem item) {
        items.add(item);
        item.setJob(this);
    }
}
