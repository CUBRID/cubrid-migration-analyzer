/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.plan;

import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_FOREIGN_KEY;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_FUNCTION_BODY;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_FUNCTION_HEADER;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_GRANT;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_INDEX;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_PRIMARY_KEY;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_PROCEDURE_BODY;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_PROCEDURE_HEADER;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_SEQUENCE;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_SYNONYM;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_TABLE;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_TRIGGER;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_VIEW_ALTER;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerExecutionPhase.DDL_VIEW_CREATE;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_FK;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_FUNC_BODY;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_FUNC_HEADER;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_GRANT;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_INDEX;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_PK;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_PROC_BODY;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_PROC_HEADER;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_SEQUENCE;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_SYNONYM;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_TABLE;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_TRIGGER;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_VIEW_ALTER;
import static com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes.TYPE_DDL_VIEW_CREATE;

import com.cubrid.cubridmigration.core.dbobject.FK;
import com.cubrid.cubridmigration.core.dbobject.Grant;
import com.cubrid.cubridmigration.core.dbobject.Index;
import com.cubrid.cubridmigration.core.dbobject.PK;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlFunction;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlProcedure;
import com.cubrid.cubridmigration.core.dbobject.Sequence;
import com.cubrid.cubridmigration.core.dbobject.Synonym;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.Trigger;
import com.cubrid.cubridmigration.core.dbobject.View;
import com.cubrid.cubridmigration.core.engine.config.SourceGrantConfig;
import com.cubrid.cubridmigration.cubrid.CUBRIDSQLHelper;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;

