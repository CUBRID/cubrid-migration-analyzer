/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.report;

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
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;

class AnalyzerTextReportBuilder {

    private final AnalyzerReport report;

    AnalyzerTextReportBuilder(AnalyzerReport report) {
        this.report = report;
    }

    String build() {
        String lineSeparator = System.lineSeparator();
        StringBuilder sb = new StringBuilder();

        appendOverview(sb, lineSeparator);
        appendAnalysisSummary(sb, lineSeparator);
        appendFailedStatements(sb, lineSeparator);
        appendExecutedFullQueries(sb, lineSeparator);

        return sb.toString();
    }

    private void appendOverview(StringBuilder sb, String lineSeparator) {
        AnalyzerOverviewViewModel overview = report.getOverview();

        sb.append("Overview").append(lineSeparator);
        if (overview != null) {
            sb.append("Program     : ").append(AnalyzerReportFormatter.nullToEmpty(overview.programVersion())).append(lineSeparator);
            appendSourceOverviews(sb, overview.sources(), lineSeparator);
            appendTargetOverview(sb, overview.target(), lineSeparator);
            sb.append("Mode        : ").append(overview.executionMode()).append(lineSeparator);
            appendSourceStatusMessages(sb, overview.sourceStatusMessages(), lineSeparator);
        } else {
            sb.append("Source      : ").append(report.getSourceType()).append(lineSeparator);
            sb.append("Target      : ").append(report.getTargetType()).append(lineSeparator);
            sb.append("Mode        : ").append(report.getExecutionMode()).append(lineSeparator);
            appendSourceStatusMessages(sb, report.getSourceStatusMessages(), lineSeparator);
        }
        sb.append("Total       : ").append(report.getAnalyzedStatementCount()).append(lineSeparator);
        sb.append("OK          : ").append(report.getSucceededStatementCount()).append(lineSeparator);
        sb.append("FAIL        : ").append(report.getFailedStatementCount()).append(lineSeparator);
        sb.append("Cost        : ")
                .append(AnalyzerReportFormatter.formatEstimatedCostWithTime(report.getTotalEstimatedFailureCost()))
                .append(lineSeparator);
        sb.append(lineSeparator);
    }

    private void appendAnalysisSummary(StringBuilder sb, String lineSeparator) {
        sb.append("Analysis summary").append(lineSeparator);
        appendObjectCounts(sb, lineSeparator);
        appendObjectExecutionSummary(sb, lineSeparator);
        sb.append(lineSeparator);
    }

    private void appendObjectCounts(StringBuilder sb, String lineSeparator) {
        AnalyzerObjectCountPreviewViewModel objectCountPreview = report.getObjectCountPreview();

        sb.append("Object counts").append(lineSeparator);
        if (objectCountPreview == null) {
            sb.append("(none)").append(lineSeparator).append(lineSeparator);
            return;
        }

        sb.append("DDL objects").append(lineSeparator);
        if (objectCountPreview.oracleSourceLoaded()) {
            sb.append("Catalog schemas : ").append(objectCountPreview.catalogSchemaCount()).append(lineSeparator);
            sb.append("Target tables   : ").append(objectCountPreview.targetTableCount()).append(lineSeparator);
            sb.append("Target PKs      : ").append(objectCountPreview.targetPkCount()).append(lineSeparator);
            sb.append("Target FKs      : ").append(objectCountPreview.targetFkCount()).append(lineSeparator);
            sb.append("Target views    : ").append(objectCountPreview.targetViewCount()).append(lineSeparator);
            sb.append("Target serials  : ").append(objectCountPreview.targetSerialCount()).append(lineSeparator);
            sb.append("Target synonyms : ").append(objectCountPreview.targetSynonymCount()).append(lineSeparator);
            sb.append("Target grants   : ").append(objectCountPreview.targetGrantCount()).append(lineSeparator);
            sb.append("Target procs    : ").append(objectCountPreview.targetProcedureCount()).append(lineSeparator);
            sb.append("Target funcs    : ").append(objectCountPreview.targetFunctionCount()).append(lineSeparator);
            sb.append("Target triggers : ").append(objectCountPreview.targetTriggerCount()).append(lineSeparator);
            sb.append(lineSeparator);
            appendOracleTableSizes(sb, lineSeparator);
            sb.append(lineSeparator);
        } else {
            sb.append("  (none)").append(lineSeparator).append(lineSeparator);
        }

        sb.append("DML statements").append(lineSeparator);
        if (objectCountPreview.xmlSourceLoaded()) {
            sb.append("SELECT count    : ").append(objectCountPreview.selectCount()).append(lineSeparator);
            sb.append("INSERT count    : ").append(objectCountPreview.insertCount()).append(lineSeparator);
            sb.append("UPDATE count    : ").append(objectCountPreview.updateCount()).append(lineSeparator);
            sb.append("DELETE count    : ").append(objectCountPreview.deleteCount()).append(lineSeparator);
        } else {
            sb.append("  (none)").append(lineSeparator);
        }
        sb.append(lineSeparator);
    }

