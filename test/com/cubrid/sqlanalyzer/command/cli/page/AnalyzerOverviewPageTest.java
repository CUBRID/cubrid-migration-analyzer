/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.cli.page;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.model.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.cli.ConsoleIO;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;

class AnalyzerOverviewPageTest {
    @Test
    @DisplayName("XML parser overview renders view model details")
    void shouldRenderXmlParserOverviewFromViewModel() {
        RecordingConsoleIO io = new RecordingConsoleIO();
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
                        "/tmp/sqlmap",
                        "UTF-8",
                        3),
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

        new AnalyzerOverviewPage(io).render(overview);
        String output = io.output();

        assertTrue(output.contains("[1/3] Overview"));
        assertTrue(output.contains("Program     : 0.0.1-SNAPSHOT"));
        assertTrue(output.contains("Source      : XML"));
        assertTrue(output.contains("XML dir     : /tmp/sqlmap"));
        assertTrue(output.contains("XML charset : UTF-8"));
        assertTrue(output.contains("XML files   : 3"));
        assertTrue(output.contains("Target      : PARSER"));
        assertTrue(output.contains("Parser      : CUBRID parser"));
        assertEquals(2, countOccurrences(output, "XML files   : 3"));
        assertTrue(output.contains("Mode        : DML"));
    }

    @Test
    @DisplayName("JDBC overview renders connection details from view model")
    void shouldRenderJdbcOverviewConnectionDetailsFromViewModel() {
        RecordingConsoleIO io = new RecordingConsoleIO();
        AnalyzerOverviewViewModel overview = new AnalyzerOverviewViewModel(
                "1.2.3",
                new AnalyzerSourceOverviewViewModel(
                        AnalyzerSourceType.ORACLE,
                        "jdbc:oracle:thin:@//oracle.example.com:1521/XEPDB1",
                        "oracle.example.com",
                        1521,
                        "XEPDB1",
                        "oracle_user",
                        "21c",
                        null,
                        null,
                        0),
                new AnalyzerTargetOverviewViewModel(
                        AnalyzerTargetType.JDBC,
                        "jdbc:cubrid:cubrid.example.com:33000:demodb:::",
                        "cubrid.example.com",
                        33000,
                        "demodb",
                        "dba",
                        "11.3",
                        null),
                AnalyzerExecutionMode.DDL);

        new AnalyzerOverviewPage(io).render(overview);
        String output = io.output();

        assertTrue(output.contains("Program     : 1.2.3"));
        assertTrue(output.contains("Oracle URL  : jdbc:oracle:thin:@//oracle.example.com:1521/XEPDB1 (21c)"));
        assertTrue(output.contains("Oracle Host : oracle.example.com:1521"));
        assertTrue(output.contains("Oracle DB   : XEPDB1"));
        assertTrue(output.contains("Oracle User : oracle_user"));
        assertTrue(output.contains("Target URL  : jdbc:cubrid:cubrid.example.com:33000:demodb::: (11.3)"));
        assertTrue(output.contains("Target Host : cubrid.example.com:33000"));
        assertTrue(output.contains("Target DB   : demodb"));
        assertTrue(output.contains("Target User : dba"));
        assertTrue(output.contains("Mode        : DDL"));
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

    private static class RecordingConsoleIO implements ConsoleIO {
        private final List<String> lines = new ArrayList<String>();

        public void print(String text) {
            lines.add(text);
        }

        public void println(String text) {
            lines.add(text);
        }

        public String readLine() {
            return "";
        }

        public String readRequired(String prompt) {
            return "";
        }

        public boolean confirm(String prompt) {
            return false;
        }

        String output() {
            return String.join(System.lineSeparator(), lines);
        }
    }
}
