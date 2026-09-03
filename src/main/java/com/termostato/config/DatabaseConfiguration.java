package com.termostato.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class DatabaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfiguration.class);

    @Bean
    @Primary
    public DataSource dataSource(BootstrapProperties properties) {
        Path database = Path.of(properties.getDatabasePath()).toAbsolutePath().normalize();
        Path parent = database.getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Impossibile creare la directory del database " + parent, exception);
        }

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + database);
        configureSQLite(dataSource);
        log.info("Database SQLite configurato su {}", database);
        return dataSource;
    }

    private void configureSQLite(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossibile inizializzare SQLite", exception);
        }
    }
}
