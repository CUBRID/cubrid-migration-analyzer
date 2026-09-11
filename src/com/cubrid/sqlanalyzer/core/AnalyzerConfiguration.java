/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core;

import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.sqlanalyzer.core.dbobject.QueryDictionary;

public class AnalyzerConfiguration extends MigrationConfiguration {

	public static final int SOURCE_TYPE_XML = 2;
    public static final int SOURCE_TYPE_DB = 3;
    
    public static final int TARGET_TYPE_CUBRID = 4;
    public static final int TARGET_TYPE_PARSER = 5;
	
	private QueryDictionary queryDict = null;
    
    public QueryDictionary getQueryDict() {
		return queryDict;
	}

	public void setQueryDict(QueryDictionary queryDict) {
		this.queryDict = queryDict;
	}
	
	public boolean isSourceXML() {
		return getSourceType() == AnalyzerConfiguration.SOURCE_TYPE_XML;
	}
	
    public boolean isTargetParser() {
    	return getDestType() == AnalyzerConfiguration.TARGET_TYPE_PARSER;
    }
}
