/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

record AnalyzerJdbcExecutionResult(boolean hasResultSet, int rowCount, int updateCount) {
}
