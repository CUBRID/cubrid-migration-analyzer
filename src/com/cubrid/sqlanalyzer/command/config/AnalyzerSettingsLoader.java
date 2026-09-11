/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class AnalyzerSettingsLoader {
    private static final String DEFAULT_SETTINGS_PATH = "settings/setting.conf";
    private static final String WORKSPACE_SETTINGS_PATH = "com.cubrid.SQLAnalyzer/settings/setting.conf";
    private static final String DEFAULT_LOG_DIRECTORY = "logs";

    private AnalyzerSettingsLoader() {
    }

    public static String[] loadStartupArguments(String[] args) {
        return loadStartupArguments(args, null);
    }

    public static String loadLogDirectory(String[] args) {
        return loadLogDirectory(args, null);
    }

    static String loadLogDirectory(String[] args, Path defaultSettingsPath) {
        Path settingsPath = resolveSettingsPath(args, defaultSettingsPath);
        if (settingsPath == null || !Files.isRegularFile(settingsPath)) {
            return DEFAULT_LOG_DIRECTORY;
        }

        String logDirectory = getFirst(loadProperties(settingsPath), "log.dir", "logDir");
        return logDirectory == null ? DEFAULT_LOG_DIRECTORY : logDirectory;
    }

    static String[] loadStartupArguments(String[] args, Path defaultSettingsPath) {
        StartupArguments startupArguments = extractSettingsOption(args);
        if (startupArguments.remainingArgs.length > 0) {
            return startupArguments.remainingArgs;
        }

        Path settingsPath = resolveSettingsPath(args, defaultSettingsPath);
        boolean explicitSettingsPath = startupArguments.settingsPath != null;
        if (settingsPath == null || !Files.isRegularFile(settingsPath)) {
            if (explicitSettingsPath) {
                throw new IllegalArgumentException("Settings file does not exist: " + settingsPath);
            }
            return startupArguments.remainingArgs;
        }

        return loadArguments(settingsPath);
    }

    private static Path resolveSettingsPath(String[] args, Path defaultSettingsPath) {
        StartupArguments startupArguments = extractSettingsOption(args);
        Path settingsPath = startupArguments.settingsPath;
        if (settingsPath == null) {
            settingsPath = defaultSettingsPath == null ? resolveDefaultSettingsPath() : defaultSettingsPath;
        }
        return settingsPath;
    }

    private static Path resolveDefaultSettingsPath() {
        Path defaultPath = Paths.get(DEFAULT_SETTINGS_PATH);
        if (Files.isRegularFile(defaultPath)) {
            return defaultPath;
        }

        Path workspacePath = Paths.get(WORKSPACE_SETTINGS_PATH);
        if (Files.isRegularFile(workspacePath)) {
            return workspacePath;
        }

        return defaultPath;
    }

    private static String[] loadArguments(Path settingsPath) {
        Properties properties = loadProperties(settingsPath);

        String rawArguments = trimToNull(properties.getProperty("arguments"));
        if (rawArguments != null) {
            List<String> tokens = new ArrayList<String>();
            Collections.addAll(tokens, splitArguments(rawArguments));
            addDebugOptions(tokens, properties);
            return tokens.toArray(new String[0]);
        }

        validateRequiredSettings(properties);

        List<String> tokens = new ArrayList<String>();
        addOption(tokens, "-ui", getFirst(properties, "ui.mode", "ui"));
        addOption(tokens, "-jr", getFirst(properties, "jdbc.repository.dir", "jdbcRepositoryDir"));
        addDebugOptions(tokens, properties);

        String source = getFirst(properties, "source.type", "source");
        if (source != null) {
            addSource(tokens, properties, source);
        } else {
            addAvailableSources(tokens, properties);
        }

        addTarget(tokens, properties, getFirst(properties, "target.type", "target"));

        return tokens.toArray(new String[0]);
    }

    private static void addDebugOptions(List<String> tokens, Properties properties) {
        if (isTrue(getFirst(properties, "debug.fullquery", "debug.fullQuery"))) {
            tokens.add("--debug-fullquery");
        }
    }

    private static void validateRequiredSettings(Properties properties) {
        List<String> missing = new ArrayList<String>();
        String source = getFirst(properties, "source.type", "source");
        if (source == null) {
            missing.add("source.type");
        } else {
            addMissingSourceSettings(properties, source, missing);
        }

        String target = getFirst(properties, "target.type", "target");
        if (target == null) {
            missing.add("target.type");
        } else if (isJdbcTarget(target)
                && !hasAny(properties, "target.jdbc", "cubrid.jdbc")) {
            addMissingIfAbsent(properties, missing, "target.jdbc.url", "cubrid.jdbc.url");
            addMissingIfAbsent(properties, missing, "target.username", "target.user",
                    "cubrid.username", "cubrid.user");
        }

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required settings: " + String.join(", ", missing));
        }
    }

    private static void addMissingSourceSettings(
            Properties properties,
            String source,
            List<String> missing) {
        String normalized = source.toLowerCase(Locale.ENGLISH);
        if ("all".equals(normalized) || "unified".equals(normalized)) {
            addMissingOracleSourceSettings(properties, missing);
            addMissingXmlSourceSettings(properties, missing);
            return;
        }
        if ("oracle".equals(normalized)) {
            addMissingOracleSourceSettings(properties, missing);
            return;
        }
        if ("xml".equals(normalized)) {
            addMissingXmlSourceSettings(properties, missing);
        }
    }

    private static void addMissingOracleSourceSettings(
            Properties properties,
            List<String> missing) {
        if (!hasAny(properties, "source.jdbc", "oracle.jdbc")) {
            if (!hasAny(properties, "source.jdbc.url", "oracle.jdbc.url")) {
                addMissingIfAbsent(properties, missing, "source.host", "oracle.host");
                addMissingIfAbsent(properties, missing, "source.port", "oracle.port");
                addMissingIfAbsent(properties, missing, "source.sid", "oracle.sid");
            }
        }
        addMissingIfAbsent(properties, missing, "source.username", "source.user",
                "oracle.username", "oracle.user");
    }

    private static void addMissingXmlSourceSettings(
            Properties properties,
            List<String> missing) {
        addMissingIfAbsent(properties, missing, "xml.directory", "xmlDirectory");
    }

    private static void addMissingIfAbsent(
            Properties properties,
            List<String> missing,
            String... names) {
        if (hasAny(properties, names)) {
            return;
        }
        missing.add(names[0]);
    }

    private static boolean hasAny(Properties properties, String... names) {
        return getFirst(properties, names) != null;
    }

    private static Properties loadProperties(Path settingsPath) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(settingsPath)) {
            properties.load(input);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read settings file: " + settingsPath, ex);
        }
        return properties;
    }

    private static void addSource(List<String> tokens, Properties properties, String source) {
        String normalized = source.toLowerCase(Locale.ENGLISH);
        if ("all".equals(normalized) || "unified".equals(normalized)) {
            addOracleSource(tokens, properties);
            addXmlSource(tokens, properties);
            return;
        }

        if ("oracle".equals(normalized)) {
            addOracleSource(tokens, properties);
            addXmlSourceIfConfigured(tokens, properties);
            return;
        }

        if ("xml".equals(normalized)) {
            addXmlSource(tokens, properties);
            addOracleSourceIfConfigured(tokens, properties);
            return;
        }

        throw new IllegalArgumentException("Unsupported source.type in setting.conf: " + source);
    }

    private static void addAvailableSources(List<String> tokens, Properties properties) {
        addOracleSourceIfConfigured(tokens, properties);
        addXmlSourceIfConfigured(tokens, properties);
    }

    private static void addOracleSourceIfConfigured(List<String> tokens, Properties properties) {
        String spec = buildConnectionSpec(properties, "source", "oracle");
        if (spec == null) {
            return;
        }

        tokens.add("-so");
        addOption(tokens, "-oj", spec);
    }

    private static void addOracleSource(List<String> tokens, Properties properties) {
        tokens.add("-so");
        addOption(tokens, "-oj", buildConnectionSpec(properties, "source", "oracle"));
    }

    private static void addXmlSourceIfConfigured(List<String> tokens, Properties properties) {
        String xmlDirectory = getFirst(properties, "xml.directory", "xmlDirectory");
        if (xmlDirectory == null) {
            return;
        }

        tokens.add("-sx");
        addOption(tokens, "-xd", xmlDirectory);
        addOption(tokens, "-xc", getFirst(properties, "xml.charset", "xmlCharset"));
    }

    private static void addXmlSource(List<String> tokens, Properties properties) {
        tokens.add("-sx");
        addOption(tokens, "-xd", getFirst(properties, "xml.directory", "xmlDirectory"));
        addOption(tokens, "-xc", getFirst(properties, "xml.charset", "xmlCharset"));
    }

    private static void addTarget(List<String> tokens, Properties properties, String target) {
        if (target == null) {
            tokens.add("-tp");
            return;
        }

        String normalized = target.toLowerCase(Locale.ENGLISH);
        if ("parser".equals(normalized)) {
            tokens.add("-tp");
            return;
        }
        if (isJdbcTarget(normalized)) {
            tokens.add("-tc");
            addOption(tokens, "-cj", buildConnectionSpec(properties, "target", "cubrid"));
            return;
        }

        throw new IllegalArgumentException("Unsupported target.type in setting.conf: " + target);
    }

    private static boolean isJdbcTarget(String target) {
        String normalized = target.toLowerCase(Locale.ENGLISH);
        return "jdbc".equals(normalized) || "cubrid".equals(normalized);
    }

    private static String buildConnectionSpec(
            Properties properties, String prefix, String legacyPrefix) {
        String spec = getFirst(properties, prefix + ".jdbc", legacyPrefix + ".jdbc");
        if (spec != null) {
            return spec;
        }

        String url = getFirst(properties, prefix + ".jdbc.url", legacyPrefix + ".jdbc.url");
        if (url == null && "source".equals(prefix)) {
            url = buildOracleJdbcUrl(properties, prefix, legacyPrefix);
        }

        String user = getFirst(
                properties,
                prefix + ".username",
                prefix + ".user",
                legacyPrefix + ".username",
                legacyPrefix + ".user");
        String password = getFirst(properties, prefix + ".password", legacyPrefix + ".password");
        if (url == null && user == null && password == null) {
            return null;
        }

        return nullToEmpty(url) + "|" + nullToEmpty(user) + "|" + nullToEmpty(password);
    }

    private static String buildOracleJdbcUrl(
            Properties properties, String prefix, String legacyPrefix) {
        String host = getFirst(properties, prefix + ".host", legacyPrefix + ".host");
        String port = getFirst(properties, prefix + ".port", legacyPrefix + ".port");
        String sid = getFirst(properties, prefix + ".sid", legacyPrefix + ".sid");
        if (host == null && port == null && sid == null) {
            return null;
        }
        if (host == null || port == null || sid == null) {
            throw new IllegalArgumentException(
                    "source.host, source.port, and source.sid are required for Oracle source settings.");
        }

        return "jdbc:oracle:thin:@//" + host + ":" + port + "/" + sid;
    }

    private static void addOption(List<String> tokens, String option, String value) {
        if (value == null) {
            return;
        }

        tokens.add(option);
        tokens.add(value);
    }

    private static StartupArguments extractSettingsOption(String[] args) {
        if (args == null || args.length == 0) {
            return new StartupArguments(new String[0], null);
        }

        List<String> remainingArgs = new ArrayList<String>();
        Path settingsPath = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-conf".equals(arg) || "--conf".equals(arg)) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for " + arg);
                }
                settingsPath = Paths.get(args[++i]);
                continue;
            }

            remainingArgs.add(arg);
        }

        return new StartupArguments(
                remainingArgs.toArray(new String[0]), settingsPath);
    }

    private static String getFirst(Properties properties, String... names) {
        for (String name : names) {
            String value = trimToNull(properties.getProperty(name));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static boolean isTrue(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ENGLISH);
        return "true".equals(normalized)
                || "yes".equals(normalized)
                || "y".equals(normalized)
                || "1".equals(normalized)
                || "on".equals(normalized);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String[] splitArguments(String value) {
        List<String> tokens = new ArrayList<String>();
        StringBuilder token = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (Character.isWhitespace(ch) && !inSingleQuote && !inDoubleQuote) {
                addToken(tokens, token);
                continue;
            }

            token.append(ch);
        }

        if (inSingleQuote || inDoubleQuote) {
            throw new IllegalArgumentException("Unclosed quote in setting.conf arguments.");
        }

        addToken(tokens, token);
        return tokens.toArray(new String[0]);
    }

    private static void addToken(List<String> tokens, StringBuilder token) {
        if (token.length() == 0) {
            return;
        }

        tokens.add(token.toString());
        token.setLength(0);
    }

    private static final class StartupArguments {
        private final String[] remainingArgs;
        private final Path settingsPath;

        private StartupArguments(String[] remainingArgs, Path settingsPath) {
            this.remainingArgs = remainingArgs;
            this.settingsPath = settingsPath;
        }
    }
}
