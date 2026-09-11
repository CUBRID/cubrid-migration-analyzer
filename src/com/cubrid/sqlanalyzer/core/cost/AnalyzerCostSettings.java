/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.cost;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AnalyzerCostSettings {
    public static final String BASE_TABLE = "table";
    public static final String BASE_VIEW = "view";
    public static final String BASE_INDEX = "index";
    public static final String BASE_SEQUENCE = "sequence";
    public static final String BASE_SYNONYM = "synonym";
    public static final String BASE_FUNCTION = "function";
    public static final String BASE_PROCEDURE = "procedure";
    public static final String BASE_DML = "dml";
    public static final String BASE_GRANT = "grant";
    public static final String BASE_PK = "pk";
    public static final String BASE_FK = "fk";
    public static final String BASE_TRIGGER = "trigger";

    public static final String HEURISTIC_TABLE_CHECK_CONSTRAINT = "table.checkConstraint";
    public static final String HEURISTIC_TABLE_ENCRYPT_KEYWORD = "table.encryptKeyword";
    public static final String HEURISTIC_INDEX_FUNCTION_BASED = "index.functionBased";
    public static final String HEURISTIC_INDEX_REVERSE_KEYWORD = "index.reverseKeyword";
    public static final String HEURISTIC_VIEW_SUBQUERY = "view.subquery";
    public static final String HEURISTIC_VIEW_JOIN = "view.join";
    public static final String HEURISTIC_DML_JOIN = "dml.join";
    public static final String HEURISTIC_DML_SUBQUERY = "dml.subquery";
    public static final String HEURISTIC_GRANT_PRIVILEGE = "grant.privilege";
    public static final String HEURISTIC_GRANT_MISSING_ON = "grant.missingOn";
    public static final String HEURISTIC_GRANT_MISSING_TO = "grant.missingTo";
    public static final String HEURISTIC_FK_ON_DELETE = "fk.onDelete";
    public static final String HEURISTIC_LENGTH_200_499 = "length.200_499";
    public static final String HEURISTIC_LENGTH_500_999 = "length.500_999";
    public static final String HEURISTIC_LENGTH_1000_PLUS = "length.1000Plus";

    private final Map<String, Float> baseCostMap;
    private final Map<String, Float> heuristicCostMap;
    private final Map<String, Float> uncoveredScoreMap;
    private final Map<String, Float> oraFunctionWeightMap;

    public AnalyzerCostSettings(
            Map<String, Float> baseCostMap,
            Map<String, Float> heuristicCostMap,
            Map<String, Float> uncoveredScoreMap,
            Map<String, Float> oraFunctionWeightMap) {
        this.baseCostMap = copy(baseCostMap);
        this.heuristicCostMap = copy(heuristicCostMap);
        this.uncoveredScoreMap = copy(uncoveredScoreMap);
        this.oraFunctionWeightMap = copy(oraFunctionWeightMap);
    }

    public static AnalyzerCostSettings defaults() {
        return new AnalyzerCostSettings(
                defaultBaseCostMap(),
                defaultHeuristicCostMap(),
                AnalyzerCostMap.ORA2PG_UNCOVERED_SCORE_MAP,
                AnalyzerCostMap.ORA2PG_ORA_FUNCTION_WEIGHT_MAP);
    }

    public float getBaseCost(String key) {
        return getRequiredCost(baseCostMap, "base", key);
    }

    public float getHeuristicCost(String key) {
        return getRequiredCost(heuristicCostMap, "heuristic", key);
    }

    public Map<String, Float> getBaseCostMap() {
        return baseCostMap;
    }

    public Map<String, Float> getHeuristicCostMap() {
        return heuristicCostMap;
    }

    public Map<String, Float> getUncoveredScoreMap() {
        return uncoveredScoreMap;
    }

    public Map<String, Float> getOraFunctionWeightMap() {
        return oraFunctionWeightMap;
    }

    public static Map<String, Float> defaultBaseCostMap() {
        Map<String, Float> map = new LinkedHashMap<String, Float>();
        map.put(BASE_TABLE, 0.1f);
        map.put(BASE_VIEW, 1.0f);
        map.put(BASE_INDEX, 0.1f);
        map.put(BASE_SEQUENCE, 0.1f);
        map.put(BASE_SYNONYM, 0.1f);
        map.put(BASE_FUNCTION, 1.0f);
        map.put(BASE_PROCEDURE, 1.0f);
        map.put(BASE_DML, 0.2f);
        map.put(BASE_GRANT, 0.1f);
        map.put(BASE_PK, 0.1f);
        map.put(BASE_FK, 0.1f);
        map.put(BASE_TRIGGER, 10.0f);
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, Float> defaultHeuristicCostMap() {
        Map<String, Float> map = new LinkedHashMap<String, Float>();
        map.put(HEURISTIC_TABLE_CHECK_CONSTRAINT, 0.1f);
        map.put(HEURISTIC_TABLE_ENCRYPT_KEYWORD, 20.0f);
        map.put(HEURISTIC_INDEX_FUNCTION_BASED, 0.2f);
        map.put(HEURISTIC_INDEX_REVERSE_KEYWORD, 1.0f);
        map.put(HEURISTIC_VIEW_SUBQUERY, 1.0f);
        map.put(HEURISTIC_VIEW_JOIN, 1.0f);
        map.put(HEURISTIC_DML_JOIN, 0.5f);
        map.put(HEURISTIC_DML_SUBQUERY, 0.5f);
        map.put(HEURISTIC_GRANT_PRIVILEGE, 0.1f);
        map.put(HEURISTIC_GRANT_MISSING_ON, 1.0f);
        map.put(HEURISTIC_GRANT_MISSING_TO, 1.0f);
        map.put(HEURISTIC_FK_ON_DELETE, 0.5f);
        map.put(HEURISTIC_LENGTH_200_499, 1.0f);
        map.put(HEURISTIC_LENGTH_500_999, 2.0f);
        map.put(HEURISTIC_LENGTH_1000_PLUS, 4.0f);
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Float> copy(Map<String, Float> source) {
        if (source == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Float>(source));
    }

    private static float getRequiredCost(Map<String, Float> map, String sectionName, String key) {
        Float value = map.get(key);
        if (value == null) {
            throw new IllegalStateException("Missing " + sectionName + " cost setting: " + key);
        }
        return value.floatValue();
    }
}
