/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.model.AnalyzerCostDetail;
import com.cubrid.sqlanalyzer.command.model.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailureStage;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;

class AnalyzerReportHtmlPreviewTest {
    private static final Path PREVIEW_HTML =
            Path.of("target", "report-preview", "plcsql-static-sql-failure.html");

    @Test
    void shouldWritePlcsqlAndStaticSqlFailureHtmlPreview() throws IOException {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);
        report.setDebugFullQuery(true);
        report.setAnalyzedStatementCount(3);
        report.setSucceededStatementCount(1);
        report.setFailedStatementCount(2);
        report.setOverview(
                new AnalyzerOverviewViewModel(
                        "preview",
                        new AnalyzerSourceOverviewViewModel(
                                AnalyzerSourceType.ORACLE,
                                "jdbc:oracle:thin:@//localhost:1521/XEPDB1",
                                "localhost",
                                1521,
                                "XEPDB1",
                                "CUBRID",
                                "preview",
                                null,
                                null,
                                0),
                        new AnalyzerTargetOverviewViewModel(
                                AnalyzerTargetType.PARSER,
                                null,
                                null,
                                0,
                                null,
                                null,
                                null,
                                "CUBRID parser"),
                        AnalyzerExecutionMode.DDL));

        String plcsqlOk =
                """
                CREATE OR REPLACE PROCEDURE CUBRID.PROC_PL_STATIC_FAIL
                AS
                    V_COUNT NUMBER;
                BEGIN
                    SELECT COUNT(*) INTO V_COUNT
                    FROM PLC_TEST_EMP
                    WHERE CONNECT_BY_ROOT EMP_ID = 1;
                END;
                """;
        String staticSqlFail =
                """
                SELECT COUNT(*)
                FROM PLC_TEST_EMP
                WHERE CONNECT_BY_ROOT EMP_ID = 1
                """;
        String plcsqlFail =
                """
                CREATE OR REPLACE PROCEDURE CUBRID.PROC_PL_FAIL
                AS
                BEGIN
                    NULL
                    COMMIT;
                END;
                """;

        report.addStatementResult(
                "DDL_PROC_BODY",
                "PROC_1",
                "CUBRID.PROC_PL_STATIC_FAIL",
                plcsqlOk,
                true,
                "parsed",
                null);
        report.addStatementResult(
                "SELECT",
                "PROC_1_STATIC_1_L5_C5",
                "CUBRID.PROC_PL_STATIC_FAIL / static SQL #1",
                staticSqlFail,
                false,
                "In line 3, column 7,\n\nERROR(-1): syntax error before 'CONNECT_BY_ROOT'",
                AnalyzerFailureStage.PARSER);
        report.addFailure(
                failure(
                        "SELECT",
                        "PROC_1_STATIC_1_L5_C5",
                        "CUBRID.PROC_PL_STATIC_FAIL / static SQL #1",
                        staticSqlFail,
                        "In line 3, column 7,\n\nERROR(-1): syntax error before 'CONNECT_BY_ROOT'",
                        0.5f,
                        "Base DML"));

        report.addStatementResult(
                "DDL_PROC_BODY",
                "PROC_2",
                "CUBRID.PROC_PL_FAIL",
                plcsqlFail,
                false,
                "In line 5, column 5,\n\nERROR(-1): mismatched input 'COMMIT'",
                AnalyzerFailureStage.PARSER);
        report.addFailure(
                failure(
                        "DDL_PROC_BODY",
                        "PROC_2",
                        "CUBRID.PROC_PL_FAIL",
                        plcsqlFail,
                        "In line 5, column 5,\n\nERROR(-1): mismatched input 'COMMIT'",
                        1.0f,
                        "Base procedure DDL"));

        Files.createDirectories(PREVIEW_HTML.getParent());
        String html = report.buildResultHtml();
        Files.writeString(PREVIEW_HTML, html, StandardCharsets.UTF_8);

        assertTrue(Files.isRegularFile(PREVIEW_HTML));
        assertTrue(html.contains("PROC_1_STATIC_1_L5_C5"));
        assertTrue(html.contains("PROC_2"));
        assertTrue(html.contains("CUBRID.PROC_PL_STATIC_FAIL"));
        assertTrue(html.contains("CUBRID.PROC_PL_FAIL"));
    }

    private AnalyzerFailure failure(
            String type,
            String id,
            String objectName,
            String sql,
            String reason,
            float cost,
            String costItem) {
        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType(type);
        failure.setStatementId(id);
        failure.setObjectName(objectName);
        failure.setSql(sql);
        failure.setReason(reason);
        failure.setFailureStage(AnalyzerFailureStage.PARSER);
        failure.setEstimatedCost(cost);
        failure.addCostDetail(new AnalyzerCostDetail(costItem, 1, cost, cost));
        return failure;
    }
}
