package com.worldcup.importreview.repo;

import com.worldcup.importreview.domain.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
}
