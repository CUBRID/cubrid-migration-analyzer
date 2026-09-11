/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.sqlanalyzer.command.config.AnalyzerArgumentsController;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;

class AnalyzerServiceTest {
    @TempDir
    Path xmlDirectory;

    @Test
    void shouldApplyJdbcTargetArgumentsToSession() {
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(
                new String[] {
                    "-sx", "-xd", "/tmp/sqlmap",
                    "-tc", "-cj", "jdbc:cubrid:localhost:33000:demodb:::|dba|secret"
                });
        AnalyzerSession session = new AnalyzerSession();

        new AnalyzerService().applyArguments(session, arguments);

        assertEquals(AnalyzerTargetType.JDBC, session.getTargetType());
        assertEquals("jdbc:cubrid:localhost:33000:demodb:::", session.getTargetJdbcUrl());
        assertEquals("dba", session.getTargetUser());
        assertEquals("secret", session.getTargetPassword());
    }

    @Test
    void shouldReportNoXmlFilesWhenXmlDirectoryIsEmpty() {
        AnalyzerSession session = new AnalyzerSession();
        session.setXmlSourceRequested(true);
        session.setXmlDirectory(xmlDirectory.toString());
        session.setXmlCharset("UTF-8");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new AnalyzerService().loadSourceCatalog(session));

        assertEquals(AnalyzerService.NO_ANALYZER_SOURCE_LOADED_MESSAGE, ex.getMessage());
        assertFalse(session.isXmlSourceLoaded());
        assertTrue(session.getSourceStatusMessages().stream()
                .anyMatch(message -> message.contains("No XML files found in directory")));
    }

    @Test
    void shouldSkipOracleSourceWhenUrlOrUserIsMissing() {
        AnalyzerSession session = new AnalyzerSession();
        session.setOracleSourceRequested(true);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new AnalyzerService().loadSourceCatalog(session));

        assertEquals(AnalyzerService.NO_ANALYZER_SOURCE_LOADED_MESSAGE, ex.getMessage());
        assertFalse(session.isOracleSourceLoaded());
        assertTrue(session.getSourceStatusMessages().stream()
                .anyMatch(message -> message.contains("Oracle JDBC URL and user are required.")));
    }

    @Test
    void shouldSkipOracleSourceButKeepXmlWhenOracleCredentialsAreMissing() throws Exception {
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

        AnalyzerSession session = new AnalyzerSession();
        session.setOracleSourceRequested(true);
        session.setXmlSourceRequested(true);
        session.setXmlDirectory(xmlDirectory.toString());
        session.setXmlCharset("UTF-8");

        new AnalyzerService().loadSourceCatalog(session);

        assertFalse(session.isOracleSourceLoaded());
        assertTrue(session.isXmlSourceLoaded());
        assertTrue(session.getSourceStatusMessages().stream()
                .anyMatch(message -> message.contains("Oracle source skipped: "
                        + "Oracle JDBC URL and user are required.")));
    }

    @Test
    void shouldKeepOracleCatalogWhenXmlDictionaryIsLoaded() throws Exception {
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

        Catalog oracleCatalog = new Catalog();
        AnalyzerSession session = new AnalyzerSession();
        session.setOracleSourceLoaded(true);
        session.setSourceCatalog(oracleCatalog);
        session.setXmlSourceRequested(true);
        session.setXmlDirectory(xmlDirectory.toString());
        session.setXmlCharset("UTF-8");

        new AnalyzerService().loadSourceCatalog(session);

        assertTrue(session.isXmlSourceLoaded());
        assertEquals(AnalyzerSourceType.ALL, session.getSourceType());
        assertSame(oracleCatalog, session.getSourceCatalog());
        assertNotNull(session.getAnalyzerCatalog());
        assertEquals(1, session.getConfig().getQueryDict().getSelectQueryMap().size());
    }
}
