/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.connection;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cubrid.cubridmigration.core.connection.ConnParameters;
import com.cubrid.cubridmigration.core.connection.JDBCData;
import com.cubrid.cubridmigration.core.connection.JDBCUtil;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.sqlanalyzer.command.config.AnalyzerPathUtils;

public final class AnalyzerJdbcConnectionSupport {
    private static final Pattern ORACLE_SID_URL =
            Pattern.compile("^jdbc:oracle:thin:@(?://)?([^:/]+):(\\d+):([^/]+)$");
    private static final Pattern ORACLE_SERVICE_URL =
            Pattern.compile("^jdbc:oracle:thin:@(?://)?([^:/]+):(\\d+)/(.+)$");
    private static final Pattern CUBRID_URL =
            Pattern.compile("^jdbc:cubrid:([^:]+):(\\d+):([^:]+):.*$");
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static String jdbcRepositoryRoot;

    private AnalyzerJdbcConnectionSupport() {
        // utility
    }

    public static void configureJdbcRepository(String jdbcRepositoryDir) {
        jdbcRepositoryRoot = jdbcRepositoryDir;
    }

    public static void initializeJdbcDrivers() {
        for (String dir : getRepositoryDirectories()) {
            File directory = new File(dir);
            if (directory.exists() && directory.isDirectory()) {
                JDBCUtil.initialJdbcByPath(directory.getAbsolutePath());
            }
        }
    }

    public static void validateConnection(
            String connectionName,
            AnalyzerJdbcConnectionInfo profile,
            AnalyzerConnParametersFactory factory) {
        ConnParameters connectionParameters = factory.create(connectionName, profile);
        try (Connection conn = connectionParameters.createConnection()) {
            if (conn == null) {
                throw new RuntimeException("Connection returned null.");
            }
        } catch (Exception ex) {
            throw new RuntimeException("JDBC connection validation failed: " + ex.getMessage(), ex);
        }
    }

    public static AnalyzerJdbcConnectionInfo parseOracleProfile(
            String jdbcUrl, String user, String password) {
        return parseOracleProfile(jdbcUrl, user, password, resolveAndLoadDriver(DatabaseType.ORACLE));
    }

    static AnalyzerJdbcConnectionInfo parseOracleProfile(
            String jdbcUrl, String user, String password, String driverLocation) {
        Matcher sidMatcher = ORACLE_SID_URL.matcher(jdbcUrl);
        if (sidMatcher.matches()) {
            return new AnalyzerJdbcConnectionInfo(
                    jdbcUrl,
                    sidMatcher.group(1),
                    Integer.parseInt(sidMatcher.group(2)),
                    sidMatcher.group(3),
                    user,
                    password,
                    DEFAULT_CHARSET,
                    driverLocation);
        }

        Matcher serviceMatcher = ORACLE_SERVICE_URL.matcher(jdbcUrl);
        if (serviceMatcher.matches()) {
            return new AnalyzerJdbcConnectionInfo(
                    jdbcUrl,
                    serviceMatcher.group(1),
                    Integer.parseInt(serviceMatcher.group(2)),
                    "/" + serviceMatcher.group(3),
                    user,
                    password,
                    DEFAULT_CHARSET,
                    driverLocation);
        }

        throw new IllegalArgumentException("Unsupported Oracle JDBC URL format: " + jdbcUrl);
    }

    public static AnalyzerJdbcConnectionInfo parseCubridProfile(
            String jdbcUrl, String user, String password) {
        return parseCubridProfile(jdbcUrl, user, password, resolveAndLoadDriver(DatabaseType.CUBRID));
    }

    static AnalyzerJdbcConnectionInfo parseCubridProfile(
            String jdbcUrl, String user, String password, String driverLocation) {
        Matcher matcher = CUBRID_URL.matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported CUBRID JDBC URL format: " + jdbcUrl);
        }

