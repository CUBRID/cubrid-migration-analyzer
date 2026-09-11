/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.cost;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bson.Document;

public final class AnalyzerCostSettingsLoader {
    private static final String DEFAULT_COST_SETTINGS_PATH = "settings/cost.json";
    private static final String WORKSPACE_COST_SETTINGS_PATH =
            "com.cubrid.SQLAnalyzer/settings/cost.json";

    private AnalyzerCostSettingsLoader() {
    }

    public static AnalyzerCostSettings loadDefault() {
        Path costSettingsPath = resolveDefaultCostSettingsPath();
        if (!Files.isRegularFile(costSettingsPath)) {
            return AnalyzerCostSettings.defaults();
        }
        return load(costSettingsPath);
    }

    public static AnalyzerCostSettings load(Path costSettingsPath) {
        AnalyzerCostSettings defaultSettings = AnalyzerCostSettings.defaults();
        if (costSettingsPath == null || !Files.isRegularFile(costSettingsPath)) {
            return defaultSettings;
        }

        Document document = parseDocument(costSettingsPath);
        Map<String, Float> baseCostMap =
                new LinkedHashMap<String, Float>(defaultSettings.getBaseCostMap());
        Map<String, Float> heuristicCostMap =
                new LinkedHashMap<String, Float>(defaultSettings.getHeuristicCostMap());
        Map<String, Float> uncoveredScoreMap =
                new LinkedHashMap<String, Float>(defaultSettings.getUncoveredScoreMap());
        Map<String, Float> oraFunctionWeightMap =
                new LinkedHashMap<String, Float>(defaultSettings.getOraFunctionWeightMap());

        mergeKnownCostMap(
                getDocument(document, "base"),
                baseCostMap,
                "base",
                AnalyzerCostSettings.defaultBaseCostMap().keySet());
        mergeKnownCostMap(
                getDocument(document, "heuristic"),
                heuristicCostMap,
                "heuristic",
                AnalyzerCostSettings.defaultHeuristicCostMap().keySet());

        Document ora2pgDocument = getDocument(document, "ora2pg");
        if (ora2pgDocument != null) {
            mergeOpenCostMap(getDocument(ora2pgDocument, "uncovered"), uncoveredScoreMap, "ora2pg.uncovered");
            mergeOpenCostMap(getDocument(ora2pgDocument, "function"), oraFunctionWeightMap, "ora2pg.function");
        }

        return new AnalyzerCostSettings(
                baseCostMap, heuristicCostMap, uncoveredScoreMap, oraFunctionWeightMap);
    }

    private static Path resolveDefaultCostSettingsPath() {
        Path defaultPath = Paths.get(DEFAULT_COST_SETTINGS_PATH);
        if (Files.isRegularFile(defaultPath)) {
            return defaultPath;
        }

        Path workspacePath = Paths.get(WORKSPACE_COST_SETTINGS_PATH);
        if (Files.isRegularFile(workspacePath)) {
            return workspacePath;
        }

        return defaultPath;
    }

    private static Document parseDocument(Path costSettingsPath) {
        try {
            String json = Files.readString(costSettingsPath, StandardCharsets.UTF_8).trim();
            if (json.isEmpty()) {
                return new Document();
            }
            return Document.parse(json);
        } catch (IOException ex) {
            throw new IllegalArgumentException(
                    "Failed to read cost settings file: " + costSettingsPath, ex);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Failed to parse cost settings file: " + costSettingsPath, ex);
        }
    }

    private static Document getDocument(Document parent, String key) {
        Object value = parent == null ? null : parent.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Document)) {
            throw new IllegalArgumentException("Cost settings section must be an object: " + key);
        }
        return (Document) value;
    }

    private static void mergeKnownCostMap(
            Document section,
            Map<String, Float> target,
            String sectionName,
            Set<String> allowedKeys) {
        if (section == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String key = entry.getKey();
            if (!allowedKeys.contains(key)) {
                throw new IllegalArgumentException(
                        "Unknown cost setting key: " + sectionName + "." + key);
            }
            target.put(key, parseCostValue(sectionName, key, entry.getValue()));
        }
    }

    private static void mergeOpenCostMap(
            Document section, Map<String, Float> target, String sectionName) {
        if (section == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : section.entrySet()) {
            target.put(entry.getKey(), parseCostValue(sectionName, entry.getKey(), entry.getValue()));
        }
    }

    private static float parseCostValue(String sectionName, String key, Object rawValue) {
        float value;
        if (rawValue instanceof Number) {
            value = ((Number) rawValue).floatValue();
        } else if (rawValue instanceof String) {
            try {
                value = Float.parseFloat(((String) rawValue).trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "Cost setting must be numeric: " + sectionName + "." + key, ex);
            }
        } else {
            throw new IllegalArgumentException(
                    "Cost setting must be numeric: " + sectionName + "." + key);
        }

        if (Float.isNaN(value) || Float.isInfinite(value) || value < 0.0f) {
            throw new IllegalArgumentException(
                    "Cost setting must be a non-negative finite number: " + sectionName + "." + key);
        }
        return value;
    }
}
