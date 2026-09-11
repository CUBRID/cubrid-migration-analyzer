/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import java.util.List;

import com.cubrid.cubridmigration.core.dbobject.PlcsqlFunction;
import com.cubrid.cubridmigration.core.dbobject.PlcsqlProcedure;
import com.cubrid.cubridmigration.core.dbobject.Sequence;
import com.cubrid.cubridmigration.core.dbobject.Synonym;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.View;
import com.cubrid.cubridmigration.core.engine.config.SourceGrantConfig;
import com.cubrid.cubridmigration.cubrid.CUBRIDSQLHelper;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;

class AnalyzerCleanupQueryBuilder {

    String build(AnalyzerSession session, AnalyzerStatement statement) {
        if (!isDDL(statement)) {
            return null;
        }

        AnalyzerConfiguration config = session.getConfig();
        CUBRIDSQLHelper helper = CUBRIDSQLHelper.getInstance(null);

        if (AnalyzerStatementTypes.TYPE_DDL_TABLE.equals(statement.getType())) {
            Table table = getStatementObject(
                    config.getTargetTableSchema(), statement.getId(), "TABLE_", "table");
            return "DROP TABLE "
                    + helper.getOwnerNameWithDot(table.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(table.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_VIEW.equals(statement.getType())
                || AnalyzerStatementTypes.TYPE_DDL_VIEW_CREATE.equals(statement.getType())) {
            View view = getStatementObject(
                    config.getTargetViewSchema(), statement.getId(), "VIEW_", "view");
            return "DROP VIEW "
                    + helper.getOwnerNameWithDot(view.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(view.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_SEQUENCE.equals(statement.getType())) {
            Sequence sequence = getStatementObject(
                    config.getTargetSerialSchema(), statement.getId(), "SEQ_", "sequence");
            return "DROP SERIAL "
                    + helper.getOwnerNameWithDot(sequence.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(sequence.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_SYNONYM.equals(statement.getType())) {
            Synonym synonym = getStatementObject(
                    config.getTargetSynonymSchema(), statement.getId(), "SYNONYM_", "synonym");
            return "DROP SYNONYM "
                    + helper.getOwnerNameWithDot(synonym.getOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(synonym.getName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_GRANT.equals(statement.getType())) {
            SourceGrantConfig grant = getStatementObject(
                    config.getExpGrantCfg(), statement.getId(), "GRANT_", "grant");
            return "REVOKE "
                    + grant.getAuthType()
                    + " ON "
                    + helper.getOwnerNameWithDot(grant.getClassOwner(), config.isAddUserSchema())
                    + helper.getQuotedObjName(grant.getClassName())
                    + " FROM "
                    + helper.getQuotedObjName(grant.getGranteeName())
                    + ";";
        }

        if (AnalyzerStatementTypes.TYPE_DDL_PROC_HEADER.equals(statement.getType())) {
            PlcsqlProcedure procedure = getStatementObject(
                    config.getTargetPlcsqlProcedureSchema(), statement.getId(), "PROC_", "procedure");
            return helper.getPlcsqlProcedureDropDDL(procedure, config.isAddUserSchema());
        }

        if (AnalyzerStatementTypes.TYPE_DDL_FUNC_HEADER.equals(statement.getType())) {
            PlcsqlFunction function = getStatementObject(
                    config.getTargetPlcsqlFunctionSchema(), statement.getId(), "FUNC_", "function");
            return helper.getPlcsqlFunctionDropDDL(function, config.isAddUserSchema());
        }

        // Remaining DDL types do not require an independent cleanup query.
        return null;
    }

    private boolean isDDL(AnalyzerStatement statement) {
        return statement.getType() != null && statement.getType().startsWith("DDL_");
    }

    private <T> T getStatementObject(List<T> objects, String id, String prefix, String objectType) {
        int index = parseStatementIndex(id, prefix);
        int size = objects == null ? 0 : objects.size();
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                    "Unexpected "
                            + objectType
                            + " statement id: "
                            + id
                            + " (index="
                            + index
                            + ", size="
                            + size
                            + ")");
        }
        return objects.get(index);
    }

    private int parseStatementIndex(String id, String prefix) {
        if (id == null || !id.startsWith(prefix)) {
            throw new IllegalArgumentException("Unexpected statement id: " + id);
        }

        try {
            return Integer.parseInt(id.substring(prefix.length())) - 1;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Unexpected statement id: " + id, ex);
        }
    }
}
