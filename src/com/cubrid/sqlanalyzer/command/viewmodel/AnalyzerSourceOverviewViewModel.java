/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.viewmodel;

import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;

public record AnalyzerSourceOverviewViewModel(
        AnalyzerSourceType type,
        String jdbcUrl,
        String host,
        int port,
        String databaseName,
        String user,
        String version,
        String xmlDirectory,
        String xmlCharset,
        int xmlFileCount) {
}