public class CatalogDDLPlanBuilder implements AnalyzerExecutionPlanBuilder {
    @Override
    public AnalyzerExecutionPlan build(AnalyzerConfiguration config) {
        AnalyzerExecutionPlan plan = new AnalyzerExecutionPlan();
        CUBRIDSQLHelper helper = CUBRIDSQLHelper.getInstance(null);

        int seq = 0;
        for (Table table : config.getTargetTableSchema()) {
            String sql = helper.getTableDDL(table, config.isAddUserSchema());
            plan.add(
                    DDL_TABLE,
                    new AnalyzerStatement(
                            TYPE_DDL_TABLE,
                            "TABLE_" + (++seq),
                            sql,
                            objectName(table.getOwner(), table.getName())));
        }

        seq = 0;
        for (Table table : config.getTargetTableSchema()) {
            PK pk = table.getPk();
            if (pk == null) {
                continue;
            }
            String sql =
                    helper.getPKDDL(
                            table.getOwner(),
                            table.getName(),
                            pk.getName(),
                            pk.getPkColumns(),
                            config.isAddUserSchema());
            plan.add(
                    DDL_PRIMARY_KEY,
                    new AnalyzerStatement(
                            TYPE_DDL_PK,
                            "PK_" + (++seq),
                            sql,
                            objectName(table.getOwner(), table.getName())));
        }

        seq = 0;
        for (Table table : config.getTargetTableSchema()) {
            for (FK fk : table.getFks()) {
                String sql =
                        helper.getFKDDL(
                                table.getOwner(), table.getName(), fk, config.isAddUserSchema());
                plan.add(
                        DDL_FOREIGN_KEY,
                        new AnalyzerStatement(
                                TYPE_DDL_FK,
                                "FK_" + (++seq),
                                sql,
                                objectName(table.getOwner(), table.getName())));
            }
        }

        seq = 0;
        for (Table table : config.getTargetTableSchema()) {
            for (Index index : table.getIndexes()) {
                String sql =
                        helper.getIndexDDL(
                                table.getOwner(),
                                table.getName(),
                                index,
                                "",
                                config.isAddUserSchema());
                plan.add(
                        DDL_INDEX,
                        new AnalyzerStatement(
                                TYPE_DDL_INDEX,
                                "INDEX_" + (++seq),
                                sql,
                                objectName(table.getOwner(), table.getName())));
            }
        }

        seq = 0;
        for (Sequence sequence : config.getTargetSerialSchema()) {
            String sql = helper.getSequenceDDL(sequence, config.isAddUserSchema());
            plan.add(
                    DDL_SEQUENCE,
                    new AnalyzerStatement(
                            TYPE_DDL_SEQUENCE,
                            "SEQ_" + (++seq),
                            sql,
                            objectName(sequence.getOwner(), sequence.getName())));
        }

        seq = 0;
        for (View view : config.getTargetViewSchema()) {
            String sql = helper.getViewDDL(view, config.isAddUserSchema());
            plan.add(
                    DDL_VIEW_CREATE,
                    new AnalyzerStatement(
                            TYPE_DDL_VIEW_CREATE,
                            "VIEW_" + (++seq),
                            sql,
                            objectName(view.getOwner(), view.getName())));
        }

        seq = 0;
        for (View view : config.getTargetViewSchema()) {
            String sql = helper.getViewAlterDDL(view, config.isAddUserSchema());
            if (CUBRIDSQLHelper.SQL_NULL.equals(sql)) {
                continue;
            }
            plan.add(
                    DDL_VIEW_ALTER,
                    new AnalyzerStatement(
                            TYPE_DDL_VIEW_ALTER,
                            "VIEW_ALTER_" + (++seq),
                            sql,
                            objectName(view.getOwner(), view.getName())));
        }

        seq = 0;
        for (Synonym synonym : config.getTargetSynonymSchema()) {
            String sql = helper.getSynonymDDL(synonym, config.isAddUserSchema());
            plan.add(
                    DDL_SYNONYM,
                    new AnalyzerStatement(
                            TYPE_DDL_SYNONYM,
                            "SYNONYM_" + (++seq),
                            sql,
                            objectName(synonym.getOwner(), synonym.getName())));
        }

        seq = 0;
        for (SourceGrantConfig grantConfig : config.getExpGrantCfg()) {
            Grant grant = config.getTargetGrantSchema(grantConfig.getTarget());
            if (grant == null) {
                continue;
            }
            String sql = helper.getGrantDDL(grant, config.isAddUserSchema());
            plan.add(
                    DDL_GRANT,
                    new AnalyzerStatement(TYPE_DDL_GRANT, "GRANT_" + (++seq), sql));
        }

        seq = 0;
        for (PlcsqlProcedure procedure : config.getTargetPlcsqlProcedureSchema()) {
            String sql = helper.getPlcsqlProcedureHeaderDDL(procedure, config.isAddUserSchema());
            plan.add(
                    DDL_PROCEDURE_HEADER,
                    new AnalyzerStatement(
                            TYPE_DDL_PROC_HEADER,
                            "PROC_" + (++seq),
                            sql,
                            objectName(procedure.getOwner(), procedure.getName())));
        }

        seq = 0;
        for (PlcsqlProcedure procedure : config.getTargetPlcsqlProcedureSchema()) {
            String sql = helper.getPlcsqlProcedureDDL(procedure, config.isAddUserSchema());
            plan.add(
                    DDL_PROCEDURE_BODY,
                    new AnalyzerStatement(
                            TYPE_DDL_PROC_BODY,
                            "PROC_BODY_" + (++seq),
                            sql,
                            objectName(procedure.getOwner(), procedure.getName())));
        }

        seq = 0;
        for (PlcsqlFunction function : config.getTargetPlcsqlFunctionSchema()) {
            String sql = helper.getPlcsqlFunctionHeaderDDL(function, config.isAddUserSchema());
            plan.add(
                    DDL_FUNCTION_HEADER,
                    new AnalyzerStatement(
                            TYPE_DDL_FUNC_HEADER,
                            "FUNC_" + (++seq),
                            sql,
                            objectName(function.getOwner(), function.getName())));
        }

        seq = 0;
        for (PlcsqlFunction function : config.getTargetPlcsqlFunctionSchema()) {
            String sql = helper.getPlcsqlFunctionDDL(function, config.isAddUserSchema());
            plan.add(
                    DDL_FUNCTION_BODY,
                    new AnalyzerStatement(
                            TYPE_DDL_FUNC_BODY,
                            "FUNC_BODY_" + (++seq),
                            sql,
                            objectName(function.getOwner(), function.getName())));
        }

        seq = 0;
        for (String triggerConfig : config.getExpTriggerCfg()) {
            Trigger trigger = getExportTrigger(config, triggerConfig);
            if (trigger == null) {
                continue;
            }
            String sql = trigger.getDDL();
            if (sql == null || sql.trim().isEmpty()) {
                sql = triggerConfig;
            }
            plan.add(
                    DDL_TRIGGER,
                    new AnalyzerStatement(
                            TYPE_DDL_TRIGGER,
                            "TRIGGER_" + (++seq),
                            sql,
                            objectName(trigger.getOwner(), trigger.getName())));
        }

        return plan;
    }

    private String objectName(String owner, String name) {
        if (name == null || name.isBlank()) {
            return owner == null ? "" : owner;
        }
        if (owner == null || owner.isBlank()) {
            return name;
        }
        return owner + "." + name;
    }

    private Trigger getExportTrigger(AnalyzerConfiguration config, String triggerConfig) {
        if (triggerConfig == null || triggerConfig.trim().isEmpty()) {
            return null;
        }

        String schema = null;
        String name = triggerConfig;
        String[] parts = triggerConfig.split("\\.");
        if (parts.length > 1) {
            schema = parts[0];
            name = parts[1];
        }
        return config.getExpTrigger(schema, name);
    }
}
