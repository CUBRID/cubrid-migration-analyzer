/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.cubrid.cubridmigration.core.connection.ConnParameters;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTableSizeViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzerOracleTableSizeFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerOracleTableSizeFetcher.class);

    static final String TABLE_SIZE_SQL =
            """
            WITH TARGET_TABLES AS (
                SELECT
                    T.TABLE_NAME,
                    T.NUM_ROWS,
                    T.IOT_TYPE
                FROM USER_TABLES T
                WHERE 
                    (T.IOT_TYPE IS NULL OR T.IOT_TYPE = 'IOT')
                    AND T.SECONDARY = 'N'
                    AND T.TEMPORARY = 'N'
                    AND (T.DROPPED IS NULL OR T.DROPPED = 'NO')
                    AND T.TABLE_NAME NOT LIKE 'BIN$%'
                    AND T.TABLE_NAME NOT LIKE 'MLOG$%'
                    AND T.TABLE_NAME NOT LIKE 'RUPD$%'
            ), SEGMENT_MAP AS (
                SELECT
                    T.TABLE_NAME,
                    T.NUM_ROWS,
                    T.TABLE_NAME AS SEGMENT_NAME,
                    'TABLE' AS SEGMENT_KIND
                FROM TARGET_TABLES T
                WHERE T.IOT_TYPE IS NULL

                UNION ALL

                SELECT 
                    T.TABLE_NAME,
                    T.NUM_ROWS,
                    I.INDEX_NAME AS SEGMENT_NAME,
                    'INDEX' AS SEGMENT_KIND
                FROM TARGET_TABLES T
                JOIN
                    USER_INDEXES I
                    ON I.TABLE_NAME = T.TABLE_NAME
                WHERE 
                    T.IOT_TYPE = 'IOT'
                    AND I.INDEX_TYPE = 'IOT - TOP'

                UNION ALL

                SELECT
                    T.TABLE_NAME,
                    T.NUM_ROWS,
                    O.TABLE_NAME AS SEGMENT_NAME,
                    'TABLE' AS SEGMENT_KIND
                FROM TARGET_TABLES T
                JOIN
                    USER_TABLES O
                    ON O.IOT_NAME = T.TABLE_NAME
                WHERE
                    T.IOT_TYPE = 'IOT'
                    AND O.IOT_TYPE IN ('IOT_OVERFLOW', 'IOT_MAPPING')
            )
            SELECT 
                M.TABLE_NAME AS TABLE_NAME,
                SUM(S.BYTES) AS BYTES,
                NVL(M.NUM_ROWS, 0) AS ESTIMATED_ROWS
            FROM SEGMENT_MAP M
            JOIN
                USER_SEGMENTS S
                ON S.SEGMENT_NAME = M.SEGMENT_NAME
                AND S.SEGMENT_TYPE LIKE M.SEGMENT_KIND || '%'
            GROUP BY M.TABLE_NAME, M.NUM_ROWS
            ORDER BY SUM(S.BYTES) DESC, M.TABLE_NAME ASC
            """;

    public List<AnalyzerTableSizeViewModel> fetch(ConnParameters sourceConParams) {
        if (sourceConParams == null) {
            return List.of();
        }

        List<AnalyzerTableSizeViewModel> tableSizes = new ArrayList<AnalyzerTableSizeViewModel>();
        try (Connection connection = sourceConParams.createConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(TABLE_SIZE_SQL)) {
            while (resultSet.next()) {
                tableSizes.add(
                        new AnalyzerTableSizeViewModel(
                                resultSet.getString("TABLE_NAME"),
                                Math.max(0L, resultSet.getLong("BYTES")),
                                Math.max(0L, resultSet.getLong("ESTIMATED_ROWS"))));
            }
        } catch (Exception ex) {
            LOG.warn("Failed to fetch Oracle table sizes.", ex);
            return List.of();
        }

        return tableSizes;
    }
}
