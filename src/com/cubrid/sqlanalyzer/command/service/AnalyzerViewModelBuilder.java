/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbobject.Version;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.report.AnalyzerReport;
import com.cubrid.sqlanalyzer.command.connection.AnalyzerJdbcConnectionInfo;
import com.cubrid.sqlanalyzer.command.connection.AnalyzerJdbcConnectionSupport;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerResultViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerSourceOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTableSizeViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerTargetOverviewViewModel;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;

public class AnalyzerViewModelBuilder {
    private static final Pattern ORACLE_VERSION_NAME_PATTERN = Pattern.compile("\\b\\d+(?:ai|c|g)\\b",
            Pattern.CASE_INSENSITIVE);

    public AnalyzerOverviewViewModel buildOverview(AnalyzerSession session) {
        return new AnalyzerOverviewViewModel(
                getProgramVersion(),
                buildSourceOverviews(session),
                buildTargetOverview(session),
                session.getExecutionMode(),
                session.getSourceStatusMessages());
    }

    public AnalyzerObjectCountPreviewViewModel buildObjectCountPreview(AnalyzerSession session) {
        AnalyzerConfiguration config = session.getConfig();
        QueryDictionary dict = config.getQueryDict();
        int catalogSchemaCount = session.getSourceCatalog() == null ? 0 : session.getSourceCatalog().getSchemas().size();
        int selectCount = dict == null ? 0 : dict.getSelectQueryMap().size();
        int insertCount = dict == null ? 0 : dict.getInsertQueryMap().size();
        int updateCount = dict == null ? 0 : dict.getUpdateQueryMap().size();
        int deleteCount = dict == null ? 0 : dict.getDeleteQueryMap().size();
        return new AnalyzerObjectCountPreviewViewModel(
                session.getSourceType(),
                catalogSchemaCount,
                config.getTargetTableSchema().size(),
                countTargetPrimaryKeys(config.getTargetTableSchema()),
                countTargetForeignKeys(config.getTargetTableSchema()),
                config.getTargetViewSchema().size(),
                config.getTargetSerialSchema().size(),
                config.getTargetSynonymSchema().size(),
                config.getExpGrantCfg().size(),
                config.getTargetPlcsqlProcedureSchema().size(),
                config.getTargetPlcsqlFunctionSchema().size(),
                config.getExpTriggerCfg().size(),
                selectCount,
                insertCount,
                updateCount,
                deleteCount,
                sumTableBytes(session.getOracleTableSizes()),
                session.getOracleTableSizes(),
                session.isOracleSourceLoaded(),
                session.isXmlSourceLoaded(),
                session.getSourceStatusMessages());
    }

    public AnalyzerResultViewModel buildResult(AnalyzerReport report, String savedReportPath) {
        return new AnalyzerResultViewModel(
                report.getSourceType() == null
                        ? List.of()
                        : report.getSourceType() == AnalyzerSourceType.ALL
                                ? List.of(AnalyzerSourceType.ORACLE, AnalyzerSourceType.XML)
                                : List.of(report.getSourceType()),
                report.getTargetType(),
                report.getExecutionMode(),
                report.getAnalyzedStatementCount(),
                report.getSucceededStatementCount(),
                report.getFailedStatementCount(),
                report.getTotalEstimatedFailureCost(),
                savedReportPath,
                null,
                report.getSourceStatusMessages(),
                report.getFailureMessages(),
                report.getFailures(),
                report.getObjectExecutionCounts());
    }

    private List<AnalyzerSourceOverviewViewModel> buildSourceOverviews(AnalyzerSession session) {
        List<AnalyzerSourceOverviewViewModel> sources = new ArrayList<AnalyzerSourceOverviewViewModel>();
        if (session.isOracleSourceRequested() || session.isOracleSourceLoaded()) {
            sources.add(buildOracleSourceOverview(session));
        }
        if (session.isXmlSourceRequested() || session.isXmlSourceLoaded()) {
            sources.add(buildXmlSourceOverview(session));
        }
        return sources;
    }

