/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

class AnalyzerLogbackConfigurationTest {
    private LoggerContext context;

    @TempDir
    Path tempDir;

    @AfterEach
    void resetLogback() {
        System.clearProperty("log.dir");
        if (context != null) {
            context.reset();
        }
    }

    @Test
    void shouldWriteErrorLevelQueryToDedicatedErrorLog() throws Exception {
        Path logDir = tempDir.resolve("logs");
        configureLogback(logDir);

        Logger logger = (Logger) LoggerFactory.getLogger(AnalyzerLogbackConfigurationTest.class);
        String failedQuery = "SELECT * FROM log_error_probe WHERE id = 42";

        logger.info("SQL execution started. query={}", failedQuery);
        logger.error(
                "SQL execution failed. query={}",
                failedQuery,
                new RuntimeException("expected failure"));
        stopAsyncAppenders();

        String regularLog = Files.readString(logDir.resolve("sql-analyzer.log"));
        String errorLog = Files.readString(logDir.resolve("sql-analyzer-error.log"));

        assertTrue(regularLog.contains("INFO"));
        assertTrue(regularLog.contains("ERROR"));
        assertTrue(regularLog.contains(failedQuery));
        assertTrue(errorLog.contains("ERROR"));
        assertTrue(errorLog.contains("SQL execution failed. query=" + failedQuery));
        assertTrue(errorLog.contains("java.lang.RuntimeException: expected failure"));
        assertFalse(errorLog.contains("SQL execution started."));
    }

    private void configureLogback(Path logDir) throws Exception {
        Files.createDirectories(logDir);
        System.setProperty("log.dir", logDir.toString());
        context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);

        Path externalConfig = Path.of("logback.xml");
        if (Files.isReadable(externalConfig)) {
            configurator.doConfigure(externalConfig.toFile());
            return;
        }

        try (InputStream config =
                AnalyzerLogbackConfigurationTest.class
                        .getClassLoader()
                        .getResourceAsStream("logback.xml")) {
            if (config == null) {
                throw new IllegalStateException("logback.xml resource was not found.");
            }
            configurator.doConfigure(config);
        }
    }

    private void stopAsyncAppenders() {
        stopAsyncAppender("ASYNC_FILE");
        stopAsyncAppender("ASYNC_ERROR_FILE");
    }

    private void stopAsyncAppender(String name) {
        Appender<ILoggingEvent> appender = context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender(name);
        if (appender instanceof AsyncAppender asyncAppender) {
            asyncAppender.stop();
        }
    }
}
