/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.runner;

public class QueryParser {
	
	static {
		try {
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.contains("win") || osName.contains("linux")) {
				System.loadLibrary("sqlvalidator");
			}
		} catch (UnsatisfiedLinkError e) {
			throw new ExceptionInInitializerError(e);
		}
	}
	
	/**
	 * Validate SQL query using CUBRID native parser.
	 * 
	 * @param query SQL string to validate
	 * @return "NO_ERROR" if valid, or error message if invalid
	 */
	public String validateSQL(String query) {
		synchronized (QueryParser.class) {
			String result = validateSQLNative(query);
			if (result == null) {
				return "CUBRID SQL parser returned no validation result.";
			}
			return result;
		}
	}

	private native String validateSQLNative(String query);

	/**
	 * Check SQL query using validateSQL and throw SQLAnalyzeException if invalid.
	 * 
	 * @param query SQL string to validate
	 * @throws SQLParserException if validation fails
	 */
	public void checkSQL(String query) throws SQLParserException {
		String result = validateSQL(query);
		if (!"NO_ERROR".equals(result)) {
			throw new SQLParserException(result);
		}
	}
}
