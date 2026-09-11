/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/**
 * XML Memento class - Provides functionality to save and restore object state in XML format
 */
public final class XMLMemento {
    
    private final Document document;
    private final Element element;

    /**
     * XML Memento constructor
     *
     * @param doc Document object
     * @param el Element object
     */
    private XMLMemento(Document doc, Element el) {
        document = doc;
        element = el;
    }

    /**
     * Create and return a child node with the given node name
     *
     * @param name node name
     * @return created child node
     */
    public XMLMemento createChild(String name) {
        Element child = document.createElement(name);
        element.appendChild(child);
        return new XMLMemento(document, child);
    }

    /**
     * Return the first child node with the given node name
     *
     * @param name node name
     * @return first child node
     */
    public XMLMemento getChild(String name) {
        NodeList nodes = element.getChildNodes();
        int size = nodes.getLength();

        if (size == 0) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.getNodeName().equals(name)) {
                    return new XMLMemento(document, element);
                }
            }
        }
        return null;
    }

    /**
     * Return all child nodes with the given node name
     *
     * @param name node name
     * @return array of child nodes
     */
    public XMLMemento[] getChildren(String name) {
        NodeList nodes = element.getChildNodes();
        int size = nodes.getLength();

        if (size == 0) {
            return new XMLMemento[0];
        }
        List<XMLMemento> children = new ArrayList<XMLMemento>();
        for (int i = 0; i < size; i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.getNodeName().equals(name)) {
                    children.add(new XMLMemento(document, element));
                }
            }
        }
        return children.toArray(new XMLMemento[children.size()]);
    }

    /**
     * Return the floating point value of the given key
     *
     * @param key key
     * @return value, or null
     */
    public Float getFloat(String key) {
        String value = element.getAttribute(key);
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return Float.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Return the boolean value of the given key
     *
     * @param key key
     * @return value, or null
     */
    public Boolean getBoolean(String key) {
        String value = element.getAttribute(key);
        if (value == null || value.length() == 0) {
            return null;
        }
        return Boolean.valueOf(value);
    }

    /**
     * Return the integer value of the given key
     *
     * @param key key
     * @return value, or null
     */
    public Integer getInteger(String key) {
        String value = element.getAttribute(key);
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Return the string value of the given key
     *
     * @param key key
     * @return value, or null
     */
    public String getString(String key) {
        String value = element.getAttribute(key);
        if (value == null || value.length() == 0) {
            return null;
        }
        return value;
    }

    /**
     * Return the text node data of this element
     *
     * @return node content
     */
    public String getTextData() {
        Text textNode = getTextNode();
        if (textNode == null) {
            return null;
        }
        return textNode.getData();
    }

    /**
     * Return all attribute names of this XML memento object
     *
     * @return list of attribute names
     */
    public List<String> getAttributeNames() {
        List<String> list = new ArrayList<String>();
        NamedNodeMap map = element.getAttributes();
        int size = map.getLength();
        for (int i = 0; i < size; i++) {
            Attr attr = (Attr) map.item(i);
            list.add(attr.getName());
        }
        return list;
    }

    /**
     * Set the value of the given key to the given floating point number
     *
     * @param key key
     * @param num value
     */
    public void putFloat(String key, float num) {
        element.setAttribute(key, String.valueOf(num));
    }

    /**
     * Set the value of the given key to the given integer
     *
     * @param key key
     * @param integer value
     */
    public void putInteger(String key, int integer) {
        element.setAttribute(key, String.valueOf(integer));
    }

    /**
     * Set the value of the given key to the given string
     *
     * @param key key
     * @param value value
     */
    public void putString(String key, String value) {
        element.setAttribute(key, value);
    }

    /**
     * Set the value of the given key to the given boolean value
     *
     * @param key key
     * @param value value
     */
    public void putBoolean(String key, boolean value) {
        element.setAttribute(key, String.valueOf(value));
    }

    /**
     * Set the string to the text node of this element
     *
     * @param data node content
     */
    public void putTextData(String data) {
        Text textNode = getTextNode();
        if (textNode == null) {
            textNode = document.createTextNode(data);
            element.appendChild(textNode);
        } else {
            textNode.setData(data);
        }
    }

    /**
     * Return the text node of the memento
     *
     * @return text node, or null
     */
    private Text getTextNode() {
        NodeList nodes = element.getChildNodes();
        int size = nodes.getLength();

        if (size == 0) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            Node node = nodes.item(i);
            if (node instanceof Text) {
                return (Text) node;
            }
        }
        return null;
    }

    /**
     * Load memento object from input stream
     *
     * @param in input stream
     * @return memento object
     */
    public static XMLMemento loadMemento(InputStream in) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document document = parser.parse(new InputSource(in));
            Node node = document.getFirstChild();

            if (node instanceof Element) {
                return new XMLMemento(document, (Element) node);
            }
        } catch (Exception ex) {
            System.err.println("XML parsing error: " + ex.getMessage());
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
                System.err.println("Stream close error: " + ex.getMessage());
            }
        }
        return null;
    }

    /**
     * Load memento object from file
     *
     * @param filename filename
     * @return memento object
     * @throws IOException IO exception
     */
    public static XMLMemento loadMemento(String filename) throws IOException {
        try (FileInputStream reader = new FileInputStream(filename)) {
            return loadMemento(reader);
        }
    }

    /**
     * Create a write root memento
     *
     * @param name root element name
     * @return memento object
     * @throws ParserConfigurationException parser configuration exception
     */
    public static XMLMemento createWriteRoot(String name) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.newDocument();
        Element element = document.createElement(name);
        document.appendChild(element);
        return new XMLMemento(document, element);
    }

    /**
     * Save memento to output stream
     *
     * @param os output stream
     * @throws IOException IO exception
     */
    public void save(OutputStream os) throws IOException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            Source source = new DOMSource(document);
            Result result = new StreamResult(os);
            transformer.transform(source, result);
        } catch (Exception ex) {
            throw new IOException("XML save error: " + ex.getMessage(), ex);
        }
    }

    /**
     * Save memento to file
     *
     * @param filename filename
     * @throws IOException IO exception
     */
    public void saveToFile(String filename) throws IOException {
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(filename));
            save(outputStream);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ex) {
                    System.err.println("Stream close error: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Save memento to string
     *
     * @return string content
     * @throws IOException IO exception
     */
    public String saveToString() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        save(out);
        return out.toString("UTF-8");
    }

    /**
     * Get the underlying Document object
     *
     * @return Document object
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Get the underlying Element object
     *
     * @return Element object
     */
    public Element getElement() {
        return element;
    }

    /**
     * Check if this memento has any children
     *
     * @return true if has children, false otherwise
     */
    public boolean hasChildren() {
        return element.hasChildNodes();
    }

    /**
     * Get the node name of this element
     *
     * @return node name
     */
    public String getNodeName() {
        return element.getNodeName();
    }

    /**
     * Remove a child node by name
     *
     * @param name child node name to remove
     */
    public void removeChild(String name) {
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element && node.getNodeName().equals(name)) {
                element.removeChild(node);
                break;
            }
        }
    }

    /**
     * Clear all children of this element
     */
    public void clearChildren() {
        while (element.hasChildNodes()) {
            element.removeChild(element.getFirstChild());
        }
    }
}
