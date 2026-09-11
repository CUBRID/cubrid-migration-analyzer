/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.config;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerUiMode;

class AnalyzerSettingsLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldLoadStructuredSettingsWhenNoCliArgumentsAreProvided() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "ui.mode=tui\n"
                        + "debug.fullquery=true\n"
                        + "source.type=xml\n"
                        + "xml.directory=/tmp/sqlmap\n"
                        + "xml.charset=EUC-KR\n"
                        + "target.type=parser\n");

        String[] args = AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile);
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(args);

        assertEquals(AnalyzerSourceType.XML, arguments.getSourceType());
        assertEquals("/tmp/sqlmap", arguments.getXmlDirectory());
        assertEquals("EUC-KR", arguments.getXmlCharset());
        assertEquals(AnalyzerTargetType.PARSER, arguments.getTargetType());
        assertEquals(AnalyzerUiMode.TUI, arguments.getUiMode());
        assertEquals(true, arguments.isDebugFullQuery());
    }

    @Test
    void shouldAppendDebugFullQueryOptionToExplicitArgumentsSetting() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "arguments=-sx -xd /tmp/sqlmap -tp\n"
                        + "debug.fullquery=true\n");

        String[] args = AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile);
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(args);

        assertArrayEquals(
                new String[] { "-sx", "-xd", "/tmp/sqlmap", "-tp", "--debug-fullquery" },
                args);
        assertEquals(true, arguments.isDebugFullQuery());
    }

    @Test
    void shouldUseTuiModeByDefaultWhenSettingsHasNoUiMode() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=xml\n"
                        + "xml.directory=/tmp/sqlmap\n"
                        + "target.type=parser\n");

        String[] args = AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile);
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(args);

        assertEquals(AnalyzerUiMode.TUI, arguments.getUiMode());
    }

    @Test
    void shouldPreferCliArgumentsOverDefaultSettings() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=xml\n"
                        + "xml.directory=/from/settings\n"
                        + "target.type=parser\n");

        String[] cliArgs = new String[] { "-sx", "-xd", "/from/cli", "-tp" };
        String[] args = AnalyzerSettingsLoader.loadStartupArguments(cliArgs, settingsFile);

        assertArrayEquals(cliArgs, args);
    }

    @Test
    void shouldBuildOracleConnectionFromSeparatedProperties() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=oracle\n"
                        + "source.host=192.168.1.6\n"
                        + "source.port=1521\n"
                        + "source.sid=xe\n"
                        + "source.username=cubrid\n"
                        + "source.password=cubrid\n"
                        + "target.type=parser\n");

        String[] args = AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile);
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(args);

        assertEquals(AnalyzerSourceType.ORACLE, arguments.getSourceType());
        assertEquals("jdbc:oracle:thin:@//192.168.1.6:1521/xe", arguments.getSourceJdbcUrl());
        assertEquals("cubrid", arguments.getSourceUser());
        assertEquals("cubrid", arguments.getSourcePassword());
        assertEquals(AnalyzerTargetType.PARSER, arguments.getTargetType());
    }

    @Test
    void shouldLoadCombinedSourcesFromStructuredSettings() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=all\n"
                        + "source.host=192.168.1.6\n"
                        + "source.port=1521\n"
                        + "source.sid=xe\n"
                        + "source.username=cubrid\n"
                        + "source.password=cubrid\n"
                        + "xml.directory=/tmp/sqlmap\n"
                        + "xml.charset=UTF-8\n"
                        + "target.type=parser\n");

        String[] args = AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile);
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(args);

        assertEquals(AnalyzerSourceType.ALL, arguments.getSourceType());
        assertEquals("jdbc:oracle:thin:@//192.168.1.6:1521/xe", arguments.getSourceJdbcUrl());
        assertEquals("/tmp/sqlmap", arguments.getXmlDirectory());
        assertEquals(AnalyzerTargetType.PARSER, arguments.getTargetType());
    }

    @Test
    void shouldLoadCubridJdbcTargetFromStructuredSettings() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=xml\n"
                        + "xml.directory=/tmp/sqlmap\n"
                        + "target.type=jdbc\n"
                        + "target.jdbc.url=jdbc:cubrid:localhost:33000:demodb:::\n"
                        + "target.username=dba\n"
                        + "target.password=secret\n");

        String[] args = AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile);
        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(args);

        assertEquals(AnalyzerTargetType.JDBC, arguments.getTargetType());
        assertEquals("jdbc:cubrid:localhost:33000:demodb:::", arguments.getTargetJdbcUrl());
        assertEquals("dba", arguments.getTargetUser());
        assertEquals("secret", arguments.getTargetPassword());
    }

    @Test
    void shouldLoadCubridJdbcTargetFromCombinedSetting() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=xml\n"
                        + "xml.directory=/tmp/sqlmap\n"
                        + "target.type=cubrid\n"
                        + "target.jdbc=jdbc:cubrid:localhost:33000:demodb:::|dba|\n");

        AnalyzerArgumentsController arguments = AnalyzerArgumentsController.parse(
                AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile));

        assertEquals(AnalyzerTargetType.JDBC, arguments.getTargetType());
        assertEquals("", arguments.getTargetPassword());
    }

    @Test
    void shouldRejectJdbcTargetWithoutConnectionSettings() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=xml\n"
                        + "xml.directory=/tmp/sqlmap\n"
                        + "target.type=jdbc\n");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile));

        assertTrue(ex.getMessage().contains("target.jdbc.url"));
        assertTrue(ex.getMessage().contains("target.username"));
    }

    @Test
    void shouldRejectSettingsMissingRequiredSourceAndTarget() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "ui.mode=tui\n"
                        + "debug.fullquery=true\n");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile));

        assertTrue(ex.getMessage().contains("Missing required settings"));
        assertTrue(ex.getMessage().contains("source.type"));
        assertTrue(ex.getMessage().contains("target.type"));
    }

    @Test
    void shouldRejectSettingsMissingSelectedSourceDetails() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(
                settingsFile,
                "source.type=all\n"
                        + "source.host=localhost\n"
                        + "source.port=1521\n"
                        + "source.username=scott\n"
                        + "target.type=parser\n");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AnalyzerSettingsLoader.loadStartupArguments(new String[0], settingsFile));

        assertTrue(ex.getMessage().contains("source.sid"));
        assertTrue(ex.getMessage().contains("xml.directory"));
    }

    @Test
    void shouldLoadExplicitSettingsFile() throws Exception {
        Path settingsFile = tempDir.resolve("custom.conf");
        Files.writeString(settingsFile, "arguments=-sx -xd /tmp/sqlmap -tp\n");

        String[] args = AnalyzerSettingsLoader.loadStartupArguments(
                new String[] { "-conf", settingsFile.toString() }, null);

        assertArrayEquals(new String[] { "-sx", "-xd", "/tmp/sqlmap", "-tp" }, args);
    }

    @Test
    void shouldLoadLogDirectoryFromSettingsFile() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(settingsFile, "log.dir=custom-logs\n");

        String logDirectory = AnalyzerSettingsLoader.loadLogDirectory(new String[0], settingsFile);

        assertEquals("custom-logs", logDirectory);
    }

    @Test
    void shouldUseDefaultLogDirectoryWhenSettingsFileHasNoLogDirectory() throws Exception {
        Path settingsFile = tempDir.resolve("setting.conf");
        Files.writeString(settingsFile, "source.type=xml\n");

        String logDirectory = AnalyzerSettingsLoader.loadLogDirectory(new String[0], settingsFile);

        assertEquals("logs", logDirectory);
    }

    @Test
    void shouldLoadLogDirectoryFromExplicitSettingsFile() throws Exception {
        Path settingsFile = tempDir.resolve("custom.conf");
        Files.writeString(settingsFile, "log.dir=/tmp/sql-analyzer-logs\n");

        String logDirectory = AnalyzerSettingsLoader.loadLogDirectory(
                new String[] { "-conf", settingsFile.toString() }, null);

        assertEquals("/tmp/sql-analyzer-logs", logDirectory);
    }
}
