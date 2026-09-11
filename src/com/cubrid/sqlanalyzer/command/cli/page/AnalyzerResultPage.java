/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.cli.page;

import com.cubrid.sqlanalyzer.command.model.AnalyzerCostDetail;
import com.cubrid.sqlanalyzer.command.report.AnalyzerCostFormatter;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.cli.ConsoleIO;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerResultViewModel;

public class AnalyzerResultPage {
    private final ConsoleIO io;

    public AnalyzerResultPage(ConsoleIO io) {
        this.io = io;
    }

    public void render(AnalyzerResultViewModel result) {
        io.println("");
        io.println("Result summary");
        io.println("Source : " + result.sourceType());
        io.println("Target : " + result.targetType());
        io.println("Mode   : " + result.executionMode());
        io.println("Total  : " + result.analyzedStatementCount());
        io.println("OK     : " + result.succeededStatementCount());
        io.println("FAIL   : " + result.failedStatementCount());
        io.println(
                "Cost   : "
                        + AnalyzerCostFormatter.formatCostWithTime(
                                result.totalEstimatedFailureCost()));

        if (!result.sourceStatusMessages().isEmpty()) {
            io.println("");
            io.println("Source status");
            for (String message : result.sourceStatusMessages()) {
                io.println("  - " + message);
            }
        }

        if (!result.failures().isEmpty()) {
            io.println("");
            io.println("Failed statements");
            for (AnalyzerFailure failure : result.failures()) {
                renderFailure(failure);
            }
            io.println("----------------------------------------");
        } else if (!result.failureMessages().isEmpty()) {
            io.println("");
            io.println("Failed statements");
            for (String failureMessage : result.failureMessages()) {
                io.println("----------------------------------------");
                io.println("- " + failureMessage);
            }
            io.println("----------------------------------------");
        }

        io.println("");
        io.println("Saved result report: " + result.savedReportPath());
        io.println("Saved HTML report: " + result.savedHtmlReportPath());
    }

    private void renderFailure(AnalyzerFailure failure) {
        io.println("----------------------------------------");
        io.println(
                "- "
                        + failure.getStatementType()
                        + " "
                        + failure.getStatementId()
                        + " ["
                        + failure.getFailureStage()
                        + "]");
        io.println("  Reason: " + failure.getReason());
        io.println(
                "  Cost  : "
                        + AnalyzerCostFormatter.formatCostWithTime(failure.getEstimatedCost()));
        io.println("  Cost details:");
        if (failure.getCostDetails().isEmpty()) {
            io.println("    (none)");
        } else {
            for (AnalyzerCostDetail costDetail : failure.getCostDetails()) {
                renderCostDetail(costDetail);
            }
        }
        io.println("  SQL : " + String.valueOf(failure.getSql()));
    }

    private void renderCostDetail(AnalyzerCostDetail costDetail) {
        io.println(
                "    - "
                        + costDetail.itemName()
                        + " : count="
                        + costDetail.count()
                        + ", unit="
                        + AnalyzerCostFormatter.formatCostWithTime(costDetail.unitCost())
                        + ", total="
                        + AnalyzerCostFormatter.formatCostWithTime(costDetail.totalCost()));
    }
}
