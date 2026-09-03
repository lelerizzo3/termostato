package com.termostato.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public class ErrorLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public ErrorLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(ErrorLogRecord record) {
        jdbcTemplate.update("""
                        INSERT INTO error_log
                        (data_ora, tipo_errore, caldaia_accesa, temperatura_rilevata, num_errori_consecutivi)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                UtcInstantCodec.format(record.dataOra()),
                record.tipoErrore(),
                record.caldaiaAccesa() == null ? null : (record.caldaiaAccesa() ? 1 : 0),
                record.temperaturaRilevata() == null ? null : record.temperaturaRilevata().doubleValue(),
                record.numErroriConsecutivi());
    }

    public List<ErrorLogRecord> findBetween(Instant fromInclusive, Instant toExclusive) {
        return jdbcTemplate.query("""
                        SELECT id, data_ora, tipo_errore, caldaia_accesa,
                               temperatura_rilevata, num_errori_consecutivi
                        FROM error_log
                        WHERE data_ora >= ? AND data_ora < ?
                        ORDER BY data_ora ASC, id ASC
                        """,
                (resultSet, rowNumber) -> {
                    int state = resultSet.getInt("caldaia_accesa");
                    Boolean boilerState = resultSet.wasNull() ? null : state != 0;
                    BigDecimal temperature = PollingLogRepository.decimal(resultSet, "temperatura_rilevata");
                    return new ErrorLogRecord(
                            resultSet.getLong("id"),
                            UtcInstantCodec.parse(resultSet.getString("data_ora")),
                            resultSet.getString("tipo_errore"),
                            boilerState,
                            temperature,
                            resultSet.getInt("num_errori_consecutivi"));
                },
                UtcInstantCodec.format(fromInclusive), UtcInstantCodec.format(toExclusive));
    }

    public int deleteBefore(Instant threshold) {
        return jdbcTemplate.update("DELETE FROM error_log WHERE data_ora < ?",
                UtcInstantCodec.format(threshold));
    }
}
