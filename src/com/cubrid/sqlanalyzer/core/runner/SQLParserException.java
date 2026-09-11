/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.runner;

/**
 * SQLAnalyzeException is thrown when SQL validation or analysis fails.
 */
public class SQLParserException extends Exception {
	private static final long serialVersionUID = 1L;

	public SQLParserException(String message) {
		super(message);
	}

	public SQLParserException(String message, Throwable cause) {
		super(message, cause);
	}
}
