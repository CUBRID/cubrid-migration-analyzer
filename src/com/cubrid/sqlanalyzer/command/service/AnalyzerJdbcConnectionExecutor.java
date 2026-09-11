/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class AnalyzerJdbcConnectionExecutor implements AnalyzerJdbcExecutor {
    private final Connection connection;

    AnalyzerJdbcConnectionExecutor(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void prepare(String sql) throws SQLException {
        try (PreparedStatement ignored = connection.prepareStatement(sql)) {
            // Preparing the statement is the validation step. Do not bind or execute it.
        }
    }

    @Override
    public AnalyzerJdbcExecutionResult execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            boolean hasResultSet = statement.execute(sql);
            if (hasResultSet) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    int rowCount = 0;
                    while (resultSet.next()) {
                        rowCount++;
                    }
                    return new AnalyzerJdbcExecutionResult(true, rowCount, 0);
                }
            }
            return new AnalyzerJdbcExecutionResult(false, 0, statement.getUpdateCount());
        }
    }

    @Override
    public void commit() throws SQLException {
        connection.commit();
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
