/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.xmlmetadata;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;

class XMLDirSchemaFetcherTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldBuildQueryDictionaryFromXmlMapper() throws Exception {
        Files.writeString(
                tempDir.resolve("sample-mapper.xml"),
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <mapper namespace="sample">
                    <select id="findAll">SELECT 1</select>
                </mapper>
                """);

        AnalyzerCatalog catalog =
                new XMLDirSchemaFetcher()
                        .fetchSchema(new XMLDirSource(tempDir.toString(), "UTF-8"));

        assertNotNull(catalog.getQueryDictionary());
        assertTrue(catalog.getQueryDictionary().getSelectQueryMap().containsKey("findAll"));
    }

    @Test
    void shouldPreserveCauseWhenXmlMapperIsMalformed() throws Exception {
        Files.writeString(
                tempDir.resolve("broken-mapper.xml"),
                "<mapper><select id=\"broken\">SELECT 1</mapper>");

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                new XMLDirSchemaFetcher()
                                        .fetchSchema(
                                                new XMLDirSource(
                                                        tempDir.toString(), "UTF-8")));

        assertTrue(exception.getMessage().contains("Failed to parse XML mapper files"));
        assertNotNull(exception.getCause());
    }
}
