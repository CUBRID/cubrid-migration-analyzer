/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cubrid.sqlanalyzer.command.config.AnalyzerArgumentsController;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;

class AnalyzerConsoleRunnerScenarioTest {
    @TempDir
    Path xmlDirectory;

    @TempDir
    Path workingDirectory;

    private String originalUserDir;

    @BeforeEach
    void redirectReportOutputUnderTempDir() {
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", workingDirectory.toString());
    }

    @AfterEach
    void restoreUserDir() {
        System.setProperty("user.dir", originalUserDir);
    }

    @Test
    void shouldCompleteNonInteractiveFlowForXmlSource() throws Exception {
        Files.writeString(
                xmlDirectory.resolve("mapper.xml"),
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <mapper namespace="sample">
                    <select id="findAll">
                        SELECT * FROM sample_table
                    </select>
                </mapper>
                """);

        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(
                new String[] { "-sx", "-xd", xmlDirectory.toString(), "-tp" });
        RecordingConsoleIO io = new RecordingConsoleIO();
        AnalyzerConsoleRunner runner = new AnalyzerConsoleRunner(io, new AnalyzerService());

        int exitCode = runner.startAnalyzer(arguments);

        assertEquals(0, exitCode);
        assertTrue(io.containsLine("XML query dictionary loaded."));
        assertTrue(io.containsLinePrefixedWith("Total  : 1"));
        assertTrue(io.containsLinePrefixedWith("OK     : 1"));
        assertTrue(io.containsLinePrefixedWith("FAIL   : 0"));
        assertTrue(io.anyLineContains("Saved result report: "));
    }

    @Test
    void shouldReturnFailureExitCodeWhenServiceThrows() {
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(
                new String[] { "-sx", "-xd", xmlDirectory.toString(), "-tp" });
        RecordingConsoleIO io = new RecordingConsoleIO();
        AnalyzerService failingService = new AnalyzerService() {
            @Override
            public void loadSourceCatalog(AnalyzerSession session) {
                throw new RuntimeException("boom");
            }
        };
        AnalyzerConsoleRunner runner = new AnalyzerConsoleRunner(io, failingService);

        int exitCode = runner.startAnalyzer(arguments);

        assertEquals(1, exitCode);
        assertTrue(io.anyLineContains("Analyzer failed: boom"));
    }

    private static final class RecordingConsoleIO implements ConsoleIO {
        private final List<String> lines = new ArrayList<>();

        @Override
        public void print(String text) {
            lines.add(text);
        }

        @Override
        public void println(String text) {
            lines.add(text);
        }

        @Override
        public String readLine() {
            return "";
        }

        @Override
        public String readRequired(String prompt) {
            throw new UnsupportedOperationException("Non-interactive flow must not prompt.");
        }

        @Override
        public boolean confirm(String prompt) {
            throw new UnsupportedOperationException("Non-interactive flow must not prompt.");
        }

        boolean containsLine(String expected) {
            return lines.stream().anyMatch(expected::equals);
        }

        boolean containsLinePrefixedWith(String prefix) {
            return lines.stream().anyMatch(line -> line.startsWith(prefix));
        }

        boolean anyLineContains(String fragment) {
            return lines.stream().anyMatch(line -> line.contains(fragment));
        }
    }
}
