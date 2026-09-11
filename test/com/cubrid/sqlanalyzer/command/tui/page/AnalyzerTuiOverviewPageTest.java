/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.tui.page;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cubrid.sqlanalyzer.command.model.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;

class AnalyzerTuiOverviewPageTest {
    @TempDir
    Path xmlDirectory;

    @Test
    @DisplayName("overview TUI page renders analyzer overview view model")
    void shouldRenderOverviewViewModelAsLanternaPage() {
        AnalyzerOverviewViewModel overview = new AnalyzerOverviewViewModel(
                "0.0.1-SNAPSHOT",
                new AnalyzerSourceOverviewViewModel(
                        AnalyzerSourceType.XML,
                        null,
                        null,
                        0,
                        null,
                        null,
                        null,
                        xmlDirectory.toString(),
                        "UTF-8",
                        1),
                new AnalyzerTargetOverviewViewModel(
                        AnalyzerTargetType.PARSER,
                        null,
                        null,
                        0,
                        null,
                        null,
                        null,
                        "CUBRID parser"),
                AnalyzerExecutionMode.DML);
        Panel panel = new AnalyzerTuiOverviewPage().build(overview);
        String screenText = String.join(System.lineSeparator(), collectLabelTexts(panel));

        assertTrue(screenText.contains("CUBRID SQL Analyzer"));
        assertTrue(screenText.contains("[1/4] Overview"));
        assertTrue(screenText.contains("Source      : XML"));
        assertTrue(screenText.contains("XML dir     : " + xmlDirectory));
        assertTrue(screenText.contains("XML charset : UTF-8"));
        assertTrue(screenText.contains("XML files   : 1"));
        assertTrue(screenText.contains("Target      : PARSER"));
        assertTrue(screenText.contains("Parser      : CUBRID parser"));
        assertEquals(2, countOccurrences(screenText, "XML files   : 1"));
        assertTrue(screenText.contains("Mode        : DML"));
    }

    @Test
    @DisplayName("overview TUI page renders scrollable text body for constrained terminal size")
    void shouldRenderScrollableOverviewBodyForConstrainedTerminalSize() {
        AnalyzerOverviewViewModel overview = new AnalyzerOverviewViewModel(
                "0.0.1-SNAPSHOT",
                new AnalyzerSourceOverviewViewModel(
                        AnalyzerSourceType.XML,
                        null,
                        null,
                        0,
                        null,
                        null,
                        null,
                        xmlDirectory.toString(),
                        "UTF-8",
                        1),
                new AnalyzerTargetOverviewViewModel(
                        AnalyzerTargetType.PARSER,
                        null,
                        null,
                        0,
                        null,
                        null,
                        null,
                        "CUBRID parser"),
                AnalyzerExecutionMode.DML);

        Panel panel = new AnalyzerTuiOverviewPage().build(overview, new TerminalSize(80, 12));
        TextBox body = collectTextBoxes(panel).get(0);

        assertTrue(body.isReadOnly());
        assertTrue(body.getText().contains("[1/4] Overview"));
        assertEquals(74, body.getPreferredSize().getColumns());
        assertEquals(7, body.getPreferredSize().getRows());
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

    private List<String> collectLabelTexts(Panel panel) {
        List<String> texts = new ArrayList<String>();
        for (Component component : panel.getChildren()) {
            if (component instanceof Label) {
                texts.add(((Label) component).getText());
            }
        }
        return texts;
    }

    private List<TextBox> collectTextBoxes(Panel panel) {
        List<TextBox> textBoxes = new ArrayList<TextBox>();
        for (Component component : panel.getChildren()) {
            if (component instanceof TextBox) {
                textBoxes.add((TextBox) component);
            }
        }
        return textBoxes;
    }
}
