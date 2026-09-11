/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cubrid.sqlanalyzer.command.model.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailureStage;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressObjectCount;

public class AnalyzerReport {

    static class StatementTypeSummary {
        int totalCount;
        int succeededCount;
        int failedCount;

        void add(boolean success) {
            totalCount++;
            if (success) {
                succeededCount++;
            } else {
                failedCount++;
            }
        }
    }

    private AnalyzerSourceType sourceType;
    private AnalyzerTargetType targetType;
    private AnalyzerExecutionMode executionMode;
    private int analyzedStatementCount;
    private int succeededStatementCount;
    private int failedStatementCount;
    private boolean debugFullQuery;
    private final List<String> failureMessages = new ArrayList<>();
    private final List<String> sourceStatusMessages = new ArrayList<>();
    private final List<AnalyzerFailure> failures = new ArrayList<>();
    private final List<StatementResult> statementResults = new ArrayList<>();
    private AnalyzerOverviewViewModel overview;
    private AnalyzerObjectCountPreviewViewModel objectCountPreview;

    public AnalyzerSourceType getSourceType() { return sourceType; }
    public void setSourceType(AnalyzerSourceType sourceType) { this.sourceType = sourceType; }

    public AnalyzerTargetType getTargetType() { return targetType; }
    public void setTargetType(AnalyzerTargetType targetType) { this.targetType = targetType; }

    public AnalyzerExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(AnalyzerExecutionMode executionMode) { this.executionMode = executionMode; }

    public int getAnalyzedStatementCount() { return analyzedStatementCount; }
    public void setAnalyzedStatementCount(int analyzedStatementCount) { this.analyzedStatementCount = analyzedStatementCount; }

    public int getSucceededStatementCount() { return succeededStatementCount; }
    public void setSucceededStatementCount(int succeededStatementCount) { this.succeededStatementCount = succeededStatementCount; }

    public int getFailedStatementCount() { return failedStatementCount; }
    public void setFailedStatementCount(int failedStatementCount) { this.failedStatementCount = failedStatementCount; }

    public boolean isDebugFullQuery() { return debugFullQuery; }
    public void setDebugFullQuery(boolean debugFullQuery) { this.debugFullQuery = debugFullQuery; }

    public AnalyzerOverviewViewModel getOverview() { return overview; }
    public void setOverview(AnalyzerOverviewViewModel overview) { this.overview = overview; }

    public AnalyzerObjectCountPreviewViewModel getObjectCountPreview() { return objectCountPreview; }
    public void setObjectCountPreview(AnalyzerObjectCountPreviewViewModel objectCountPreview) { this.objectCountPreview = objectCountPreview; }

    public List<String> getFailureMessages() { return failureMessages; }

    public List<String> getSourceStatusMessages() { return sourceStatusMessages; }

    public void clearSourceStatusMessages() {
        sourceStatusMessages.clear();
    }

    public void addSourceStatusMessage(String sourceStatusMessage) {
        if (sourceStatusMessage != null && !sourceStatusMessage.isEmpty()) {
            sourceStatusMessages.add(sourceStatusMessage);
        }
    }

    public void clearFailures() {
        failureMessages.clear();
        failures.clear();
        statementResults.clear();
    }

    public void addFailureMessage(String failureMessage) {
        failureMessages.add(failureMessage);
    }

    public List<AnalyzerFailure> getFailures() { return failures; }

    public void addFailure(AnalyzerFailure failure) {
        failures.add(failure);
    }

    public void addStatementResult(
            String statementType,
            String statementId,
            String sql,
            boolean success,
            String detail,
            AnalyzerFailureStage failureStage) {
        addStatementResult(statementType, statementId, null, sql, success, detail, failureStage);
    }

    public void addStatementResult(
            String statementType,
            String statementId,
            String objectName,
            String sql,
            boolean success,
            String detail,
            AnalyzerFailureStage failureStage) {
        statementResults.add(new StatementResult(
                statementType, statementId, objectName, sql, success, detail, failureStage));
    }

    List<StatementResult> getStatementResults() {
        return Collections.unmodifiableList(statementResults);
    }

    public List<AnalyzerProgressObjectCount> getObjectExecutionCounts() {
        Map<String, StatementTypeSummary> summaries = new LinkedHashMap<>();
        for (StatementResult statementResult : statementResults) {
            if ("CLEANUP".equals(statementResult.statementType)) {
                continue;
            }
            String objectType = AnalyzerReportFormatter.displayObjectType(
                    statementResult.statementType);
            StatementTypeSummary summary = summaries.get(objectType);
            if (summary == null) {
                summary = new StatementTypeSummary();
                summaries.put(objectType, summary);
            }
            summary.add(statementResult.success);
        }

        List<AnalyzerProgressObjectCount> result = new ArrayList<>();
        for (Map.Entry<String, StatementTypeSummary> entry : summaries.entrySet()) {
            StatementTypeSummary summary = entry.getValue();
            result.add(new AnalyzerProgressObjectCount(
                    entry.getKey(),
                    summary.totalCount,
                    summary.succeededCount,
                    summary.failedCount));
        }
        return result;
    }

    public float getTotalEstimatedFailureCost() {
        float total = 0.0f;
        for (AnalyzerFailure failure : failures) {
            total += failure.getEstimatedCost();
        }
        return total;
    }

    public String saveResultReport() {
        try {
            return new AnalyzerReportWriter().save(this);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save analyzer report: " + e.getMessage(), e);
        }
    }

    public String buildResultText() {
        return new AnalyzerTextReportBuilder(this).build();
    }

    public String buildResultHtml() {
        return new AnalyzerHtmlReportBuilder(this, System.currentTimeMillis()).build();
    }
}
