/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.dbobject;

import com.cubrid.cubridmigration.core.dbobject.Catalog;

public class AnalyzerCatalog extends Catalog {

	private static final long serialVersionUID = -7688506032493422503L;
	
	private	QueryDictionary queryDict = null;
	
	public QueryDictionary getQueryDictionary() {
		return queryDict;
	}

	public void setQueryDictionary(QueryDictionary queryDict) {
		this.queryDict = queryDict;
	}
}
