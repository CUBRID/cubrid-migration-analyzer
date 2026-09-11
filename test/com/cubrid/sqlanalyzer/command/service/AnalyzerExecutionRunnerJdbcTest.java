/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cubrid.sqlanalyzer.command.model.AnalyzerFailureStage;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressStage;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;

/**
 * Exercises the JDBC-target branch of {@link AnalyzerExecutionRunner} through the
 * {@link AnalyzerJdbcExecutor} seam, so prepare-only/failure-stage decisions are
 * verified without a real CUBRID connection or CMT's {@code ConnParameters}.
 */
class AnalyzerExecutionRunnerJdbcTest {
    @Test
    void shouldPrepareDmlStatementsWithoutExecutingOrCommitting() {
        ScriptedJdbcExecutor executor = new ScriptedJdbcExecutor();
        AnalyzerExecutionRunner executionRunner = new AnalyzerExecutionRunner(config -> executor);

        AnalyzerSession session = new AnalyzerSession();
        session.setTargetType(AnalyzerTargetType.JDBC);
        session.setXmlSourceLoaded(true);
        QueryDictionary queryDictionary = new QueryDictionary();
        queryDictionary.addSelectQuery("Q_SELECT", "SELECT * FROM emp WHERE empno = ?");
        queryDictionary.addInsertQuery("Q_INSERT", "INSERT INTO emp (empno, ename) VALUES (?, ?)");
        session.getConfig().setQueryDict(queryDictionary);

        List<AnalyzerProgressEventViewModel> events = new ArrayList<>();
        executionRunner.run(session, events::add);

        assertEquals(2, session.getSucceededStatementCount());
        assertEquals(0, session.getFailedStatementCount());
        assertEquals(
                List.of(
                        "SELECT * FROM emp WHERE empno = ?",
                        "INSERT INTO emp (empno, ename) VALUES (?, ?)"),
                executor.preparedSql());
        assertTrue(executor.executedSql().isEmpty());
        assertEquals(0, executor.commitCount());
        assertTrue(executor.isClosed());
        assertTrue(events.stream().anyMatch(e ->
                "Q_SELECT".equals(e.statementId()) && "prepared".equals(e.detail())));
        assertTrue(events.stream().anyMatch(e ->
                "Q_INSERT".equals(e.statementId()) && "prepared".equals(e.detail())));
    }

    @Test
    void shouldTagJdbcFailureStageWhenPrepareThrows() {
        ScriptedJdbcExecutor executor = new ScriptedJdbcExecutor();
        executor.whenPreparingThrow(
                "INSERT INTO emp (empno) VALUES (?)", new SQLException("syntax error"));
        AnalyzerExecutionRunner executionRunner = new AnalyzerExecutionRunner(config -> executor);

        AnalyzerSession session = new AnalyzerSession();
        session.setTargetType(AnalyzerTargetType.JDBC);
        session.setXmlSourceLoaded(true);
        QueryDictionary queryDictionary = new QueryDictionary();
        queryDictionary.addInsertQuery("Q_INSERT", "INSERT INTO emp (empno) VALUES (?)");
        session.getConfig().setQueryDict(queryDictionary);

        List<AnalyzerProgressEventViewModel> events = new ArrayList<>();
        executionRunner.run(session, events::add);

        assertEquals(1, session.getFailedStatementCount());
        assertEquals(0, executor.commitCount());
        assertEquals(1, session.getReport().getFailures().size());
        assertEquals(AnalyzerFailureStage.JDBC, session.getReport().getFailures().get(0).getFailureStage());
        assertTrue(events.stream().anyMatch(e ->
                e.stage() == AnalyzerProgressStage.STATEMENT_FAILED
                        && "Q_INSERT".equals(e.statementId())
                        && e.failureStage() == AnalyzerFailureStage.JDBC));
    }
}
