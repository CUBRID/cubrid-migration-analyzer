/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class AnalyzerConsoleIOController implements ConsoleIO {
    private final BufferedReader reader;
    private final PrintStream out;

    public AnalyzerConsoleIOController(InputStream in, PrintStream out) {
        this.reader = new BufferedReader(new InputStreamReader(in));
        this.out = out;
    }

    @Override
    public void print(String text) {
        out.print(text);
    }

    @Override
    public void println(String text) {
        out.println(text);
    }

    @Override
    public String readLine() {
        try {
            String line = reader.readLine();
            return line == null ? "" : line.trim();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read console input.", ex);
        }
    }

    @Override
    public String readRequired(String prompt) {
        while (true) {
            print(prompt);
            String line = readLine();
            if (line.isEmpty()) {
                println("Input is required.");
                return line;
            }
            return line;
        }
    }

    @Override
    public boolean confirm(String prompt) {
        while (true) {
            print(prompt);
            String line = readLine();
            if ("y".equalsIgnoreCase(line) || "yes".equalsIgnoreCase(line)) {
                return true;
            }
            if ("n".equalsIgnoreCase(line) || "no".equalsIgnoreCase(line)) {
                return false;
            }
            println("Please enter y or n.");
        }
    }
}
