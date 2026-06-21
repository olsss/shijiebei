package com.worldcup.importreview.repo;

import com.worldcup.importreview.domain.ImportItem;
import com.worldcup.importreview.domain.ImportItemStatus;
import com.worldcup.importreview.domain.ImportItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportItemRepository extends JpaRepository<ImportItem, Long> {
    List<ImportItem> findByStatus(ImportItemStatus status);
    List<ImportItem> findByItemType(ImportItemType itemType);
    List<ImportItem> findByStatusAndItemType(ImportItemStatus status, ImportItemType itemType);
}
