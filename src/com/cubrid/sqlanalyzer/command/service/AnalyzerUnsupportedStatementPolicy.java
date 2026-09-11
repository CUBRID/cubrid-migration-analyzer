/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;

final class AnalyzerUnsupportedStatementPolicy {
    static final String TRIGGER_UNSUPPORTED_REASON = "Trigger migration is not supported.";

    private AnalyzerUnsupportedStatementPolicy() {
    }

    static String getUnsupportedReason(AnalyzerStatement statement) {
        if (statement == null) {
            return null;
        }

        if (AnalyzerStatementTypes.TYPE_DDL_TRIGGER.equals(statement.getType())) {
            return TRIGGER_UNSUPPORTED_REASON;
        }

        return null;
    }
}
