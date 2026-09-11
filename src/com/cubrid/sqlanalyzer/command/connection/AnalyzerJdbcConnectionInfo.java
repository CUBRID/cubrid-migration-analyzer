/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.connection;

public class AnalyzerJdbcConnectionInfo {
    private final String jdbcUrl;
    private final String host;
    private final int port;
    private final String databaseName;
    private final String user;
    private final String password;
    private final String charset;
    private final String driverLocation;

    public AnalyzerJdbcConnectionInfo(
            String jdbcUrl,
            String host,
            int port,
            String databaseName,
            String user,
            String password,
            String charset,
            String driverLocation) {
        this.jdbcUrl = jdbcUrl;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.user = user;
        this.password = password;
        this.charset = charset;
        this.driverLocation = driverLocation;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getCharset() {
        return charset;
    }

    public String getDriverLocation() {
        return driverLocation;
    }
}
