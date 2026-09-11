/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.runner;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PlcsqlChecker implements AutoCloseable {
    private static final int PLCSQL_SYNTAX_ERROR_CODE = -1;
    private static final String PLCSQL_JAR_PROPERTY = "sqlanalyzer.plcsql.jar";
    private static final String PLCSQL_COMPILER_MAIN = "com.cubrid.plcsql.compiler.PlcsqlCompilerMain";
    private static final String PLCSQL_SYNTAX_ERROR = "com.cubrid.plcsql.compiler.error.SyntaxError";

    private volatile PlcsqlBridge bridge;
    private volatile boolean closed;

    public void checkSQL(String query) throws SQLParserException {
        checkSQLAndGetStaticSqls(query);
    }

    public PlcsqlCheckResult checkSQLAndGetStaticSqls(String query) throws SQLParserException {
        try {
            return getBridge().checkSyntaxAndGetStaticSqls(query);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null && PLCSQL_SYNTAX_ERROR.equals(cause.getClass().getName())) {
                try {
                    throw new SQLParserException(formatSyntaxError(cause), cause);
                } catch (ReflectiveOperationException formatFailure) {
                    throw new SQLParserException("Failed to read PL/CSQL syntax error.", formatFailure);
                }
            }
            throw new SQLParserException("PL/CSQL checker failed.", cause == null ? e : cause);
        } catch (SQLParserException e) {
            throw e;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new SQLParserException("PL/CSQL checker failed.", e);
        }
    }

    private PlcsqlBridge getBridge() throws SQLParserException {
        if (closed) {
            throw new SQLParserException("PL/CSQL checker is closed.");
        }

        PlcsqlBridge current = bridge;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            if (closed) {
                throw new SQLParserException("PL/CSQL checker is closed.");
            }
            if (bridge == null) {
                bridge = createBridge();
            }
            return bridge;
        }
    }

    private PlcsqlBridge createBridge() throws SQLParserException {
        Path jarPath = resolvePlcsqlJar();
        ChildFirstClassLoader loader = null;
        try {
            loader =
                    new ChildFirstClassLoader(
                            new URL[] {jarPath.toUri().toURL()}, PlcsqlChecker.class.getClassLoader());
            Class<?> compilerMain = Class.forName(PLCSQL_COMPILER_MAIN, true, loader);
            Method checkSyntaxAndGetStaticSqls =
                    compilerMain.getMethod("checkSyntaxAndGetStaticSqls", String.class);
            return new PlcsqlBridge(loader, checkSyntaxAndGetStaticSqls);
        } catch (ReflectiveOperationException | RuntimeException | java.io.IOException e) {
            closeQuietly(loader);
            throw new SQLParserException("Failed to load PL/CSQL checker from: " + jarPath, e);
        }
    }

    @Override
    public void close() {
        PlcsqlBridge current;
        synchronized (this) {
            closed = true;
            current = bridge;
            bridge = null;
        }
        if (current != null) {
            current.close();
        }
    }

    private Path resolvePlcsqlJar() throws SQLParserException {
        String configuredPath = System.getProperty(PLCSQL_JAR_PROPERTY);
        if (configuredPath != null && !configuredPath.isBlank()) {
            return requireJar(Path.of(configuredPath));
        }

        Path developmentJar = Path.of("pl_server", "pl_server.jar");
        if (Files.isRegularFile(developmentJar)) {
            return developmentJar;
        }

        Path distributionJar = Path.of("lib", "pl_server.jar");
        if (Files.isRegularFile(distributionJar)) {
            return distributionJar;
        }

        throw new SQLParserException(
                "PL/CSQL checker jar is missing. Set -D"
                        + PLCSQL_JAR_PROPERTY
                        + " or place pl_server.jar under pl_server/.");
    }

    private Path requireJar(Path jarPath) throws SQLParserException {
        if (!Files.isRegularFile(jarPath)) {
            throw new SQLParserException("PL/CSQL checker jar is missing: " + jarPath);
        }
        return jarPath;
    }

    private String formatSyntaxError(Throwable error) throws ReflectiveOperationException {
        String message = error.getMessage();
        if (message == null) {
            message = "PL/CSQL SyntaxError message is null.";
        } else if (message.isBlank()) {
            message = "PL/CSQL SyntaxError message is blank.";
        }

        return String.format(
                "In line %d, column %d,%n%nERROR(%d): %s",
                getIntField(error, "line"),
                getIntField(error, "column"),
                PLCSQL_SYNTAX_ERROR_CODE,
                message);
    }

    private static int getIntField(Object target, String fieldName) throws ReflectiveOperationException {
        Field field = target.getClass().getField(fieldName);
        return field.getInt(target);
    }

    private static String getStringField(Object target, String fieldName)
            throws ReflectiveOperationException {
        Field field = target.getClass().getField(fieldName);
        Object value = field.get(target);
        return value == null ? "" : String.valueOf(value);
    }

    public static final class PlcsqlCheckResult {
        private final List<StaticSql> staticSqls;

        private PlcsqlCheckResult(List<StaticSql> staticSqls) {
            this.staticSqls = List.copyOf(staticSqls);
        }

        public List<StaticSql> getStaticSqls() {
            return staticSqls;
        }
    }

    public static final class StaticSql {
        private final String code;
        private final int row;
        private final int column;

        private StaticSql(String code, int row, int column) {
            this.code = code;
            this.row = row;
            this.column = column;
        }

        public String getCode() {
            return code;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }
    }

    private static final class PlcsqlBridge {
        private final ChildFirstClassLoader loader;
        private final Method checkSyntaxAndGetStaticSqls;

        private PlcsqlBridge(ChildFirstClassLoader loader, Method checkSyntaxAndGetStaticSqls) {
            this.loader = loader;
            this.checkSyntaxAndGetStaticSqls = checkSyntaxAndGetStaticSqls;
        }

        private PlcsqlCheckResult checkSyntaxAndGetStaticSqls(String query)
                throws ReflectiveOperationException {
            Thread thread = Thread.currentThread();
            ClassLoader originalClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(loader);
            try {
                Object result = checkSyntaxAndGetStaticSqls.invoke(null, query);
                return new PlcsqlCheckResult(toStaticSqls(result));
            } finally {
                thread.setContextClassLoader(originalClassLoader);
            }
        }

        private List<StaticSql> toStaticSqls(Object result) throws ReflectiveOperationException {
            if (!(result instanceof List<?>)) {
                return List.of();
            }

            List<StaticSql> staticSqls = new ArrayList<StaticSql>();
            for (Object staticSql : (List<?>) result) {
                if (staticSql == null) {
                    continue;
                }
                staticSqls.add(
                        new StaticSql(
                                getStringField(staticSql, "code"),
                                getIntField(staticSql, "row"),
                                getIntField(staticSql, "col")));
            }
            return staticSqls;
        }

        private void close() {
            closeQuietly(loader);
        }
    }

    private static void closeQuietly(ChildFirstClassLoader loader) {
        if (loader == null) {
            return;
        }
        try {
            loader.close();
        } catch (IOException ignored) {
        }
    }

    private static final class ChildFirstClassLoader extends URLClassLoader {
        private static final String[] CHILD_FIRST_PACKAGES = {
            "com.cubrid.plcsql.",
            "com.cubrid.jsp.",
            "org.antlr.",
            "org.stringtemplate."
        };

        private ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loadedClass = findLoadedClass(name);
                if (loadedClass == null) {
                    loadedClass = loadClassInternal(name);
                }
                if (resolve) {
                    resolveClass(loadedClass);
                }
                return loadedClass;
            }
        }

        private Class<?> loadClassInternal(String name) throws ClassNotFoundException {
            if (isChildFirst(name)) {
                try {
                    return findClass(name);
                } catch (ClassNotFoundException ignored) {
                    return super.loadClass(name, false);
                }
            }
            return super.loadClass(name, false);
        }

        private boolean isChildFirst(String className) {
            for (String packageName : CHILD_FIRST_PACKAGES) {
                if (className.startsWith(packageName)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public URL getResource(String name) {
            URL resource = findResource(name);
            if (resource != null) {
                return resource;
            }
            return super.getResource(name);
        }
    }
}
