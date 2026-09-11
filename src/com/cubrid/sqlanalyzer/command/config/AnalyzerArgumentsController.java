/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.config;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerUiMode;

public class AnalyzerArgumentsController {
    private static final String DEFAULT_XML_CHARSET = "UTF-8";

    private boolean interactive = true;

    @Parameter(names = "-jr", description = "Optional JDBC driver repository directory")
    private String jdbcRepositoryDir;

    @Parameter(names = { "-ui", "--ui" }, description = "UI mode: console or tui")
    private String parsedUiMode = AnalyzerUiMode.TUI.name().toLowerCase();

    @Parameter(names = "-tui", description = "Shortcut for -ui tui")
    private boolean tuiMode;

    @Parameter(names = "--debug-fullquery", description = "Include every analyzed full query in the report")
    private boolean debugFullQuery;

    private AnalyzerUiMode uiMode = AnalyzerUiMode.TUI;
    private AnalyzerSourceType sourceType;
    private AnalyzerTargetType targetType = AnalyzerTargetType.PARSER;
    private String sourceJdbcUrl;
    private String sourceUser;
    private String sourcePassword;
    private String xmlDirectory;
    private String xmlCharset = DEFAULT_XML_CHARSET;
    private String targetJdbcUrl;
    private String targetUser;
    private String targetPassword;
    private boolean oracleSourceRequested;
    private boolean xmlSourceRequested;
    private final List<String> sourceInputMessages = new ArrayList<String>();

    /**
     * Buffer for arguments without an option name, e.g. legacy JDBC repository
     * directory.
     */
    @Parameter(description = "jdbcRepositoryDir")
    private List<String> positionalArgumentBuffer = new ArrayList<String>();

    @Parameter(names = "-so", description = "Source is Oracle JDBC")
    private boolean oracleSource;

    @Parameter(names = "-sx", description = "Source is XML directory")
    private boolean xmlSource;

    @Parameter(names = "-oj", description = "Oracle connection spec: <jdbcUrl|user|password>")
    private String oracleConnectionSpec;

    @Parameter(names = "-xd", description = "XML directory path")
    private String parsedXmlDirectory;

    @Parameter(names = "-xc", description = "XML charset. Default: UTF-8")
    private String parsedXmlCharset = DEFAULT_XML_CHARSET;

    @Parameter(names = "-tp", description = "Target is embedded parser")
    private boolean parserTarget;

    @Parameter(names = "-tc", description = "Target is CUBRID JDBC")
    private boolean jdbcTarget;

    @Parameter(names = "-cj", description = "CUBRID connection spec: <jdbcUrl|user|password>")
    private String cubridConnectionSpec;

    public static AnalyzerArgumentsController parse(String[] args) {
        AnalyzerArgumentsController arguments = new AnalyzerArgumentsController();
        if (args == null || args.length == 0) {
            return arguments;
        }

        JCommander commander = newCommander(arguments);
        try {
            commander.parse(args);
        } catch (ParameterException ex) {
            throw new IllegalArgumentException(ex.getMessage() + System.lineSeparator() + usage());
        }

        arguments.applyParsedValues(args);
        if (arguments.isInteractive()) {
            return arguments;
        }
        arguments.validate();
        return arguments;
    }

    private static JCommander newCommander(AnalyzerArgumentsController arguments) {
        return JCommander.newBuilder()
                .addObject(arguments)
                .programName("java -jar analyzer.jar")
                .build();
    }

    public static String usage() {
        return "Usage:" + System.lineSeparator()
                + "  java -jar analyzer.jar -so -oj <jdbcUrl|user|password>"
                + System.lineSeparator()
                + "  java -jar analyzer.jar -sx -xd <xmlDirectory>"
                + System.lineSeparator()
                + "  java -jar analyzer.jar -so -oj <jdbcUrl|user|password> -sx -xd <xmlDirectory>"
                + System.lineSeparator()
                + "  java -jar analyzer.jar -conf <settingsFile>"
                + System.lineSeparator()
                + System.lineSeparator()
                + "Options:" + System.lineSeparator()
                + "  -conf <path> Settings file path. Default: settings/setting.conf"
                + System.lineSeparator()
                + "  -ui <mode>   UI mode: console or tui. Default: tui"
                + System.lineSeparator()
                + "  -tui         Shortcut for -ui tui" + System.lineSeparator()
                + "  -jr <path>   Optional JDBC driver repository directory"
                + System.lineSeparator()
                + "  --debug-fullquery Include every analyzed full query in the report"
                + System.lineSeparator()
                + "  -so          Enable Oracle JDBC source for DDL" + System.lineSeparator()
                + "  -oj <spec>   Oracle connection spec: <jdbcUrl|user|password>"
                + System.lineSeparator()
                + "  -sx          Enable XML directory source for DML" + System.lineSeparator()
                + "  -xd <path>   XML directory path" + System.lineSeparator()
                + "  -xc <name>   XML charset. Default: UTF-8" + System.lineSeparator()
                + "  -tp          Use embedded parser target (default)" + System.lineSeparator()
                + "  -tc          Use CUBRID JDBC target"
                + System.lineSeparator()
                + "  -cj <spec>   CUBRID connection spec: <jdbcUrl|user|password>";
    }

