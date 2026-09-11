/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.core.runner.PlcsqlChecker.PlcsqlCheckResult;

class PlcsqlCheckerTest {
    private static final String AUTONOMOUS_TRANSACTION_PROCEDURE =
            """
            CREATE OR REPLACE PROCEDURE CUBRID.PROC_PL_FAIL
            AS
                PRAGMA AUTONOMOUS_TRANSACTION;
            BEGIN
                NULL;
                COMMIT;
            END;
            """;
    private static final String MISSING_SEMICOLON_PROCEDURE =
            """
            CREATE OR REPLACE PROCEDURE CUBRID.PROC_PL_FAIL
            AS
            BEGIN
                NULL
                COMMIT;
            END;
            """;

    @Test
    void shouldAcceptAutonomousTransactionPragmaProcedureWithoutStaticSqls() throws Exception {
        try (PlcsqlChecker checker = new PlcsqlChecker()) {
            PlcsqlCheckResult result =
                    checker.checkSQLAndGetStaticSqls(AUTONOMOUS_TRANSACTION_PROCEDURE);

            assertNotNull(result);
            assertEquals(0, result.getStaticSqls().size());
        }
    }

    @Test
    void shouldFailWhenStatementSemicolonIsMissing() throws Exception {
        try (PlcsqlChecker checker = new PlcsqlChecker()) {
            SQLParserException error =
                    assertThrows(
                            SQLParserException.class,
                            () -> checker.checkSQLAndGetStaticSqls(MISSING_SEMICOLON_PROCEDURE));

            assertTrue(error.getMessage().contains("In line"));
        }
    }
}
