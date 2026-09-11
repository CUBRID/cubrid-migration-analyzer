/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.plan;

import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;

public interface AnalyzerExecutionPlanBuilder {
    AnalyzerExecutionPlan build(AnalyzerConfiguration config);
}