    private void appendOracleTableSizes(StringBuilder sb, String lineSeparator) {
        AnalyzerObjectCountPreviewViewModel objectCountPreview = report.getObjectCountPreview();

        sb.append("Oracle table size total : ")
                .append(AnalyzerReportFormatter.formatBytes(objectCountPreview.totalTableBytes()))
                .append(lineSeparator);
        sb.append("Oracle table sizes").append(lineSeparator);
        if (objectCountPreview.tableSizes().isEmpty()) {
            sb.append("  (none)").append(lineSeparator);
            return;
        }

        sb.append(String.format(Locale.US, "  %-32s %12s %12s", "Table", "Size", "Est. rows"))
                .append(lineSeparator);
        for (AnalyzerTableSizeViewModel tableSize : objectCountPreview.tableSizes()) {
            sb.append(String.format(
                    Locale.US,
                    "  %-32s %12s %12s",
                    AnalyzerReportFormatter.fitText(tableSize.tableName(), 32),
                    AnalyzerReportFormatter.formatBytes(tableSize.bytes()),
                    AnalyzerReportFormatter.formatNumber(tableSize.estimatedRows())))
                    .append(lineSeparator);
        }
    }

    private void appendSourceOverview(
            StringBuilder sb,
            AnalyzerSourceOverviewViewModel source,
            String lineSeparator) {
        if (source == null) {
            return;
        }

        sb.append("Source      : ").append(source.type()).append(lineSeparator);
        if (source.type() == AnalyzerSourceType.ORACLE) {
            sb.append("Oracle URL  : ")
                    .append(AnalyzerReportFormatter.nullToEmpty(source.jdbcUrl()))
                    .append(AnalyzerReportFormatter.formatVersionSuffix(source.version()))
                    .append(lineSeparator);
            sb.append("Oracle Host : ")
                    .append(AnalyzerReportFormatter.formatHost(source.host(), source.port()))
                    .append(lineSeparator);
            sb.append("Oracle DB   : ")
                    .append(AnalyzerReportFormatter.nullToEmpty(source.databaseName()))
                    .append(lineSeparator);
            sb.append("Oracle User : ")
                    .append(AnalyzerReportFormatter.nullToEmpty(source.user()))
                    .append(lineSeparator);
            return;
        }

        sb.append("XML dir     : ").append(AnalyzerReportFormatter.nullToEmpty(source.xmlDirectory())).append(lineSeparator);
        sb.append("XML charset : ").append(AnalyzerReportFormatter.nullToEmpty(source.xmlCharset())).append(lineSeparator);
        sb.append("XML files   : ").append(source.xmlFileCount()).append(lineSeparator);
    }

    private void appendSourceOverviews(
            StringBuilder sb,
            List<AnalyzerSourceOverviewViewModel> sources,
            String lineSeparator) {
        if (sources == null || sources.isEmpty()) {
            sb.append("Source      : ").append(lineSeparator);
            return;
        }

        for (AnalyzerSourceOverviewViewModel source : sources) {
            appendSourceOverview(sb, source, lineSeparator);
        }
    }

