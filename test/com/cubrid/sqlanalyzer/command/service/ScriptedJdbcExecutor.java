/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java {@link AnalyzerJdbcExecutor} test double: scripts canned results/failures per SQL
 * string and records what was executed/committed, without touching java.sql or CMT's
 * {@code ConnParameters}.
 */
final class ScriptedJdbcExecutor implements AnalyzerJdbcExecutor {
    private final Map<String, AnalyzerJdbcExecutionResult> results = new HashMap<>();
    private final Map<String, SQLException> failures = new HashMap<>();
    private final Map<String, SQLException> prepareFailures = new HashMap<>();
    private final List<String> preparedSql = new ArrayList<>();
    private final List<String> executedSql = new ArrayList<>();
    private int commitCount;
    private boolean closed;

    void whenExecuting(String sql, AnalyzerJdbcExecutionResult result) {
        results.put(sql, result);
    }

    void whenExecutingThrow(String sql, SQLException exception) {
        failures.put(sql, exception);
    }

    void whenPreparingThrow(String sql, SQLException exception) {
        prepareFailures.put(sql, exception);
    }

    @Override
    public void prepare(String sql) throws SQLException {
        preparedSql.add(sql);
        if (prepareFailures.containsKey(sql)) {
            throw prepareFailures.get(sql);
        }
    }

    @Override
    public AnalyzerJdbcExecutionResult execute(String sql) throws SQLException {
        executedSql.add(sql);
        if (failures.containsKey(sql)) {
            throw failures.get(sql);
        }
        return results.getOrDefault(sql, new AnalyzerJdbcExecutionResult(false, 0, 0));
    }

    @Override
    public void commit() {
        commitCount++;
    }

    @Override
    public void close() {
        closed = true;
    }

    int commitCount() {
        return commitCount;
    }

    boolean isClosed() {
        return closed;
    }

    List<String> executedSql() {
        return executedSql;
    }

    List<String> preparedSql() {
        return preparedSql;
    }
}
