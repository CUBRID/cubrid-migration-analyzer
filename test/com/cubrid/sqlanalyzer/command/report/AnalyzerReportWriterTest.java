/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;

class AnalyzerReportWriterTest {
    @TempDir
    Path reportDir;

    @Test
    void shouldWriteTextAndHtmlReportsUnderInjectedDirectory() throws Exception {
        AnalyzerReport report = new AnalyzerReport();
        report.setSourceType(AnalyzerSourceType.XML);
        report.setTargetType(AnalyzerTargetType.PARSER);

        String savedPath = new AnalyzerReportWriter(reportDir.toFile()).save(report);

        File savedTextFile = new File(savedPath);
        assertTrue(savedTextFile.exists());
        assertTrue(savedTextFile.getParentFile().equals(reportDir.toFile()));

        File savedHtmlFile = new File(savedPath.substring(0, savedPath.length() - ".txt".length()) + ".html");
        assertTrue(savedHtmlFile.exists());

        assertTrue(Files.readString(savedTextFile.toPath()).contains("Overview"));
        assertTrue(Files.readString(savedHtmlFile.toPath()).contains("<!DOCTYPE html>"));
    }

    @Test
    void shouldFailWhenReportDirectoryCannotBeCreated() throws Exception {
        File unwritableParent = new File(reportDir.toFile(), "blocked-file");
        Files.writeString(unwritableParent.toPath(), "not a directory");
        File reportDirUnderFile = new File(unwritableParent, "report");

        org.junit.jupiter.api.Assertions.assertThrows(
                java.io.IOException.class,
                () -> new AnalyzerReportWriter(reportDirUnderFile).save(new AnalyzerReport()));
    }
}
