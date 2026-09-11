/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.cost;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.model.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.report.AnalyzerReport;

class FailureCostCalculatorTest {
    @Test
    void shouldUseConfiguredCostSettingsWhenCalculatingFailureCost() {
        AnalyzerCostSettings defaults = AnalyzerCostSettings.defaults();
        Map<String, Float> baseCosts =
                new LinkedHashMap<String, Float>(defaults.getBaseCostMap());
        Map<String, Float> heuristicCosts =
                new LinkedHashMap<String, Float>(defaults.getHeuristicCostMap());
        Map<String, Float> uncoveredCosts =
                new LinkedHashMap<String, Float>(defaults.getUncoveredScoreMap());

        baseCosts.put(AnalyzerCostSettings.BASE_DML, 2.0f);
        heuristicCosts.put(AnalyzerCostSettings.HEURISTIC_DML_JOIN, 3.0f);
        uncoveredCosts.put("TRUNC", 4.0f);

        AnalyzerCostSettings settings = new AnalyzerCostSettings(
                baseCosts,
                heuristicCosts,
                uncoveredCosts,
                defaults.getOraFunctionWeightMap());
        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType("SELECT");
        failure.setSql("select TRUNC(created_at) from orders join users on orders.user_id = users.id");

        AnalyzerReport report = new AnalyzerReport();
        report.addFailure(failure);

        new FailureCostCalculator(settings).analyzeAfterExecution(report);

        assertEquals(9.0f, failure.getEstimatedCost(), 0.001f);
    }
}
