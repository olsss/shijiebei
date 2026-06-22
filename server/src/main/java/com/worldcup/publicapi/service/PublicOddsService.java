package com.worldcup.publicapi.service;

import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMarketDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMarketDictionary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMarketSummary;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsMatchDetail;
import com.worldcup.publicapi.dto.PublicApiDtos.PublicOddsSelection;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PublicOddsService {
    private final JdbcTemplate jdbcTemplate;
    private final PublicApiMapper mapper;

    public PublicOddsService(JdbcTemplate jdbcTemplate, PublicApiMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PublicOddsMarketSummary> overview() {
        return jdbcTemplate.query(
                marketSummarySelect() + " ORDER BY m.matchday DESC, oms.captured_at DESC, oms.id DESC",
                (rs, rowNum) -> new PublicOddsMarketSummary(
                        rs.getLong("id"),
                        nullableLong(rs, "match_id"),
                        mapper.sanitizeText(rs.getString("match_name")),
                        localDate(rs, "matchday"),
                        mapper.sanitizeText(rs.getString("jc_code")),
                        mapper.sanitizeText(rs.getString("bookmaker")),
                        mapper.sanitizeToken(rs.getString("market_code")),
                        mapper.sanitizeText(rs.getString("market_name")),
                        mapper.sanitizeToken(rs.getString("snapshot_type")),
                        rs.getBigDecimal("handicap_line"),
                        mapper.sanitizeText(rs.getString("line_value")),
                        localDateTime(rs, "captured_at"),
                        rs.getLong("selection_count")
                )
        );
    }

    @Transactional(readOnly = true)
    public PublicOddsMatchDetail matchOdds(long matchId) {
        MatchRow match = findMatch(matchId);
        List<MarketRow> marketRows = marketsForMatch(matchId);
        Map<Long, List<PublicOddsSelection>> selectionsByMarketId = selectionsByMarketIds(
                marketRows.stream().map(MarketRow::id).toList()
        );
        List<PublicOddsMarketDetail> markets = marketRows.stream()
                .map(row -> new PublicOddsMarketDetail(
                        row.id(),
                        row.matchId(),
                        row.matchName(),
                        row.matchday(),
                        row.jcCode(),
                        row.bookmaker(),
                        row.marketCode(),
                        row.marketName(),
                        row.snapshotType(),
                        row.handicapLine(),
                        row.lineValue(),
                        row.capturedAt(),
                        row.selectionCount(),
                        row.sourceRef(),
                        selectionsByMarketId.getOrDefault(row.id(), List.of())
                ))
                .toList();
        return new PublicOddsMatchDetail(match.id(), match.matchName(), match.matchday(), match.jcCode(), markets);
    }

    @Transactional(readOnly = true)
    public List<String> bookmakers() {
        return jdbcTemplate.query(
                "SELECT DISTINCT bookmaker FROM odds_market_snapshots ORDER BY bookmaker",
                (rs, rowNum) -> mapper.sanitizeText(rs.getString("bookmaker"))
        );
    }

    @Transactional(readOnly = true)
    public List<PublicOddsMarketDictionary> markets() {
        return jdbcTemplate.query(
                "SELECT market_code, MIN(market_name) AS market_name FROM odds_market_snapshots GROUP BY market_code ORDER BY market_code",
                (rs, rowNum) -> new PublicOddsMarketDictionary(
                        mapper.sanitizeToken(rs.getString("market_code")),
                        mapper.sanitizeText(rs.getString("market_name"))
                )
        );
    }

    private MatchRow findMatch(long matchId) {
        return jdbcTemplate.query(
                        "SELECT id, match_name, matchday, jc_code FROM matches WHERE id=?",
                        (rs, rowNum) -> new MatchRow(
                                rs.getLong("id"),
                                mapper.sanitizeText(rs.getString("match_name")),
                                localDate(rs, "matchday"),
                                mapper.sanitizeText(rs.getString("jc_code"))
                        ),
                        matchId
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found"));
    }

    private List<MarketRow> marketsForMatch(long matchId) {
        return jdbcTemplate.query(
                marketDetailSelect() + " WHERE oms.match_id=? ORDER BY oms.bookmaker, oms.market_code, oms.captured_at DESC, oms.id DESC",
                (rs, rowNum) -> new MarketRow(
                        rs.getLong("id"),
                        nullableLong(rs, "match_id"),
                        mapper.sanitizeText(rs.getString("match_name")),
                        localDate(rs, "matchday"),
                        mapper.sanitizeText(rs.getString("jc_code")),
                        mapper.sanitizeText(rs.getString("bookmaker")),
                        mapper.sanitizeToken(rs.getString("market_code")),
                        mapper.sanitizeText(rs.getString("market_name")),
                        mapper.sanitizeToken(rs.getString("snapshot_type")),
                        rs.getBigDecimal("handicap_line"),
                        mapper.sanitizeText(rs.getString("line_value")),
                        localDateTime(rs, "captured_at"),
                        rs.getLong("selection_count"),
                        mapper.sanitizeText(rs.getString("source_ref"))
                ),
                matchId
        );
    }

    private Map<Long, List<PublicOddsSelection>> selectionsByMarketIds(List<Long> marketIds) {
        if (marketIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PublicOddsSelection>> grouped = new LinkedHashMap<>();
        String sql = "SELECT id, market_snapshot_id, selection_code, selection_name, odds_value, implied_probability, selection_status "
                + "FROM odds_selection_snapshots WHERE market_snapshot_id IN (" + placeholders(marketIds.size()) + ") "
                + "ORDER BY market_snapshot_id, id";
        jdbcTemplate.query(sql, rs -> {
            long marketId = rs.getLong("market_snapshot_id");
            grouped.computeIfAbsent(marketId, ignored -> new ArrayList<>()).add(new PublicOddsSelection(
                    rs.getLong("id"),
                    marketId,
                    mapper.sanitizeToken(rs.getString("selection_code")),
                    mapper.sanitizeText(rs.getString("selection_name")),
                    rs.getBigDecimal("odds_value"),
                    rs.getBigDecimal("implied_probability"),
                    mapper.sanitizeToken(rs.getString("selection_status"))
            ));
        }, marketIds.toArray());
        return grouped;
    }

    private String marketSummarySelect() {
        return "SELECT oms.id, oms.match_id, m.match_name, m.matchday, m.jc_code, oms.bookmaker, oms.market_code, oms.market_name, "
                + "oms.snapshot_type, oms.handicap_line, oms.line_value, oms.captured_at, "
                + "(SELECT COUNT(*) FROM odds_selection_snapshots oss WHERE oss.market_snapshot_id=oms.id) AS selection_count "
                + "FROM odds_market_snapshots oms LEFT JOIN matches m ON m.id=oms.match_id";
    }

    private String marketDetailSelect() {
        return "SELECT oms.id, oms.match_id, m.match_name, m.matchday, m.jc_code, oms.bookmaker, oms.market_code, oms.market_name, "
                + "oms.snapshot_type, oms.handicap_line, oms.line_value, oms.captured_at, "
                + "(SELECT COUNT(*) FROM odds_selection_snapshots oss WHERE oss.market_snapshot_id=oms.id) AS selection_count, "
                + "oms.source_ref "
                + "FROM odds_market_snapshots oms LEFT JOIN matches m ON m.id=oms.match_id";
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }

    private LocalDate localDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private record MatchRow(Long id, String matchName, LocalDate matchday, String jcCode) {
    }

    private record MarketRow(
            Long id,
            Long matchId,
            String matchName,
            LocalDate matchday,
            String jcCode,
            String bookmaker,
            String marketCode,
            String marketName,
            String snapshotType,
            java.math.BigDecimal handicapLine,
            String lineValue,
            LocalDateTime capturedAt,
            long selectionCount,
            String sourceRef
    ) {
    }
}
