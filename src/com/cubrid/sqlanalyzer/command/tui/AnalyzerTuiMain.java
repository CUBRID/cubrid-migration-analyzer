/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.tui;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cubrid.sqlanalyzer.command.config.AnalyzerArgumentsController;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.config.AnalyzerSettingsLoader;
import com.cubrid.sqlanalyzer.command.connection.AnalyzerJdbcConnectionSupport;
import com.cubrid.sqlanalyzer.command.config.AnalyzerLogInitializer;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;

public class AnalyzerTuiMain {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerTuiMain.class);

    public static void main(String[] args) throws IOException {
        AnalyzerLogInitializer.initLog(AnalyzerSettingsLoader.loadLogDirectory(args));
        LOG.info("SQL Analyzer TUI command started. argsCount={}", args == null ? 0 : args.length);

        AnalyzerArgumentsController arguments;
        try {
            arguments = AnalyzerArgumentsController.parse(
                    AnalyzerSettingsLoader.loadStartupArguments(args));
        } catch (IllegalArgumentException ex) {
            LOG.error("Failed to parse TUI startup arguments.", ex);
            System.err.println(ex.getMessage());
            System.exit(1);
            return;
        }

        if (arguments.isInteractive()) {
            LOG.error("TUI mode was requested without analyzer options.");
            System.err.println("TUI mode requires analyzer options or -conf <settingsFile>.");
            System.exit(1);
            return;
        }

        if (arguments.getJdbcRepositoryDir() != null
                && !arguments.getJdbcRepositoryDir().isEmpty()) {
            LOG.info("Configuring JDBC repository. path={}", arguments.getJdbcRepositoryDir());
            AnalyzerJdbcConnectionSupport.configureJdbcRepository(arguments.getJdbcRepositoryDir());
        }
        LOG.info("Initializing JDBC drivers.");
        AnalyzerJdbcConnectionSupport.initializeJdbcDrivers();

        AnalyzerService analyzerService = new AnalyzerService();
        AnalyzerSession session = new AnalyzerSession();
        try {
            analyzerService.applyArguments(session, arguments);
            analyzerService.prepareConfiguration(session);
            if (session.getTargetType() == AnalyzerTargetType.JDBC) {
                analyzerService.validateJdbcTargetConnection(session);
            }
            new AnalyzerTuiRunner().start(session, analyzerService);
            LOG.info("SQL Analyzer TUI command finished successfully.");
        } catch (IOException | RuntimeException ex) {
            LOG.error("SQL Analyzer TUI command failed.", ex);
            System.err.println("Analyzer failed: " + ex.getMessage());
            System.exit(1);
        }
    }
}
