/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.viewmodel;

import java.util.List;

import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;

public record AnalyzerObjectCountPreviewViewModel(
        AnalyzerSourceType sourceType,
        int catalogSchemaCount,
        int targetTableCount,
        long targetPkCount,
        long targetFkCount,
        int targetViewCount,
        int targetSerialCount,
        int targetSynonymCount,
        int targetGrantCount,
        int targetProcedureCount,
        int targetFunctionCount,
        int targetTriggerCount,
        int selectCount,
        int insertCount,
        int updateCount,
        int deleteCount,
        long totalTableBytes,
        List<AnalyzerTableSizeViewModel> tableSizes,
        boolean oracleSourceLoaded,
        boolean xmlSourceLoaded,
        List<String> sourceStatusMessages) {

    public AnalyzerObjectCountPreviewViewModel(
            AnalyzerSourceType sourceType,
            int catalogSchemaCount,
            int targetTableCount,
            long targetPkCount,
            long targetFkCount,
            int targetViewCount,
            int targetSerialCount,
            int targetSynonymCount,
            int targetGrantCount,
            int targetProcedureCount,
            int targetFunctionCount,
            int targetTriggerCount,
            int selectCount,
            int insertCount,
            int updateCount,
            int deleteCount) {
        this(
                sourceType,
                catalogSchemaCount,
                targetTableCount,
                targetPkCount,
                targetFkCount,
                targetViewCount,
                targetSerialCount,
                targetSynonymCount,
                targetGrantCount,
                targetProcedureCount,
                targetFunctionCount,
                targetTriggerCount,
                selectCount,
                insertCount,
                updateCount,
                deleteCount,
                0,
                List.of(),
                sourceType == AnalyzerSourceType.ORACLE || sourceType == AnalyzerSourceType.ALL,
                sourceType == AnalyzerSourceType.XML || sourceType == AnalyzerSourceType.ALL,
                List.of());
    }

    public AnalyzerObjectCountPreviewViewModel(
            AnalyzerSourceType sourceType,
            int catalogSchemaCount,
            int targetTableCount,
            long targetPkCount,
            long targetFkCount,
            int targetViewCount,
            int targetSerialCount,
            int targetSynonymCount,
            int targetGrantCount,
            int targetProcedureCount,
            int targetFunctionCount,
            int targetTriggerCount,
            int selectCount,
            int insertCount,
            int updateCount,
            int deleteCount,
            long totalTableBytes,
            List<AnalyzerTableSizeViewModel> tableSizes) {
        this(
                sourceType,
                catalogSchemaCount,
                targetTableCount,
                targetPkCount,
                targetFkCount,
                targetViewCount,
                targetSerialCount,
                targetSynonymCount,
                targetGrantCount,
                targetProcedureCount,
                targetFunctionCount,
                targetTriggerCount,
                selectCount,
                insertCount,
                updateCount,
                deleteCount,
                totalTableBytes,
                tableSizes,
                sourceType == AnalyzerSourceType.ORACLE || sourceType == AnalyzerSourceType.ALL,
                sourceType == AnalyzerSourceType.XML || sourceType == AnalyzerSourceType.ALL,
                List.of());
    }

    public AnalyzerObjectCountPreviewViewModel {
        tableSizes = tableSizes == null ? List.of() : List.copyOf(tableSizes);
        sourceStatusMessages =
                sourceStatusMessages == null ? List.of() : List.copyOf(sourceStatusMessages);
    }
}
