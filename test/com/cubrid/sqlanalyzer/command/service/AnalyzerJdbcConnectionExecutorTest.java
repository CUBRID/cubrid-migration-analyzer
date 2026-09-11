/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class AnalyzerJdbcConnectionExecutorTest {
    @Test
    void shouldPrepareDmlWithBindMarkersWithoutExecutingIt() throws Exception {
        AtomicReference<String> preparedSql = new AtomicReference<>();
        AtomicBoolean statementClosed = new AtomicBoolean();
        AtomicBoolean statementExecuted = new AtomicBoolean();

        PreparedStatement preparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {PreparedStatement.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        statementClosed.set(true);
                        return null;
                    }
                    if (method.getName().startsWith("execute")) {
                        statementExecuted.set(true);
                    }
                    return defaultValue(method.getReturnType());
                });

        Connection connection = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if ("prepareStatement".equals(method.getName())) {
                        preparedSql.set((String) args[0]);
                        return preparedStatement;
                    }
                    return defaultValue(method.getReturnType());
                });

        String sql = "UPDATE emp SET ename = ? WHERE empno = ?";
        new AnalyzerJdbcConnectionExecutor(connection).prepare(sql);

        assertEquals(sql, preparedSql.get());
        assertTrue(statementClosed.get());
        assertFalse(statementExecuted.get());
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive() || returnType == void.class) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return 0;
    }
}
