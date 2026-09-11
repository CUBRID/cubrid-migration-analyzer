/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import java.sql.SQLException;

/**
 * Narrow seam over a JDBC target connection: only the operations
 * {@link AnalyzerExecutionRunner} actually needs, so JDBC-target orchestration
 * (commit decisions, DDL labeling, cleanup ordering) can be tested without a real
 * driver/connection.
 */
interface AnalyzerJdbcExecutor extends AutoCloseable {
    void prepare(String sql) throws SQLException;

    AnalyzerJdbcExecutionResult execute(String sql) throws SQLException;

    void commit() throws SQLException;

    @Override
    void close() throws SQLException;
}
