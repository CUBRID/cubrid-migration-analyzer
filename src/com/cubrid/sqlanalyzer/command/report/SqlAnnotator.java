/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.report;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SqlAnnotator {

    private static final Pattern ERROR_LOCATION_PATTERN = Pattern.compile(
            "In line\\s+(\\d+),\\s*column\\s+(\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UNEXPECTED_TOKEN_PATTERN = Pattern.compile(
            "unexpected\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BEFORE_TOKEN_PATTERN = Pattern.compile(
            "before\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE);

    private SqlAnnotator() {
    }

    static final class SqlContextLocation {
        final int lineNumber;
        final int columnNumber;
        final boolean estimated;

        SqlContextLocation(int lineNumber, int columnNumber, boolean estimated) {
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.estimated = estimated;
        }
    }

    static void appendAnnotatedSqlLines(
            StringBuilder sb,
            String sql,
            SqlContextLocation location,
            String lineSeparator,
            UnaryOperator<String> transform,
            String linePrefix) {
        String[] lines = splitSqlLines(sql);
        SqlContextLocation safeLocation = validSqlContextLocation(location, lines.length);
        int lineNumberWidth = String.valueOf(lines.length).length();
        for (int lineNumber = 1; lineNumber <= lines.length; lineNumber++) {
            String sqlLine = lines[lineNumber - 1];
            sb.append(transform.apply(linePrefix));
            sb.append(transform.apply(AnalyzerReportFormatter.formatLineNumber(lineNumber, lineNumberWidth)));
            sb.append(" | ");
            sb.append(transform.apply(sqlLine));
            sb.append(lineSeparator);
            if (safeLocation != null && lineNumber == safeLocation.lineNumber) {
                sb.append(transform.apply(linePrefix));
                sb.append(" ".repeat(lineNumberWidth))
                        .append(" | ")
                        .append(" ".repeat(AnalyzerReportFormatter.caretOffset(sqlLine, safeLocation.columnNumber)))
                        .append("^");
                if (safeLocation.estimated) {
                    sb.append(" estimated");
                }
                sb.append(lineSeparator);
            }
        }
    }

    static SqlContextLocation findSqlContextLocation(String reason, String sql) {
        if (sql == null || sql.isEmpty()) {
            return null;
        }
        String[] lines = splitSqlLines(sql);
        Matcher locationMatcher = ERROR_LOCATION_PATTERN.matcher(
                AnalyzerReportFormatter.nullToEmpty(reason));
        if (locationMatcher.find()) {
            int lineNumber = parsePositiveInt(locationMatcher.group(1));
            int columnNumber = parsePositiveInt(locationMatcher.group(2));
            if (lineNumber >= 1 && lineNumber <= lines.length) {
                return new SqlContextLocation(lineNumber, Math.max(1, columnNumber), false);
            }
        }
        return findTokenLocation(reason, lines);
    }

    static SqlContextLocation validSqlContextLocation(SqlContextLocation location, int lineCount) {
        if (location == null
                || location.lineNumber < 1
                || location.lineNumber > lineCount) {
            return null;
        }
        return location;
    }

    static String[] splitSqlLines(String sql) {
        return AnalyzerReportFormatter.nullToEmpty(sql).split("\\R", -1);
    }

    private static SqlContextLocation findTokenLocation(String reason, String[] lines) {
        List<String> candidates = new ArrayList<String>();
        addTokenCandidates(candidates, UNEXPECTED_TOKEN_PATTERN, reason);
        addTokenCandidates(candidates, BEFORE_TOKEN_PATTERN, reason);

        for (String candidate : candidates) {
            if (candidate.isEmpty()) {
                continue;
            }
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                int columnIndex = lines[lineIndex].indexOf(candidate);
                if (columnIndex >= 0) {
                    return new SqlContextLocation(lineIndex + 1, columnIndex + 1, true);
                }
            }
        }
        return null;
    }

    private static void addTokenCandidates(
            List<String> candidates,
            Pattern pattern,
            String reason) {
        Matcher matcher = pattern.matcher(AnalyzerReportFormatter.nullToEmpty(reason));
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate != null && !candidate.isEmpty()) {
                candidates.add(candidate);
            }
        }
    }

    private static int parsePositiveInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
