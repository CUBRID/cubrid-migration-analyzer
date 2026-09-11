/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.tui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

class AnalyzerTuiRunnerTest {
    @Test
    void shouldIdentifyWhenNoAnalyzerSourceCouldBeLoaded() {
        AnalyzerTuiRunner runner = new AnalyzerTuiRunner();

        assertTrue(runner.isNoAnalyzerSourceLoaded(
                new IllegalStateException(AnalyzerService.NO_ANALYZER_SOURCE_LOADED_MESSAGE)));
        assertFalse(runner.isNoAnalyzerSourceLoaded(new IllegalStateException("other error")));
    }

    @Test
    void shouldRenderNoSourceLoadedContentWithSourceStatus() {
        AnalyzerSession session = new AnalyzerSession();
        session.addSourceStatusMessage("Oracle source skipped: Connection refused");
        session.addSourceStatusMessage("XML source skipped: No XML files found in directory: /tmp/sqlmap");

        Panel content = new AnalyzerTuiRunner().buildNoSourceLoadedContent(
                session,
                new IllegalStateException(AnalyzerService.NO_ANALYZER_SOURCE_LOADED_MESSAGE));
        String screenText = String.join(System.lineSeparator(), collectLabelTexts(content));

        assertTrue(screenText.contains(AnalyzerService.NO_ANALYZER_SOURCE_LOADED_MESSAGE));
        assertTrue(screenText.contains("No Oracle metadata or XML files were loaded."));
        assertTrue(screenText.contains("Source status"));
        assertTrue(screenText.contains("Oracle source skipped: Connection refused"));
        assertTrue(screenText.contains(
                "XML source skipped: No XML files found in directory: /tmp/sqlmap"));
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
}
