package com.worldcup.oddscenter.service;

import com.worldcup.oddscenter.api.dto.OddsCenterDtos.OddsMarketDetailResponse;
import com.worldcup.oddscenter.api.dto.OddsCenterDtos.OddsMarketDictionaryResponse;
import com.worldcup.oddscenter.api.dto.OddsCenterDtos.OddsMarketSummaryResponse;
import com.worldcup.oddscenter.api.dto.OddsCenterDtos.OddsMatchDetailResponse;
import com.worldcup.oddscenter.api.dto.OddsCenterDtos.OddsSelectionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OddsCenterQueryService {
    private final JdbcTemplate jdbcTemplate;

    public OddsCenterQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<OddsMarketSummaryResponse> overview() {
        return jdbcTemplate.query(
                marketSummarySelect() + " ORDER BY m.matchday DESC, oms.captured_at DESC, oms.id DESC",
                summaryMapper()
        );
    }

    @Transactional(readOnly = true)
    public OddsMatchDetailResponse matchOdds(long matchId) {
        MatchRow match = findMatch(matchId);
        List<OddsMarketDetailResponse> markets = jdbcTemplate.query(
                marketDetailSelect() + " WHERE oms.match_id=? ORDER BY oms.bookmaker, oms.market_code, oms.captured_at DESC, oms.id DESC",
                detailMapper(),
                matchId
        );
        return new OddsMatchDetailResponse(match.id(), match.matchName(), match.matchday(), match.jcCode(), markets);
    }

    @Transactional(readOnly = true)
    public List<String> bookmakers() {
        return jdbcTemplate.query(
                "SELECT DISTINCT bookmaker FROM odds_market_snapshots ORDER BY bookmaker",
                (rs, rowNum) -> rs.getString("bookmaker")
        );
    }

    @Transactional(readOnly = true)
    public List<OddsMarketDictionaryResponse> markets() {
        return jdbcTemplate.query(
                "SELECT market_code, MIN(market_name) AS market_name FROM odds_market_snapshots GROUP BY market_code ORDER BY market_code",
                (rs, rowNum) -> new OddsMarketDictionaryResponse(rs.getString("market_code"), rs.getString("market_name"))
        );
    }

    private MatchRow findMatch(long matchId) {
        return jdbcTemplate.query(
                        "SELECT id, match_name, matchday, jc_code FROM matches WHERE id=?",
                        (rs, rowNum) -> new MatchRow(
                                rs.getLong("id"),
                                rs.getString("match_name"),
                                localDate(rs, "matchday"),
                                rs.getString("jc_code")
                        ),
                        matchId
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "比赛不存在"));
    }

    private List<OddsSelectionResponse> selections(long marketId) {
        return jdbcTemplate.query(
                "SELECT id, market_snapshot_id, selection_code, selection_name, odds_value, implied_probability, selection_status, raw_payload " +
                        "FROM odds_selection_snapshots WHERE market_snapshot_id=? ORDER BY id",
                selectionMapper(),
                marketId
        );
    }

    private String marketSummarySelect() {
        return "SELECT oms.id, oms.match_id, m.match_name, m.matchday, m.jc_code, oms.bookmaker, oms.market_code, oms.market_name, " +
                "oms.snapshot_type, oms.handicap_line, oms.line_value, oms.captured_at, " +
                "(SELECT COUNT(*) FROM odds_selection_snapshots oss WHERE oss.market_snapshot_id=oms.id) AS selection_count " +
                "FROM odds_market_snapshots oms LEFT JOIN matches m ON m.id=oms.match_id";
    }

    private String marketDetailSelect() {
        return "SELECT oms.id, oms.match_id, m.match_name, m.matchday, m.jc_code, oms.bookmaker, oms.market_code, oms.market_name, " +
                "oms.snapshot_type, oms.handicap_line, oms.line_value, oms.captured_at, oms.source_ref, oms.raw_payload " +
                "FROM odds_market_snapshots oms LEFT JOIN matches m ON m.id=oms.match_id";
    }

    private RowMapper<OddsMarketSummaryResponse> summaryMapper() {
        return (rs, rowNum) -> new OddsMarketSummaryResponse(
                rs.getLong("id"),
                nullableLong(rs, "match_id"),
                rs.getString("match_name"),
                localDate(rs, "matchday"),
                rs.getString("jc_code"),
                rs.getString("bookmaker"),
                rs.getString("market_code"),
                rs.getString("market_name"),
                rs.getString("snapshot_type"),
                rs.getBigDecimal("handicap_line"),
                rs.getString("line_value"),
                localDateTime(rs, "captured_at"),
                rs.getLong("selection_count")
        );
    }

    private RowMapper<OddsMarketDetailResponse> detailMapper() {
        return (rs, rowNum) -> {
            long marketId = rs.getLong("id");
            return new OddsMarketDetailResponse(
                    marketId,
                    nullableLong(rs, "match_id"),
                    rs.getString("match_name"),
                    localDate(rs, "matchday"),
                    rs.getString("jc_code"),
                    rs.getString("bookmaker"),
                    rs.getString("market_code"),
                    rs.getString("market_name"),
                    rs.getString("snapshot_type"),
                    rs.getBigDecimal("handicap_line"),
                    rs.getString("line_value"),
                    localDateTime(rs, "captured_at"),
                    rs.getString("source_ref"),
                    rs.getString("raw_payload"),
                    selections(marketId)
            );
        };
    }

    private RowMapper<OddsSelectionResponse> selectionMapper() {
        return (rs, rowNum) -> new OddsSelectionResponse(
                rs.getLong("id"),
                rs.getLong("market_snapshot_id"),
                rs.getString("selection_code"),
                rs.getString("selection_name"),
                rs.getBigDecimal("odds_value"),
                rs.getBigDecimal("implied_probability"),
                rs.getString("selection_status"),
                rs.getString("raw_payload")
        );
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
}