    private void appendSourceStatusMessages(
            StringBuilder sb,
            List<String> messages,
            String lineSeparator) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        sb.append("Source status").append(lineSeparator);
        for (String message : messages) {
            sb.append("  - ").append(AnalyzerReportFormatter.nullToEmpty(message)).append(lineSeparator);
        }
    }

    private void appendTargetOverview(
            StringBuilder sb,
            AnalyzerTargetOverviewViewModel target,
            String lineSeparator) {
        if (target == null) {
            return;
        }

        sb.append("Target      : ").append(target.type()).append(lineSeparator);
        if (target.type() == AnalyzerTargetType.JDBC) {
            sb.append("Target URL  : ")
                    .append(AnalyzerReportFormatter.nullToEmpty(target.jdbcUrl()))
                    .append(AnalyzerReportFormatter.formatVersionSuffix(target.version()))
                    .append(lineSeparator);
            sb.append("Target Host : ")
                    .append(AnalyzerReportFormatter.formatHost(target.host(), target.port()))
                    .append(lineSeparator);
            sb.append("Target DB   : ")
                    .append(AnalyzerReportFormatter.nullToEmpty(target.databaseName()))
                    .append(lineSeparator);
            sb.append("Target User : ")
                    .append(AnalyzerReportFormatter.nullToEmpty(target.user()))
                    .append(lineSeparator);
            return;
        }

        if (target.type() == AnalyzerTargetType.PARSER) {
            sb.append("Parser      : ")
                    .append(AnalyzerReportFormatter.nullToEmpty(target.parserVersion()))
                    .append(lineSeparator);
        }
    }

    private void appendObjectExecutionSummary(StringBuilder sb, String lineSeparator) {
        sb.append("Execution results").append(lineSeparator);
        Map<String, AnalyzerReport.StatementTypeSummary> summaries = buildStatementTypeSummaries();
        if (summaries.isEmpty()) {
            sb.append("(none)").append(lineSeparator);
            return;
        }

        sb.append(String.format(Locale.US, "%-24s %7s %7s %7s", "Type", "Total", "OK", "FAIL"))
                .append(lineSeparator);
        for (Map.Entry<String, AnalyzerReport.StatementTypeSummary> entry : summaries.entrySet()) {
            AnalyzerReport.StatementTypeSummary summary = entry.getValue();
            sb.append(String.format(
                    Locale.US,
                    "%-24s %7d %7d %7d",
                    entry.getKey(),
                    summary.totalCount,
                    summary.succeededCount,
                    summary.failedCount))
                    .append(lineSeparator);
        }
    }

    private void appendFailedStatements(StringBuilder sb, String lineSeparator) {
        List<AnalyzerFailure> failures = report.getFailures();
        List<String> failureMessages = report.getFailureMessages();

        if (!failures.isEmpty()) {
            sb.append("Failed statements").append(lineSeparator);
            for (AnalyzerFailure failure : failures) {
                appendFailureBlock(sb, failure, lineSeparator);
            }
            sb.append("----------------------------------------").append(lineSeparator);
        } else if (!failureMessages.isEmpty()) {
            sb.append("Failed statements").append(lineSeparator);
            for (String failureMessage : failureMessages) {
                sb.append("----------------------------------------").append(lineSeparator);
                sb.append("- ").append(AnalyzerReportFormatter.nullToEmpty(failureMessage)).append(lineSeparator);
            }
            sb.append("----------------------------------------").append(lineSeparator);
        } else {
            sb.append("Failed statements").append(lineSeparator);
            sb.append("(none)").append(lineSeparator);
        }
    }

    private void appendExecutedFullQueries(StringBuilder sb, String lineSeparator) {
        if (!report.isDebugFullQuery()) {
            return;
        }

        sb.append(lineSeparator);
        sb.append("Executed full queries").append(lineSeparator);
        List<StatementResult> statementResults = report.getStatementResults();
        if (statementResults.isEmpty()) {
            sb.append("(none)").append(lineSeparator);
            return;
        }

        for (StatementResult statementResult : statementResults) {
            sb.append("----------------------------------------").append(lineSeparator);
            sb.append("- ")
                    .append(AnalyzerReportFormatter.nullToEmpty(statementResult.statementType))
                    .append(" ")
                    .append(AnalyzerReportFormatter.nullToEmpty(statementResult.statementId))
                    .append(" [")
                    .append(statementResult.success ? "OK" : "FAIL")
                    .append("]")
                    .append(lineSeparator);
            if (!AnalyzerReportFormatter.nullToEmpty(statementResult.objectName).isEmpty()) {
                sb.append("  Object: ")
                        .append(AnalyzerReportFormatter.nullToEmpty(statementResult.objectName))
                        .append(lineSeparator);
            }
            sb.append("  Detail: ")
                    .append(AnalyzerReportFormatter.nullToEmpty(statementResult.detail))
                    .append(lineSeparator);
            sb.append("  SQL:").append(lineSeparator);
            SqlAnnotator.appendAnnotatedSqlLines(
                    sb,
                    statementResult.sql,
                    null,
                    lineSeparator,
                    AnalyzerReportFormatter::nullToEmpty,
                    "    ");
        }
        sb.append("----------------------------------------").append(lineSeparator);
    }

    private Map<String, AnalyzerReport.StatementTypeSummary> buildStatementTypeSummaries() {
        Map<String, AnalyzerReport.StatementTypeSummary> summaries =
                new LinkedHashMap<String, AnalyzerReport.StatementTypeSummary>();
        for (StatementResult statementResult : report.getStatementResults()) {
            if ("CLEANUP".equals(statementResult.statementType)) {
                continue;
            }

            String statementType = AnalyzerReportFormatter.displayStatementSummaryType(
                    statementResult.statementType);
            AnalyzerReport.StatementTypeSummary summary = summaries.get(statementType);
            if (summary == null) {
                summary = new AnalyzerReport.StatementTypeSummary();
                summaries.put(statementType, summary);
            }
            summary.add(statementResult.success);
        }
        return summaries;
    }

    private void appendFailureBlock(
            StringBuilder sb, AnalyzerFailure failure, String lineSeparator) {
        sb.append("----------------------------------------").append(lineSeparator);
        sb.append("- ")
                .append(AnalyzerReportFormatter.nullToEmpty(failure.getStatementType()))
                .append(" ")
                .append(AnalyzerReportFormatter.nullToEmpty(failure.getStatementId()))
                .append(" [")
                .append(failure.getFailureStage())
                .append("]")
                .append(lineSeparator);
        if (!AnalyzerReportFormatter.nullToEmpty(failure.getObjectName()).isEmpty()) {
            sb.append("  Object: ")
                    .append(AnalyzerReportFormatter.nullToEmpty(failure.getObjectName()))
                    .append(lineSeparator);
        }
        sb.append("  Reason: ").append(AnalyzerReportFormatter.nullToEmpty(failure.getReason())).append(lineSeparator);
        sb.append("  Cost  : ")
                .append(AnalyzerReportFormatter.formatEstimatedCostWithTime(failure.getEstimatedCost()))
                .append(lineSeparator);
        appendCostDetails(sb, failure, lineSeparator);
        appendAnnotatedSql(sb, failure, lineSeparator);
    }

    private void appendAnnotatedSql(
            StringBuilder sb, AnalyzerFailure failure, String lineSeparator) {
        String sql = AnalyzerReportFormatter.nullToEmpty(failure.getSql());
        String[] lines = SqlAnnotator.splitSqlLines(sql);
        SqlAnnotator.SqlContextLocation location = SqlAnnotator.validSqlContextLocation(
                SqlAnnotator.findSqlContextLocation(failure.getReason(), sql),
                lines.length);
        if (location != null) {
            sb.append("  Location: line ")
                    .append(location.lineNumber)
                    .append(", column ")
                    .append(location.columnNumber);
            if (location.estimated) {
                sb.append(" (estimated)");
            }
            sb.append(lineSeparator);
        }

        sb.append("  SQL:").append(lineSeparator);
        SqlAnnotator.appendAnnotatedSqlLines(
                sb, sql, location, lineSeparator, AnalyzerReportFormatter::nullToEmpty, "    ");
    }

    private void appendCostDetails(
            StringBuilder sb, AnalyzerFailure failure, String lineSeparator) {
        sb.append("  Cost details:").append(lineSeparator);
        if (failure.getCostDetails().isEmpty()) {
            sb.append("    (none)").append(lineSeparator);
            return;
        }

        for (AnalyzerCostDetail costDetail : failure.getCostDetails()) {
            sb.append("    - ")
                    .append(AnalyzerReportFormatter.nullToEmpty(costDetail.itemName()))
                    .append(" : count=")
                    .append(costDetail.count())
                    .append(", unit=")
                    .append(AnalyzerReportFormatter.formatEstimatedCostWithTime(costDetail.unitCost()))
                    .append(", total=")
                    .append(AnalyzerReportFormatter.formatEstimatedCostWithTime(costDetail.totalCost()))
                    .append(lineSeparator);
        }
    }
}
