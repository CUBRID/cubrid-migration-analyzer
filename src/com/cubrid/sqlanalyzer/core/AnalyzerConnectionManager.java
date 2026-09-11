/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

/**
 * SQL Analyzer Connection Manager
 */
public class AnalyzerConnectionManager {
    
    private List<AnalyzerConnectionInfo> connections = new ArrayList<>();
    private XMLManager xmlManager = XMLManager.getInstance();
    
    /**
     * Connection information class
     */
    public static class AnalyzerConnectionInfo {
        private String name;
        private String host;
        private int port;
        private String database;
        private String username;
        private String password;
        private String charset;
        
        public AnalyzerConnectionInfo(String name, String host, int port, String database, 
                                    String username, String password, String charset) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
            this.charset = charset;
        }
        
        // Getter/Setter methods
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getCharset() { return charset; }
        public void setCharset(String charset) { this.charset = charset; }
    }
    
    /**
     * Load connection data from file
     *
     * @param file XML file
     */
    public void loadConnectionData(File file) {
        try {
            parseXML(new FileInputStream(file));
        } catch (Exception e) {
            System.err.println("Connection data load error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parse XML and load connection information (based on CMTConParamManager's loadFromFile method)
     *
     * @param in input stream
     * @throws ParserConfigurationException parser configuration exception
     */
    public void parseXML(InputStream in) throws ParserConfigurationException {
        try {
            // Use XMLManager to parse XML
            XMLMemento memento = xmlManager.loadFromFile(in);
            if (memento == null) {
                System.err.println("Cannot load XML memento.");
                return;
            }
            
            // Find "database" child elements and convert to connection information
            XMLMemento[] children = memento.getChildren("database");
            
            for (int i = 0; i < children.length; i++) {
                final XMLMemento child = children[i];
                
                // Skip XML databases
                Boolean isXmlDatabase = child.getBoolean("isXMLDatabase");
                if (isXmlDatabase != null && isXmlDatabase) {
                    continue;
                }
                
                // Extract connection information
                String dbName = child.getString("dbName");
                String username = child.getString("user");
                String password = child.getString("password");
                String hostIP = child.getString("hostIP");
                String portStr = child.getString("port");
                String charSet = child.getString("charSet");
                String conName = child.getString("name");
                
                // Validate required information
                if (dbName == null || hostIP == null || portStr == null) {
                    System.err.println("Required connection information is missing: " + conName);
                    continue;
                }
                
                try {
                    int port = Integer.parseInt(portStr);
                    
                    // Create connection information object
                    AnalyzerConnectionInfo connectionInfo = new AnalyzerConnectionInfo(
                        conName, hostIP, port, dbName, username, password, charSet);
                    
                    // Add to connection list
                    addConnection(connectionInfo);
                    
                    System.out.println("Connection info loaded: " + conName + " (" + hostIP + ":" + port + "/" + dbName + ")");
                    
                } catch (NumberFormatException e) {
                    System.err.println("Port number parsing error: " + portStr);
                }
            }
            
        } catch (Exception ex) {
            System.err.println("XML parsing error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
                System.err.println("Stream close error: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Add connection information
     *
     * @param connectionInfo connection information
     */
    public void addConnection(AnalyzerConnectionInfo connectionInfo) {
        if (connectionInfo != null && connectionInfo.getName() != null) {
            // Check for duplicate names
            connections.removeIf(existing -> existing.getName().equals(connectionInfo.getName()));
            connections.add(connectionInfo);
        }
    }
    
    /**
     * Remove connection information
     *
     * @param name connection name
     */
    public void removeConnection(String name) {
        connections.removeIf(conn -> name.equals(conn.getName()));
    }
    
    /**
     * Find connection information by name
     *
     * @param name connection name
     * @return connection information, or null
     */
    public AnalyzerConnectionInfo getConnection(String name) {
        return connections.stream()
                .filter(conn -> name.equals(conn.getName()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Return all connection information
     *
     * @return list of connection information
     */
    public List<AnalyzerConnectionInfo> getConnections() {
        return new ArrayList<>(connections);
    }
    
    /**
     * Save connection information to XML file
     *
     * @param file file to save
     */
    public void saveConnectionsToFile(File file) {
        try {
            // Create root memento
        	XMLMemento memento = xmlManager.createRoot("databases");
            
            // Convert each connection information to XML
            for (AnalyzerConnectionInfo cp : connections) {
            	XMLMemento child = memento.createChild("database");
                child.putBoolean("isXMLDatabase", false);
                child.putString("name", cp.getName());
                child.putString("dbName", cp.getDatabase());
                child.putString("charSet", cp.getCharset());
                child.putString("user", cp.getUsername());
                child.putString("password", cp.getPassword());
                child.putString("hostIP", cp.getHost());
                child.putString("port", String.valueOf(cp.getPort()));
            }
            
            // Save to file
            xmlManager.saveToFile(memento, file);
            System.out.println("Connection information saved: " + file.getAbsolutePath());
            
        } catch (Exception ex) {
            System.err.println("Connection information save error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
