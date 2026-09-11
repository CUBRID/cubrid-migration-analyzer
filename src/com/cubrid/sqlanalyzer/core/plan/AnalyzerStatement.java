/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.plan;

public class AnalyzerStatement {
    private final String type;
    private final String id;
    private final String sql;
    private final String objectName;

    public AnalyzerStatement(String type, String id, String sql) {
        this(type, id, sql, null);
    }

    public AnalyzerStatement(String type, String id, String sql, String objectName) {
        this.type = type;
        this.id = id;
        this.sql = sql;
        this.objectName = objectName;
    }

    public String getType() { return type; }
    public String getId() { return id; }
    public String getSQL() { return sql; }
    public String getObjectName() { return objectName; }
}