    private AnalyzerSourceOverviewViewModel buildOracleSourceOverview(AnalyzerSession session) {
        AnalyzerJdbcConnectionInfo profile = null;
        try {
            profile = AnalyzerJdbcConnectionSupport.parseOracleProfile(
                    session.getSourceJdbcUrl(),
                    session.getSourceUser(),
                    session.getSourcePassword());
        } catch (RuntimeException ex) {
            // The first overview page is rendered before source validation.
        }
        Catalog catalog = session.getSourceCatalog();
        String version = catalog == null || catalog.getVersion() == null
                ? null
                : getOracleVersionName(catalog.getVersion());
        return new AnalyzerSourceOverviewViewModel(
                AnalyzerSourceType.ORACLE,
                session.getSourceJdbcUrl(),
                profile == null ? null : profile.getHost(),
                profile == null ? 0 : profile.getPort(),
                profile == null ? null : profile.getDatabaseName(),
                session.getSourceUser(),
                version,
                null,
                null,
                0);
    }

    private AnalyzerSourceOverviewViewModel buildXmlSourceOverview(AnalyzerSession session) {
        return new AnalyzerSourceOverviewViewModel(
                AnalyzerSourceType.XML,
                null,
                null,
                0,
                null,
                null,
                null,
                session.getXmlDirectory(),
                session.getXmlCharset(),
                countXmlFiles(session.getXmlDirectory()));
    }

    private AnalyzerTargetOverviewViewModel buildTargetOverview(AnalyzerSession session) {
        if (session.getTargetType() == AnalyzerTargetType.JDBC) {
            AnalyzerJdbcConnectionInfo profile = AnalyzerJdbcConnectionSupport.parseCubridProfile(
                    session.getTargetJdbcUrl(),
                    session.getTargetUser(),
                    session.getTargetPassword());
            return new AnalyzerTargetOverviewViewModel(
                    session.getTargetType(),
                    session.getTargetJdbcUrl(),
                    profile.getHost(),
                    profile.getPort(),
                    profile.getDatabaseName(),
                    session.getTargetUser(),
                    getTargetVersionName(session),
                    null);
        }

        return new AnalyzerTargetOverviewViewModel(
                session.getTargetType(),
                null,
                null,
                0,
                null,
                null,
                null,
                getParserVersion());
    }

    private long countTargetPrimaryKeys(List<Table> tables) {
        return tables.stream()
                .filter(t -> t.getPk() != null && !t.getPk().getPkColumns().isEmpty())
                .count();
    }

    private long countTargetForeignKeys(List<Table> tables) {
        return tables.stream()
                .mapToLong(t -> t.getFks().size())
                .sum();
    }

    private long sumTableBytes(List<AnalyzerTableSizeViewModel> tableSizes) {
        return tableSizes.stream()
                .mapToLong(t -> Math.max(0L, t.bytes()))
                .sum();
    }

    private String getProgramVersion() {
        Package packageInfo = AnalyzerViewModelBuilder.class.getPackage();
        String version = packageInfo == null ? null : packageInfo.getImplementationVersion();
        return version == null || version.isEmpty() ? "0.0.1-SNAPSHOT" : version;
    }

    private String getParserVersion() {
        return "CUBRID parser";
    }

    private String getTargetVersionName(AnalyzerSession session) {
        if (session.getConfig().getTargetConParams() == null) {
            return null;
        }

        try (Connection connection = session.getConfig().getTargetConParams().createConnection()) {
            return getCubridVersionName(connection.getMetaData());
        } catch (SQLException ex) {
            return null;
        }
    }

    private int countXmlFiles(String xmlDirectory) {
        if (xmlDirectory == null || xmlDirectory.isEmpty()) {
            return 0;
        }

        File directory = new File(xmlDirectory);
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        return files == null ? 0 : files.length;
    }

    private String getOracleVersionName(Version version) {
        String productVersion = version.getDbProductVersion();
        if (productVersion != null) {
            Matcher matcher = ORACLE_VERSION_NAME_PATTERN.matcher(productVersion);
            if (matcher.find()) {
                return matcher.group().toLowerCase();
            }
        }

        int majorVersion = version.getDbMajorVersion();
        if (majorVersion <= 0) {
            return null;
        }
        return majorVersion == 11 || majorVersion == 10
                ? majorVersion + "g"
                : majorVersion + "c";
    }

    private String getCubridVersionName(DatabaseMetaData metaData) throws SQLException {
        int majorVersion = metaData.getDatabaseMajorVersion();
        int minorVersion = metaData.getDatabaseMinorVersion();
        if (majorVersion > 0 && minorVersion >= 0) {
            return majorVersion + "." + minorVersion;
        }

        return metaData.getDatabaseProductVersion();
    }
}
