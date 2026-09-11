/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.tui.page;

import java.util.List;
import java.util.Locale;

import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressObjectCount;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.ProgressBar;

public class AnalyzerTuiProgressPage {
    public Panel build() {
        return buildView(null).getPanel();
    }

    public ProgressView buildView() {
        return buildView(null);
    }

    public ProgressView buildView(TerminalSize terminalSize) {
        return new ProgressView(terminalSize);
    }

    public static class ProgressView {
        private final Panel panel;
        private final Label progressLabel;
        private final Label okLabel;
        private final Label failLabel;
        private final Label currentLabel;
        private final Label statusLabel;
        private final ProgressBar progressBar;
        private final Panel objectSummaryPanel;

        private ProgressView(TerminalSize terminalSize) {
            panel = new Panel();
            progressLabel = new Label("Progress : 0 / 0");
            okLabel = new Label("OK       : 0");
            failLabel = new Label("FAIL     : 0");
            currentLabel = new Label("Current  : Waiting for analysis to start.");
            statusLabel = new Label("Analysis is running...");
            progressBar = new ProgressBar(0, 100, 0);
            progressBar.setPreferredWidth(40);
            objectSummaryPanel = new Panel();
            objectSummaryPanel.addComponent(new Label("(none)"));

            panel.addComponent(new Label("[3/4] Analysis progress"));
            panel.addComponent(new Label(""));
            panel.addComponent(progressLabel);
            panel.addComponent(okLabel);
            panel.addComponent(failLabel);
            panel.addComponent(progressBar);
            panel.addComponent(new Label(""));
            panel.addComponent(currentLabel);
            panel.addComponent(new Label(""));
            panel.addComponent(new Label("Object summary"));
            panel.addComponent(new Label(
                    "  Type             Total   OK FAIL"));
            panel.addComponent(objectSummaryPanel);
            panel.addComponent(new Label(""));
            panel.addComponent(statusLabel);
        }

        public Panel getPanel() {
            return panel;
        }

        public void update(AnalyzerProgressEventViewModel event) {
            if (event == null) {
                return;
            }

            int total = Math.max(0, event.totalCount());
            int completed = Math.max(0, event.completedCount());
            int progressMax = Math.max(1, total);
            int progressValue = Math.min(completed, progressMax);

            progressLabel.setText("Progress : " + completed + " / " + total);
            okLabel.setText("OK       : " + Math.max(0, event.succeededCount()));
            failLabel.setText("FAIL     : " + Math.max(0, event.failedCount()));
            progressBar.setMax(progressMax);
            progressBar.setValue(progressValue);
            updateObjectSummary(event.counts().objectCounts());

            String message = formatMessage(event);
            if (!message.isEmpty()) {
                currentLabel.setText("Current  : " + message);
            }
        }

        private void updateObjectSummary(List<AnalyzerProgressObjectCount> objectCounts) {
            int objectCountSize = objectCounts == null ? 0 : objectCounts.size();
            objectSummaryPanel.removeAllComponents();
            if (objectCountSize == 0) {
                objectSummaryPanel.addComponent(new Label("(none)"));
                return;
            }
            for (int i = 0; i < objectCountSize; i++) {
                objectSummaryPanel.addComponent(
                        new Label("  " + formatObjectSummary(objectCounts.get(i))));
            }
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

        public void markCompleted() {
            statusLabel.setText("Analysis completed. Open the result.");
        }

        private String formatMessage(AnalyzerProgressEventViewModel event) {
            if (event.message() != null && !event.message().isEmpty()) {
                return event.message();
            }
            return String.valueOf(event.stage());
        }
    }
}
