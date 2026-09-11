/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.tui.page;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiProgressPage.ProgressView;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressCounts;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressObjectCount;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatement;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;

class AnalyzerTuiProgressPageTest {
    @Test
    @DisplayName("progress TUI page renders live running state")
    void shouldRenderRunningState() {
        Panel panel = new AnalyzerTuiProgressPage().build();
        String screenText = String.join(System.lineSeparator(), collectLabelTexts(panel));

        assertTrue(screenText.contains("[3/4] Analysis progress"));
        assertTrue(screenText.contains("Progress : 0 / 0"));
        assertTrue(screenText.contains("OK       : 0"));
        assertTrue(screenText.contains("FAIL     : 0"));
        assertTrue(screenText.contains("Object summary"));
        assertTrue(screenText.contains("Analysis is running..."));
    }

    @Test
    @DisplayName("progress TUI page hides recent events")
    void shouldHideRecentEvents() {
        ProgressView progressView = new AnalyzerTuiProgressPage().buildView();

        for (int i = 1; i <= 12; i++) {
            progressView.update(
                    AnalyzerProgressEventViewModel.statementSucceeded(
                            new AnalyzerStatement("SELECT", "q" + i, "select " + i),
                            "parsed",
                            new AnalyzerProgressCounts(12, i, i, 0)));
        }

        String screenText = String.join(
                System.lineSeparator(), collectLabelTexts(progressView.getPanel()));

        assertTrue(screenText.contains("Progress : 12 / 12"));
        assertTrue(screenText.contains("OK       : 12"));
        assertTrue(screenText.contains("FAIL     : 0"));
        assertTrue(screenText.contains("Current  : [OK] SELECT q12"));
        assertFalse(screenText.contains("Recent"));
    }

    @Test
    @DisplayName("progress TUI page renders object summary without a scrollable text box")
    void shouldRenderObjectSummaryWithoutScrollableTextBox() {
        ProgressView progressView = new AnalyzerTuiProgressPage().buildView(new TerminalSize(70, 22));
        String screenText = String.join(
                System.lineSeparator(), collectLabelTexts(progressView.getPanel()));

        assertTrue(screenText.contains("(none)"));
        assertTrue(collectTextBoxes(progressView.getPanel()).isEmpty());
    }

    @Test
    @DisplayName("progress TUI page renders object summary counts")
    void shouldRenderObjectSummaryCounts() {
        ProgressView progressView = new AnalyzerTuiProgressPage().buildView();

        progressView.update(
                AnalyzerProgressEventViewModel.statementFailed(
                        new AnalyzerStatement("DDL_TABLE", "TABLE_2", "create table t2(c int)"),
                        "syntax error",
                        null,
                        new AnalyzerProgressCounts(
                                4,
                                2,
                                1,
                                1,
                                List.of(
                                        new AnalyzerProgressObjectCount("TABLE", 2, 1, 1),
                                        new AnalyzerProgressObjectCount("VIEW_CREATE", 2, 0, 0)))));

        String screenText = String.join(
                System.lineSeparator(), collectLabelTexts(progressView.getPanel()));

        assertTrue(screenText.contains("Object summary"));
        assertTrue(screenText.contains("TABLE"));
        assertTrue(screenText.contains("VIEW_CREATE"));
        assertTrue(screenText.contains("    2    1    1"));
    }

    @Test
    @DisplayName("progress TUI page expands every object summary row")
    void shouldExpandEveryObjectSummaryRow() {
        ProgressView progressView = new AnalyzerTuiProgressPage().buildView();
        List<AnalyzerProgressObjectCount> objectCounts = new ArrayList<AnalyzerProgressObjectCount>();
        for (int i = 1; i <= 10; i++) {
            objectCounts.add(new AnalyzerProgressObjectCount("TYPE_" + i, 1, 0, 0));
        }

        progressView.update(
                AnalyzerProgressEventViewModel.statementSucceeded(
                        new AnalyzerStatement("TYPE_10", "ID_10", "select 1"),
                        "parsed",
                        new AnalyzerProgressCounts(10, 1, 1, 0, objectCounts)));

        String objectSummaryText = String.join(
                System.lineSeparator(), collectLabelTexts(progressView.getPanel()));

        assertTrue(objectSummaryText.contains("TYPE_1"));
        assertTrue(objectSummaryText.contains("TYPE_10"));
        assertFalse(objectSummaryText.contains("more"));
        assertTrue(collectTextBoxes(progressView.getPanel()).isEmpty());
    }

    @Test
    @DisplayName("progress TUI page renders completion state")
    void shouldRenderCompletionState() {
        ProgressView progressView = new AnalyzerTuiProgressPage().buildView();

        progressView.markCompleted();

        String screenText = String.join(
                System.lineSeparator(), collectLabelTexts(progressView.getPanel()));

        assertTrue(screenText.contains("Analysis completed. Open the result."));
    }

    private List<String> collectLabelTexts(Panel panel) {
        List<String> texts = new ArrayList<String>();
        for (Component component : panel.getChildren()) {
            if (component instanceof Label) {
                texts.add(((Label) component).getText());
            } else if (component instanceof Panel) {
                texts.addAll(collectLabelTexts((Panel) component));
            }
        }
        return texts;
    }

    private List<TextBox> collectTextBoxes(Panel panel) {
        List<TextBox> textBoxes = new ArrayList<TextBox>();
        for (Component component : panel.getChildren()) {
            if (component instanceof Panel) {
                textBoxes.addAll(collectTextBoxes((Panel) component));
            } else if (component instanceof TextBox) {
                textBoxes.add((TextBox) component);
            }
        }
        return textBoxes;
    }
}
