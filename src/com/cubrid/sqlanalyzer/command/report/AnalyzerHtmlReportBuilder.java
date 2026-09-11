/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.cubrid.sqlanalyzer.command.model.AnalyzerCostDetail;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTableSizeViewModel;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;

class AnalyzerHtmlReportBuilder {

    private enum HtmlSummaryPart { DDL, DML, PLCSQL }

    private static class HtmlSummaryRow {
        final String objectType;
        final List<HtmlSummaryRow> childRows = new ArrayList<>();
        final List<AnalyzerTableSizeViewModel> tableSizeRows = new ArrayList<>();
        long totalCount;
        int errorCount;
        float cost;

        HtmlSummaryRow(String objectType, long totalCount) {
            this.objectType = objectType;
            this.totalCount = totalCount;
        }
    }

    private static class HtmlFailureGroup {
        StatementResult parentStatement;
        AnalyzerFailure parentFailure;
        final List<AnalyzerFailure> staticSqlFailures = new ArrayList<>();
    }

    private static class HtmlConclusionSummary {
        int analyzedCount;
        int failedCount;
        float cost;
    }

    private static class HtmlSummaryTotals {
        long totalCount;
        int errorCount;
        float cost;
    }

    private final AnalyzerReport report;
    private final long generatedAt;

    AnalyzerHtmlReportBuilder(AnalyzerReport report, long generatedAt) {
        this.report = report;
        this.generatedAt = generatedAt;
    }

    String build() {
        float totalCost = report.getTotalEstimatedFailureCost();
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html>\n");
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<title>SQL Analyzer Report</title>\n");
        sb.append("<style>\n");
        appendHtmlStyle(sb);
        sb.append("</style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("<header>\n");
        sb.append("<h1>SQL Analyzer Report</h1>\n");
        sb.append("<p>Generated at ")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerReportFormatter.formatGeneratedAt(generatedAt)))
                .append("</p>\n");
        sb.append("</header>\n");

        appendHtmlConnectionInfo(sb);
        appendHtmlConclusion(sb, totalCost);
        appendHtmlTableSummary(sb);
        appendHtmlFailureDetails(sb);
        appendHtmlExecutedFullQueries(sb);

