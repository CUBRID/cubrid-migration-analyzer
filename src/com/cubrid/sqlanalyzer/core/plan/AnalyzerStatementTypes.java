/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.plan;

public final class AnalyzerStatementTypes {
    public static final String TYPE_DDL_TABLE = "DDL_TABLE";
    public static final String TYPE_DDL_PK = "DDL_PK";
    public static final String TYPE_DDL_FK = "DDL_FK";
    public static final String TYPE_DDL_INDEX = "DDL_INDEX";
    public static final String TYPE_DDL_SEQUENCE = "DDL_SEQUENCE";
    public static final String TYPE_DDL_VIEW = "DDL_VIEW";
    public static final String TYPE_DDL_VIEW_CREATE = "DDL_VIEW_CREATE";
    public static final String TYPE_DDL_VIEW_ALTER = "DDL_VIEW_ALTER";
    public static final String TYPE_DDL_SYNONYM = "DDL_SYNONYM";
    public static final String TYPE_DDL_GRANT = "DDL_GRANT";
    public static final String TYPE_DDL_PROC_HEADER = "DDL_PROC_HEADER";
    public static final String TYPE_DDL_PROC_BODY = "DDL_PROC_BODY";
    public static final String TYPE_DDL_FUNC_HEADER = "DDL_FUNC_HEADER";
    public static final String TYPE_DDL_FUNC_BODY = "DDL_FUNC_BODY";
    public static final String TYPE_DDL_TRIGGER = "DDL_TRIGGER";
    public static final String TYPE_STATIC_SQL = "STATIC_SQL";

    private AnalyzerStatementTypes() {}
}