    public boolean isInteractive() {
        return interactive;
    }

    public String getJdbcRepositoryDir() {
        return jdbcRepositoryDir;
    }

    public AnalyzerUiMode getUiMode() {
        return uiMode;
    }

    public boolean isTuiMode() {
        return uiMode == AnalyzerUiMode.TUI;
    }

    public boolean isDebugFullQuery() {
        return debugFullQuery;
    }

    public AnalyzerSourceType getSourceType() {
        return sourceType;
    }

    public AnalyzerTargetType getTargetType() {
        return targetType;
    }

    public boolean isOracleSourceRequested() {
        return oracleSourceRequested;
    }

    public boolean isXmlSourceRequested() {
        return xmlSourceRequested;
    }

    public List<String> getSourceInputMessages() {
        return sourceInputMessages;
    }

    public String getSourceJdbcUrl() {
        return sourceJdbcUrl;
    }

    public String getSourceUser() {
        return sourceUser;
    }

    public String getSourcePassword() {
        return sourcePassword;
    }

    public String getXmlDirectory() {
        return xmlDirectory;
    }

    public String getXmlCharset() {
        return xmlCharset;
    }

    public String getTargetJdbcUrl() {
        return targetJdbcUrl;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public String getTargetPassword() {
        return targetPassword;
    }

    private void applyParsedValues(String[] args) {
        if (positionalArgumentBuffer.size() > 1) {
            throw new IllegalArgumentException(
                    "Only one JDBC repository directory argument is allowed."
                            + System.lineSeparator()
                            + usage());
        }

        if (!positionalArgumentBuffer.isEmpty()) {
            jdbcRepositoryDir = positionalArgumentBuffer.get(0);
        }

        if (args.length == 1 && !args[0].startsWith("-")) {
            interactive = true;
            return;
        }

        interactive = false;
        applyUiMode();
        oracleSourceRequested = oracleSource || oracleConnectionSpec != null;
        xmlSourceRequested = xmlSource || parsedXmlDirectory != null;

        if (oracleSourceRequested && xmlSourceRequested) {
            sourceType = AnalyzerSourceType.ALL;
        } else if (oracleSourceRequested) {
            sourceType = AnalyzerSourceType.ORACLE;
        } else if (xmlSourceRequested) {
            sourceType = AnalyzerSourceType.XML;
        }

        boolean jdbcTargetRequested = jdbcTarget || cubridConnectionSpec != null;
        if (parserTarget && jdbcTargetRequested) {
            throw new IllegalArgumentException(
                    "Parser and CUBRID JDBC targets cannot be selected together."
                            + System.lineSeparator()
                            + usage());
        }
        targetType = jdbcTargetRequested ? AnalyzerTargetType.JDBC : AnalyzerTargetType.PARSER;

        if (oracleConnectionSpec != null) {
            String[] values = splitConnectionSpec(oracleConnectionSpec, "-oj", sourceInputMessages);
            if (values != null) {
                sourceJdbcUrl = values[0];
                sourceUser = values[1];
                sourcePassword = values[2];
            }
        }

        xmlDirectory = parsedXmlDirectory;
        xmlCharset = parsedXmlCharset;

        if (cubridConnectionSpec != null) {
            String[] values = splitConnectionSpec(cubridConnectionSpec, "-cj", sourceInputMessages);
            if (values != null) {
                targetJdbcUrl = values[0];
                targetUser = values[1];
                targetPassword = values[2];
            }
        }
    }

    private void applyUiMode() {
        if (tuiMode) {
            uiMode = AnalyzerUiMode.TUI;
            return;
        }

        if (parsedUiMode == null || parsedUiMode.trim().isEmpty()) {
            uiMode = AnalyzerUiMode.TUI;
            return;
        }

        String normalized = parsedUiMode.trim().toUpperCase();
        try {
            uiMode = AnalyzerUiMode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unsupported UI mode: " + parsedUiMode + System.lineSeparator() + usage());
        }
    }

    private void validate() {
        if (!oracleSourceRequested && !xmlSourceRequested) {
            throw new IllegalArgumentException("Source option is required." + System.lineSeparator() + usage());
        }
        if (targetType == AnalyzerTargetType.JDBC
                && (targetJdbcUrl == null || targetUser == null)) {
            throw new IllegalArgumentException(
                    "CUBRID JDBC target requires -cj <jdbcUrl|user|password>."
                            + System.lineSeparator()
                            + usage());
        }
    }

    private static String[] splitConnectionSpec(String spec, String option, List<String> messages) {
        String[] values = spec.split("\\|", 3);
        if (values.length != 3) {
            values = spec.split(",", 3);
        }
        if (values.length != 3) {
            messages.add(option + " is invalid and will be skipped. Expected <jdbcUrl|user|password>.");
            return null;
        }
        if (values[0].isEmpty() || values[1].isEmpty()) {
            messages.add(option + " is invalid and will be skipped. jdbcUrl and user are required.");
            return null;
        }
        return values;
    }
}
