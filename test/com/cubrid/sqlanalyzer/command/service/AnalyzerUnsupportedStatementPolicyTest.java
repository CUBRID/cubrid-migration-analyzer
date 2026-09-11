/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;

class AnalyzerUnsupportedStatementPolicyTest {
    @Test
    void shouldRejectTriggerStatement() {
        AnalyzerStatement statement = new AnalyzerStatement(
                AnalyzerStatementTypes.TYPE_DDL_TRIGGER,
                "TRIGGER_1",
                "CREATE TRIGGER trg BEFORE INSERT ON t EXECUTE PRINT 'x';");

        assertEquals(
                AnalyzerUnsupportedStatementPolicy.TRIGGER_UNSUPPORTED_REASON,
                AnalyzerUnsupportedStatementPolicy.getUnsupportedReason(statement));
    }

    @Test
    void shouldNotRejectAutonomousTransactionKeyword() {
        AnalyzerStatement statement = new AnalyzerStatement(
                AnalyzerStatementTypes.TYPE_DDL_PROC_BODY,
                "PROC_BODY_1",
                "CREATE OR REPLACE PROCEDURE P AS\n"
                        + "    PRAGMA AUTONOMOUS_TRANSACTION;\n"
                        + "BEGIN\n"
                        + "    NULL;\n"
                        + "END;");

        assertNull(AnalyzerUnsupportedStatementPolicy.getUnsupportedReason(statement));
    }

    @Test
    void shouldIgnoreNullStatement() {
        assertNull(AnalyzerUnsupportedStatementPolicy.getUnsupportedReason(null));
    }

    @Test
    void shouldNotRejectRegularProcedureStatement() {
        AnalyzerStatement statement = new AnalyzerStatement(
                AnalyzerStatementTypes.TYPE_DDL_PROC_BODY,
                "PROC_BODY_1",
                "CREATE OR REPLACE PROCEDURE P AS\n"
                        + "BEGIN\n"
                        + "    NULL;\n"
                        + "END;");

        assertNull(AnalyzerUnsupportedStatementPolicy.getUnsupportedReason(statement));
    }
}
