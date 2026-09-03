CREATE TABLE polling_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    data_ora TEXT NOT NULL,
    caldaia_accesa INTEGER NOT NULL CHECK (caldaia_accesa IN (0, 1)),
    temperatura_rilevata REAL NOT NULL,
    temperatura_target REAL,
    override_attivo INTEGER NOT NULL CHECK (override_attivo IN (0, 1)),
    temperatura_override REAL
);

CREATE INDEX idx_polling_log_data_ora ON polling_log (data_ora);

CREATE TABLE error_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    data_ora TEXT NOT NULL,
    tipo_errore TEXT NOT NULL,
    caldaia_accesa INTEGER,
    temperatura_rilevata REAL,
    num_errori_consecutivi INTEGER NOT NULL
);

CREATE INDEX idx_error_log_data_ora ON error_log (data_ora);
