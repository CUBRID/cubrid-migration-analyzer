/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.viewmodel;

public enum AnalyzerProgressStage {
    PLANNING,
    ANALYZING,
    STATEMENT_SUCCEEDED,
    STATEMENT_FAILED,
    CLEANUP_SUCCEEDED,
    CLEANUP_FAILED,
    COMPLETED,
    EMPTY
}
