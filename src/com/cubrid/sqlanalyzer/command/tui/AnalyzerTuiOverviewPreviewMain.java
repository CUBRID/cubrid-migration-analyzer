/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.tui;

import java.io.IOException;

import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.model.AnalyzerExecutionMode;
import com.cubrid.sqlanalyzer.command.model.AnalyzerSourceType;
import com.cubrid.sqlanalyzer.command.model.AnalyzerTargetType;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;

public class AnalyzerTuiOverviewPreviewMain {
    public static void main(String[] args) throws IOException {
        AnalyzerOverviewViewModel overview = new AnalyzerService().getOverview(createPreviewSession());
        new AnalyzerTuiRunner().showOverview(overview);
    }

    private static AnalyzerSession createPreviewSession() {
        AnalyzerSession session = new AnalyzerSession();
        session.setSourceType(AnalyzerSourceType.XML);
        session.setXmlDirectory("./sqlmap");
        session.setXmlCharset("UTF-8");
        session.setTargetType(AnalyzerTargetType.PARSER);
        session.setExecutionMode(AnalyzerExecutionMode.DML);
        return session;
    }
}
