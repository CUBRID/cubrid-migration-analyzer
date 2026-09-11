/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressCounts;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressObjectCount;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPlan;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;

class ObjectProgressTracker {
    private int totalCount;
    private final Map<String, MutableObjectProgressCount> objectCounts =
            new LinkedHashMap<String, MutableObjectProgressCount>();

    private ObjectProgressTracker(int totalCount) {
        this.totalCount = totalCount;
    }

    static ObjectProgressTracker from(AnalyzerExecutionPlan executionPlan) {
        ObjectProgressTracker tracker =
                new ObjectProgressTracker(executionPlan.getStatements().size());
        for (AnalyzerStatement statement : executionPlan.getStatements()) {
            tracker.getOrCreate(displayObjectType(statement)).totalCount++;
        }
        return tracker;
    }

    void addDiscovered(AnalyzerStatement statement) {
        totalCount++;
        getOrCreate(displayObjectType(statement)).totalCount++;
    }

    void record(AnalyzerStatement statement, boolean success) {
        MutableObjectProgressCount count = getOrCreate(displayObjectType(statement));
        if (success) {
            count.succeededCount++;
            return;
        }
        count.failedCount++;
    }

    AnalyzerProgressCounts snapshot(int completedCount, int succeededCount, int failedCount) {
        List<AnalyzerProgressObjectCount> snapshots =
                new ArrayList<AnalyzerProgressObjectCount>();
        for (Map.Entry<String, MutableObjectProgressCount> entry : objectCounts.entrySet()) {
            MutableObjectProgressCount count = entry.getValue();
            snapshots.add(
                    new AnalyzerProgressObjectCount(
                            entry.getKey(),
                            count.totalCount,
                            count.succeededCount,
                            count.failedCount));
        }
        return new AnalyzerProgressCounts(
                totalCount,
                completedCount,
                succeededCount,
                failedCount,
                snapshots);
    }

    private MutableObjectProgressCount getOrCreate(String objectType) {
        MutableObjectProgressCount count = objectCounts.get(objectType);
        if (count == null) {
            count = new MutableObjectProgressCount();
            objectCounts.put(objectType, count);
        }
        return count;
    }

    private static String displayObjectType(AnalyzerStatement statement) {
        if (statement == null || statement.getType() == null || statement.getType().isEmpty()) {
            return "UNKNOWN";
        }

        String type = statement.getType();
        if (type.startsWith("DDL_")) {
            return type.substring("DDL_".length());
        }
        return type;
    }

    private static class MutableObjectProgressCount {
        private int totalCount;
        private int succeededCount;
        private int failedCount;
    }
}
