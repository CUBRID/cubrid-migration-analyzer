/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.model;

public enum AnalyzerFailureStage {
    PARSER,
    JDBC,
    CLEANUP,
    UNSUPPORTED
}
