/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.tui.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.googlecode.lanterna.TerminalSize;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTableSizeViewModel;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.table.Table;

public class AnalyzerTuiObjectCountPage {
    private static final int TABLE_SIZE_VISIBLE_ROWS = 8;
    private static final int TABLE_NAME_WIDTH = 44;
    private static final int TABLE_RESERVED_ROWS = 23;

    public Panel build(AnalyzerObjectCountPreviewViewModel preview) {
        return build(preview, null);
    }

    public Panel build(AnalyzerObjectCountPreviewViewModel preview, TerminalSize terminalSize) {
        if (terminalSize != null) {
            Panel panel = new Panel();
            panel.addComponent(AnalyzerTuiLayout.readOnlyTextBox(buildLines(preview), terminalSize, 5));
            return panel;
        }

        Panel panel = new Panel();
        for (String line : buildSummaryLines(preview)) {
            panel.addComponent(new Label(line));
        }
        if (preview.oracleSourceLoaded() && !preview.tableSizes().isEmpty()) {
            panel.addComponent(buildTableSizeTable(preview.tableSizes(), terminalSize));
        }
        for (String line : buildDmlLines(preview)) {
            panel.addComponent(new Label(line));
        }
        return panel;
    }

    List<String> buildLines(AnalyzerObjectCountPreviewViewModel preview) {
        List<String> lines = buildSummaryLines(preview);
        if (preview.oracleSourceLoaded() && !preview.tableSizes().isEmpty()) {
            lines.add("  No.  Table                                         Size       Est. rows");
            int rowNumber = 1;
            for (AnalyzerTableSizeViewModel tableSize : preview.tableSizes()) {
                lines.add(formatTableSizeRow(rowNumber++, tableSize));
            }
        }
        lines.addAll(buildDmlLines(preview));
        return lines;
    }

    private List<String> buildSummaryLines(AnalyzerObjectCountPreviewViewModel preview) {
        List<String> lines = new ArrayList<String>();
        lines.add("[2/4] Object count preview");
        lines.add("DDL objects");
        if (preview.oracleSourceLoaded()) {
            lines.add("Catalog schemas : " + preview.catalogSchemaCount());
            lines.add("Target tables   : " + preview.targetTableCount());
            lines.add("Target PKs      : " + preview.targetPkCount());
            lines.add("Target FKs      : " + preview.targetFkCount());
            lines.add("Target views    : " + preview.targetViewCount());
            lines.add("Target serials  : " + preview.targetSerialCount());
            lines.add("Target synonyms : " + preview.targetSynonymCount());
            lines.add("Target grants   : " + preview.targetGrantCount());
            lines.add("Target procs    : " + preview.targetProcedureCount());
            lines.add("Target funcs    : " + preview.targetFunctionCount());
            lines.add("Target triggers : " + preview.targetTriggerCount());
            lines.add("");
            lines.add("Oracle table size total : " + formatBytes(preview.totalTableBytes()));
            lines.add("Oracle table sizes");
            if (preview.tableSizes().isEmpty()) {
                lines.add("  (none)");
            }
        } else {
            appendSourceSkippedLines(lines, preview, "Oracle");
        }

        return lines;
    }

    private List<String> buildDmlLines(AnalyzerObjectCountPreviewViewModel preview) {
        List<String> lines = new ArrayList<String>();
        lines.add("DML statements");
        if (preview.xmlSourceLoaded()) {
            lines.add("SELECT count    : " + preview.selectCount());
            lines.add("INSERT count    : " + preview.insertCount());
            lines.add("UPDATE count    : " + preview.updateCount());
            lines.add("DELETE count    : " + preview.deleteCount());
        } else {
            appendSourceSkippedLines(lines, preview, "XML");
        }
        return lines;
    }

    private void appendSourceSkippedLines(
            List<String> lines,
            AnalyzerObjectCountPreviewViewModel preview,
            String sourceName) {
        boolean appended = false;
        String prefix = sourceName + " source skipped:";
        for (String message : preview.sourceStatusMessages()) {
            if (message != null && message.startsWith(prefix)) {
                lines.add("  " + message);
                appended = true;
            }
        }
        if (!appended) {
            lines.add("  " + sourceName + " source not loaded.");
        }
    }

    private Table<String> buildTableSizeTable(
            List<AnalyzerTableSizeViewModel> tableSizes,
            TerminalSize terminalSize) {
        int tableWidth = Math.min(74, AnalyzerTuiLayout.contentWidth(terminalSize));
        int tableNameWidth = Math.max(8, Math.min(TABLE_NAME_WIDTH, tableWidth - 34));
        int visibleRows = AnalyzerTuiLayout.cappedVisibleRows(
                terminalSize,
                TABLE_RESERVED_ROWS,
                TABLE_SIZE_VISIBLE_ROWS,
                Math.min(TABLE_SIZE_VISIBLE_ROWS, tableSizes.size()));

        Table<String> table = new Table<String>("No.", "Table", "Size", "Est. rows");
        int rowNumber = 1;
        for (AnalyzerTableSizeViewModel tableSize : tableSizes) {
            table.getTableModel().addRow(
                    String.valueOf(rowNumber++),
                    fitText(tableSize.tableName(), tableNameWidth),
                    formatBytes(tableSize.bytes()),
                    formatNumber(tableSize.estimatedRows()));
        }
        table.setVisibleRows(visibleRows);
        table.setVisibleColumns(4);
        table.setPreferredSize(
                new TerminalSize(
                        tableWidth,
                        visibleRows + 2));
        return table;
    }

    private String formatTableSizeRow(int rowNumber, AnalyzerTableSizeViewModel tableSize) {
        return String.format(
                Locale.US,
                "  %3d  %-44s %10s %15s",
                rowNumber,
                fitText(tableSize.tableName(), TABLE_NAME_WIDTH),
                formatBytes(tableSize.bytes()),
                formatNumber(tableSize.estimatedRows()));
    }

    private String formatBytes(long bytes) {
        long safeBytes = Math.max(0L, bytes);
        if (safeBytes < 1024L) {
            return safeBytes + " B";
        }

        double value = safeBytes;
        String[] units = { "B", "KB", "MB", "GB", "TB", "PB" };
        int unitIndex = 0;
        while (value >= 1024.0 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        return String.format(Locale.US, "%.2f %s", value, units[unitIndex]);
    }

    private String fitText(String value, int maxLength) {
        String text = value == null ? "" : value;
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 1) + ".";
    }

    private String formatNumber(long number) {
        return String.format(Locale.US, "%,d", Math.max(0L, number));
    }
}