        return new AnalyzerJdbcConnectionInfo(
                jdbcUrl,
                matcher.group(1),
                Integer.parseInt(matcher.group(2)),
                matcher.group(3),
                user,
                password,
                resolveCharset(jdbcUrl),
                driverLocation);
    }

    public static AnalyzerConnParametersFactory createFactory(DatabaseType databaseType) {
        return new AnalyzerDbTypeConnParametersFactory(databaseType, DEFAULT_CHARSET);
    }

    private static String resolveCharset(String jdbcUrl) {
        int charsetIndex = jdbcUrl.toLowerCase().indexOf("charset=");
        if (charsetIndex < 0) {
            return DEFAULT_CHARSET;
        }

        String charset = jdbcUrl.substring(charsetIndex + "charset=".length());
        int nextSeparator = charset.indexOf('&');
        if (nextSeparator >= 0) {
            charset = charset.substring(0, nextSeparator);
        }
        return charset.isEmpty() ? DEFAULT_CHARSET : charset;
    }

    private static String resolveAndLoadDriver(DatabaseType databaseType) {
        String[] searchDirectories = getRepositoryDirectories();
        for (String dirPath : searchDirectories) {
            File driver = findDriverJar(dirPath, databaseType);
            if (driver == null) {
                continue;
            }

            try {
                String canonicalPath = driver.getCanonicalPath();
                JDBCData jdbcData = databaseType.getJDBCData(canonicalPath);
                if (jdbcData == null && !databaseType.addJDBCData(canonicalPath)) {
                    throw new IllegalStateException(
                            "Failed to load JDBC driver from: " + canonicalPath);
                }
                return canonicalPath;
            } catch (IOException ex) {
                throw new RuntimeException("Failed to resolve JDBC driver path.", ex);
            }
        }

        throw new IllegalStateException(
                "JDBC driver not found for " + databaseType.getName() + " in configured project directories.");
    }

    private static File findDriverJar(String directoryPath, DatabaseType databaseType) {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }

        return Arrays.stream(files)
                .filter(File::isFile)
                .filter(f -> f.getName().toLowerCase().endsWith(".jar"))
                .filter(f -> matchesDriverName(f.getName().toLowerCase(), databaseType))
                .findFirst()
                .orElse(null);
    }

    private static boolean matchesDriverName(String lowerName, DatabaseType databaseType) {
        if (DatabaseType.ORACLE.equals(databaseType)) {
            return lowerName.startsWith("ojdbc");
        }
        if (DatabaseType.CUBRID.equals(databaseType)) {
            return lowerName.contains("cubrid");
        }
        return false;
    }

    private static String[] getRepositoryDirectories() {
        Set<String> directories = new LinkedHashSet<String>();
        if (jdbcRepositoryRoot != null && !jdbcRepositoryRoot.isEmpty()) {
            directories.add(jdbcRepositoryRoot);
            directories.add(AnalyzerPathUtils.mergePath(jdbcRepositoryRoot, "oracle"));
            directories.add(AnalyzerPathUtils.mergePath(jdbcRepositoryRoot, "cubrid"));
            return directories.toArray(new String[0]);
        }

        String installPath = AnalyzerPathUtils.getInstallPath();
        String jdbcLibDir = AnalyzerPathUtils.getJdbcLibDir();

        if (jdbcLibDir != null && !jdbcLibDir.isEmpty()) {
            directories.add(jdbcLibDir);
            directories.add(AnalyzerPathUtils.mergePath(jdbcLibDir, "oracle"));
            directories.add(AnalyzerPathUtils.mergePath(jdbcLibDir, "cubrid"));
        }

        if (installPath != null && !installPath.isEmpty()) {
            directories.add(AnalyzerPathUtils.mergePath(installPath, "ojdbc"));
            directories.add(AnalyzerPathUtils.mergePath(installPath, "lib"));
        }

        return directories.toArray(new String[0]);
    }
}
