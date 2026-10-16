/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnalyzerOracleTableSizeFetcherTest {
    @Test
    void shouldMapIotSegmentsToTheBaseTable() {
        String sql = AnalyzerOracleTableSizeFetcher.TABLE_SIZE_SQL;

        assertTrue(sql.contains("I.INDEX_TYPE = 'IOT - TOP'"));
        assertTrue(sql.contains("I.INDEX_NAME AS SEGMENT_NAME"));
        assertTrue(sql.contains("O.IOT_TYPE IN ('IOT_OVERFLOW', 'IOT_MAPPING')"));
        assertTrue(sql.contains("O.IOT_NAME = T.TABLE_NAME"));
    }

    @Test
    void shouldJoinUserSegmentsOnlyOnce() {
        String sql = AnalyzerOracleTableSizeFetcher.TABLE_SIZE_SQL;

        assertEquals(1, sql.split("USER_SEGMENTS", -1).length - 1);
    }

    @Test
    void shouldExcludeTheSameSystemTablePrefixesAsCmt() {
        String sql = AnalyzerOracleTableSizeFetcher.TABLE_SIZE_SQL;

        assertTrue(sql.contains("T.TABLE_NAME NOT LIKE 'BIN$%'"));
        assertTrue(sql.contains("T.TABLE_NAME NOT LIKE 'MLOG$%'"));
        assertTrue(sql.contains("T.TABLE_NAME NOT LIKE 'RUPD$%'"));
    }
}
