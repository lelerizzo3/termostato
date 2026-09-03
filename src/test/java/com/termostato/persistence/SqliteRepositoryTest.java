package com.termostato.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqliteRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void salvaLeggeEFiltraLogConTimestampUtc() {
        JdbcTemplate jdbc = jdbcTemplate();
        PollingLogRepository polling = new PollingLogRepository(jdbc);
        ErrorLogRepository errors = new ErrorLogRepository(jdbc);
        Instant first = Instant.parse("2026-09-03T08:00:00Z");
        Instant second = Instant.parse("2026-09-03T08:00:00.100Z");

        polling.save(new PollingLogRecord(null, first, true, new BigDecimal("19.3"),
                new BigDecimal("20.5"), false, null));
        errors.save(new ErrorLogRecord(null, second, "errore", false, new BigDecimal("19.3"), 1));

        List<PollingLogRecord> records = polling.findBetween(first, second.plusNanos(1));
        List<ErrorLogRecord> errorRecords = errors.findBetween(first, second.plusNanos(1));

        assertEquals(1, records.size());
        assertEquals(first, records.getFirst().dataOra());
        assertEquals(1, errorRecords.size());
        assertEquals(second, errorRecords.getFirst().dataOra());
        assertEquals(1, polling.deleteBefore(second));
        assertEquals(1, errors.deleteBefore(second.plusNanos(1)));
    }

    private JdbcTemplate jdbcTemplate() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("logs.db"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE polling_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    data_ora TEXT NOT NULL,
                    caldaia_accesa INTEGER NOT NULL,
                    temperatura_rilevata REAL NOT NULL,
                    temperatura_target REAL,
                    override_attivo INTEGER NOT NULL,
                    temperatura_override REAL
                )
                """);
        jdbc.execute("""
                CREATE TABLE error_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    data_ora TEXT NOT NULL,
                    tipo_errore TEXT NOT NULL,
                    caldaia_accesa INTEGER,
                    temperatura_rilevata REAL,
                    num_errori_consecutivi INTEGER NOT NULL
                )
                """);
        return jdbc;
    }
}
