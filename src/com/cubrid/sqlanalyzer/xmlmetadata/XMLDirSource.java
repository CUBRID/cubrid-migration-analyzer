/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.xmlmetadata;

import java.io.FileInputStream;
import java.io.Reader;
import java.io.Serializable;

import com.cubrid.cubridmigration.core.dbmetadata.IDBSource;
import com.cubrid.cubridmigration.core.io.IReaderEvent;
import com.cubrid.cubridmigration.core.io.RmInvalidXMLCharReader;

public class XMLDirSource implements IDBSource, Serializable, Cloneable	{

    private static final long serialVersionUID = 2459494843213118124L;

    private final String filePath;
    private final String charset;
    private IReaderEvent event;

    public XMLDirSource(String filePath, String charset) {
        this(filePath, charset, null);
    }

    public XMLDirSource(String filePath, String charset, IReaderEvent event) {
        this.filePath = filePath;
        this.charset = charset;
        this.event = event;
    }

    public String getCharset() {
        return charset;
    }

    public IReaderEvent getEvent() {
        return event;
    }

    public String getFilePath() {
        return filePath;
    }

    /**
     * Create XML reader
     *
     * @return XML reader RmInvalidXMLCharReader
     */
    
    public Reader createReader() {
        try {
            RmInvalidXMLCharReader reader =
                    new RmInvalidXMLCharReader(new FileInputStream(filePath), charset);
            reader.setReaderEvent(event);
            return reader;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setEvent(IReaderEvent event) {
        this.event = event;
    }

    /**
     * Clone
     *
     * @return MYSQLXMLDumpSource
     */
    public XMLDirSource clone() {
        try {
            return (XMLDirSource) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
