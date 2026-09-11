/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.Trigger;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailureStage;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressStage;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;

/**
 * Drives {@link AnalyzerExecutionRunner} with hand-built Oracle-catalog domain objects
 * (no XML/txt fixture, no real Oracle connection) to verify analyzer-owned DDL
 * orchestration: commit decisions, reverse-order cleanup, and the unsupported-statement
 * skip for triggers. DDL text correctness itself is CMT's ({@code CUBRIDSQLHelper})
 * responsibility, not asserted here.
 */
class AnalyzerExecutionRunnerOracleDdlTest {
    @Test
    void shouldExecuteTableDdlAndRunCleanupDropsInReverseOrder() {
        Table empTable = new Table();
        empTable.setOwner("HR");
        empTable.setName("EMP");

        Table deptTable = new Table();
        deptTable.setOwner("HR");
        deptTable.setName("DEPT");

        AnalyzerSession session = new AnalyzerSession();
        session.setTargetType(AnalyzerTargetType.JDBC);
        session.setOracleSourceLoaded(true);
        session.getConfig().addTargetTableSchema(empTable);
        session.getConfig().addTargetTableSchema(deptTable);

        ScriptedJdbcExecutor executor = new ScriptedJdbcExecutor();
        AnalyzerExecutionRunner executionRunner = new AnalyzerExecutionRunner(config -> executor);

        List<AnalyzerProgressEventViewModel> events = new ArrayList<>();
        executionRunner.run(session, events::add);

        assertEquals(2, session.getSucceededStatementCount());
        assertEquals(0, session.getFailedStatementCount());

        List<String> executedSql = executor.executedSql();
        assertEquals(4, executedSql.size());
        assertTrue(executedSql.get(0).contains("CREATE TABLE") && executedSql.get(0).contains("EMP"));
        assertTrue(executedSql.get(1).contains("CREATE TABLE") && executedSql.get(1).contains("DEPT"));
        assertTrue(executedSql.get(2).contains("DROP TABLE") && executedSql.get(2).contains("DEPT"));
        assertTrue(executedSql.get(3).contains("DROP TABLE") && executedSql.get(3).contains("EMP"));
        assertEquals(4, executor.commitCount());
        assertTrue(executor.isClosed());
    }

    @Test
    void shouldSkipTriggerAsUnsupportedWithoutReachingJdbcExecution() {
        Trigger trigger = new Trigger();
        trigger.setOwner("HR");
        trigger.setName("TRG1");
        trigger.setDDL("irrelevant: never parsed or executed");

        Schema schema = new Schema();
        schema.setName("HR");
        schema.getTriggers().add(trigger);

        Catalog catalog = new Catalog();
        catalog.addSchema(schema);

        AnalyzerSession session = new AnalyzerSession();
        session.setTargetType(AnalyzerTargetType.JDBC);
        session.setOracleSourceLoaded(true);
        AnalyzerConfiguration config = session.getConfig();
        config.addExpTriggerCfg("HR.TRG1");
        config.setSrcCatalog(catalog, false);

        ScriptedJdbcExecutor executor = new ScriptedJdbcExecutor();
        AnalyzerExecutionRunner executionRunner = new AnalyzerExecutionRunner(cfg -> executor);

        List<AnalyzerProgressEventViewModel> events = new ArrayList<>();
        executionRunner.run(session, events::add);

        assertEquals(0, session.getSucceededStatementCount());
        assertEquals(1, session.getFailedStatementCount());
        assertTrue(executor.executedSql().isEmpty());

        assertEquals(1, session.getReport().getFailures().size());
        assertEquals(
                AnalyzerFailureStage.UNSUPPORTED,
                session.getReport().getFailures().get(0).getFailureStage());
        assertTrue(events.stream().anyMatch(e ->
                e.stage() == AnalyzerProgressStage.STATEMENT_FAILED
                        && e.failureStage() == AnalyzerFailureStage.UNSUPPORTED));
    }
}
