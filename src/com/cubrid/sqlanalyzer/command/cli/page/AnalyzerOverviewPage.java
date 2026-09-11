/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.cli.page;

import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.cli.ConsoleIO;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;

public class AnalyzerOverviewPage {
    private final ConsoleIO io;

    public AnalyzerOverviewPage(ConsoleIO io) {
        this.io = io;
    }

    public void render(AnalyzerOverviewViewModel overview) {
        io.println("");
        io.println("[1/3] Overview");
        io.println("Program     : " + formatText(overview.programVersion()));
        renderSources(overview);
        renderTarget(overview.source(), overview.target());
        io.println("Mode        : " + overview.executionMode());
        renderSourceStatus(overview);
    }

    private void renderSources(AnalyzerOverviewViewModel overview) {
        if (overview.sources().isEmpty()) {
            io.println("Source      : (none)");
            return;
        }

        for (AnalyzerSourceOverviewViewModel source : overview.sources()) {
            renderSource(source);
        }
    }

    private void renderSource(AnalyzerSourceOverviewViewModel source) {
        io.println("Source      : " + source.type());
        if (source.type() == AnalyzerSourceType.ORACLE) {
            io.println("Oracle URL  : " + formatText(source.jdbcUrl()) + formatVersionSuffix(source.version()));
            io.println("Oracle Host : " + formatHost(source.host(), source.port()));
            io.println("Oracle DB   : " + formatText(source.databaseName()));
            io.println("Oracle User : " + formatText(source.user()));
        } else {
            io.println("XML dir     : " + formatText(source.xmlDirectory()));
            io.println("XML charset : " + formatText(source.xmlCharset()));
            io.println("XML files   : " + source.xmlFileCount());
        }
    }

    private void renderSourceStatus(AnalyzerOverviewViewModel overview) {
        if (overview.sourceStatusMessages().isEmpty()) {
            return;
        }

        io.println("Source status");
        for (String message : overview.sourceStatusMessages()) {
            io.println("  - " + formatText(message));
        }
    }

    private void renderTarget(AnalyzerSourceOverviewViewModel source, AnalyzerTargetOverviewViewModel target) {
        io.println("Target      : " + target.type());
        if (target.type() == AnalyzerTargetType.JDBC) {
            io.println("Target URL  : " + formatText(target.jdbcUrl()) + formatVersionSuffix(target.version()));
            io.println("Target Host : " + formatHost(target.host(), target.port()));
            io.println("Target DB   : " + formatText(target.databaseName()));
            io.println("Target User : " + formatText(target.user()));
        } else if (target.type() == AnalyzerTargetType.PARSER
                && source != null
                && source.xmlDirectory() != null
                && !source.xmlDirectory().isEmpty()) {
            io.println("Parser      : " + formatText(target.parserVersion()));
            io.println("XML dir     : " + formatText(source.xmlDirectory()));
            io.println("XML files   : " + source.xmlFileCount());
        } else if (target.type() == AnalyzerTargetType.PARSER) {
            io.println("Parser      : " + formatText(target.parserVersion()));
        }
    }

    private String formatVersionSuffix(String version) {
        return version == null || version.isEmpty() ? "" : " (" + version + ")";
    }

    private String formatHost(String host, int port) {
        if (host == null || host.isEmpty()) {
            return "";
        }

        return port > 0 ? host + ":" + port : host;
    }

    private String formatText(String value) {
        return value == null ? "" : value;
    }
}
