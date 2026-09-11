/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.config;

import java.io.File;
import java.io.IOException;

public final class AnalyzerPathUtils {
    private AnalyzerPathUtils() {
        // utility
    }

    public static String getInstallPath() {
        return toCanonicalPath(System.getProperty("user.dir"));
    }

    public static String getJdbcLibDir() {
        return mergePath(getInstallPath(), "jdbc");
    }

    public static String mergePath(String parent, String child) {
        return new File(parent, child).getPath();
    }

    private static String toCanonicalPath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalStateException("Analyzer install path is not configured.");
        }

        try {
            return new File(path).getCanonicalPath();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to resolve analyzer install path: " + path, ex);
        }
    }
}
