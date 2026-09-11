/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnalyzerJdbcConnectionSupportTest {
    private static final String FAKE_DRIVER_LOCATION = "fake-driver.jar";

    @Test
    @DisplayName("Oracle SID-style URL is parsed into host/port/SID")
    void shouldParseOracleSidUrl() {
        AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseOracleProfile(
                "jdbc:oracle:thin:@//localhost:1521:XE", "scott", "tiger", FAKE_DRIVER_LOCATION);

        assertEquals("localhost", profile.getHost());
        assertEquals(1521, profile.getPort());
        assertEquals("XE", profile.getDatabaseName());
        assertEquals("scott", profile.getUser());
        assertEquals("tiger", profile.getPassword());
        assertEquals(FAKE_DRIVER_LOCATION, profile.getDriverLocation());
    }

    @Test
    @DisplayName("Oracle service-name URL is parsed with a leading slash on the database name")
    void shouldParseOracleServiceNameUrl() {
        AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseOracleProfile(
                "jdbc:oracle:thin:@localhost:1521/ORCLPDB1", "scott", "tiger", FAKE_DRIVER_LOCATION);

        assertEquals("localhost", profile.getHost());
        assertEquals(1521, profile.getPort());
        assertEquals("/ORCLPDB1", profile.getDatabaseName());
    }

    @Test
    @DisplayName("Unsupported Oracle URL format is rejected")
    void shouldRejectUnsupportedOracleUrl() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AnalyzerJdbcConnectionSupport.parseOracleProfile(
                        "jdbc:oracle:thin:localhost", "scott", "tiger", FAKE_DRIVER_LOCATION));

        assertEquals("Unsupported Oracle JDBC URL format: jdbc:oracle:thin:localhost", ex.getMessage());
    }

    @Test
    @DisplayName("CUBRID URL is parsed into host/port/database with default charset")
    void shouldParseCubridUrl() {
        AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseCubridProfile(
                "jdbc:cubrid:localhost:33000:demodb:::", "public", "", FAKE_DRIVER_LOCATION);

        assertEquals("localhost", profile.getHost());
        assertEquals(33000, profile.getPort());
        assertEquals("demodb", profile.getDatabaseName());
        assertEquals("UTF-8", profile.getCharset());
    }

    @Test
    @DisplayName("CUBRID URL charset query parameter overrides the default")
    void shouldParseCubridUrlWithExplicitCharset() {
        AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseCubridProfile(
                "jdbc:cubrid:localhost:33000:demodb:::charset=euckr&other=1",
                "public",
                "",
                FAKE_DRIVER_LOCATION);

        assertEquals("euckr", profile.getCharset());
    }

    @Test
    @DisplayName("Unsupported CUBRID URL format is rejected")
    void shouldRejectUnsupportedCubridUrl() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AnalyzerJdbcConnectionSupport.parseCubridProfile(
                        "jdbc:cubrid:localhost", "public", "", FAKE_DRIVER_LOCATION));

        assertEquals("Unsupported CUBRID JDBC URL format: jdbc:cubrid:localhost", ex.getMessage());
    }
}
