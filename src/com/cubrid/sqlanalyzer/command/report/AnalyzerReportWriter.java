/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;

class AnalyzerReportWriter {

    private final File reportDir;

    AnalyzerReportWriter() {
        this(new File(System.getProperty("user.dir"), "report"));
    }

    AnalyzerReportWriter(File reportDir) {
        this.reportDir = reportDir;
    }

    String save(AnalyzerReport report) throws IOException {
        if (!reportDir.exists() && !reportDir.mkdirs()) {
            throw new IOException("Failed to create report directory: " + reportDir);
        }

        long generatedAt = System.currentTimeMillis();
        String timestamp = buildTimestamp(generatedAt);

        File reportFile = new File(reportDir, timestamp + ".txt");
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8"))) {
            writer.print(new AnalyzerTextReportBuilder(report).build());
            writer.flush();
        }

        File htmlReportFile = new File(reportDir, timestamp + ".html");
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(htmlReportFile), "UTF-8"))) {
            writer.print(new AnalyzerHtmlReportBuilder(report, generatedAt).build());
            writer.flush();
        }

        return reportFile.getAbsolutePath();
    }

    private String buildTimestamp(long generatedAt) {
        return "analyzer_result_"
                + CUBRIDTimeUtil.getDateFormat(
                        "yyyy_MM_dd_HH_mm_ss_SSS", Locale.US, TimeZone.getDefault())
                        .format(new Date(generatedAt));
    }
}
