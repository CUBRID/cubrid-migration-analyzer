/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.cost;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalyzerCostSettingsLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldMergeJsonOverridesWithDefaultCostSettings() throws Exception {
        Path costFile = tempDir.resolve("cost.json");
        Files.writeString(
                costFile,
                "{"
                        + "\"base\":{\"trigger\":8.0},"
                        + "\"heuristic\":{\"dml.join\":3.0},"
                        + "\"ora2pg\":{"
                        + "\"uncovered\":{\"TRUNC\":0.5,\"CUSTOM FEATURE\":7.0},"
                        + "\"function\":{\"CustomFunction\":2.0}"
                        + "}"
                        + "}");

        AnalyzerCostSettings settings = AnalyzerCostSettingsLoader.load(costFile);

        assertEquals(8.0f, settings.getBaseCost(AnalyzerCostSettings.BASE_TRIGGER), 0.001f);
        assertEquals(0.1f, settings.getBaseCost(AnalyzerCostSettings.BASE_TABLE), 0.001f);
        assertEquals(
                3.0f,
                settings.getHeuristicCost(AnalyzerCostSettings.HEURISTIC_DML_JOIN),
                0.001f);
        assertEquals(0.5f, settings.getUncoveredScoreMap().get("TRUNC"), 0.001f);
        assertEquals(7.0f, settings.getUncoveredScoreMap().get("CUSTOM FEATURE"), 0.001f);
        assertEquals(2.0f, settings.getOraFunctionWeightMap().get("CustomFunction"), 0.001f);
    }

    @Test
    void shouldRejectUnknownBaseCostKey() throws Exception {
        Path costFile = tempDir.resolve("cost.json");
        Files.writeString(costFile, "{\"base\":{\"unknown\":1.0}}");

        assertThrows(IllegalArgumentException.class, () -> AnalyzerCostSettingsLoader.load(costFile));
    }

    @Test
    void shouldRejectNegativeCostValue() throws Exception {
        Path costFile = tempDir.resolve("cost.json");
        Files.writeString(costFile, "{\"heuristic\":{\"dml.join\":-1.0}}");

        assertThrows(IllegalArgumentException.class, () -> AnalyzerCostSettingsLoader.load(costFile));
    }
}
