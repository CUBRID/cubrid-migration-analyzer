/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.model.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;

class AnalyzerViewModelBuilderTest {
    @Test
    void shouldShowRequestedSourcesBeforeMetadataIsLoaded() {
        AnalyzerSession session = new AnalyzerSession();
        session.setOracleSourceRequested(true);
        session.setSourceJdbcUrl("bad-jdbc-url");
        session.setSourceUser("oracle_user");
        session.setXmlSourceRequested(true);
        session.setXmlDirectory("/tmp/sqlmap");
        session.setXmlCharset("UTF-8");
        session.setTargetType(AnalyzerTargetType.PARSER);
        session.setExecutionMode(AnalyzerExecutionMode.ALL);

        AnalyzerOverviewViewModel overview = new AnalyzerViewModelBuilder().buildOverview(session);

        assertEquals(
                List.of(AnalyzerSourceType.ORACLE, AnalyzerSourceType.XML),
                overview.sources().stream().map(source -> source.type()).toList());
    }
}
