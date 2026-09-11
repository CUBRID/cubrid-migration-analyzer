/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.report;

import com.cubrid.sqlanalyzer.command.model.AnalyzerFailureStage;

class StatementResult {
    final String statementType;
    final String statementId;
    final String objectName;
    final String sql;
    final boolean success;
    final String detail;
    final AnalyzerFailureStage failureStage;

    StatementResult(
            String statementType,
            String statementId,
            String objectName,
            String sql,
            boolean success,
            String detail,
            AnalyzerFailureStage failureStage) {
        this.statementType = statementType;
        this.statementId = statementId;
        this.objectName = objectName;
        this.sql = sql;
        this.success = success;
        this.detail = detail;
        this.failureStage = failureStage;
    }
}
