/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * XML Manager class - Handles XML file read/write operations using XMLMemento
 */
public class XMLManager {
    
    private static XMLManager instance;
    
    private XMLManager() {
        // Singleton pattern
    }
    
    /**
     * Return XMLManager instance
     *
     * @return XMLManager instance
     */
    public static XMLManager getInstance() {
        if (instance == null) {
            instance = new XMLManager();
        }
        return instance;
    }
    
    /**
     * Load memento from XML file
     *
     * @param file XML file
     * @return XML memento object
     * @throws IOException IO exception
     */
    public XMLMemento loadFromFile(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }
        
        try (FileInputStream reader = new FileInputStream(file)) {
            XMLMemento memento = XMLMemento.loadMemento(reader);
            if (memento == null) {
                throw new IOException("Cannot parse XML file: " + file.getAbsolutePath());
            }
            return memento;
        } catch (Exception ex) {
            throw new IOException("XML file load error: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Load memento from input stream
     *
     * @param in input stream
     * @return XML memento object
     * @throws IOException IO exception
     */
    public XMLMemento loadFromFile(InputStream in) throws IOException {
        try {
            XMLMemento memento = XMLMemento.loadMemento(in);
            if (memento == null) {
                throw new IOException("Cannot parse XML stream");
            }
            return memento;
        } catch (Exception ex) {
            throw new IOException("XML stream load error: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Load memento from file path
     *
     * @param filePath file path
     * @return XML memento object
     * @throws IOException IO exception
     */
    public XMLMemento loadFromFile(String filePath) throws IOException {
        return XMLMemento.loadMemento(filePath);
    }
    
    /**
     * Save XML memento to file
     *
     * @param memento XML memento object
     * @param file file to save
     * @throws IOException IO exception
     */
    public void saveToFile(XMLMemento memento, File file) throws IOException {
        if (memento != null) {
            try (FileOutputStream writer = new FileOutputStream(file)) {
                memento.save(writer);
            } catch (Exception ex) {
                throw new IOException("XML file save error: " + ex.getMessage(), ex);
            }
        } else {
            throw new IllegalArgumentException("Memento object is null");
        }
    }
    
    /**
     * Save XML memento to file path
     *
     * @param memento XML memento object
     * @param filePath file path to save
     * @throws IOException IO exception
     */
    public void saveToFile(XMLMemento memento, String filePath) throws IOException {
        if (memento != null) {
            memento.saveToFile(filePath);
        } else {
            throw new IllegalArgumentException("Memento object is null");
        }
    }
    
    /**
     * Create new XML memento root
     *
     * @param rootName root element name
     * @return XML memento object
     * @throws Exception exception
     */
    public XMLMemento createRoot(String rootName) throws Exception {
        return XMLMemento.createWriteRoot(rootName);
    }
    
    /**
     * Validate XML file structure
     *
     * @param file XML file to validate
     * @return true if valid, false otherwise
     */
    public boolean validateXMLFile(File file) {
        try {
            XMLMemento memento = loadFromFile(file);
            return memento != null && memento.hasChildren();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get all child elements with specific name from memento
     *
     * @param memento XML memento object
     * @param childName child element name
     * @return list of child mementos
     */
    public List<XMLMemento> getChildElements(XMLMemento memento, String childName) {
        List<XMLMemento> children = new ArrayList<>();
        if (memento != null) {
            XMLMemento[] childArray = memento.getChildren(childName);
            for (XMLMemento child : childArray) {
                children.add(child);
            }
        }
        return children;
    }
    
    /**
     * Find memento by attribute value
     *
     * @param memento root memento to search in
     * @param childName child element name to search
     * @param attributeName attribute name
     * @param attributeValue attribute value to match
     * @return matching memento, or null if not found
     */
    public XMLMemento findMementoByAttribute(XMLMemento memento, String childName, 
                                           String attributeName, String attributeValue) {
        if (memento != null) {
            XMLMemento[] children = memento.getChildren(childName);
            for (XMLMemento child : children) {
                String value = child.getString(attributeName);
                if (attributeValue.equals(value)) {
                    return child;
                }
            }
        }
        return null;
    }
    
    /**
     * Merge two mementos
     *
     * @param target target memento
     * @param source source memento to merge
     */
    public void mergeMementos(XMLMemento target, XMLMemento source) {
        if (target != null && source != null) {
            // Copy all attributes from source to target
            List<String> attributeNames = source.getAttributeNames();
            for (String attrName : attributeNames) {
                String value = source.getString(attrName);
                if (value != null) {
                    target.putString(attrName, value);
                }
            }
            
            // Copy text data if exists
            String textData = source.getTextData();
            if (textData != null) {
                target.putTextData(textData);
            }
        }
    }
    
    /**
     * Create a backup of XML file
     *
     * @param originalFile original file
     * @param backupFile backup file
     * @throws IOException IO exception
     */
    public void createBackup(File originalFile, File backupFile) throws IOException {
        if (originalFile.exists()) {
            XMLMemento memento = loadFromFile(originalFile);
            saveToFile(memento, backupFile);
        }
    }
    
    /**
     * Convert memento to string representation
     *
     * @param memento XML memento object
     * @return string representation
     * @throws IOException IO exception
     */
    public String mementoToString(XMLMemento memento) throws IOException {
        if (memento != null) {
            return memento.saveToString();
        }
        return "";
    }
    
    /**
     * Create memento from string
     *
     * @param xmlString XML string
     * @return XML memento object
     * @throws IOException IO exception
     */
    public XMLMemento mementoFromString(String xmlString) throws IOException {
        if (xmlString != null && !xmlString.trim().isEmpty()) {
            try (InputStream is = new java.io.ByteArrayInputStream(xmlString.getBytes("UTF-8"))) {
                return XMLMemento.loadMemento(is);
            }
        }
        return null;
    }
}
