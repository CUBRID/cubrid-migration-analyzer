/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.report;

import java.util.Locale;

public final class AnalyzerCostFormatter {
    public static final float MINUTES_PER_COST_POINT = 5.0f;
    private static final float MINUTES_PER_HOUR = 60.0f;

    private AnalyzerCostFormatter() {
    }

    public static String formatCost(float cost) {
        return String.format(Locale.US, "%.1f", cost);
    }

    public static String formatCostWithTime(float cost) {
        return formatCost(cost) + " (" + formatTime(cost) + ")";
    }

    public static float toMinutes(float cost) {
        return cost * MINUTES_PER_COST_POINT;
    }

    public static float toHours(float cost) {
        return toMinutes(cost) / MINUTES_PER_HOUR;
    }

    public static String formatTime(float cost) {
        return String.format(Locale.US, "%.2f hr", toHours(cost));
    }
}
