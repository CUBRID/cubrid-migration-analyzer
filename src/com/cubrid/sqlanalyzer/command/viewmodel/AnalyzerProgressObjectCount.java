/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.viewmodel;

public record AnalyzerProgressObjectCount(
        String objectType,
        int totalCount,
        int succeededCount,
        int failedCount) {
}
