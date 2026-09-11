/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.model.AnalyzerFailureStage;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressStage;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;

class AnalyzerExecutionRunnerTest {
    private final AnalyzerExecutionRunner executionRunner = new AnalyzerExecutionRunner();

    @Test
    void shouldReportEmptyPlanWhenNoSourceIsLoaded() {
        AnalyzerSession session = new AnalyzerSession();
        session.setTargetType(AnalyzerTargetType.PARSER);
        List<AnalyzerProgressEventViewModel> events = new ArrayList<>();

        executionRunner.run(session, events::add);

        assertEquals(0, session.getAnalyzedStatementCount());
        assertEquals(0, session.getSucceededStatementCount());
        assertEquals(0, session.getFailedStatementCount());
        assertTrue(events.stream().anyMatch(e -> e.stage() == AnalyzerProgressStage.EMPTY));
    }

    @Test
    void shouldCountSucceededAndFailedStatementsForXmlSource() {
        AnalyzerSession session = new AnalyzerSession();
        session.setTargetType(AnalyzerTargetType.PARSER);
        session.setXmlSourceLoaded(true);

        QueryDictionary queryDictionary = new QueryDictionary();
        queryDictionary.addSelectQuery("Q_OK", "SELECT * FROM emp");
        queryDictionary.addSelectQuery("Q_FAIL", "SELECT FROM WHERE");
        session.getConfig().setQueryDict(queryDictionary);

        List<AnalyzerProgressEventViewModel> events = new ArrayList<>();

        executionRunner.run(session, events::add);

        assertEquals(2, session.getAnalyzedStatementCount());
        assertEquals(1, session.getSucceededStatementCount());
        assertEquals(1, session.getFailedStatementCount());
        assertEquals(1, session.getReport().getFailures().size());

        var failure = session.getReport().getFailures().get(0);
        assertEquals("Q_FAIL", failure.getStatementId());
        assertEquals(AnalyzerFailureStage.PARSER, failure.getFailureStage());
        assertTrue(session.getReport().getTotalEstimatedFailureCost() > 0f);

        assertTrue(events.stream().anyMatch(e ->
                e.stage() == AnalyzerProgressStage.STATEMENT_SUCCEEDED
                        && "Q_OK".equals(e.statementId())));
        assertTrue(events.stream().anyMatch(e ->
                e.stage() == AnalyzerProgressStage.STATEMENT_FAILED
                        && "Q_FAIL".equals(e.statementId())
                        && e.failureStage() == AnalyzerFailureStage.PARSER));
        assertTrue(events.stream().anyMatch(e -> e.stage() == AnalyzerProgressStage.COMPLETED));
    }

    @Test
    void shouldRejectUnsupportedTargetType() {
        AnalyzerSession session = new AnalyzerSession();
        session.setXmlSourceLoaded(true);

        QueryDictionary queryDictionary = new QueryDictionary();
        queryDictionary.addSelectQuery("Q_OK", "SELECT * FROM emp");
        session.getConfig().setQueryDict(queryDictionary);
        session.setTargetType(null);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> executionRunner.run(session, event -> { }));

        assertTrue(ex.getMessage().contains("Unsupported target type"));
    }
}
