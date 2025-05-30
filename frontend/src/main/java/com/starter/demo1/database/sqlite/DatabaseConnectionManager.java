package com.starter.demo1.database.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionManager {
    private static HikariDataSource dataSource;

    @Getter
    private static DSLContext dslContext;

    private DatabaseConnectionManager() {
    }

    static {
        initializeDataSource();
        initializeDslContext();
    }

    private static void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:database.db");
        config.setUsername("");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        config.addDataSourceProperty("foreign_keys", "true");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");

        dataSource = new HikariDataSource(config);
    }

    private static void initializeDslContext() {
        dslContext = DSL.using(dataSource, SQLDialect.SQLITE);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }


    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}