/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.tui.page;

import java.util.List;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.TextBox;

public final class AnalyzerTuiLayout {
    public static final TerminalSize MINIMUM_TERMINAL_SIZE = new TerminalSize(70, 20);
    private static final TerminalSize FALLBACK_TERMINAL_SIZE = new TerminalSize(100, 30);
    private static final int HORIZONTAL_MARGIN = 6;

    private AnalyzerTuiLayout() {
    }

    static int contentWidth(TerminalSize terminalSize) {
        int columns = safeSize(terminalSize).getColumns();
        return Math.max(24, columns - HORIZONTAL_MARGIN);
    }

    public static boolean isTooSmall(TerminalSize terminalSize) {
        TerminalSize size = safeSize(terminalSize);
        return size.getColumns() < MINIMUM_TERMINAL_SIZE.getColumns()
                || size.getRows() < MINIMUM_TERMINAL_SIZE.getRows();
    }

    static int visibleRows(TerminalSize terminalSize, int reservedRows, int defaultRows) {
        if (terminalSize == null) {
            return Math.max(3, defaultRows);
        }
        int rows = safeSize(terminalSize).getRows();
        return Math.max(3, rows - reservedRows);
    }

    static int cappedVisibleRows(
            TerminalSize terminalSize,
            int reservedRows,
            int defaultRows,
            int maxRows) {
        int visibleRows = visibleRows(terminalSize, reservedRows, defaultRows);
        return Math.max(1, Math.min(Math.max(1, maxRows), visibleRows));
    }

    static TextBox readOnlyTextBox(
            List<String> lines,
            TerminalSize terminalSize,
            int reservedRows) {
        TextBox textBox = new TextBox(
                new TerminalSize(
                        contentWidth(terminalSize),
                        visibleRows(terminalSize, reservedRows, 10)),
                String.join("\n", lines),
                TextBox.Style.MULTI_LINE);
        textBox.setReadOnly(true);
        textBox.setLayoutData(
                LinearLayout.createLayoutData(
                        LinearLayout.Alignment.Fill,
                        LinearLayout.GrowPolicy.CanGrow));
        return textBox;
    }

    private static TerminalSize safeSize(TerminalSize terminalSize) {
        return terminalSize == null ? FALLBACK_TERMINAL_SIZE : terminalSize;
    }
}
