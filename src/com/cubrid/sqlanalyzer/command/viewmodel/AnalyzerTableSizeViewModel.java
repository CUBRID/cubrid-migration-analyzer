/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.viewmodel;

public record AnalyzerTableSizeViewModel(
        String tableName,
        long bytes,
        long estimatedRows) {

    public AnalyzerTableSizeViewModel(String tableName, long bytes) {
        this(tableName, bytes, 0);
    }
}
