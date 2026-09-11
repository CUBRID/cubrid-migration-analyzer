/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.connection;

import com.cubrid.cubridmigration.core.connection.ConnParameters;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;

public class AnalyzerDbTypeConnParametersFactory implements AnalyzerConnParametersFactory {
    private final DatabaseType databaseType;
    private final String defaultCharset;

    public AnalyzerDbTypeConnParametersFactory(DatabaseType databaseType, String defaultCharset) {
        this.databaseType = databaseType;
        this.defaultCharset = defaultCharset;
    }

    @Override
    public ConnParameters create(String connectionName, AnalyzerJdbcConnectionInfo profile) {
        String charset = profile.getCharset();
        if (charset == null || charset.isEmpty()) {
            charset = defaultCharset;
        }

        ConnParameters cp =
                ConnParameters.getConParam(
                        connectionName,
                        profile.getHost(),
                        profile.getPort(),
                        profile.getDatabaseName(),
                        databaseType,
                        charset,
                        profile.getUser(),
                        profile.getPassword(),
                        profile.getDriverLocation(),
                        null);
        cp.setDriverFileName(profile.getDriverLocation());
        cp.setUserJDBCURL(profile.getJdbcUrl());
        return cp;
    }
}
