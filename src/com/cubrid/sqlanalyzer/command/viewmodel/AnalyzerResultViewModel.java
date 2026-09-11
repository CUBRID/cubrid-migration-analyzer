/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.viewmodel;

import java.util.List;

import com.cubrid.sqlanalyzer.command.model.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.model.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;

public record AnalyzerResultViewModel(
        List<AnalyzerSourceType> sourceTypes,
        AnalyzerTargetType targetType,
        AnalyzerExecutionMode executionMode,
        int analyzedStatementCount,
        int succeededStatementCount,
        int failedStatementCount,
        float totalEstimatedFailureCost,
        String savedReportPath,
        String savedHtmlReportPath,
        List<String> sourceStatusMessages,
        List<String> failureMessages,
        List<AnalyzerFailure> failures,
        List<AnalyzerProgressObjectCount> objectExecutionCounts) {

    public AnalyzerResultViewModel(
            AnalyzerSourceType sourceType,
            AnalyzerTargetType targetType,
            AnalyzerExecutionMode executionMode,
            int analyzedStatementCount,
            int succeededStatementCount,
            int failedStatementCount,
            float totalEstimatedFailureCost,
            String savedReportPath,
            List<String> failureMessages,
            List<AnalyzerFailure> failures,
            List<AnalyzerProgressObjectCount> objectExecutionCounts) {
        this(
                sourceType == null ? List.of() : List.of(sourceType),
                targetType,
                executionMode,
                analyzedStatementCount,
                succeededStatementCount,
                failedStatementCount,
                totalEstimatedFailureCost,
                savedReportPath,
                null,
                List.of(),
                failureMessages,
                failures,
                objectExecutionCounts);
    }

    public AnalyzerResultViewModel(
            AnalyzerSourceType sourceType,
            AnalyzerTargetType targetType,
            AnalyzerExecutionMode executionMode,
            int analyzedStatementCount,
            int succeededStatementCount,
            int failedStatementCount,
            float totalEstimatedFailureCost,
            String savedReportPath,
            List<String> failureMessages,
            List<AnalyzerFailure> failures) {
        this(
                sourceType == null ? List.of() : List.of(sourceType),
                targetType,
                executionMode,
                analyzedStatementCount,
                succeededStatementCount,
                failedStatementCount,
                totalEstimatedFailureCost,
                savedReportPath,
                null,
                List.of(),
                failureMessages,
                failures,
                List.of());
    }

    public AnalyzerResultViewModel {
        savedHtmlReportPath =
                savedHtmlReportPath == null || savedHtmlReportPath.isEmpty()
                        ? deriveHtmlReportPath(savedReportPath)
                        : savedHtmlReportPath;
        sourceTypes = sourceTypes == null ? List.of() : List.copyOf(sourceTypes);
        sourceStatusMessages =
                sourceStatusMessages == null ? List.of() : List.copyOf(sourceStatusMessages);
        failureMessages = failureMessages == null ? List.of() : List.copyOf(failureMessages);
        failures = failures == null ? List.of() : List.copyOf(failures);
        objectExecutionCounts =
                objectExecutionCounts == null ? List.of() : List.copyOf(objectExecutionCounts);
    }

    public AnalyzerSourceType sourceType() {
        if (sourceTypes.isEmpty()) {
            return null;
        }
        return sourceTypes.size() > 1 ? AnalyzerSourceType.ALL : sourceTypes.get(0);
    }

    private static String deriveHtmlReportPath(String savedReportPath) {
        if (savedReportPath == null || savedReportPath.isEmpty()) {
            return "";
        }
        if (savedReportPath.endsWith(".txt")) {
            return savedReportPath.substring(0, savedReportPath.length() - 4) + ".html";
        }
        return savedReportPath + ".html";
    }
}
