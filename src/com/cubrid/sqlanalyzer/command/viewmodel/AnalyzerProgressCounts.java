/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.viewmodel;

import java.util.List;

public record AnalyzerProgressCounts(
        int totalCount,
        int completedCount,
        int succeededCount,
        int failedCount,
        List<AnalyzerProgressObjectCount> objectCounts) {

    public AnalyzerProgressCounts(
            int totalCount,
            int completedCount,
            int succeededCount,
            int failedCount) {
        this(totalCount, completedCount, succeededCount, failedCount, List.of());
    }

    public AnalyzerProgressCounts {
        objectCounts = objectCounts == null ? List.of() : List.copyOf(objectCounts);
    }

    public static AnalyzerProgressCounts initial(int totalCount) {
        return new AnalyzerProgressCounts(totalCount, 0, 0, 0);
    }
}
