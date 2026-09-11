/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import java.util.Locale;

import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;
import com.cubrid.sqlanalyzer.core.runner.PlcsqlChecker.StaticSql;

class StaticSqlStatementBuilder {

    AnalyzerStatement build(
            AnalyzerStatement parentStatement, StaticSql staticSql, int staticSqlIndex) {
        String parentId = parentStatement.getId();
        if (parentId == null || parentId.isBlank()) {
            parentId = "PLCSQL";
        }

        StringBuilder id = new StringBuilder(parentId)
                .append("_STATIC_")
                .append(staticSqlIndex);
        if (staticSql.getRow() > 0) {
            id.append("_L").append(staticSql.getRow());
        }
        if (staticSql.getColumn() > 0) {
            id.append("_C").append(staticSql.getColumn());
        }

        return new AnalyzerStatement(
                inferStaticSqlType(staticSql.getCode()),
                id.toString(),
                staticSql.getCode(),
                staticSqlObjectName(parentStatement, staticSqlIndex));
    }

    private String staticSqlObjectName(AnalyzerStatement parentStatement, int staticSqlIndex) {
        String parentObjectName = parentStatement.getObjectName();
        if (parentObjectName == null || parentObjectName.isBlank()) {
            return "";
        }
        return parentObjectName + " / static SQL #" + staticSqlIndex;
    }

    private String inferStaticSqlType(String sql) {
        String normalizedSql = stripLeadingSqlComments(sql).stripLeading().toUpperCase(Locale.ENGLISH);
        if (startsWithKeyword(normalizedSql, "SELECT")) {
            return "SELECT";
        }
        if (startsWithKeyword(normalizedSql, "INSERT")) {
            return "INSERT";
        }
        if (startsWithKeyword(normalizedSql, "UPDATE")) {
            return "UPDATE";
        }
        if (startsWithKeyword(normalizedSql, "DELETE")) {
            return "DELETE";
        }
        return AnalyzerStatementTypes.TYPE_STATIC_SQL;
    }

    private String stripLeadingSqlComments(String sql) {
        String remaining = sql == null ? "" : sql;
        while (true) {
            remaining = remaining.stripLeading();
            if (remaining.startsWith("--")) {
                int lineEnd = remaining.indexOf('\n');
                if (lineEnd < 0) {
                    return "";
                }
                remaining = remaining.substring(lineEnd + 1);
                continue;
            }
            if (remaining.startsWith("/*")) {
                int commentEnd = remaining.indexOf("*/");
                if (commentEnd < 0) {
                    return "";
                }
                remaining = remaining.substring(commentEnd + 2);
                continue;
            }
            return remaining;
        }
    }

    private boolean startsWithKeyword(String sql, String keyword) {
        if (!sql.startsWith(keyword)) {
            return false;
        }
        if (sql.length() == keyword.length()) {
            return true;
        }

        char nextChar = sql.charAt(keyword.length());
        return !Character.isLetterOrDigit(nextChar) && nextChar != '_';
    }
}
