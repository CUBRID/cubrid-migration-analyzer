/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.report;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.model.AnalyzerCostDetail;
import com.cubrid.sqlanalyzer.command.model.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailureStage;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTableSizeViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;

class AnalyzerReportTest {
    @Test
    @DisplayName("result text includes summary and failure details")
    void shouldBuildResultTextWithStatementAndFailureSections() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.XML);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DML);
        report.setAnalyzedStatementCount(1);
        report.setSucceededStatementCount(0);
        report.setFailedStatementCount(1);
        report.addStatementResult(
                "SELECT",
                "Q0",
                "SELECT 1",
                true,
                "parsed",
                null);
        report.addStatementResult(
                "SELECT",
                "Q1",
                "SELECT * FROM emp",
                false,
                "syntax error",
                AnalyzerFailureStage.PARSER);

        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType("SELECT");
        failure.setStatementId("Q1");
        failure.setSql("SELECT * FROM emp");
        failure.setReason("syntax error");
        failure.setFailureStage(AnalyzerFailureStage.PARSER);
        failure.setEstimatedCost(0.7f);
        failure.addCostDetail(new AnalyzerCostDetail("Base DML", 1, 0.2f, 0.2f));
        failure.addCostDetail(new AnalyzerCostDetail("JOIN detected", 1, 0.5f, 0.5f));
        report.addFailure(failure);

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Overview"));
        assertTrue(resultText.contains("Source      : XML"));
        assertTrue(resultText.contains("Target      : PARSER"));
        assertTrue(resultText.contains("Mode        : DML"));
        assertTrue(resultText.contains("FAIL        : 1"));
        assertTrue(resultText.contains("Cost        : 0.7"));
        assertTrue(resultText.contains("Cost        : 0.7 (0.06 hr)"));
        assertTrue(resultText.contains("Analysis summary"));
        assertTrue(resultText.contains("Object counts"));
        assertTrue(resultText.contains("Execution results"));
        assertTrue(resultText.contains("Type"));
        assertTrue(resultText.contains("Total"));
        assertTrue(resultText.contains("OK"));
        assertTrue(resultText.contains("SELECT                         2       1       1"));
        assertTrue(resultText.contains("- SELECT Q1 [PARSER]"));
        assertTrue(resultText.contains("Reason: syntax error"));
        assertTrue(resultText.contains("Cost  : 0.7"));
        assertTrue(resultText.contains("Cost  : 0.7 (0.06 hr)"));
        assertTrue(resultText.contains("Cost details:"));
        assertTrue(resultText.contains("Base DML : count=1, unit=0.2 (0.02 hr), total=0.2 (0.02 hr)"));
        assertTrue(resultText.contains("JOIN detected : count=1, unit=0.5 (0.04 hr), total=0.5 (0.04 hr)"));
        assertTrue(resultText.contains("----------------------------------------"));
        assertFalse(resultText.contains("Result summary"));
        assertFalse(resultText.contains("Statement results"));
        assertFalse(resultText.contains("SELECT Q0"));
    }

    @Test
    @DisplayName("result text includes overview connection details when available")
    void shouldBuildResultTextWithOverviewConnectionDetails() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.XML);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DML);
        report.setOverview(
                new AnalyzerOverviewViewModel(
                        "0.0.1-SNAPSHOT",
                        new AnalyzerSourceOverviewViewModel(
                                AnalyzerSourceType.XML,
                                null,
                                null,
                                0,
                                null,
                                null,
                                null,
                                "/tmp/sqlmap",
                                "UTF-8",
                                3),
                        new AnalyzerTargetOverviewViewModel(
                                AnalyzerTargetType.PARSER,
                                null,
                                null,
                                0,
                                null,
                                null,
                                null,
                                "CUBRID parser"),
                        AnalyzerExecutionMode.DML));

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Overview"));
        assertTrue(resultText.contains("Program     : 0.0.1-SNAPSHOT"));
        assertTrue(resultText.contains("Source      : XML"));
        assertTrue(resultText.contains("XML dir     : /tmp/sqlmap"));
        assertTrue(resultText.contains("XML charset : UTF-8"));
        assertTrue(resultText.contains("XML files   : 3"));
        assertTrue(resultText.contains("Target      : PARSER"));
        assertTrue(resultText.contains("Parser      : CUBRID parser"));
        assertEquals(1, countOccurrences(resultText, "XML files   : 3"));
        assertTrue(resultText.contains("Mode        : DML"));
        assertTrue(resultText.contains("Total       : 0"));
        assertFalse(resultText.contains("Result summary"));
    }

    @Test
    @DisplayName("result text includes zero object counts for empty Oracle catalog")
    void shouldBuildResultTextWithZeroOracleObjectCounts() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);
        report.setAnalyzedStatementCount(0);
        report.setSucceededStatementCount(0);
        report.setFailedStatementCount(0);
        report.setObjectCountPreview(
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.ORACLE,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0));

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Analysis summary"));
        assertTrue(resultText.contains("Object counts"));
        assertTrue(resultText.contains("Catalog schemas : 0"));
        assertTrue(resultText.contains("Target tables   : 0"));
        assertTrue(resultText.contains("Target PKs      : 0"));
        assertTrue(resultText.contains("Target FKs      : 0"));
        assertTrue(resultText.contains("Target views    : 0"));
        assertTrue(resultText.contains("Target serials  : 0"));
        assertTrue(resultText.contains("Target synonyms : 0"));
        assertTrue(resultText.contains("Target grants   : 0"));
        assertTrue(resultText.contains("Target procs    : 0"));
        assertTrue(resultText.contains("Target funcs    : 0"));
        assertTrue(resultText.contains("Target triggers : 0"));
        assertTrue(resultText.contains("Execution results"));
        assertTrue(resultText.contains("(none)"));
    }

    @Test
    @DisplayName("result html includes zero object counts for empty Oracle objects")
    void shouldBuildResultHtmlWithZeroOracleObjectCounts() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);
        report.setObjectCountPreview(
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.ORACLE,
                        1,
                        2,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0));

        String resultHtml = report.buildResultHtml();

        assertTrue(resultHtml.contains("<td>SCHEMA</td><td class=\"number\">1</td>"));
        assertTrue(resultHtml.contains("<td>TABLE</td><td class=\"number\">2</td>"));
        assertTrue(resultHtml.contains("<td>PK</td><td class=\"number\">1</td>"));
        assertTrue(resultHtml.contains("<td>FK</td><td class=\"number\">0</td>"));
        assertTrue(resultHtml.contains("<td>VIEW</td><td class=\"number\">1</td>"));
        assertTrue(resultHtml.contains("<td>SERIAL</td><td class=\"number\">0</td>"));
        assertTrue(resultHtml.contains("<td>SYNONYM</td><td class=\"number\">0</td>"));
        assertTrue(resultHtml.contains("<td>GRANT</td><td class=\"number\">0</td>"));
        assertTrue(resultHtml.contains("<td>TRIGGER</td><td class=\"number\">0</td>"));
        assertTrue(resultHtml.contains("<td>PROCEDURE</td><td class=\"number\">0</td>"));
        assertTrue(resultHtml.contains("<td>FUNCTION</td><td class=\"number\">0</td>"));
    }

    @Test
    @DisplayName("result text includes Oracle table size summary")
    void shouldBuildResultTextWithOracleTableSizes() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);
        report.setObjectCountPreview(
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.ORACLE,
                        1,
                        2,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        3_145_728L,
                        List.of(
                                new AnalyzerTableSizeViewModel("EMP", 2_097_152L, 1_234L),
                                new AnalyzerTableSizeViewModel("DEPT", 1_048_576L, 56L))));

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Oracle table size total : 3.00 MB"));
        assertTrue(resultText.contains("Oracle table sizes"));
        assertTrue(resultText.contains("Est. rows"));
        assertTrue(resultText.contains("EMP"));
        assertTrue(resultText.contains("2.00 MB"));
        assertTrue(resultText.contains("1,234"));
        assertTrue(resultText.contains("DEPT"));
        assertTrue(resultText.contains("1.00 MB"));
        assertTrue(resultText.contains("56"));
    }

    @Test
    @DisplayName("result text highlights parser error line and column")
    void shouldBuildResultTextWithSqlErrorContext() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);

        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType("DDL_TABLE");
        failure.setStatementId("TABLE_1");
        failure.setSql("CREATE TABLE t(\ncol_a int,\ncol_b DEFAULT broken\n);");
        failure.setReason("In line 3, column 15, Syntax error");
        failure.setFailureStage(AnalyzerFailureStage.PARSER);
        report.addFailure(failure);

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Location: line 3, column 15"));
        assertTrue(resultText.contains("SQL:"));
        assertTrue(resultText.contains("1 | CREATE TABLE t("));
        assertTrue(resultText.contains("3 | col_b DEFAULT broken"));
        assertTrue(resultText.contains("4 | );"));
        assertTrue(resultText.contains("^"));
        assertFalse(resultText.contains("SQL context:"));
    }

    @Test
    @DisplayName("result text estimates SQL error context from parser token when parser line is external")
    void shouldBuildResultTextWithEstimatedSqlErrorContext() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);

        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType("DDL_TABLE");
        failure.setStatementId("TABLE_25");
        failure.setSql(
                "CREATE TABLE tools4644(\n"
                        + "col_raw bit varying(800) DEFAULT X'HEXTORAW('64656661756C745F726177')',\n"
                        + "col_nvarchar2 varchar(100) DEFAULT (u'default_nvarchar2')\n"
                        + ");");
        failure.setReason(
                "In line 150, column 27 before 'C745F726177')', "
                        + "Syntax error: unexpected '64656661756', expecting REFERENCES");
        failure.setFailureStage(AnalyzerFailureStage.PARSER);
        report.addFailure(failure);

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Location: line 2, column"));
        assertTrue(resultText.contains("(estimated)"));
        assertTrue(resultText.contains("1 | CREATE TABLE tools4644("));
        assertTrue(resultText.contains("col_raw bit varying"));
        assertTrue(resultText.contains("4 | );"));
        assertTrue(resultText.contains("^ estimated"));
        assertFalse(resultText.contains("SQL context:"));
    }

    @Test
    @DisplayName("result text includes zero query counts for empty XML source")
    void shouldBuildResultTextWithZeroXmlQueryCounts() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.XML);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DML);
        report.setObjectCountPreview(
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.XML,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0));

        String resultText = report.buildResultText();

        assertTrue(resultText.contains("Object counts"));
        assertTrue(resultText.contains("SELECT count    : 0"));
        assertTrue(resultText.contains("INSERT count    : 0"));
        assertTrue(resultText.contains("UPDATE count    : 0"));
        assertTrue(resultText.contains("DELETE count    : 0"));
    }

    @Test
    @DisplayName("result html includes connection summary, object summary, detail, and conclusion")
    void shouldBuildResultHtml() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);
        report.setAnalyzedStatementCount(2);
        report.setSucceededStatementCount(1);
        report.setFailedStatementCount(1);
        report.setOverview(
                new AnalyzerOverviewViewModel(
                        "0.0.1-SNAPSHOT",
                        new AnalyzerSourceOverviewViewModel(
                                AnalyzerSourceType.ORACLE,
                                "jdbc:oracle:thin:@localhost:1521/XE",
                                "localhost",
                                1521,
                                "XE",
                                "HR",
                                "Oracle 19c",
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
        report.setObjectCountPreview(
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.ORACLE,
                        1,
                        2,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        3_145_728L,
                        List.of(new AnalyzerTableSizeViewModel("EMP", 2_097_152L, 1_234L))));
        report.addSourceStatusMessage("Oracle source loaded.");
        report.addSourceStatusMessage("XML source skipped: No XML files found in directory: /tmp/sqlmap");
        report.addStatementResult(
                "DDL_TABLE",
                "TABLE_1",
                "CREATE TABLE t(id int)",
                false,
                "syntax error",
                AnalyzerFailureStage.PARSER);

        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType("DDL_TABLE");
        failure.setStatementId("TABLE_1");
        failure.setSql("CREATE TABLE t(\ncol DEFAULT broken\n);");
        failure.setReason("In line 2, column 13, Syntax error before 'broken'");
        failure.setFailureStage(AnalyzerFailureStage.PARSER);
        failure.setEstimatedCost(1.2f);
        failure.addCostDetail(new AnalyzerCostDetail("Parser failure", 1, 1.2f, 1.2f));
        report.addFailure(failure);

        String resultHtml = report.buildResultHtml();

        assertEquals(0, countOccurrences(resultHtml, "<section"));
        assertFalse(resultHtml.contains("class=\"report-section\""));
        assertTrue(resultHtml.contains("body>h2:first-of-type{margin-top:0;}"));
        assertFalse(resultHtml.contains(".compact-table{"));
        assertTrue(resultHtml.contains(
                "table{border-collapse:collapse;table-layout:fixed;width:100%;"));
        assertTrue(resultHtml.contains("vertical-align:top;word-break:break-word;"));
        assertTrue(resultHtml.contains(".info-table col{width:50%;}"));
        assertTrue(resultHtml.contains(".section-body{display:block;max-width:100%;"
                + "box-sizing:border-box;background:#fff;border:1px solid #d7dde4;"));
        assertTrue(resultHtml.contains(".section-body[hidden]{display:none;}"));
        assertTrue(resultHtml.contains("<h2>Connection Info</h2>"));
        assertTrue(resultHtml.contains("<div class=\"section-body\" "
                + "id=\"summary-report-section-connection-info-body\">"));
        assertTrue(resultHtml.contains("<table class=\"info-table\">\n"
                + "<colgroup><col><col></colgroup>\n"
                + "<tr><td class=\"metric\">Source Oracle SID</td><td>XE</td></tr>"));
        assertFalse(resultHtml.contains("<tr><td class=\"metric\">Connected User</td>"));
        assertTrue(resultHtml.contains("<tr><td class=\"metric\">Source Oracle Status</td><td>Executed</td></tr>"));
        assertTrue(resultHtml.contains("<tr><td class=\"metric\">XML Directory Status</td><td>"
                + "Not executed - No XML files found in directory: /tmp/sqlmap</td></tr>"));
        assertTrue(resultHtml.contains("<tr><td class=\"metric\">Source schema</td><td>HR</td></tr>"));
        assertTrue(resultHtml.contains("<tr><td class=\"metric\">Target type</td><td>PARSER</td></tr>"));
        assertTrue(resultHtml.contains("<tr><td class=\"metric\">Source table size</td><td>3.00 MB</td></tr>"));
        assertFalse(resultHtml.contains("jdbc:oracle:thin:@localhost:1521/XE"));
        assertFalse(resultHtml.contains("<td class=\"metric\">Parser</td><td>Yes</td>"));
        assertFalse(resultHtml.contains("<td class=\"metric\">Schema name</td><td>HR</td>"));
        assertTrue(resultHtml.indexOf("<h2>Connection Info</h2>")
                < resultHtml.indexOf("<h2>Conclusion</h2>"));
        assertTrue(resultHtml.indexOf("<h2>Conclusion</h2>")
                < resultHtml.indexOf("<h2>Summary</h2>"));
        assertTrue(resultHtml.contains("<h2>Summary</h2>"));
        assertTrue(resultHtml.contains("<div class=\"section-body\" "
                + "id=\"summary-report-section-summary-body\">"));
        assertFalse(resultHtml.contains("section-collapsed-summary"));
        assertTrue(resultHtml.contains("<span class=\"metric\">Compatibility:</span> 80.00%"));
        assertTrue(resultHtml.contains("<h3>Object Summary</h3>"));
        assertTrue(resultHtml.contains("<span class=\"metric\">DB Objects (DDL):</span> "
                + "80.00% (total 5, 1 errors)"));
        assertTrue(resultHtml.contains("<span class=\"metric\">XML Queries (DML):</span> "
                + "0.00% (total 0, 0 errors)"));
        assertTrue(resultHtml.contains("<span class=\"metric\">PL/CSQL:</span> 0.00% "
                + "(total 0, 0 errors; triggers and procedures cannot be converted)"));
        assertTrue(resultHtml.contains("<h3>Estimated Work Time</h3>"));
        assertTrue(resultHtml.contains("<span class=\"metric\">Total estimated time:</span> 0.10 hr"));
        assertTrue(resultHtml.contains("<span class=\"metric\">DBA estimated work:</span> 0.10 hr"));
        assertTrue(resultHtml.contains("<span class=\"metric\">Developer estimated work:</span> 0.00 hr"));
        assertTrue(resultHtml.contains("<h2>Detail Summary</h2>"));
        assertTrue(resultHtml.contains("<div class=\"section-body\" "
                + "id=\"summary-report-section-detail-summary-body\">"));
        assertTrue(resultHtml.contains(">&#9656;</button>TABLE</td>"));
        assertTrue(resultHtml.contains("data-summary-parent=\"summary-table\" hidden>"
                + "<td colspan=\"4\" class=\"nested-summary-cell\">"));
        assertTrue(resultHtml.contains("<table class=\"nested-summary-table\">"));
        assertTrue(resultHtml.contains("<tr><th>Table</th><th>Size</th><th>Est. rows</th></tr>"));
        assertTrue(resultHtml.contains("<tr><td>EMP</td><td class=\"number\">2.00 MB</td>"
                + "<td class=\"number\">1,234</td></tr>"));
        assertTrue(resultHtml.contains("1.2 (0.10 hr)"));
        assertTrue(resultHtml.contains("<h2>Fail Summary</h2>"));
        assertTrue(resultHtml.contains("<div class=\"section-body\" "
                + "id=\"summary-report-section-fail-summary-body\">"));
        assertTrue(resultHtml.contains("<h2>Fail Detail</h2>"));
        assertTrue(resultHtml.contains("aria-expanded=\"false\" onclick=\"toggleReportSection(this,'summary-report-section-fail-detail')"));
        assertTrue(resultHtml.contains("<div class=\"section-body\" "
                + "id=\"summary-report-section-fail-detail-body\" hidden>"));
        assertTrue(resultHtml.contains("<tr><th>Object Type</th><th>Count</th></tr>"));
        assertTrue(resultHtml.contains("<tr><td>TABLE</td><td class=\"number status-fail\">1</td></tr>"));
        assertTrue(resultHtml.contains("<h2>Conclusion</h2>"));
        assertTrue(resultHtml.contains("<div class=\"section-body\" "
                + "id=\"summary-report-section-conclusion-body\">"));
        assertTrue(resultHtml.contains("<table>\n"
                + "<tr><th>Category</th><th>Analyzed</th><th>Failed</th>"
                + "<th>Total Cost</th><th>Estimated Time</th></tr>"));
        assertTrue(resultHtml.contains("<details class=\"detail-item\">"));
        assertFalse(resultHtml.contains("<details class=\"detail-item\" open>"));
        assertTrue(resultHtml.contains("line 2, column 13"));
        assertTrue(resultHtml.contains("1 | CREATE TABLE t("));
        assertTrue(resultHtml.contains("2 | col DEFAULT broken"));
        assertTrue(resultHtml.contains("  |             ^"));
        assertTrue(resultHtml.contains("3 | );"));
        assertTrue(resultHtml.contains("CREATE TABLE t("));
        assertTrue(resultHtml.contains("Syntax error before &#39;broken&#39;"));
        assertTrue(resultHtml.contains("<h2>Conclusion</h2>"));
        assertTrue(resultHtml.contains("<tr><th>Category</th><th>Analyzed</th><th>Failed</th>"
                + "<th>Total Cost</th><th>Estimated Time</th></tr>"));
        assertTrue(resultHtml.contains("<tr><td class=\"metric\">DDL + PL/CSQL</td>"
                + "<td class=\"number\">1</td><td class=\"number status-fail\">1</td>"
                + "<td class=\"number\">1.2</td><td class=\"number\">0.10 hr</td></tr>"));
        assertTrue(resultHtml.contains("<tr><td class=\"metric\">DML</td>"
                + "<td class=\"number\">0</td><td class=\"number status-ok\">0</td>"
                + "<td class=\"number\">0.0</td><td class=\"number\">0.00 hr</td></tr>"));
        assertTrue(resultHtml.contains("Estimated Time"));
        assertTrue(resultHtml.contains("0.10 hr"));
    }

    @Test
    @DisplayName("result html groups view create and alter under expandable view summary")
    void shouldGroupViewExecutionRowsUnderViewSummaryInResultHtml() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ALL);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.ALL);
        report.setObjectCountPreview(
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.ALL,
                        1,
                        0,
                        0,
                        0,
                        2,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        1,
                        0,
                        0,
                        0));
        report.addStatementResult(
                "DDL_VIEW_CREATE",
                "VIEW_1",
                "CREATE VIEW v1;",
                true,
                "parsed",
                null);
        report.addStatementResult(
                "DDL_VIEW_CREATE",
                "VIEW_2",
                "CREATE VIEW v2;",
                true,
                "parsed",
                null);
        report.addStatementResult(
                "DDL_VIEW_ALTER",
                "VIEW_ALTER_1",
                "ALTER VIEW v1 ADD QUERY SELECT 1;",
                false,
                "syntax error",
                AnalyzerFailureStage.PARSER);
        report.addStatementResult(
                "SELECT",
                "Q1",
                "SELECT 1",
                true,
                "parsed",
                null);

        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType("DDL_VIEW_ALTER");
        failure.setStatementId("VIEW_ALTER_1");
        failure.setSql("ALTER VIEW v1 ADD QUERY SELECT broken");
        failure.setReason("syntax error");
        failure.setFailureStage(AnalyzerFailureStage.PARSER);
        failure.setEstimatedCost(0.8f);
        report.addFailure(failure);

        String resultHtml = report.buildResultHtml();

        assertFalse(resultHtml.contains("<details class=\"summary-part\">"));
        assertTrue(resultHtml.contains("<h3>1. Database Objects (DDL Migration)</h3>"));
        assertTrue(resultHtml.contains("<h3>2. Application Queries (DML/SQL Mapping Migration)</h3>"));
        assertTrue(resultHtml.contains("<h3>PL/CSQL</h3>"));
        assertTrue(resultHtml.contains("toggleSummaryRows(this,'summary-view')"));
        assertTrue(resultHtml.contains(">&#9656;</button>VIEW</td>"));
        assertTrue(resultHtml.contains("<td class=\"number\">2</td>"));
        assertTrue(resultHtml.contains("<td class=\"number status-fail\">1</td>"));
        assertTrue(resultHtml.contains("<td class=\"number\">0.8 (0.07 hr)</td>"));
        assertTrue(resultHtml.contains("<tr class=\"summary-total-row\"><td>Total</td><td class=\"number\">3</td>"
                + "<td class=\"number status-fail\">1</td><td class=\"number\">0.8 (0.07 hr)</td></tr>"));
        assertTrue(resultHtml.contains("<tr class=\"summary-total-row\"><td>Total</td><td class=\"number\">1</td>"
                + "<td class=\"number status-ok\">0</td><td class=\"number\">0.0 (0.00 hr)</td></tr>"));
        assertTrue(resultHtml.contains("<tr class=\"summary-total-row\"><td>Total</td><td class=\"number\">0</td>"
                + "<td class=\"number status-ok\">0</td><td class=\"number\">0.0 (0.00 hr)</td></tr>"));
        assertTrue(resultHtml.contains("data-summary-parent=\"summary-view\" hidden><td>"
                + "<span class=\"summary-child-object\">VIEW_CREATE</span>"));
        assertTrue(resultHtml.contains("data-summary-parent=\"summary-view\" hidden><td>"
                + "<span class=\"summary-child-object\">VIEW_ALTER</span>"));
        assertTrue(resultHtml.indexOf(">&#9656;</button>VIEW</td>")
                < resultHtml.indexOf("<span class=\"summary-child-object\">VIEW_CREATE</span>"));
        assertTrue(resultHtml.indexOf(">&#9656;</button>VIEW</td>")
                < resultHtml.indexOf("<h3>2. Application Queries (DML/SQL Mapping Migration)</h3>"));
        assertTrue(resultHtml.indexOf("<h3>2. Application Queries (DML/SQL Mapping Migration)</h3>")
                < resultHtml.indexOf("<td>SELECT</td>"));
        assertTrue(resultHtml.indexOf("<td>SELECT</td>")
                < resultHtml.indexOf("<h3>PL/CSQL</h3>"));
    }

    @Test
    @DisplayName("result html conclusion separates DDL and PLCSQL from DML")
    void shouldSplitConclusionByDdlPlcsqlAndDml() {
        AnalyzerReport report = new AnalyzerReport();
        report.addStatementResult(
                "DDL_TABLE",
                "TABLE_1",
                "CREATE TABLE t(id int)",
                true,
                "parsed",
                null);
        report.addStatementResult(
                "DDL_PROC_BODY",
                "PROC_1",
                "CREATE PROCEDURE p AS BEGIN NULL; END;",
                true,
                "parsed",
                null);
        report.addStatementResult(
                "SELECT",
                "PROC_1_STATIC_1_L1_C1",
                "SELECT broken",
                false,
                "syntax error",
                AnalyzerFailureStage.PARSER);
        report.addStatementResult(
                "SELECT",
                "Q1",
                "SELECT broken",
                false,
                "syntax error",
                AnalyzerFailureStage.PARSER);

        AnalyzerFailure staticSqlFailure = new AnalyzerFailure();
        staticSqlFailure.setStatementType("SELECT");
        staticSqlFailure.setStatementId("PROC_1_STATIC_1_L1_C1");
        staticSqlFailure.setEstimatedCost(0.5f);
        report.addFailure(staticSqlFailure);

        AnalyzerFailure dmlFailure = new AnalyzerFailure();
        dmlFailure.setStatementType("SELECT");
        dmlFailure.setStatementId("Q1");
        dmlFailure.setEstimatedCost(0.7f);
        report.addFailure(dmlFailure);

        String resultHtml = report.buildResultHtml();

        assertTrue(resultHtml.contains("<tr><td class=\"metric\">DDL + PL/CSQL</td>"
                + "<td class=\"number\">3</td><td class=\"number status-fail\">1</td>"
                + "<td class=\"number\">0.5</td><td class=\"number\">0.04 hr</td></tr>"));
        assertTrue(resultHtml.contains("<tr><td class=\"metric\">DML</td>"
                + "<td class=\"number\">1</td><td class=\"number status-fail\">1</td>"
                + "<td class=\"number\">0.7</td><td class=\"number\">0.06 hr</td></tr>"));
    }

    @Test
    @DisplayName("sequence execution rows are reported as serial")
    void shouldReportSequenceExecutionRowsAsSerial() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);
        report.setObjectCountPreview(
                new AnalyzerObjectCountPreviewViewModel(
                        AnalyzerSourceType.ORACLE,
                        1,
                        0,
                        0,
                        0,
                        0,
                        2,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0));
        report.addStatementResult(
                "DDL_SEQUENCE",
                "SEQ_1",
                "CREATE SERIAL s1",
                true,
                "parsed",
                null);
        report.addStatementResult(
                "DDL_SEQUENCE",
                "SEQ_2",
                "CREATE SERIAL s2",
                true,
                "parsed",
                null);

        String resultText = report.buildResultText();
        String resultHtml = report.buildResultHtml();

        assertTrue(resultText.contains("Target serials  : 2"));
        assertTrue(resultText.contains("SERIAL                         2       2       0"));
        assertFalse(resultText.contains("DDL_SEQUENCE"));
        assertEquals(1, countOccurrences(resultHtml, "<td>SERIAL</td>"));
        assertTrue(resultHtml.contains("<td>SERIAL</td><td class=\"number\">2</td>"));
        assertFalse(resultHtml.contains("<td>SEQUENCE</td>"));
    }

    @Test
    @DisplayName("result html shows estimated SQL location with full failed SQL")
    void shouldBuildResultHtmlWithEstimatedSqlLocation() {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.ORACLE);
        report.setTargetType(AnalyzerTargetType.PARSER);
        report.setExecutionMode(AnalyzerExecutionMode.DDL);

        AnalyzerFailure failure = new AnalyzerFailure();
        failure.setStatementType("DDL_TABLE");
        failure.setStatementId("TABLE_25");
        failure.setSql(
                "CREATE TABLE tools4644(\n"
                        + "col_raw bit varying(800) DEFAULT X'HEXTORAW('64656661756C745F726177')',\n"
                        + "col_nvarchar2 varchar(100) DEFAULT (u'default_nvarchar2')\n"
                        + ");");
        failure.setReason(
                "In line 150, column 27 before 'C745F726177')', "
                        + "Syntax error: unexpected '64656661756', expecting REFERENCES");
        failure.setFailureStage(AnalyzerFailureStage.PARSER);
        report.addFailure(failure);

        String resultHtml = report.buildResultHtml();

        assertTrue(resultHtml.contains("line 2, column"));
        assertTrue(resultHtml.contains("(estimated)"));
        assertTrue(resultHtml.contains("1 | CREATE TABLE tools4644("));
        assertTrue(resultHtml.contains("2 | col_raw bit varying"));
        assertTrue(resultHtml.contains("^ estimated"));
        assertTrue(resultHtml.contains("4 | );"));
    }

    @Test
    @DisplayName("debug full query option includes successful statements in reports")
    void shouldIncludeExecutedFullQueriesWhenDebugFullQueryIsEnabled() {
        AnalyzerReport report = new AnalyzerReport();
        report.addStatementResult(
                "SELECT",
                "Q_OK",
                "EMP_QUERY",
                "SELECT *\nFROM emp",
                true,
                "parsed",
                null);

        String normalHtml = report.buildResultHtml();
        String normalText = report.buildResultText();

        assertFalse(normalHtml.contains("Executed Full Queries"));
        assertFalse(normalText.contains("Executed full queries"));

        report.setDebugFullQuery(true);
        String debugHtml = report.buildResultHtml();
        String debugText = report.buildResultText();

        assertTrue(debugHtml.contains("Executed Full Queries"));
        assertTrue(debugHtml.contains("Q_OK"));
        assertTrue(debugHtml.contains("EMP_QUERY"));
        assertTrue(debugHtml.contains("1 | SELECT *"));
        assertTrue(debugHtml.contains("2 | FROM emp"));
        assertTrue(debugText.contains("Executed full queries"));
        assertTrue(debugText.contains("Q_OK"));
        assertTrue(debugText.contains("EMP_QUERY"));
        assertTrue(debugText.contains("1 | SELECT *"));
        assertTrue(debugText.contains("2 | FROM emp"));
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = text.indexOf(pattern);
        while (index >= 0) {
            count++;
            index = text.indexOf(pattern, index + pattern.length());
        }
        return count;
    }
}
