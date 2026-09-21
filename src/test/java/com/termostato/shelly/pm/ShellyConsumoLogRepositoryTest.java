package com.termostato.shelly.pm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShellyConsumoLogRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void salvaLeggeEFiltraPerDeviceIdERange() {
        JdbcTemplate jdbc = jdbcTemplate();
        ShellyConsumoLogRepository repo = new ShellyConsumoLogRepository(jdbc);

        Instant t1 = Instant.parse("2026-09-20T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-20T10:00:30Z");
        Instant t3 = Instant.parse("2026-09-20T10:01:00Z");

        repo.save(new ShellyConsumoLogRecord(null, t1, "frigo", new BigDecimal("45.2"),
                new BigDecimal("229.8"), new BigDecimal("0.197")));
        repo.save(new ShellyConsumoLogRecord(null, t2, "frigo", new BigDecimal("50.0"),
                new BigDecimal("230.0"), new BigDecimal("0.217")));
        // altro device: non deve comparire nel risultato per "frigo"
        repo.save(new ShellyConsumoLogRecord(null, t2, "modem", new BigDecimal("10.0"),
                new BigDecimal("230.0"), new BigDecimal("0.043")));

        List<ShellyConsumoLogRecord> results = repo.findBetween("frigo", t1, t3);

        assertEquals(2, results.size());
        assertEquals(t1, results.get(0).dataOra());
        assertEquals("frigo", results.get(0).deviceId());
        assertEquals(new BigDecimal("45.2"), results.get(0).potenzaW());
        assertEquals(t2, results.get(1).dataOra());
    }

    @Test
    void findBetweenEscludeRangeEsterni() {
        JdbcTemplate jdbc = jdbcTemplate();
        ShellyConsumoLogRepository repo = new ShellyConsumoLogRepository(jdbc);

        Instant t1 = Instant.parse("2026-09-20T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-20T11:00:00Z");
        Instant t3 = Instant.parse("2026-09-20T12:00:00Z");

        repo.save(new ShellyConsumoLogRecord(null, t1, "modem", new BigDecimal("5.0"),
                new BigDecimal("230.0"), new BigDecimal("0.02")));
        repo.save(new ShellyConsumoLogRecord(null, t3, "modem", new BigDecimal("6.0"),
                new BigDecimal("230.0"), new BigDecimal("0.026")));

        List<ShellyConsumoLogRecord> results = repo.findBetween("modem", t1, t2);

        assertEquals(1, results.size());
        assertEquals(t1, results.get(0).dataOra());
    }

    @Test
    void campiNullabiliSalvatiCorrettamente() {
        JdbcTemplate jdbc = jdbcTemplate();
        ShellyConsumoLogRepository repo = new ShellyConsumoLogRepository(jdbc);

        Instant t = Instant.parse("2026-09-20T10:00:00Z");
        repo.save(new ShellyConsumoLogRecord(null, t, "test", null, null, null));

        List<ShellyConsumoLogRecord> results = repo.findBetween("test", t, t.plusSeconds(1));

        assertEquals(1, results.size());
        assertNull(results.get(0).potenzaW());
        assertNull(results.get(0).tensioneV());
        assertNull(results.get(0).correnteA());
    }

    @Test
    void deleteBeforeCancellaRecordPrecedenti() {
        JdbcTemplate jdbc = jdbcTemplate();
        ShellyConsumoLogRepository repo = new ShellyConsumoLogRepository(jdbc);

        Instant t1 = Instant.parse("2026-09-20T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-20T11:00:00Z");
        Instant t3 = Instant.parse("2026-09-20T12:00:00Z");

        repo.save(new ShellyConsumoLogRecord(null, t1, "frigo", new BigDecimal("1.0"), null, null));
        repo.save(new ShellyConsumoLogRecord(null, t2, "frigo", new BigDecimal("2.0"), null, null));
        repo.save(new ShellyConsumoLogRecord(null, t3, "frigo", new BigDecimal("3.0"), null, null));

        int deleted = repo.deleteBefore(t2);

        assertEquals(1, deleted);
        List<ShellyConsumoLogRecord> remaining = repo.findBetween("frigo", Instant.EPOCH, Instant.parse("2099-12-31T23:59:59Z"));
        assertEquals(2, remaining.size());
    }

    private JdbcTemplate jdbcTemplate() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("shelly-test.db"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE shelly_consumo_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    data_ora TEXT NOT NULL,
                    device_id TEXT NOT NULL,
                    potenza_w REAL,
                    tensione_v REAL,
                    corrente_a REAL
                )
                """);
        return jdbc;
    }
}
