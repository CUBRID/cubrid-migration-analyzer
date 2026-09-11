/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.plan;

import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DML_DELETE;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DML_INSERT;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DML_SELECT;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DML_UPDATE;

import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;

public class QueryDictionaryPlanBuilder implements AnalyzerExecutionPlanBuilder {
    @Override
    public AnalyzerExecutionPlan build(AnalyzerConfiguration config) {
        AnalyzerExecutionPlan plan = new AnalyzerExecutionPlan();
        QueryDictionary dict = config.getQueryDict();
        if (dict == null) {
            return plan;
        }

        dict.getSelectQueryMap().forEach((id, sql) ->
                plan.add(DML_SELECT, new AnalyzerStatement("SELECT", id, sql)));
        dict.getInsertQueryMap().forEach((id, sql) ->
                plan.add(DML_INSERT, new AnalyzerStatement("INSERT", id, sql)));
        dict.getUpdateQueryMap().forEach((id, sql) ->
                plan.add(DML_UPDATE, new AnalyzerStatement("UPDATE", id, sql)));
        dict.getDeleteQueryMap().forEach((id, sql) ->
                plan.add(DML_DELETE, new AnalyzerStatement("DELETE", id, sql)));

        return plan;
    }
}
