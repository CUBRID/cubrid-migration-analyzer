/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public final class AnalyzerLogInitializer {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static Logger systemLog;

    private AnalyzerLogInitializer() {
    }

    public static void initLog(String logDirectory) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            if (systemLog != null) {
                systemLog.debug("Log system already initialized. skip.");
            }
            return;
        }

        System.setProperty("log.dir", logDirectory);
        reconfigureLogback();
        systemLog = LoggerFactory.getLogger(AnalyzerLogInitializer.class);
        systemLog.info("Logging initialized. logDir={}", logDirectory);
    }

    private static void reconfigureLogback() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();

            Path external = Paths.get("settings", "logback.xml");
            if (Files.isReadable(external)) {
                configurator.doConfigure(external.toFile());
                return;
            }

            try (InputStream input = AnalyzerLogInitializer.class
                    .getClassLoader()
                    .getResourceAsStream("logback.xml")) {
                if (input == null) {
                    throw new IllegalStateException("logback.xml resource was not found.");
                }
                configurator.doConfigure(input);
            }
        } catch (JoranException | RuntimeException | java.io.IOException ex) {
            System.err.println("Failed to configure Logback: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
