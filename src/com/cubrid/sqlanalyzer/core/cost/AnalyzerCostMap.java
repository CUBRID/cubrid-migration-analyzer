/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.cost;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AnalyzerCostMap {
    public static final Map<String, Float> ORA2PG_UNCOVERED_SCORE_MAP =
            createOra2PgUncoveredScoreMap();

    public static final Map<String, Float> ORA2PG_ORA_FUNCTION_WEIGHT_MAP =
            createOra2PgOraFunctionWeightMap();

    private AnalyzerCostMap() {
    }

    private static Map<String, Float> createOra2PgUncoveredScoreMap() {
        Map<String, Float> map = new LinkedHashMap<String, Float>();

        map.put("TRUNC", 0.1f);
        map.put("IS TABLE OF", 4.0f);
        map.put("OUTER JOIN", 2.0f);
        map.put("CONNECT BY", 3.0f);
        map.put("BULK COLLECT", 5.0f);
        map.put("GOTO", 2.0f);
        map.put("FORALL", 1.0f);
        map.put("ROWNUM", 1.0f);
        map.put("NOTFOUND", 0.0f);
        map.put("ISOPEN", 1.0f);
        map.put("ROWCOUNT", 1.0f);
        map.put("ROWID", 2.0f);
        map.put("UROWID", 2.0f);
        map.put("IS RECORD", 1.0f);
        map.put("SQLCODE", 1.0f);
        map.put("TABLE", 2.0f);
        map.put("DBMS_", 3.0f);
        map.put("DBMS_OUTPUT.put", 1.0f);
        map.put("UTL_", 3.0f);
        map.put("CTX_", 3.0f);
        // In Ora2Pg, EXTRACT is defined twice and the later value wins.
        map.put("EXTRACT", 3.0f);
        map.put("EXCEPTION", 2.0f);
        map.put("TO_NUMBER", 0.1f);
        map.put("REGEXP_LIKE", 0.1f);
        map.put("REGEXP_COUNT", 0.2f);
        map.put("REGEXP_INSTR", 1.0f);
        map.put("REGEXP_SUBSTR", 1.0f);
        map.put("TG_OP", 0.0f);
        map.put("CURSOR", 1.0f);
        map.put("PIPE ROW", 1.0f);
        map.put("ORA_ROWSCN", 3.0f);
        map.put("SAVEPOINT", 1.0f);
        map.put("DBLINK", 1.0f);
        map.put("PLVDATE", 2.0f);
        map.put("PLVSTR", 2.0f);
        map.put("PLVCHR", 2.0f);
        map.put("PLVSUBST", 2.0f);
        map.put("PLVLEX", 2.0f);
        map.put("PLUNIT", 2.0f);
        map.put("ADD_MONTHS", 0.1f);
        map.put("LAST_DAY", 1.0f);
        map.put("NEXT_DAY", 1.0f);
        map.put("MONTHS_BETWEEN", 1.0f);
        map.put("SDO_", 3.0f);
        map.put("PRAGMA", 3.0f);
        map.put("MDSYS", 1.0f);
        map.put("MERGE INTO", 3.0f);
        map.put("COMMIT", 1.0f);
        map.put("CONTAINS", 1.0f);
        map.put("SCORE", 1.0f);
        map.put("FUZZY", 1.0f);
        map.put("NEAR", 1.0f);
        map.put("TO_CHAR", 0.1f);
        map.put("TO_NCHAR", 0.1f);
        map.put("ANYDATA", 2.0f);
        map.put("CONCAT", 0.1f);
        map.put("TIMEZONE", 1.0f);
        map.put("JSON", 3.0f);
        map.put("TO_CLOB", 0.1f);
        map.put("XMLTYPE", 3.0f);
        map.put("CREATENONSCHEMABASEDXML", 3.0f);
        map.put("CREATESCHEMABASEDXML", 3.0f);
        map.put("CREATEXML", 3.0f);
        map.put("EXISTSNODE", 3.0f);
        map.put("GETNAMESPACE", 3.0f);
        map.put("GETROOTELEMENT", 3.0f);
        map.put("GETSCHEMAURL", 3.0f);
        map.put("ISFRAGMENT", 3.0f);
        map.put("ISSCHEMABASED", 3.0f);
        map.put("ISSCHEMAVALID", 3.0f);
        map.put("ISSCHEMAVALIDATED", 3.0f);
        map.put("SCHEMAVALIDATE", 3.0f);
        map.put("SETSCHEMAVALIDATED", 3.0f);
        map.put("TOOBJECT", 3.0f);
        map.put("TRANSFORM", 3.0f);
        map.put("FND_CONC_GLOBAL", 3.0f);
        map.put("FND_CONCURRENT", 3.0f);
        map.put("FND_FILE", 1.0f);
        map.put("FND_PROGRAM", 3.0f);
        map.put("FND_SET", 3.0f);
        map.put("FND_REQUEST", 3.0f);
        map.put("FND_REQUEST_INFO", 3.0f);
        map.put("FND_SUBMIT", 3.0f);
        map.put("FND_GLOBAL", 1.0f);
        map.put("FND_PROFILE", 1.0f);
        map.put("FND_CURRENCY", 3.0f);
        map.put("FND_ORG", 3.0f);
        map.put("FND_STANDARD", 3.0f);
        map.put("FND_UTILITIES", 3.0f);
        map.put("ADD CONSTRAINT", 3.0f);
        map.put("HTP", 0.2f);
        map.put("'SSSSS'", 2.0f);
        map.put("'J'", 2.0f);
        map.put("WHEN OTHER", 10.0f);

        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Float> createOra2PgOraFunctionWeightMap() {
        Map<String, Float> map = new LinkedHashMap<String, Float>();

        map.put("AsciiStr", 1.0f);
        map.put("Compose", 1.0f);
        map.put("Decompose", 1.0f);
        map.put("Dump", 1.0f);
        map.put("VSize", 1.0f);
        map.put("Bin_To_Num", 1.0f);
        map.put("CharToRowid", 1.0f);
        map.put("HexToRaw", 1.0f);
        map.put("NumToDSInterval", 1.0f);
        map.put("NumToYMInterval", 1.0f);
        map.put("RawToHex", 1.0f);
        map.put("To_Clob", 1.0f);
        map.put("To_DSInterval", 1.0f);
        map.put("To_Lob", 1.0f);
        map.put("To_Multi_Byte", 1.0f);
        map.put("To_NClob", 1.0f);
        map.put("To_Single_Byte", 1.0f);
        map.put("To_YMInterval", 1.0f);
        map.put("BFilename", 1.0f);
        map.put("Cardinality", 1.0f);
        map.put("Group_ID", 1.0f);
        map.put("LNNVL", 1.0f);
        map.put("NANVL", 1.0f);
        map.put("Sys_Context", 1.0f);
        map.put("Uid", 1.0f);
        map.put("UserEnv", 1.0f);
        map.put("BitAnd", 1.0f);
        map.put("Median", 1.0f);
        map.put("Remainder", 1.0f);
        map.put("DbTimeZone", 1.0f);
        map.put("New_Time", 1.0f);
        map.put("SessionTimeZone", 1.0f);
        map.put("Tz_Offset", 1.0f);
        map.put("Get_Env", 1.0f);
        map.put("From_Tz", 1.0f);

        return Collections.unmodifiableMap(map);
    }
}
