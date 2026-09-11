/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;

class CalculatorBaseCostTest extends CostTestSupport {
    @Test
    @DisplayName("simple SELECT uses the DML base cost")
    void shouldReturnBaseCostForSimpleSelect() {
        float cost = estimateCost("SELECT", "SELECT * FROM emp");

        assertEquals(0.2f, cost, DELTA);
    }

    @Test
    @DisplayName("simple TABLE DDL reflects the current TABLE keyword weight")
    void shouldReflectCurrentCostForSimpleTableDdl() {
        float cost = estimateCost(AnalyzerStatementTypes.TYPE_DDL_TABLE, "CREATE TABLE t1 (id INT)");

        assertEquals(2.1f, cost, DELTA);
    }

    @Test
    @DisplayName("trigger DDL uses the unsupported trigger base cost")
    void shouldReturnTriggerBaseCost() {
        float cost =
                estimateCost(
                        AnalyzerStatementTypes.TYPE_DDL_TRIGGER,
                        "CREATE OR REPLACE TRIGGER tr BEFORE INSERT ON t BEGIN NULL; END;");

        assertEquals(10.0f, cost, DELTA);
    }
}
