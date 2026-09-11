/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.viewmodel;

import java.util.Objects;

import com.cubrid.sqlanalyzer.command.model.AnalyzerFailureStage;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;

public record AnalyzerProgressEventViewModel(
        AnalyzerProgressStage stage,
        String message,
        String statementType,
        String statementId,
        String sql,
        String detail,
        AnalyzerFailureStage failureStage,
        AnalyzerProgressCounts counts) {

    public AnalyzerProgressEventViewModel {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(counts, "counts");
    }

    public static AnalyzerProgressEventViewModel planning(int totalCount) {
        return planning(AnalyzerProgressCounts.initial(totalCount));
    }

    public static AnalyzerProgressEventViewModel planning(AnalyzerProgressCounts counts) {
        return new AnalyzerProgressEventViewModel(
                AnalyzerProgressStage.PLANNING,
                "Generated SQL statements: " + counts.totalCount(),
                null,
                null,
                null,
                null,
                null,
                counts);
    }

    public static AnalyzerProgressEventViewModel empty(int totalCount) {
        return empty(AnalyzerProgressCounts.initial(totalCount));
    }

    public static AnalyzerProgressEventViewModel empty(AnalyzerProgressCounts counts) {
        return new AnalyzerProgressEventViewModel(
                AnalyzerProgressStage.EMPTY,
                "No SQL statements were generated for the selected source/mode.",
                null,
                null,
                null,
                null,
                null,
                counts);
    }

    public static AnalyzerProgressEventViewModel statementSucceeded(
            AnalyzerStatement statement,
            String detail,
            AnalyzerProgressCounts counts) {
        Objects.requireNonNull(statement, "statement");
        String message = "[OK] " + statement.getType() + " " + statement.getId();
        if (detail != null && !"parsed".equals(detail)) {
            message += " : " + detail;
        }
        return new AnalyzerProgressEventViewModel(
                AnalyzerProgressStage.STATEMENT_SUCCEEDED,
                message,
                statement.getType(),
                statement.getId(),
                statement.getSQL(),
                detail,
                null,
                counts);
    }

    public static AnalyzerProgressEventViewModel statementFailed(
            AnalyzerStatement statement,
            String detail,
            AnalyzerFailureStage failureStage,
            AnalyzerProgressCounts counts) {
        Objects.requireNonNull(statement, "statement");
        String message = "[FAIL] " + statement.getType() + " " + statement.getId()
                + (detail != null ? " : " + detail : "");
        return new AnalyzerProgressEventViewModel(
                AnalyzerProgressStage.STATEMENT_FAILED,
                message,
                statement.getType(),
                statement.getId(),
                statement.getSQL(),
                detail,
                failureStage,
                counts);
    }

    public static AnalyzerProgressEventViewModel cleanupSucceeded(
            String cleanupId, String cleanupQuery, AnalyzerProgressCounts counts) {
        return new AnalyzerProgressEventViewModel(
                AnalyzerProgressStage.CLEANUP_SUCCEEDED,
                "[CLEANUP OK] " + cleanupQuery,
                "CLEANUP",
                cleanupId,
                cleanupQuery,
                "cleanup executed",
                null,
                counts);
    }

    public static AnalyzerProgressEventViewModel cleanupFailed(
            String message,
            String cleanupId,
            String cleanupQuery,
            String detail,
            AnalyzerProgressCounts counts) {
        return new AnalyzerProgressEventViewModel(
                AnalyzerProgressStage.CLEANUP_FAILED,
                message,
                "CLEANUP",
                cleanupId,
                cleanupQuery,
                detail,
                AnalyzerFailureStage.CLEANUP,
                counts);
    }

    public static AnalyzerProgressEventViewModel completed(AnalyzerProgressCounts counts) {
        return new AnalyzerProgressEventViewModel(
                AnalyzerProgressStage.COMPLETED,
                "Analysis completed. Total="
                        + counts.completedCount()
                        + ", Success="
                        + counts.succeededCount()
                        + ", Failed="
                        + counts.failedCount(),
                null,
                null,
                null,
                null,
                null,
                counts);
    }

    public int totalCount() {
        return counts.totalCount();
    }

    public int completedCount() {
        return counts.completedCount();
    }

    public int succeededCount() {
        return counts.succeededCount();
    }

    public int failedCount() {
        return counts.failedCount();
    }
}
