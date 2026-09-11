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

    private static final String TABLE_SIZE_SQL =
            "SELECT "
                    + "T.TABLE_NAME AS TABLE_NAME, "
                    + "SUM(S.BYTES) AS BYTES, "
                    + "NVL(T.NUM_ROWS, 0) AS ESTIMATED_ROWS "
                    + "FROM USER_SEGMENTS S "
                    + "JOIN USER_TABLES T ON T.TABLE_NAME = S.SEGMENT_NAME "
                    + "WHERE S.SEGMENT_TYPE LIKE 'TABLE%' "
                    + "AND T.SECONDARY = 'N' "
                    + "AND T.TEMPORARY = 'N' "
                    + "AND (T.DROPPED IS NULL OR T.DROPPED = 'NO') "
                    + "AND (T.IOT_TYPE IS NULL OR T.IOT_TYPE = 'IOT') "
                    + "GROUP BY T.TABLE_NAME, T.NUM_ROWS "
                    + "ORDER BY SUM(S.BYTES) DESC, T.TABLE_NAME ASC";

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
