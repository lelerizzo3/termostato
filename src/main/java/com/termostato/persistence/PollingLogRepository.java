package com.termostato.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Repository
public class PollingLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public PollingLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(PollingLogRecord record) {
        jdbcTemplate.update("""
                        INSERT INTO polling_log
                        (data_ora, caldaia_accesa, temperatura_rilevata, temperatura_target,
                         override_attivo, temperatura_override)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                UtcInstantCodec.format(record.dataOra()),
                record.caldaiaAccesa() ? 1 : 0,
                record.temperaturaRilevata().doubleValue(),
                nullableDouble(record.temperaturaTarget()),
                record.overrideAttivo() ? 1 : 0,
                nullableDouble(record.temperaturaOverride()));
    }

    public List<PollingLogRecord> findBetween(Instant fromInclusive, Instant toExclusive) {
        return jdbcTemplate.query("""
                        SELECT id, data_ora, caldaia_accesa, temperatura_rilevata,
                               temperatura_target, override_attivo, temperatura_override
                        FROM polling_log
                        WHERE data_ora >= ? AND data_ora < ?
                        ORDER BY data_ora ASC, id ASC
                        """,
                this::map,
                UtcInstantCodec.format(fromInclusive), UtcInstantCodec.format(toExclusive));
    }

    public int deleteBefore(Instant threshold) {
        return jdbcTemplate.update("DELETE FROM polling_log WHERE data_ora < ?",
                UtcInstantCodec.format(threshold));
    }

    private PollingLogRecord map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PollingLogRecord(
                resultSet.getLong("id"),
                UtcInstantCodec.parse(resultSet.getString("data_ora")),
                resultSet.getInt("caldaia_accesa") != 0,
                decimal(resultSet, "temperatura_rilevata"),
                decimal(resultSet, "temperatura_target"),
                resultSet.getInt("override_attivo") != 0,
                decimal(resultSet, "temperatura_override"));
    }

    private static Double nullableDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    static BigDecimal decimal(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}
