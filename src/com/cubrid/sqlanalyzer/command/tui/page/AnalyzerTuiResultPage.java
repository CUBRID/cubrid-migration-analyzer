/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.tui.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cubrid.sqlanalyzer.command.report.AnalyzerCostFormatter;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressObjectCount;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerResultViewModel;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

public class AnalyzerTuiResultPage {
    private static final int RESERVED_ROWS = 6;

    public Panel build(AnalyzerResultViewModel result) {
        Panel panel = new Panel();
        for (String line : buildLines(result)) {
            panel.addComponent(new Label(line));
        }
        return panel;
    }

    public Panel build(AnalyzerResultViewModel result, TerminalSize terminalSize) {
        Panel panel = new Panel();
        panel.addComponent(AnalyzerTuiLayout.readOnlyTextBox(
                buildLines(result),
                terminalSize,
                RESERVED_ROWS));
        return panel;
    }

    List<String> buildLines(AnalyzerResultViewModel result) {
        List<String> lines = new ArrayList<String>();
        lines.add("[4/4] Result summary");
        lines.add("Source : " + result.sourceType());
        lines.add("Target : " + result.targetType());
        lines.add("Mode   : " + result.executionMode());
        lines.add("Total  : " + result.analyzedStatementCount());
        lines.add("OK     : " + result.succeededStatementCount());
        lines.add("FAIL   : " + result.failedStatementCount());
        lines.add("Cost   : " + AnalyzerCostFormatter.formatCostWithTime(
                result.totalEstimatedFailureCost()));

        if (!result.sourceStatusMessages().isEmpty()) {
            lines.add("");
            lines.add("Source status");
            for (String message : result.sourceStatusMessages()) {
                lines.add("  - " + formatText(message));
            }
        }

        lines.add("");
        lines.add("Object execution summary");
        if (result.objectExecutionCounts().isEmpty()) {
            lines.add("(none)");
        } else {
            lines.add("  Type             Total   OK FAIL");
            for (AnalyzerProgressObjectCount objectCount : result.objectExecutionCounts()) {
                lines.add("  " + formatObjectSummary(objectCount));
            }
        }

        lines.add("");
        lines.add("Report : " + formatText(result.savedReportPath()));
        lines.add("HTML   : " + formatText(result.savedHtmlReportPath()));
        lines.add("See the report file for detailed execution logs.");
        return lines;
    }

    private String formatText(String value) {
        return value == null ? "" : value;
    }

    private String formatObjectSummary(AnalyzerProgressObjectCount objectCount) {
        return String.format(
                Locale.US,
                "%-15s %5d %4d %4d",
                fitText(objectCount.objectType(), 15),
                Math.max(0, objectCount.totalCount()),
                Math.max(0, objectCount.succeededCount()),
                Math.max(0, objectCount.failedCount()));
    }

    private String fitText(String value, int maxLength) {
        String text = value == null ? "" : value;
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 1) + ".";
    }
}
