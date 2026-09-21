CREATE TABLE shelly_consumo_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    data_ora TEXT NOT NULL,
    device_id TEXT NOT NULL,
    potenza_w REAL,
    tensione_v REAL,
    corrente_a REAL
);

CREATE INDEX idx_shelly_consumo_log_device_data ON shelly_consumo_log (device_id, data_ora);
