/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.cost;

import com.cubrid.sqlanalyzer.command.model.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.report.AnalyzerReport;

abstract class CostTestSupport {
    protected static final float DELTA = 0.0001f;

    private final FailureCostCalculator calculator = new FailureCostCalculator();

    protected float estimateCost(String statementType, String sql) {
        return analyzeFailure(statementType, sql).getEstimatedCost();
    }

    protected AnalyzerFailure analyzeFailure(String statementType, String sql) {
        AnalyzerReport report = new AnalyzerReport();
        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType(statementType);
        failure.setStatementId("TEST_1");
        failure.setSql(sql);
        failure.setReason("test");
        report.addFailure(failure);

        calculator.analyzeAfterExecution(report);
        return failure;
    }

    protected String buildSqlWithLength(int targetLength) {
        String prefix = "SELECT '";
        String suffix = "'";
        int payloadLength = targetLength - prefix.length() - suffix.length();
        if (payloadLength < 0) {
            throw new IllegalArgumentException(
                    "targetLength must be at least " + (prefix.length() + suffix.length()));
        }

        // Whitespace is normalized by the calculator, so use literal content to control length.
        return prefix + "x".repeat(payloadLength) + suffix;
    }
}
