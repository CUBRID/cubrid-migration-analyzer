/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import java.sql.SQLException;

import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;

interface AnalyzerJdbcExecutorFactory {
    AnalyzerJdbcExecutor open(AnalyzerConfiguration config) throws SQLException;
}
