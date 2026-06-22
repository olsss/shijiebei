package com.worldcup.coredata.service;

import com.worldcup.coredata.api.dto.CoreDataOverviewResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoreDataOverviewService {
    private final JdbcTemplate jdbcTemplate;

    public CoreDataOverviewService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public CoreDataOverviewResponse overview() {
        return new CoreDataOverviewResponse(
                count("teams"),
                count("players"),
                count("matches"),
                count("analysis_reports"),
                count("bets"),
                count("odds_snapshots"),
                count("source_evidence"),
                count("import_item_mappings")
        );
    }

    private long count(String table) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return value == null ? 0 : value;
    }
}
