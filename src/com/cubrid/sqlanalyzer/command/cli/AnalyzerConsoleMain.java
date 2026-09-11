/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.cli;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cubrid.sqlanalyzer.command.config.AnalyzerArgumentsController;
import com.cubrid.sqlanalyzer.command.config.AnalyzerLogInitializer;
import com.cubrid.sqlanalyzer.command.config.AnalyzerSettingsLoader;
import com.cubrid.sqlanalyzer.command.connection.AnalyzerJdbcConnectionSupport;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.cubrid.sqlanalyzer.command.tui.AnalyzerTuiRunner;

public class AnalyzerConsoleMain {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerConsoleMain.class);

    public static void main(String[] args) {
        AnalyzerLogInitializer.initLog(AnalyzerSettingsLoader.loadLogDirectory(args));
        LOG.info("SQL Analyzer command started. argsCount={}", args == null ? 0 : args.length);

        AnalyzerArgumentsController arguments;
        try {
            arguments = AnalyzerArgumentsController.parse(
                    AnalyzerSettingsLoader.loadStartupArguments(args));
        } catch (IllegalArgumentException ex) {
            LOG.error("Failed to parse analyzer startup arguments.", ex);
            System.err.println(ex.getMessage());
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

        if (arguments.isTuiMode()) {
            LOG.info("Starting analyzer in TUI mode.");
            System.exit(startTuiAnalyzer(arguments));
            return;
        }

        LOG.info("Starting analyzer in console mode. interactive={}", arguments.isInteractive());
        ConsoleIO io = new AnalyzerConsoleIOController(System.in, System.out);
        AnalyzerConsoleRunner runner = new AnalyzerConsoleRunner(io);
        int exitCode = arguments.isInteractive()
                ? runner.startAnalyzer()
                : runner.startAnalyzer(arguments);
        LOG.info("SQL Analyzer command finished. exitCode={}", exitCode);
        System.exit(exitCode);
    }

    private static int startTuiAnalyzer(AnalyzerArgumentsController arguments) {
        if (arguments.isInteractive()) {
            System.err.println("TUI mode requires analyzer options or -conf <settingsFile>.");
            return 1;
        }

        AnalyzerService analyzerService = new AnalyzerService();
        AnalyzerSession session = new AnalyzerSession();
        try {
            analyzerService.applyArguments(session, arguments);
            analyzerService.prepareConfiguration(session);
            if (session.getTargetType() == AnalyzerTargetType.JDBC) {
                analyzerService.validateJdbcTargetConnection(session);
            }
            new AnalyzerTuiRunner().start(session, analyzerService);
            LOG.info("TUI analyzer finished successfully.");
            return 0;
        } catch (IOException | RuntimeException ex) {
            LOG.error("TUI analyzer failed.", ex);
            System.err.println("Analyzer failed: " + ex.getMessage());
            ex.printStackTrace();
            return 1;
        }
    }
}