        appendHtmlScript(sb);
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }

    private void appendHtmlStyle(StringBuilder sb) {
        sb.append("body{margin:0;padding:32px;background:#f5f7f9;color:#1f2933;font-family:Arial,sans-serif;font-size:13px;}\n");
        sb.append("header{border-bottom:1px solid #d7dde4;margin-bottom:24px;}\n");
        sb.append("h1{margin:0 0 8px;color:#006f9f;font-size:26px;}\n");
        sb.append("h2{margin:28px 0 12px;color:#006f9f;font-size:18px;}\n");
        sb.append("h3{margin:18px 0 8px;color:#1f2933;font-size:15px;}\n");
        sb.append("p{margin:0 0 16px;}\n");
        sb.append("table{border-collapse:collapse;table-layout:fixed;width:100%;background:#fff;margin-bottom:16px;}\n");
        sb.append(".info-table col{width:50%;}\n");
        sb.append("th,td{border:1px solid #d7dde4;padding:8px 10px;text-align:left;vertical-align:top;word-break:break-word;}\n");
        sb.append("th{background:#eef3f7;color:#c14900;font-weight:bold;}\n");
        sb.append(".metric{font-weight:bold;color:#006f9f;}\n");
        sb.append(".number{text-align:right;white-space:nowrap;}\n");
        sb.append(".muted{color:#667085;}\n");
        sb.append(".status-ok{color:#147a3b;font-weight:bold;}\n");
        sb.append(".status-fail{color:#b42318;font-weight:bold;}\n");
        sb.append(".row-toggle{border:0;background:transparent;color:#006f9f;cursor:pointer;font-weight:bold;margin:0 6px 0 0;padding:0;width:16px;}\n");
        sb.append(".summary-child-object{display:inline-block;padding-left:22px;}\n");
        sb.append(".summary-total-row td{background:#f8fafc;font-weight:bold;}\n");
        sb.append(".nested-summary-cell{background:#f8fafc;padding:10px 10px 10px 32px;}\n");
        sb.append(".nested-summary-table{margin:0;background:#fff;}\n");
        sb.append("details{background:#fff;border:1px solid #d7dde4;margin:0 0 12px;padding:10px;}\n");
        sb.append("summary{cursor:pointer;color:#006f9f;font-weight:bold;}\n");
        sb.append("details.detail-item[open]>summary{padding-bottom:10px;}\n");
        sb.append("body>h2:first-of-type{margin-top:0;}\n");
        sb.append(".section-heading{display:flex;align-items:center;gap:8px;margin:0 0 12px;padding-bottom:10px;border-bottom:1px solid #e5e9ef;}\n");
        sb.append(".section-heading.collapsible{cursor:pointer;}\n");
        sb.append(".section-heading h2{margin:0;}\n");
        sb.append(".section-toggle{border:1px solid #d7dde4;background:#f8fafc;color:#006f9f;cursor:pointer;font-weight:bold;width:24px;height:24px;line-height:20px;padding:0;}\n");
        sb.append(".section-body{display:block;max-width:100%;box-sizing:border-box;background:#fff;border:1px solid #d7dde4;padding:12px;margin:0 0 16px;}\n");
        sb.append(".section-body[hidden]{display:none;}\n");
        sb.append(".section-body>table:last-child,.section-body>details:last-child{margin-bottom:0;}\n");
        sb.append("pre{white-space:pre-wrap;word-break:break-word;background:#f8fafc;border:1px solid #e5e9ef;padding:10px;margin:8px 0 0;}\n");
        sb.append(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:16px;}\n");
        sb.append(".box{background:#fff;border:1px solid #d7dde4;padding:12px;}\n");
    }

    private void appendHtmlScript(StringBuilder sb) {
        sb.append("<script>\n");
        sb.append("function toggleSummaryRows(button, groupId){\n");
        sb.append("var rows=document.querySelectorAll('[data-summary-parent=\"'+groupId+'\"]');\n");
        sb.append("var expanded=button.getAttribute('aria-expanded')==='true';\n");
        sb.append("for(var i=0;i<rows.length;i++){rows[i].hidden=expanded;}\n");
        sb.append("button.setAttribute('aria-expanded',String(!expanded));\n");
        sb.append("button.innerHTML=expanded?'&#9656;':'&#9662;';\n");
        sb.append("}\n");
        sb.append("function toggleReportSection(button, sectionId){\n");
        sb.append("if(!button){return;}\n");
        sb.append("var body=document.getElementById(sectionId+'-body');\n");
        sb.append("var summary=document.getElementById(sectionId+'-summary');\n");
        sb.append("var expanded=button.getAttribute('aria-expanded')==='true';\n");
        sb.append("if(body){body.hidden=expanded;}\n");
        sb.append("if(summary){summary.hidden=!expanded;}\n");
        sb.append("button.setAttribute('aria-expanded',String(!expanded));\n");
        sb.append("button.innerHTML=expanded?'&#9656;':'&#9662;';\n");
        sb.append("}\n");
        sb.append("function toggleReportSectionFromHeading(heading, sectionId){\n");
        sb.append("var button=heading?heading.querySelector('.section-toggle'):null;\n");
        sb.append("toggleReportSection(button,sectionId);\n");
        sb.append("}\n");
        sb.append("</script>\n");
    }

    private void appendHtmlReportSectionStart(StringBuilder sb, String title) {
        String sectionId = htmlSummaryGroupId("report-section-" + title);
        sb.append("<div class=\"section-heading\"><h2>")
                .append(AnalyzerReportFormatter.escapeHtml(title))
                .append("</h2></div>\n");
        sb.append("<div class=\"section-body\" id=\"")
                .append(sectionId)
                .append("-body\">\n");
    }

    private void appendHtmlCollapsibleReportSectionStart(StringBuilder sb, String title) {
        String sectionId = htmlSummaryGroupId("report-section-" + title);
        sb.append("<div class=\"section-heading collapsible\" onclick=\"toggleReportSectionFromHeading(this,'")
                .append(sectionId)
                .append("')\"><button type=\"button\" class=\"section-toggle\"")
                .append(" aria-expanded=\"false\" onclick=\"toggleReportSection(this,'")
                .append(sectionId)
                .append("');event.stopPropagation();\">")
                .append("&#9656;")
                .append("</button><h2>")
                .append(AnalyzerReportFormatter.escapeHtml(title))
                .append("</h2></div>\n");
        sb.append("<div class=\"section-body\" id=\"")
                .append(sectionId)
                .append("-body\" hidden>\n");
    }

    private void appendHtmlReportSectionEnd(StringBuilder sb) {
        sb.append("</div>\n");
    }

    private void appendHtmlConnectionInfo(StringBuilder sb) {
        appendHtmlReportSectionStart(sb, "Connection Info");
        sb.append("<table class=\"info-table\">\n");
        sb.append("<colgroup><col><col></colgroup>\n");
        appendHtmlInfoRow(sb, "Source Oracle SID", resolveSourceOracleSid());
        appendHtmlInfoRow(sb, "Source Oracle Status", resolveSourceStatus(AnalyzerSourceType.ORACLE));
        appendHtmlInfoRow(sb, "XML Directory Status", resolveSourceStatus(AnalyzerSourceType.XML));
        appendHtmlInfoRow(sb, "Source schema", resolveSchemaName());
        appendHtmlInfoRow(sb, "Target type", resolveTargetTypeName());
        appendHtmlInfoRow(sb, "Source table size", resolveSourceTableSize());
        sb.append("</table>\n");
        appendHtmlReportSectionEnd(sb);
    }

    private void appendHtmlInfoRow(StringBuilder sb, String label, String value) {
        sb.append("<tr><td class=\"metric\">")
                .append(AnalyzerReportFormatter.escapeHtml(label))
                .append("</td><td>")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerReportFormatter.nullToEmpty(value)))
                .append("</td></tr>\n");
    }

    private String formatHtmlSummaryObjectName(String objectName) {
        String safeObjectName = AnalyzerReportFormatter.nullToEmpty(objectName);
        if (safeObjectName.isEmpty()) {
            return "";
        }
        return " - " + AnalyzerReportFormatter.escapeHtml(safeObjectName);
    }

    private void appendHtmlTableSummary(StringBuilder sb) {
        List<HtmlSummaryRow> rows = buildHtmlSummaryRows();
        appendHtmlReportSectionStart(sb, "Summary");
        sb.append(buildHtmlCollapsedSummary(rows));
        appendHtmlReportSectionEnd(sb);
        appendHtmlReportSectionStart(sb, "Detail Summary");
        if (rows.isEmpty()) {
            sb.append("<table>\n");
            sb.append("<tr><th>Object</th><th>Total</th><th>Error</th><th>Cost</th></tr>\n");
            sb.append("<tr><td colspan=\"4\" class=\"muted\">(none)</td></tr>\n");
            sb.append("</table>\n");
        } else {
            appendHtmlSummaryPart(sb, "1. Database Objects (DDL Migration)",
                    filterHtmlSummaryRows(rows, HtmlSummaryPart.DDL));
            appendHtmlSummaryPart(sb, "2. Application Queries (DML/SQL Mapping Migration)",
                    filterHtmlSummaryRows(rows, HtmlSummaryPart.DML));
            appendHtmlSummaryPart(sb, "PL/CSQL",
                    filterHtmlSummaryRows(rows, HtmlSummaryPart.PLCSQL));
        }
        appendHtmlReportSectionEnd(sb);
    }

    private String buildHtmlCollapsedSummary(List<HtmlSummaryRow> rows) {
        HtmlSummaryTotals ddl = summarizeHtmlRows(filterHtmlSummaryRows(rows, HtmlSummaryPart.DDL));
        HtmlSummaryTotals dml = summarizeHtmlRows(filterHtmlSummaryRows(rows, HtmlSummaryPart.DML));
        HtmlSummaryTotals plcsql =
                summarizeHtmlRows(filterHtmlSummaryRows(rows, HtmlSummaryPart.PLCSQL));
        HtmlSummaryTotals total = new HtmlSummaryTotals();
        total.totalCount = ddl.totalCount + dml.totalCount + plcsql.totalCount;
        total.errorCount = ddl.errorCount + dml.errorCount + plcsql.errorCount;
        total.cost = ddl.cost + dml.cost + plcsql.cost;

        StringBuilder sb = new StringBuilder();
        sb.append("<p><span class=\"metric\">Compatibility:</span> ")
                .append(AnalyzerReportFormatter.escapeHtml(
                        formatHtmlPercentage(total.errorCount, total.totalCount)))
                .append("</p>\n");
        sb.append("<h3>Object Summary</h3>\n");
        sb.append("<ul>\n");
        appendHtmlCollapsedObjectSummaryItem(sb, "DB Objects (DDL)", ddl, "");
        appendHtmlCollapsedObjectSummaryItem(sb, "XML Queries (DML)", dml, "");
        appendHtmlCollapsedObjectSummaryItem(
                sb, "PL/CSQL", plcsql, "; triggers and procedures cannot be converted");
        sb.append("</ul>\n");
        sb.append("<h3>Estimated Work Time</h3>\n");
        sb.append("<p><span class=\"metric\">Total estimated time:</span> ")
                .append(AnalyzerReportFormatter.escapeHtml(formatHtmlWorkTime(total.cost)))
                .append("</p>\n");
        sb.append("<ul>\n");
        sb.append("<li><span class=\"metric\">DBA estimated work:</span> ")
                .append(AnalyzerReportFormatter.escapeHtml(formatHtmlWorkTime(ddl.cost + plcsql.cost)))
                .append("</li>\n");
        sb.append("<li><span class=\"metric\">Developer estimated work:</span> ")
                .append(AnalyzerReportFormatter.escapeHtml(formatHtmlWorkTime(dml.cost)))
                .append("</li>\n");
        sb.append("</ul>\n");
        return sb.toString();
    }

    private void appendHtmlCollapsedObjectSummaryItem(
            StringBuilder sb, String label, HtmlSummaryTotals totals, String suffix) {
        sb.append("<li><span class=\"metric\">")
                .append(AnalyzerReportFormatter.escapeHtml(label))
                .append(":</span> ")
                .append(AnalyzerReportFormatter.escapeHtml(
                        formatHtmlPercentage(totals.errorCount, totals.totalCount)))
                .append(" (")
                .append(AnalyzerReportFormatter.escapeHtml(
                        formatHtmlErrorSummary(totals.errorCount, totals.totalCount, suffix)))
                .append(")</li>\n");
    }

    private HtmlSummaryTotals summarizeHtmlRows(List<HtmlSummaryRow> rows) {
        HtmlSummaryTotals totals = new HtmlSummaryTotals();
        for (HtmlSummaryRow row : rows) {
            totals.totalCount += row.totalCount;
            totals.errorCount += row.errorCount;
            totals.cost += row.cost;
        }
        return totals;
    }

    private String formatHtmlPercentage(int errorCount, long totalCount) {
        double percentage = totalCount <= 0 ? 0.0
                : ((totalCount - errorCount) * 100.0) / totalCount;
        return String.format(Locale.US, "%.2f%%", percentage);
    }

    private String formatHtmlErrorSummary(int errorCount, long totalCount, String suffix) {
        return "total "
                + AnalyzerReportFormatter.formatNumber(totalCount)
                + ", "
                + AnalyzerReportFormatter.formatNumber(errorCount)
                + " errors"
                + AnalyzerReportFormatter.nullToEmpty(suffix);
    }

    private String formatHtmlWorkTime(float cost) {
        float hours = AnalyzerCostFormatter.toHours(cost);
        if (hours >= 24.0f) {
            float days = hours / 24.0f;
            return String.format(Locale.US, "%.2f %s", days, days == 1.0f ? "day" : "days");
        }
        return String.format(Locale.US, "%.2f hr", hours);
    }

    private void appendHtmlSummaryPart(
            StringBuilder sb, String title, List<HtmlSummaryRow> rows) {
        sb.append("<h3>")
                .append(AnalyzerReportFormatter.escapeHtml(title))
                .append("</h3>\n");
        sb.append("<table>\n");
        sb.append("<tr><th>Object</th><th>Total</th><th>Error</th><th>Cost</th></tr>\n");
        if (rows.isEmpty()) {
            sb.append("<tr><td colspan=\"4\" class=\"muted\">(none)</td></tr>\n");
        } else {
            for (HtmlSummaryRow row : rows) {
                appendHtmlSummaryRow(sb, row);
            }
        }
        appendHtmlSummaryTotalRow(sb, rows);
        sb.append("</table>\n");
    }

    private void appendHtmlSummaryTotalRow(StringBuilder sb, List<HtmlSummaryRow> rows) {
        long totalCount = 0;
        int errorCount = 0;
        float cost = 0;
        for (HtmlSummaryRow row : rows) {
            totalCount += row.totalCount;
            errorCount += row.errorCount;
            cost += row.cost;
        }
        sb.append("<tr class=\"summary-total-row\"><td>Total</td><td class=\"number\">")
                .append(AnalyzerReportFormatter.formatNumber(totalCount))
                .append("</td><td class=\"number ")
                .append(errorCount > 0 ? "status-fail" : "status-ok")
                .append("\">")
                .append(AnalyzerReportFormatter.formatNumber(errorCount))
                .append("</td><td class=\"number\">")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerReportFormatter.formatEstimatedCostWithTime(cost)))
                .append("</td></tr>\n");
    }

    private List<HtmlSummaryRow> filterHtmlSummaryRows(
            List<HtmlSummaryRow> rows, HtmlSummaryPart part) {
        List<HtmlSummaryRow> result = new ArrayList<>();
        for (HtmlSummaryRow row : rows) {
            if (htmlSummaryPart(row) == part) {
                result.add(row);
            }
        }
        return result;
    }

    private HtmlSummaryPart htmlSummaryPart(HtmlSummaryRow row) {
        String objectType = AnalyzerReportFormatter.nullToEmpty(
                row == null ? null : row.objectType);
        if (isDmlSummaryType(objectType)) {
            return HtmlSummaryPart.DML;
        }
        if (isPlcsqlSummaryType(objectType)) {
            return HtmlSummaryPart.PLCSQL;
        }
        return HtmlSummaryPart.DDL;
    }

    private boolean isDmlSummaryType(String objectType) {
        return "SELECT".equals(objectType)
                || "INSERT".equals(objectType)
                || "UPDATE".equals(objectType)
                || "DELETE".equals(objectType)
                || AnalyzerStatementTypes.TYPE_STATIC_SQL.equals(objectType);
    }

    private boolean isPlcsqlSummaryType(String objectType) {
        return "PROCEDURE".equals(objectType)
                || "FUNCTION".equals(objectType)
                || "PROC_HEADER".equals(objectType)
                || "PROC_BODY".equals(objectType)
                || "FUNC_HEADER".equals(objectType)
                || "FUNC_BODY".equals(objectType);
    }

    private void appendHtmlSummaryRow(StringBuilder sb, HtmlSummaryRow row) {
        if (!row.tableSizeRows.isEmpty()) {
            appendHtmlTableSizeExpandableSummaryRow(sb, row);
            return;
        }
        if (!row.childRows.isEmpty()) {
            appendHtmlExpandableSummaryRow(sb, row);
            return;
        }
        appendHtmlPlainSummaryRow(sb, row);
    }

    private void appendHtmlPlainSummaryRow(StringBuilder sb, HtmlSummaryRow row) {
        appendHtmlPlainSummaryRow(sb, row, "", false);
    }

    private void appendHtmlPlainSummaryRow(
            StringBuilder sb, HtmlSummaryRow row, String rowAttributes, boolean childRow) {
        sb.append("<tr").append(rowAttributes).append("><td>");
        if (childRow) {
            sb.append("<span class=\"summary-child-object\">");
        }
        sb.append(AnalyzerReportFormatter.escapeHtml(row.objectType));
        if (childRow) {
            sb.append("</span>");
        }
        sb.append("</td><td class=\"number\">")
                .append(AnalyzerReportFormatter.formatNumber(row.totalCount))
                .append("</td><td class=\"number ")
                .append(row.errorCount > 0 ? "status-fail" : "status-ok")
                .append("\">")
                .append(AnalyzerReportFormatter.formatNumber(row.errorCount))
                .append("</td><td class=\"number\">")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerReportFormatter.formatEstimatedCostWithTime(row.cost)))
                .append("</td></tr>\n");
    }

    private void appendHtmlTableSizeExpandableSummaryRow(StringBuilder sb, HtmlSummaryRow row) {
        String groupId = htmlSummaryGroupId(row.objectType);
        sb.append("<tr><td><button type=\"button\" class=\"row-toggle\" aria-expanded=\"false\"")
                .append(" onclick=\"toggleSummaryRows(this,'")
                .append(groupId)
                .append("')\">&#9656;</button>")
                .append(AnalyzerReportFormatter.escapeHtml(row.objectType))
                .append("</td><td class=\"number\">")
                .append(AnalyzerReportFormatter.formatNumber(row.totalCount))
                .append("</td><td class=\"number ")
                .append(row.errorCount > 0 ? "status-fail" : "status-ok")
                .append("\">")
                .append(AnalyzerReportFormatter.formatNumber(row.errorCount))
                .append("</td><td class=\"number\">")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerReportFormatter.formatEstimatedCostWithTime(row.cost)))
                .append("</td></tr>\n");
        sb.append("<tr class=\"summary-child-row\" data-summary-parent=\"")
                .append(groupId)
                .append("\" hidden><td colspan=\"4\" class=\"nested-summary-cell\">\n");
        appendHtmlTableSizeNestedTable(sb, row.tableSizeRows);
        sb.append("</td></tr>\n");
    }

    private void appendHtmlTableSizeNestedTable(
            StringBuilder sb, List<AnalyzerTableSizeViewModel> tableSizes) {
        sb.append("<table class=\"nested-summary-table\">\n");
        sb.append("<tr><th>Table</th><th>Size</th><th>Est. rows</th></tr>\n");
        for (AnalyzerTableSizeViewModel tableSize : tableSizes) {
            sb.append("<tr><td>")
                    .append(AnalyzerReportFormatter.escapeHtml(tableSize.tableName()))
                    .append("</td><td class=\"number\">")
                    .append(AnalyzerReportFormatter.escapeHtml(
                            AnalyzerReportFormatter.formatBytes(tableSize.bytes())))
                    .append("</td><td class=\"number\">")
                    .append(AnalyzerReportFormatter.formatNumber(tableSize.estimatedRows()))
                    .append("</td></tr>\n");
        }
        sb.append("</table>\n");
    }

    private void appendHtmlExpandableSummaryRow(StringBuilder sb, HtmlSummaryRow row) {
        String groupId = htmlSummaryGroupId(row.objectType);
        sb.append("<tr><td><button type=\"button\" class=\"row-toggle\" aria-expanded=\"false\"")
                .append(" onclick=\"toggleSummaryRows(this,'")
                .append(groupId)
                .append("')\">&#9656;</button>")
                .append(AnalyzerReportFormatter.escapeHtml(row.objectType))
                .append("</td><td class=\"number\">")
                .append(AnalyzerReportFormatter.formatNumber(row.totalCount))
                .append("</td><td class=\"number ")
                .append(row.errorCount > 0 ? "status-fail" : "status-ok")
                .append("\">")
                .append(AnalyzerReportFormatter.formatNumber(row.errorCount))
                .append("</td><td class=\"number\">")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerReportFormatter.formatEstimatedCostWithTime(row.cost)))
                .append("</td></tr>\n");
        for (HtmlSummaryRow childRow : row.childRows) {
            appendHtmlPlainSummaryRow(
                    sb, childRow,
                    " class=\"summary-child-row\" data-summary-parent=\"" + groupId + "\" hidden",
                    true);
        }
    }

    private String htmlSummaryGroupId(String objectType) {
        String safeObjectType = AnalyzerReportFormatter.nullToEmpty(objectType)
                .toLowerCase(Locale.US);
        safeObjectType = safeObjectType.replaceAll("[^a-z0-9]+", "-");
        if (safeObjectType.isEmpty()) {
            return "summary-unknown";
        }
        return "summary-" + safeObjectType;
    }

    private void appendHtmlFailureDetails(StringBuilder sb) {
        List<AnalyzerFailure> failures = report.getFailures();
        appendHtmlReportSectionStart(sb, "Fail Summary");
        String failSummary = buildHtmlDetailCollapsedSummary(failures);
        if (failSummary.isEmpty()) {
            sb.append("<p class=\"muted\">(none)</p>\n");
        } else {
            sb.append(failSummary);
        }
        appendHtmlReportSectionEnd(sb);
        appendHtmlCollapsibleReportSectionStart(sb, "Fail Detail");
        if (failures.isEmpty()) {
            sb.append("<p class=\"muted\">(none)</p>\n");
            appendHtmlReportSectionEnd(sb);
            return;
        }

        Map<String, HtmlFailureGroup> staticSqlFailureGroups = buildStaticSqlFailureGroups();
        for (AnalyzerFailure failure : failures) {
            if (AnalyzerReportFormatter.staticSqlParentId(failure.getStatementId()) != null) {
                continue;
            }
            appendHtmlFailureDetail(sb, failure);
        }
        for (HtmlFailureGroup group : staticSqlFailureGroups.values()) {
            appendHtmlStaticSqlFailureGroup(sb, group);
        }
        appendHtmlReportSectionEnd(sb);
    }

    private String buildHtmlDetailCollapsedSummary(List<AnalyzerFailure> failures) {
        Map<String, Integer> failureCounts = new LinkedHashMap<>();
        for (AnalyzerFailure failure : failures) {
            String objectType = AnalyzerReportFormatter.displayObjectType(
                    failure.getStatementType());
            failureCounts.put(objectType, failureCounts.getOrDefault(objectType, 0) + 1);
        }
        if (failureCounts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        sb.append("<tr><th>Object Type</th><th>Count</th></tr>\n");
        for (Map.Entry<String, Integer> entry : failureCounts.entrySet()) {
            sb.append("<tr><td>")
                    .append(AnalyzerReportFormatter.escapeHtml(entry.getKey()))
                    .append("</td><td class=\"number status-fail\">")
                    .append(AnalyzerReportFormatter.formatNumber(entry.getValue()))
                    .append("</td></tr>\n");
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    private void appendHtmlExecutedFullQueries(StringBuilder sb) {
        if (!report.isDebugFullQuery()) {
            return;
        }

        List<StatementResult> statementResults = report.getStatementResults();
        appendHtmlReportSectionStart(sb, "Executed Full Queries");
        if (statementResults.isEmpty()) {
            sb.append("<p class=\"muted\">(none)</p>\n");
            appendHtmlReportSectionEnd(sb);
            return;
        }

        for (StatementResult statementResult : statementResults) {
            sb.append("<details>\n");
            sb.append("<summary>")
                    .append(AnalyzerReportFormatter.escapeHtml(
                            AnalyzerReportFormatter.nullToEmpty(statementResult.statementType)))
                    .append(" ")
                    .append(AnalyzerReportFormatter.escapeHtml(
                            AnalyzerReportFormatter.nullToEmpty(statementResult.statementId)))
                    .append(formatHtmlSummaryObjectName(statementResult.objectName))
                    .append(" [")
                    .append(statementResult.success ? "OK" : "FAIL")
                    .append("]</summary>\n");
            sb.append("<table>\n");
            appendHtmlInfoRow(sb, "Object",
                    AnalyzerReportFormatter.displayObjectType(statementResult.statementType));
            appendHtmlInfoRow(sb, "Statement ID", statementResult.statementId);
            appendHtmlInfoRow(sb, "Object name", statementResult.objectName);
            appendHtmlInfoRow(sb, "Status", statementResult.success ? "OK" : "FAIL");
            appendHtmlInfoRow(sb, "Stage", statementResult.failureStage == null
                    ? "" : String.valueOf(statementResult.failureStage));
            appendHtmlInfoRow(sb, "Detail", statementResult.detail);
            sb.append("</table>\n");
            appendHtmlStatementSql(sb, "Full Query", statementResult.sql);
            sb.append("</details>\n");
        }
        appendHtmlReportSectionEnd(sb);
    }

    private Map<String, HtmlFailureGroup> buildStaticSqlFailureGroups() {
        Map<String, HtmlFailureGroup> groups = new LinkedHashMap<>();
        for (AnalyzerFailure failure : report.getFailures()) {
            String parentId = AnalyzerReportFormatter.staticSqlParentId(failure.getStatementId());
            if (parentId == null) {
                continue;
            }
            HtmlFailureGroup group = groups.get(parentId);
            if (group == null) {
                group = new HtmlFailureGroup();
                group.parentStatement = findStatementResult(parentId);
                group.parentFailure = findFailure(parentId);
                groups.put(parentId, group);
            }
            group.staticSqlFailures.add(failure);
        }
        return groups;
    }

    private void appendHtmlStaticSqlFailureGroup(StringBuilder sb, HtmlFailureGroup group) {
        StatementResult parent = group.parentStatement;
        AnalyzerFailure parentFailure = group.parentFailure;
        String parentType = parentFailure != null
                ? parentFailure.getStatementType()
                : parent == null ? "PLCSQL" : parent.statementType;
        String parentId = parentFailure != null
                ? parentFailure.getStatementId()
                : parent == null
                        ? AnalyzerReportFormatter.staticSqlParentId(
                                group.staticSqlFailures.get(0).getStatementId())
                        : parent.statementId;
        String parentObjectName = parentFailure != null
                ? parentFailure.getObjectName()
                : parent == null ? "" : parent.objectName;

        sb.append("<details class=\"detail-item\">\n");
        sb.append("<summary>")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerReportFormatter.nullToEmpty(parentType)))
                .append(" ")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerReportFormatter.nullToEmpty(parentId)))
                .append(formatHtmlSummaryObjectName(parentObjectName))
                .append(" [STATIC SQL]</summary>\n");
        sb.append("<table>\n");
        appendHtmlInfoRow(sb, "Object",
                AnalyzerReportFormatter.displayObjectType(parentType));
        appendHtmlInfoRow(sb, "Statement ID", parentId);
        appendHtmlInfoRow(sb, "Object name", parentObjectName);
        appendHtmlInfoRow(sb, "Status", parentFailure == null ? "parsed" : "failed");
        appendHtmlInfoRow(sb, "Static SQL failures",
                AnalyzerReportFormatter.formatNumber(group.staticSqlFailures.size()));
        sb.append("</table>\n");

        if (parentFailure != null) {
            appendHtmlFailureBody(sb, parentFailure, "PL/CSQL Query");
        } else if (parent != null) {
            appendHtmlStatementSql(sb, "PL/CSQL Query", parent.sql);
        }

        for (AnalyzerFailure staticSqlFailure : group.staticSqlFailures) {
            sb.append("<h3>Static SQL Failure</h3>\n");
            appendHtmlFailureBody(sb, staticSqlFailure, "Static SQL Query");
        }
        sb.append("</details>\n");
    }

    private void appendHtmlStatementSql(StringBuilder sb, String title, String sql) {
        sb.append("<h3>").append(AnalyzerReportFormatter.escapeHtml(title)).append("</h3>\n");
        sb.append("<pre>");
        SqlAnnotator.appendAnnotatedSqlLines(
                sb, AnalyzerReportFormatter.nullToEmpty(sql), null, "\n",
                AnalyzerReportFormatter::escapeHtml, "");
        sb.append("</pre>\n");
    }

    private void appendHtmlFailureDetail(StringBuilder sb, AnalyzerFailure failure) {
        sb.append("<details class=\"detail-item\">\n");
        sb.append("<summary>")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerReportFormatter.nullToEmpty(failure.getStatementType())))
                .append(" ")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerReportFormatter.nullToEmpty(failure.getStatementId())))
                .append(formatHtmlSummaryObjectName(failure.getObjectName()))
                .append(" [")
                .append(AnalyzerReportFormatter.escapeHtml(
                        String.valueOf(failure.getFailureStage())))
                .append("]</summary>\n");
        appendHtmlFailureBody(sb, failure, "Full Query");
        sb.append("</details>\n");
    }

    private void appendHtmlFailureBody(
            StringBuilder sb, AnalyzerFailure failure, String queryTitle) {
        SqlAnnotator.SqlContextLocation location =
                SqlAnnotator.findSqlContextLocation(failure.getReason(), failure.getSql());
        sb.append("<table>\n");
        appendHtmlInfoRow(sb, "Object",
                AnalyzerReportFormatter.displayObjectType(failure.getStatementType()));
        appendHtmlInfoRow(sb, "Statement ID", failure.getStatementId());
        appendHtmlInfoRow(sb, "Object name", failure.getObjectName());
        appendHtmlInfoRow(sb, "Failure stage", String.valueOf(failure.getFailureStage()));
        appendHtmlInfoRow(sb, "Location", formatHtmlLocation(location));
        appendHtmlInfoRow(sb, "Cost",
                AnalyzerReportFormatter.formatEstimatedCostWithTime(failure.getEstimatedCost()));
        appendHtmlInfoRow(sb, "Reason", failure.getReason());
        sb.append("</table>\n");
        appendHtmlCostDetails(sb, failure);
        appendHtmlAnnotatedSql(sb, failure, location, queryTitle);
    }

    private void appendHtmlAnnotatedSql(
            StringBuilder sb,
            AnalyzerFailure failure,
            SqlAnnotator.SqlContextLocation location,
            String title) {
        String sql = AnalyzerReportFormatter.nullToEmpty(failure.getSql());
        sb.append("<h3>").append(AnalyzerReportFormatter.escapeHtml(title)).append("</h3>\n");
        sb.append("<pre>");
        SqlAnnotator.appendAnnotatedSqlLines(
                sb, sql, location, "\n", AnalyzerReportFormatter::escapeHtml, "");
        sb.append("</pre>\n");
    }

    private void appendHtmlCostDetails(StringBuilder sb, AnalyzerFailure failure) {
        sb.append("<h3>Error Cost Details</h3>\n");
        sb.append("<table>\n");
        sb.append("<tr><th>Item</th><th>Count</th><th>Unit Cost</th><th>Total Cost</th></tr>\n");
        if (failure.getCostDetails().isEmpty()) {
            sb.append("<tr><td colspan=\"4\" class=\"muted\">(none)</td></tr>\n");
        } else {
            for (AnalyzerCostDetail costDetail : failure.getCostDetails()) {
                sb.append("<tr><td>")
                        .append(AnalyzerReportFormatter.escapeHtml(costDetail.itemName()))
                        .append("</td><td class=\"number\">")
                        .append(AnalyzerReportFormatter.formatNumber(costDetail.count()))
                        .append("</td><td class=\"number\">")
                        .append(AnalyzerReportFormatter.escapeHtml(
                                AnalyzerReportFormatter.formatEstimatedCostWithTime(
                                        costDetail.unitCost())))
                        .append("</td><td class=\"number\">")
                        .append(AnalyzerReportFormatter.escapeHtml(
                                AnalyzerReportFormatter.formatEstimatedCostWithTime(
                                        costDetail.totalCost())))
                        .append("</td></tr>\n");
            }
        }
        sb.append("</table>\n");
    }

    private void appendHtmlConclusion(StringBuilder sb, float totalCost) {
        appendHtmlReportSectionStart(sb, "Conclusion");
        Map<String, HtmlConclusionSummary> summaries = buildHtmlConclusionSummaries(totalCost);
        sb.append("<table>\n");
        sb.append("<tr><th>Category</th><th>Analyzed</th><th>Failed</th><th>Total Cost</th><th>Estimated Time</th></tr>\n");
        appendHtmlConclusionRow(sb, "DDL + PL/CSQL", summaries.get("DDL_PLCSQL"));
        appendHtmlConclusionRow(sb, "DML", summaries.get("DML"));
        sb.append("</table>\n");
        appendHtmlReportSectionEnd(sb);
    }

    private Map<String, HtmlConclusionSummary> buildHtmlConclusionSummaries(float totalCost) {
        Map<String, HtmlConclusionSummary> summaries = new LinkedHashMap<>();
        summaries.put("DDL_PLCSQL", new HtmlConclusionSummary());
        summaries.put("DML", new HtmlConclusionSummary());

        for (StatementResult statementResult : report.getStatementResults()) {
            if ("CLEANUP".equals(statementResult.statementType)) {
                continue;
            }
            HtmlConclusionSummary summary = summaries.get(
                    htmlConclusionCategory(statementResult));
            summary.analyzedCount++;
            if (!statementResult.success) {
                summary.failedCount++;
            }
        }

        for (AnalyzerFailure failure : report.getFailures()) {
            HtmlConclusionSummary summary = summaries.get(htmlConclusionCategory(failure));
            summary.cost += failure.getEstimatedCost();
        }

        List<StatementResult> statementResults = report.getStatementResults();
        if (statementResults.isEmpty() && report.getAnalyzedStatementCount() > 0) {
            HtmlConclusionSummary summary = summaries.get("DDL_PLCSQL");
            summary.analyzedCount = report.getAnalyzedStatementCount();
            summary.failedCount = report.getFailedStatementCount();
            summary.cost = totalCost;
        }

        return summaries;
    }

    private String htmlConclusionCategory(StatementResult statementResult) {
        if (AnalyzerReportFormatter.staticSqlParentObjectType(
                statementResult.statementId) != null) {
            return "DDL_PLCSQL";
        }
        HtmlSummaryPart part = htmlSummaryPart(new HtmlSummaryRow(
                AnalyzerReportFormatter.displayObjectType(statementResult.statementType), 0));
        return part == HtmlSummaryPart.DML ? "DML" : "DDL_PLCSQL";
    }

    private String htmlConclusionCategory(AnalyzerFailure failure) {
        if (AnalyzerReportFormatter.staticSqlParentObjectType(failure.getStatementId()) != null) {
            return "DDL_PLCSQL";
        }
        HtmlSummaryPart part = htmlSummaryPart(new HtmlSummaryRow(
                AnalyzerReportFormatter.displayObjectType(failure.getStatementType()), 0));
        return part == HtmlSummaryPart.DML ? "DML" : "DDL_PLCSQL";
    }

    private void appendHtmlConclusionRow(
            StringBuilder sb, String category, HtmlConclusionSummary summary) {
        HtmlConclusionSummary safe = summary == null ? new HtmlConclusionSummary() : summary;
        sb.append("<tr><td class=\"metric\">")
                .append(AnalyzerReportFormatter.escapeHtml(category))
                .append("</td><td class=\"number\">")
                .append(AnalyzerReportFormatter.formatNumber(safe.analyzedCount))
                .append("</td><td class=\"number ")
                .append(safe.failedCount > 0 ? "status-fail" : "status-ok")
                .append("\">")
                .append(AnalyzerReportFormatter.formatNumber(safe.failedCount))
                .append("</td><td class=\"number\">")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerCostFormatter.formatCost(safe.cost)))
                .append("</td><td class=\"number\">")
                .append(AnalyzerReportFormatter.escapeHtml(
                        AnalyzerCostFormatter.formatTime(safe.cost)))
                .append("</td></tr>\n");
    }

    private List<HtmlSummaryRow> buildHtmlSummaryRows() {
        Map<String, HtmlSummaryRow> rows = new LinkedHashMap<>();
        appendObjectCountSummaryRows(rows);

        Map<String, AnalyzerReport.StatementTypeSummary> rootExecutionSummaries =
                new LinkedHashMap<>();
        for (StatementResult statementResult : report.getStatementResults()) {
            if ("CLEANUP".equals(statementResult.statementType)) {
                continue;
            }

            String objectType = AnalyzerReportFormatter.displayObjectType(
                    statementResult.statementType);
            String parentObjectType = AnalyzerReportFormatter.staticSqlParentObjectType(
                    statementResult.statementId);
            if (parentObjectType == null) {
                AnalyzerReport.StatementTypeSummary summary =
                        rootExecutionSummaries.get(objectType);
                if (summary == null) {
                    summary = new AnalyzerReport.StatementTypeSummary();
                    rootExecutionSummaries.put(objectType, summary);
                }
                summary.add(statementResult.success);
                continue;
            }

            addHtmlSummaryChildResult(rows, parentObjectType,
                    "STATIC " + objectType, statementResult.success);
        }

        for (Map.Entry<String, AnalyzerReport.StatementTypeSummary> entry :
                rootExecutionSummaries.entrySet()) {
            HtmlSummaryRow row = getOrCreateHtmlSummaryRow(rows, entry.getKey());
            row.totalCount = Math.max(row.totalCount, entry.getValue().totalCount);
            row.errorCount = entry.getValue().failedCount;
        }

        for (AnalyzerFailure failure : report.getFailures()) {
            String objectType = AnalyzerReportFormatter.displayObjectType(
                    failure.getStatementType());
            String parentObjectType = AnalyzerReportFormatter.staticSqlParentObjectType(
                    failure.getStatementId());
            HtmlSummaryRow row;
            if (parentObjectType == null) {
                row = getOrCreateHtmlSummaryRow(rows, objectType);
            } else {
                addHtmlSummaryChildCost(rows, parentObjectType,
                        "STATIC " + objectType, failure.getEstimatedCost());
                continue;
            }
            row.cost += failure.getEstimatedCost();
            if (row.errorCount == 0) {
                row.errorCount = 1;
            }
        }

        groupChildSummaryRows(rows, "VIEW", "VIEW_CREATE", "VIEW_ALTER");
        groupChildSummaryRows(rows, "PROCEDURE", "PROC_HEADER", "PROC_BODY");
        groupChildSummaryRows(rows, "FUNCTION", "FUNC_HEADER", "FUNC_BODY");
        appendTableSizeSummaryRows(rows);
        return new ArrayList<>(rows.values());
    }

    private void appendTableSizeSummaryRows(Map<String, HtmlSummaryRow> rows) {
        AnalyzerObjectCountPreviewViewModel objectCountPreview = report.getObjectCountPreview();
        if (objectCountPreview == null || objectCountPreview.tableSizes().isEmpty()) {
            return;
        }
        HtmlSummaryRow tableRow = getOrCreateHtmlSummaryRow(rows, "TABLE");
        tableRow.tableSizeRows.clear();
        tableRow.tableSizeRows.addAll(objectCountPreview.tableSizes());
    }

    private void groupChildSummaryRows(
            Map<String, HtmlSummaryRow> rows,
            String parentObjectType,
            String... childObjectTypes) {
        HtmlSummaryRow parentRow = rows.get(parentObjectType);
        boolean hadExistingChildRows = parentRow != null && !parentRow.childRows.isEmpty();
        long fallbackTotalCount = 0;
        int errorCount = 0;
        float cost = 0;
        for (String childObjectType : childObjectTypes) {
            HtmlSummaryRow childRow = rows.remove(childObjectType);
            if (childRow == null) {
                continue;
            }
            if (parentRow == null) {
                parentRow = new HtmlSummaryRow(parentObjectType, 0);
                rows.put(parentObjectType, parentRow);
            }
            parentRow.childRows.add(childRow);
            fallbackTotalCount = Math.max(fallbackTotalCount, childRow.totalCount);
            errorCount += childRow.errorCount;
            cost += childRow.cost;
        }

        if (parentRow == null || parentRow.childRows.isEmpty()) {
            return;
        }

        if (parentRow.totalCount == 0) {
            parentRow.totalCount = fallbackTotalCount;
        } else if (hadExistingChildRows) {
            parentRow.totalCount += fallbackTotalCount;
        }
        parentRow.errorCount = hadExistingChildRows
                ? parentRow.errorCount + errorCount
                : Math.max(parentRow.errorCount, errorCount);
        parentRow.cost += cost;
    }

    private void appendObjectCountSummaryRows(Map<String, HtmlSummaryRow> rows) {
        AnalyzerObjectCountPreviewViewModel objectCountPreview = report.getObjectCountPreview();
        if (objectCountPreview == null) {
            return;
        }

        if (objectCountPreview.oracleSourceLoaded()) {
            putHtmlSummaryRow(rows, "SCHEMA", objectCountPreview.catalogSchemaCount());
            putHtmlSummaryRow(rows, "TABLE", objectCountPreview.targetTableCount());
            putHtmlSummaryRow(rows, "PK", objectCountPreview.targetPkCount());
            putHtmlSummaryRow(rows, "FK", objectCountPreview.targetFkCount());
            putHtmlSummaryRow(rows, "VIEW", objectCountPreview.targetViewCount());
            putHtmlSummaryRow(rows, "SERIAL", objectCountPreview.targetSerialCount());
            putHtmlSummaryRow(rows, "SYNONYM", objectCountPreview.targetSynonymCount());
            putHtmlSummaryRow(rows, "GRANT", objectCountPreview.targetGrantCount());
            putHtmlSummaryRow(rows, "PROCEDURE", objectCountPreview.targetProcedureCount());
            putHtmlSummaryRow(rows, "FUNCTION", objectCountPreview.targetFunctionCount());
            putHtmlSummaryRow(rows, "TRIGGER", objectCountPreview.targetTriggerCount());
        }

        if (objectCountPreview.xmlSourceLoaded()) {
            putHtmlSummaryRow(rows, "SELECT", objectCountPreview.selectCount());
            putHtmlSummaryRow(rows, "INSERT", objectCountPreview.insertCount());
            putHtmlSummaryRow(rows, "UPDATE", objectCountPreview.updateCount());
            putHtmlSummaryRow(rows, "DELETE", objectCountPreview.deleteCount());
        }
    }

    private void putHtmlSummaryRow(
            Map<String, HtmlSummaryRow> rows, String objectType, long count) {
        rows.put(objectType, new HtmlSummaryRow(objectType, count));
    }

    private HtmlSummaryRow getOrCreateHtmlSummaryRow(
            Map<String, HtmlSummaryRow> rows, String objectType) {
        String safeObjectType = AnalyzerReportFormatter.nullToEmpty(objectType);
        if (safeObjectType.isEmpty()) {
            safeObjectType = "UNKNOWN";
        }
        HtmlSummaryRow row = rows.get(safeObjectType);
        if (row == null) {
            row = new HtmlSummaryRow(safeObjectType, 0);
            rows.put(safeObjectType, row);
        }
        return row;
    }

    private HtmlSummaryRow getOrCreateHtmlSummaryChildRow(
            Map<String, HtmlSummaryRow> rows,
            String parentObjectType,
            String childObjectType) {
        HtmlSummaryRow parentRow = getOrCreateHtmlSummaryRow(rows, parentObjectType);
        String safeChildObjectType = AnalyzerReportFormatter.nullToEmpty(childObjectType);
        if (safeChildObjectType.isEmpty()) {
            safeChildObjectType = "UNKNOWN";
        }
        for (HtmlSummaryRow childRow : parentRow.childRows) {
            if (safeChildObjectType.equals(childRow.objectType)) {
                return childRow;
            }
        }
        HtmlSummaryRow childRow = new HtmlSummaryRow(safeChildObjectType, 0);
        parentRow.childRows.add(childRow);
        return childRow;
    }

    private void addHtmlSummaryChildResult(
            Map<String, HtmlSummaryRow> rows,
            String parentObjectType,
            String childObjectType,
            boolean success) {
        HtmlSummaryRow parentRow = getOrCreateHtmlSummaryRow(rows, parentObjectType);
        HtmlSummaryRow childRow =
                getOrCreateHtmlSummaryChildRow(rows, parentObjectType, childObjectType);
        parentRow.totalCount++;
        childRow.totalCount++;
        if (!success) {
            parentRow.errorCount++;
            childRow.errorCount++;
        }
    }

    private void addHtmlSummaryChildCost(
            Map<String, HtmlSummaryRow> rows,
            String parentObjectType,
            String childObjectType,
            float cost) {
        HtmlSummaryRow parentRow = getOrCreateHtmlSummaryRow(rows, parentObjectType);
        HtmlSummaryRow childRow =
                getOrCreateHtmlSummaryChildRow(rows, parentObjectType, childObjectType);
        parentRow.cost += cost;
        childRow.cost += cost;
        if (childRow.errorCount == 0 && cost > 0) {
            parentRow.errorCount++;
            childRow.errorCount = 1;
        }
    }

    private boolean isParserTarget() {
        if (report.getTargetType() == AnalyzerTargetType.PARSER) {
            return true;
        }
        AnalyzerOverviewViewModel overview = report.getOverview();
        return overview != null
                && overview.target() != null
                && overview.target().type() == AnalyzerTargetType.PARSER;
    }

    private String resolveSourceOracleSid() {
        AnalyzerOverviewViewModel overview = report.getOverview();
        if (overview == null || overview.sources().isEmpty()) {
            return "";
        }
        for (AnalyzerSourceOverviewViewModel source : overview.sources()) {
            if (source.type() == AnalyzerSourceType.ORACLE) {
                return AnalyzerReportFormatter.nullToEmpty(source.databaseName());
            }
        }
        return "";
    }

    private String resolveSchemaName() {
        AnalyzerOverviewViewModel overview = report.getOverview();
        if (overview == null || overview.sources().isEmpty()) {
            return "";
        }
        for (AnalyzerSourceOverviewViewModel source : overview.sources()) {
            if (source.type() == AnalyzerSourceType.ORACLE
                    && !AnalyzerReportFormatter.nullToEmpty(source.user()).isEmpty()) {
                return source.user();
            }
        }
        AnalyzerSourceOverviewViewModel source = overview.source();
        return source == null ? "" : AnalyzerReportFormatter.nullToEmpty(source.databaseName());
    }

    private String resolveTargetTypeName() {
        AnalyzerOverviewViewModel overview = report.getOverview();
        if (overview != null && overview.target() != null
                && overview.target().type() != null) {
            return String.valueOf(overview.target().type());
        }
        return report.getTargetType() == null ? "" : String.valueOf(report.getTargetType());
    }

    private String resolveSourceTableSize() {
        AnalyzerObjectCountPreviewViewModel objectCountPreview = report.getObjectCountPreview();
        return objectCountPreview == null
                ? "0 B"
                : AnalyzerReportFormatter.formatBytes(objectCountPreview.totalTableBytes());
    }

    private String resolveSourceStatus(AnalyzerSourceType sourceType) {
        String prefix = sourceType == AnalyzerSourceType.ORACLE ? "Oracle source " : "XML source ";
        for (String message : sourceStatusMessages()) {
            String safeMessage = AnalyzerReportFormatter.nullToEmpty(message);
            if (!safeMessage.startsWith(prefix)) {
                continue;
            }
            if (safeMessage.startsWith(prefix + "loaded")) {
                return "Executed";
            }
            if (safeMessage.startsWith(prefix + "skipped: ")) {
                return "Not executed - " + safeMessage.substring((prefix + "skipped: ").length());
            }
            return safeMessage;
        }

        if (!isSourceRequested(sourceType)) {
            return "Not requested";
        }
        return "Not executed";
    }

    private List<String> sourceStatusMessages() {
        List<String> messages = new ArrayList<>(report.getSourceStatusMessages());
        AnalyzerOverviewViewModel overview = report.getOverview();
        if (overview != null) {
            messages.addAll(overview.sourceStatusMessages());
        }
        return messages;
    }

    private boolean isSourceRequested(AnalyzerSourceType sourceType) {
        AnalyzerSourceType reportSourceType = report.getSourceType();
        if (reportSourceType == AnalyzerSourceType.ALL || reportSourceType == sourceType) {
            return true;
        }
        AnalyzerOverviewViewModel overview = report.getOverview();
        if (overview != null) {
            for (AnalyzerSourceOverviewViewModel source : overview.sources()) {
                if (source.type() == sourceType) {
                    return true;
                }
            }
        }
        AnalyzerObjectCountPreviewViewModel objectCountPreview = report.getObjectCountPreview();
        if (objectCountPreview == null) {
            return false;
        }
        if (sourceType == AnalyzerSourceType.ORACLE) {
            return objectCountPreview.oracleSourceLoaded();
        }
        if (sourceType == AnalyzerSourceType.XML) {
            return objectCountPreview.xmlSourceLoaded();
        }
        return false;
    }

    private String formatHtmlLocation(SqlAnnotator.SqlContextLocation location) {
        if (location == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("line ").append(location.lineNumber)
                .append(", column ").append(location.columnNumber);
        if (location.estimated) {
            sb.append(" (estimated)");
        }
        return sb.toString();
    }

    private StatementResult findStatementResult(String statementId) {
        String id = AnalyzerReportFormatter.nullToEmpty(statementId);
        for (StatementResult result : report.getStatementResults()) {
            if (id.equals(AnalyzerReportFormatter.nullToEmpty(result.statementId))) {
                return result;
            }
        }
        return null;
    }

    private AnalyzerFailure findFailure(String statementId) {
        String id = AnalyzerReportFormatter.nullToEmpty(statementId);
        for (AnalyzerFailure failure : report.getFailures()) {
            if (id.equals(AnalyzerReportFormatter.nullToEmpty(failure.getStatementId()))) {
                return failure;
            }
        }
        return null;
    }
}
