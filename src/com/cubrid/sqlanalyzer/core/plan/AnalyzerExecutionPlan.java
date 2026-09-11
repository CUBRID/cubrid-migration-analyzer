/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AnalyzerExecutionPlan {
    private final Map<AnalyzerExecutionPhase, List<AnalyzerStatement>> statementsByPhase =
            new EnumMap<>(AnalyzerExecutionPhase.class);
    private int statementCount;
    private List<AnalyzerStatement> orderedStatements;

    public void add(AnalyzerExecutionPhase phase, AnalyzerStatement statement) {
        Objects.requireNonNull(phase, "phase");
        if (statement != null) {
            statementsByPhase
                    .computeIfAbsent(phase, ignored -> new ArrayList<>())
                    .add(statement);
            statementCount++;
            orderedStatements = null;
        }
    }

    public void addAll(AnalyzerExecutionPlan plan) {
        if (plan == null) {
            return;
        }

        for (AnalyzerExecutionPhase phase : AnalyzerExecutionPhase.values()) {
            for (AnalyzerStatement statement :
                    plan.statementsByPhase.getOrDefault(phase, Collections.emptyList())) {
                add(phase, statement);
            }
        }
    }

    public List<AnalyzerStatement> getStatements() {
        if (orderedStatements == null) {
            List<AnalyzerStatement> statements = new ArrayList<>(statementCount);
            for (AnalyzerExecutionPhase phase : AnalyzerExecutionPhase.values()) {
                statements.addAll(
                        statementsByPhase.getOrDefault(phase, Collections.emptyList()));
            }
            orderedStatements = Collections.unmodifiableList(statements);
        }
        return orderedStatements;
    }

    public boolean isEmpty() {
        return statementCount == 0;
    }
}
