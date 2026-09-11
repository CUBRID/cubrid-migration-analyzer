/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.report;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;

final class AnalyzerReportFormatter {

    private AnalyzerReportFormatter() {
    }

    static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    static String escapeHtml(String value) {
        String text = nullToEmpty(value);
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&': escaped.append("&amp;"); break;
                case '<': escaped.append("&lt;"); break;
                case '>': escaped.append("&gt;"); break;
                case '"': escaped.append("&quot;"); break;
                case '\'': escaped.append("&#39;"); break;
                default: escaped.append(ch); break;
            }
        }
        return escaped.toString();
    }

    static String formatVersionSuffix(String version) {
        return version == null || version.isEmpty() ? "" : " (" + version + ")";
    }

    static String formatHost(String host, int port) {
        if (host == null || host.isEmpty()) {
            return "";
        }
        return port > 0 ? host + ":" + port : host;
    }

    static String formatBytes(long bytes) {
        long safeBytes = Math.max(0L, bytes);
        if (safeBytes < 1024L) {
            return safeBytes + " B";
        }
        double value = safeBytes;
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int unitIndex = 0;
        while (value >= 1024.0 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        return String.format(Locale.US, "%.2f %s", value, units[unitIndex]);
    }

    static String formatNumber(long value) {
        return String.format(Locale.US, "%,d", Math.max(0L, value));
    }

    static String fitText(String value, int maxLength) {
        String text = value == null ? "" : value;
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 1) + ".";
    }

    static String formatLineNumber(int lineNumber, int width) {
        return String.format(Locale.US, "%" + width + "d", lineNumber);
    }

    static int caretOffset(String sqlLine, int columnNumber) {
        int safeColumnNumber = Math.max(1, columnNumber);
        int maxOffset = sqlLine == null ? 0 : sqlLine.length();
        return Math.min(safeColumnNumber - 1, maxOffset);
    }

    static String formatEstimatedCostWithTime(float estimatedCost) {
        return AnalyzerCostFormatter.formatCostWithTime(estimatedCost);
    }

    static String formatGeneratedAt(long generatedAt) {
        long timeValue = generatedAt > 0 ? generatedAt : System.currentTimeMillis();
        return CUBRIDTimeUtil.getDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.US, TimeZone.getDefault())
                .format(new Date(timeValue));
    }

    static String displayObjectType(String statementType) {
        String type = nullToEmpty(statementType);
        if (type.isEmpty()) {
            return "UNKNOWN";
        }
        if (AnalyzerStatementTypes.TYPE_DDL_SEQUENCE.equals(type)) {
            return "SERIAL";
        }
        if (type.startsWith("DDL_")) {
            return type.substring("DDL_".length());
        }
        return type;
    }

    static String displayStatementSummaryType(String statementType) {
        String type = nullToEmpty(statementType);
        if (type.isEmpty()) {
            return "UNKNOWN";
        }
        if (AnalyzerStatementTypes.TYPE_DDL_SEQUENCE.equals(type)) {
            return "SERIAL";
        }
        return type;
    }

    static String staticSqlParentId(String statementId) {
        String id = nullToEmpty(statementId);
        int staticMarkerIndex = id.indexOf("_STATIC_");
        if (staticMarkerIndex <= 0) {
            return null;
        }
        return id.substring(0, staticMarkerIndex);
    }

    static String staticSqlParentObjectType(String statementId) {
        String parentId = staticSqlParentId(statementId);
        if (parentId == null) {
            return null;
        }
        if (parentId.startsWith("PROC_")) {
            return "PROCEDURE";
        }
        if (parentId.startsWith("FUNC_")) {
            return "FUNCTION";
        }
        return "PLCSQL";
    }
}
