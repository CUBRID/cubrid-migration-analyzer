/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.xmlmetadata;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.dmlparser.SqlMapHandler;

public class XMLDirSchemaFetcher {
    public AnalyzerCatalog fetchSchema(XMLDirSource xmlDir) {
        try {
            String xmlDirectory = xmlDir.getFilePath();
            List<File> fileList = getXmlFilesFromDirectory(xmlDirectory);

            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            parserFactory.setNamespaceAware(true);
            parserFactory.setValidating(false);

            SAXParser parser = parserFactory.newSAXParser();
            SqlMapHandler analyzerHandler = new SqlMapHandler();

            for (File file : fileList) {
                parser.parse(file, analyzerHandler);
            }

            AnalyzerCatalog catalog = new AnalyzerCatalog();
            catalog.setQueryDictionary(analyzerHandler.getQueryDictionary());
            return catalog;
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed to parse XML mapper files from: " + xmlDir.getFilePath(), ex);
        }
    }
    
    private List<File> getXmlFilesFromDirectory(String directoryPath) {
        List<File> xmlFiles = new ArrayList<File>();
        File directory = new File(directoryPath);
        
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".xml");
                }
            });
            
            if (files != null) {
                xmlFiles.addAll(Arrays.asList(files));
            }
        }
        
        return xmlFiles;
    }
}
