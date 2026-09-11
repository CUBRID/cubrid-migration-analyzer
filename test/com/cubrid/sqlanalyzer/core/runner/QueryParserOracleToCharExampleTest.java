/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Example showing how Oracle-derived TO_CHAR defaults reach the CUBRID parser. */
class QueryParserOracleToCharExampleTest {
    private final QueryParser parser = new QueryParser();

    @Test
    void parseToCharSysdateDefaultWithDoubleQuotedDdd() {
        String ddl =
                """
                CREATE TABLE oracle_date_format_example (
                    id INTEGER,
                    day_of_year VARCHAR(3) DEFAULT to_char(SYSDATE, "DDD")
                );
                """;

        String result = parser.validateSQL(ddl);

        System.out.println("double-quoted DDD result: " + result);
        assertEquals("NO_ERROR", result);
    }

    @Test
    void parseToCharSysdateDefaultWithSingleQuotedDdd() {
        String ddl =
                """
                CREATE TABLE oracle_date_format_example (
                    id INTEGER,
                    day_of_year VARCHAR(3) DEFAULT to_char(SYSDATE, 'DDD')
                );
                """;

        String result = parser.validateSQL(ddl);

        System.out.println("single-quoted DDD result: " + result);
        assertEquals("NO_ERROR", result);
    }
}
