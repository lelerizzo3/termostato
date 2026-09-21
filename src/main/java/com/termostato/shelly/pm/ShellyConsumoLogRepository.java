package com.termostato.shelly.pm;

import com.termostato.persistence.UtcInstantCodec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Repository
public class ShellyConsumoLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public ShellyConsumoLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(ShellyConsumoLogRecord record) {
        jdbcTemplate.update("""
                        INSERT INTO shelly_consumo_log
                        (data_ora, device_id, potenza_w, tensione_v, corrente_a)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                UtcInstantCodec.format(record.dataOra()),
                record.deviceId(),
                nullableDouble(record.potenzaW()),
                nullableDouble(record.tensioneV()),
                nullableDouble(record.correnteA()));
    }

    public List<ShellyConsumoLogRecord> findBetween(String deviceId,
                                                     Instant fromInclusive,
                                                     Instant toExclusive) {
        return jdbcTemplate.query("""
                        SELECT id, data_ora, device_id, potenza_w, tensione_v, corrente_a
                        FROM shelly_consumo_log
                        WHERE device_id = ? AND data_ora >= ? AND data_ora < ?
                        ORDER BY data_ora ASC, id ASC
                        """,
                this::map,
                deviceId,
                UtcInstantCodec.format(fromInclusive),
                UtcInstantCodec.format(toExclusive));
    }

    public int deleteBefore(Instant threshold) {
        return jdbcTemplate.update("DELETE FROM shelly_consumo_log WHERE data_ora < ?",
                UtcInstantCodec.format(threshold));
    }

    private ShellyConsumoLogRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new ShellyConsumoLogRecord(
                rs.getLong("id"),
                UtcInstantCodec.parse(rs.getString("data_ora")),
                rs.getString("device_id"),
                decimal(rs, "potenza_w"),
                decimal(rs, "tensione_v"),
                decimal(rs, "corrente_a"));
    }

    private static Double nullableDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private static BigDecimal decimal(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}
