/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculatorLengthCostTest extends CostTestSupport {
    @Test
    @DisplayName("queries up to 199 characters do not add length cost")
    void shouldNotApplyLengthCostBelow200Characters() {
        float cost = estimateCost("SELECT", buildSqlWithLength(199));

        assertEquals(0.2f, cost, DELTA);
    }

    @Test
    @DisplayName("queries at 200 characters add 1.0 length cost")
    void shouldApplyLengthCostAt200CharacterBoundary() {
        float cost = estimateCost("SELECT", buildSqlWithLength(200));

        assertEquals(1.2f, cost, DELTA);
    }

    @Test
    @DisplayName("queries from 200 to 499 characters add 1.0 length cost")
    void shouldApplyOnePointZeroLengthCostUpTo499Characters() {
        float cost = estimateCost("SELECT", buildSqlWithLength(499));

        assertEquals(1.2f, cost, DELTA);
    }

    @Test
    @DisplayName("queries from 500 to 999 characters add 2.0 length cost")
    void shouldApplyTwoPointZeroLengthCostAt500Characters() {
        float cost = estimateCost("SELECT", buildSqlWithLength(500));

        assertEquals(2.2f, cost, DELTA);
    }

    @Test
    @DisplayName("queries up to 999 characters stay in the 2.0 length-cost tier")
    void shouldKeepTwoPointZeroLengthCostAt999Characters() {
        float cost = estimateCost("SELECT", buildSqlWithLength(999));

        assertEquals(2.2f, cost, DELTA);
    }

    @Test
    @DisplayName("queries at 1000 characters add 4.0 length cost")
    void shouldApplyFourPointZeroLengthCostAt1000Characters() {
        float cost = estimateCost("SELECT", buildSqlWithLength(1000));

        assertEquals(4.2f, cost, DELTA);
    }
}
