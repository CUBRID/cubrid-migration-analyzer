/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.cli;

public interface ConsoleIO {
    void print(String text);

    void println(String text);

    String readLine();

    String readRequired(String prompt);

    boolean confirm(String prompt);
}
