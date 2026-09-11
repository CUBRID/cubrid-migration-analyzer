/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cubrid.sqlanalyzer.command.cli.page.AnalyzerObjectCountPage;
import com.cubrid.sqlanalyzer.command.cli.page.AnalyzerOverviewPage;
import com.cubrid.sqlanalyzer.command.cli.page.AnalyzerResultPage;
import com.cubrid.sqlanalyzer.command.config.AnalyzerArgumentsController;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressStage;

public class AnalyzerConsoleRunner {
    private static final String DEFAULT_XML_CHARSET = "UTF-8";
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerConsoleRunner.class);

    private final ConsoleIO io;
    private final AnalyzerService analyzerService;
    private final AnalyzerOverviewPage overviewPage;
    private final AnalyzerObjectCountPage objectCountPage;
    private final AnalyzerResultPage resultPage;

    public AnalyzerConsoleRunner(ConsoleIO io) {
        this(io, new AnalyzerService());
    }

    AnalyzerConsoleRunner(ConsoleIO io, AnalyzerService analyzerService) {
        this.io = io;
        this.analyzerService = analyzerService;
        this.overviewPage = new AnalyzerOverviewPage(io);
        this.objectCountPage = new AnalyzerObjectCountPage(io);
        this.resultPage = new AnalyzerResultPage(io);
    }

    public int startAnalyzer() {
        LOG.info("Interactive console analyzer flow started.");
        io.println("======================================");
        io.println("CUBRID SQL Analyzer Console");
        io.println("======================================");

        AnalyzerSession session = new AnalyzerSession();

        try {
            configureSources(session);
            session.setTargetType(AnalyzerTargetType.PARSER);
            analyzerService.applyExecutionMode(session);
            analyzerService.prepareConfiguration(session);
            loadSourceCatalog(session);
            renderPreviewPages(session);

            if (!io.confirm("Continue analysis? (y/n): ")) {
                io.println("Analysis canceled.");
                return 0;
            }

            runAnalysis(session);
            printResult(session);
            LOG.info("Interactive console analyzer flow finished successfully.");
            return 0;
        } catch (RuntimeException ex) {
            LOG.error("Interactive console analyzer flow failed.", ex);
            io.println("Analyzer failed: " + ex.getMessage());
            ex.printStackTrace();
            return 1;
        }
    }

    public int startAnalyzer(AnalyzerArgumentsController arguments) {
        LOG.info("Non-interactive console analyzer flow started.");
        io.println("======================================");
        io.println("CUBRID SQL Analyzer Console");
        io.println("======================================");

        AnalyzerSession session = new AnalyzerSession();

        try {
            analyzerService.applyArguments(session, arguments);
            analyzerService.prepareConfiguration(session);
            validateJdbcTargetConnection(session);
            loadSourceCatalog(session);
            renderPreviewPages(session);
            runAnalysis(session);
            printResult(session);
            LOG.info("Non-interactive console analyzer flow finished successfully.");
            return 0;
        } catch (RuntimeException ex) {
            LOG.error("Non-interactive console analyzer flow failed.", ex);
            io.println("Analyzer failed: " + ex.getMessage());
            ex.printStackTrace();
            return 1;
        }
    }

    private void configureSources(AnalyzerSession session) {
        io.println("");
        io.println("[1/4] Configure sources");
        if (io.confirm("Use Oracle JDBC source for DDL? (y/n): ")) {
            session.setOracleSourceRequested(true);
            promptOracleSource(session);
        }

        if (io.confirm("Use XML mapper source for DML? (y/n): ")) {
            session.setXmlSourceRequested(true);
            promptXmlSource(session);
        }

        if (!session.isOracleSourceRequested() && !session.isXmlSourceRequested()) {
            throw new IllegalStateException("At least one source is required.");
        }
    }

    private void promptOracleSource(AnalyzerSession session) {
        session.setSourceJdbcUrl(io.readRequired("Oracle JDBC URL: "));
        session.setSourceUser(io.readRequired("Oracle user: "));
        session.setSourcePassword(io.readRequired("Oracle password: "));
    }

    private void promptXmlSource(AnalyzerSession session) {
        session.setXmlDirectory(io.readRequired("XML directory path: "));
        String charset = readLineWithDefault("XML charset [UTF-8]: ", DEFAULT_XML_CHARSET);
        session.setXmlCharset(charset.isEmpty() ? DEFAULT_XML_CHARSET : charset);
    }

    private void validateJdbcTargetConnection(AnalyzerSession session) {
        if (session.getTargetType() == AnalyzerTargetType.JDBC) {
            analyzerService.validateJdbcTargetConnection(session);
        }
    }

    private void loadSourceCatalog(AnalyzerSession session) {
        LOG.info("Loading source metadata. sourceType={}", session.getSourceType());
        io.println("");
        io.println("Loading source metadata...");
        analyzerService.loadSourceCatalog(session);
        for (String message : session.getSourceStatusMessages()) {
            io.println(message);
        }
        if (session.isOracleSourceLoaded()) {
            io.println("Oracle catalog loaded.");
        }
        if (session.isXmlSourceLoaded()) {
            io.println("XML query dictionary loaded.");
        }
    }

    private void renderPreviewPages(AnalyzerSession session) {
        overviewPage.render(analyzerService.getOverview(session));
        objectCountPage.render(analyzerService.getObjectCountPreview(session));
    }

    private void runAnalysis(AnalyzerSession session) {
        LOG.info("Console analysis progress started.");
        io.println("");
        io.println("[4/4] Analysis progress");
        analyzerService.runAnalysis(
                session,
                event -> {
                    String message = formatProgressEvent(event);
                    if (message != null && !message.isEmpty()) {
                        io.println(message);
                    }
                });
        LOG.info("Console analysis progress finished.");
    }

    private void printResult(AnalyzerSession session) {
        resultPage.render(analyzerService.saveResult(session));
    }

    private String formatProgressEvent(AnalyzerProgressEventViewModel event) {
        if (event.stage() == AnalyzerProgressStage.PLANNING) {
            return "";
        }
        if (event.message() != null && !event.message().isEmpty()) {
            return event.message();
        }
        return String.valueOf(event.stage());
    }

    private String readLineWithDefault(String prompt, String defaultValue) {
        io.print(prompt);
        String input = io.readLine();
        return input.isEmpty() ? defaultValue : input;
    }
}
