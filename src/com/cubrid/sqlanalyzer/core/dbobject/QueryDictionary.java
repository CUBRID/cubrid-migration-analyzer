/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.dbobject;

import java.util.HashMap;
import java.util.Map;

public class QueryDictionary {
	private Map<String, String> selectQueryMap = new HashMap<String, String>();
	private Map<String, String> insertQueryMap = new HashMap<String, String>();
	private Map<String, String> updateQueryMap = new HashMap<String, String>();
	private Map<String, String> deleteQueryMap = new HashMap<String, String>();

	public void addSelectQuery(String name, String query) {
		selectQueryMap.put(name, query);
	}
	
	public void addInsertQuery(String name, String query) {
		insertQueryMap.put(name, query);
	}
	
	public void addUpdateQuery(String name, String query) {
		updateQueryMap.put(name, query);
	}
	
	public void addDeleteQuery(String name, String query) {
		deleteQueryMap.put(name, query);
	}

	public Map<String, String> getSelectQueryMap() {
		return selectQueryMap;
	}
	
	public Map<String, String> getInsertQueryMap() {
		return insertQueryMap;
	}
	
	public Map<String, String> getUpdateQueryMap() {
		return updateQueryMap;
	}
	
	public Map<String, String> getDeleteQueryMap() {
		return deleteQueryMap;
	}
}
