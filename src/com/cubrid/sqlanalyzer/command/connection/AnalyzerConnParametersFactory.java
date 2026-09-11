/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.connection;

import com.cubrid.cubridmigration.core.connection.ConnParameters;

public interface AnalyzerConnParametersFactory {
    ConnParameters create(String connectionName, AnalyzerJdbcConnectionInfo profile);
}
