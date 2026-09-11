/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.service;

import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerProgressEventViewModel;

public interface AnalyzerProgressListener {
    void onProgress(AnalyzerProgressEventViewModel event);
}
