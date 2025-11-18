package com.debug.queryapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/connections")
public class SavedConnectionsController {

    private static final Logger LOGGER = Logger.getLogger(SavedConnectionsController.class.getName());

    /**
     * Get all saved connections.
     * GET /api/v1/connections
     */
    @GetMapping
    public ResponseEntity<?> getSavedConnections() {
        try {
            File xmlFile = getConnectionsXmlFile();

            if (!xmlFile.exists()) {
                // Return empty list if file doesn't exist yet
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "connections", new ArrayList<>()
                ));
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            List<Map<String, Object>> connections = new ArrayList<>();
            NodeList connectionNodes = doc.getElementsByTagName("connection");

            for (int i = 0; i < connectionNodes.getLength(); i++) {
                Element connectionElement = (Element) connectionNodes.item(i);

                Map<String, Object> connection = new HashMap<>();
                connection.put("id", connectionElement.getAttribute("id"));
                connection.put("name", connectionElement.getAttribute("name"));
                connection.put("type", getElementText(connectionElement, "type"));
                connection.put("host", getElementText(connectionElement, "host"));
                connection.put("port", getElementText(connectionElement, "port"));
                connection.put("database", getElementText(connectionElement, "database"));
                connection.put("username", getElementText(connectionElement, "username"));
                // Don't send password to frontend for security

                connections.add(connection);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "connections", connections
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading saved connections", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to load saved connections",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Save a new connection.
     * POST /api/v1/connections
     */
    @PostMapping
    public ResponseEntity<?> saveConnection(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String type = request.get("type");
            String host = request.get("host");
            String port = request.get("port");
            String database = request.get("database");
            String username = request.get("username");
            String password = request.get("password");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Connection name is required"
                ));
            }

            File xmlFile = getConnectionsXmlFile();
            Document doc;

            if (xmlFile.exists()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.parse(xmlFile);
                doc.getDocumentElement().normalize();
            } else {
                // Create new document
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.newDocument();
                Element root = doc.createElement("connections");
                doc.appendChild(root);
            }

            // Check if connection name already exists
            NodeList existingConnections = doc.getElementsByTagName("connection");
            for (int i = 0; i < existingConnections.getLength(); i++) {
                Element conn = (Element) existingConnections.item(i);
                if (conn.getAttribute("name").equals(name.trim())) {
                    return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "Connection name already exists"
                    ));
                }
            }

            // Create new connection element
            Element connectionElement = doc.createElement("connection");
            String connectionId = UUID.randomUUID().toString();
            connectionElement.setAttribute("id", connectionId);
            connectionElement.setAttribute("name", name.trim());

            addElement(doc, connectionElement, "type", type);
            addElement(doc, connectionElement, "host", host);
            addElement(doc, connectionElement, "port", port);
            addElement(doc, connectionElement, "database", database);
            addElement(doc, connectionElement, "username", username);
            addElement(doc, connectionElement, "password", password); // Note: In production, encrypt this!

            doc.getDocumentElement().appendChild(connectionElement);

            saveXmlDocument(doc, xmlFile);

            LOGGER.info("Connection saved: " + name);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Connection saved successfully",
                "connectionId", connectionId
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving connection", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to save connection",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Update an existing connection.
     * PUT /api/v1/connections/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateConnection(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            File xmlFile = getConnectionsXmlFile();

            if (!xmlFile.exists()) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Connection not found"
                ));
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList connectionNodes = doc.getElementsByTagName("connection");
            boolean found = false;

            for (int i = 0; i < connectionNodes.getLength(); i++) {
                Element connectionElement = (Element) connectionNodes.item(i);
                if (connectionElement.getAttribute("id").equals(id)) {
                    // Update connection attributes
                    String name = request.get("name");
                    if (name != null && !name.trim().isEmpty()) {
                        connectionElement.setAttribute("name", name.trim());
                    }

                    // Update child elements
                    updateElementText(doc, connectionElement, "type", request.get("type"));
                    updateElementText(doc, connectionElement, "host", request.get("host"));
                    updateElementText(doc, connectionElement, "port", request.get("port"));
                    updateElementText(doc, connectionElement, "database", request.get("database"));
                    updateElementText(doc, connectionElement, "username", request.get("username"));

                    String password = request.get("password");
                    if (password != null && !password.isEmpty()) {
                        updateElementText(doc, connectionElement, "password", password);
                    }

                    found = true;
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Connection not found"
                ));
            }

            saveXmlDocument(doc, xmlFile);

            LOGGER.info("Connection updated: " + id);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Connection updated successfully"
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating connection", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to update connection",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete a saved connection.
     * DELETE /api/v1/connections/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConnection(@PathVariable String id) {
        try {
            File xmlFile = getConnectionsXmlFile();

            if (!xmlFile.exists()) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Connection not found"
                ));
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList connectionNodes = doc.getElementsByTagName("connection");
            boolean found = false;

            for (int i = 0; i < connectionNodes.getLength(); i++) {
                Element connectionElement = (Element) connectionNodes.item(i);
                if (connectionElement.getAttribute("id").equals(id)) {
                    connectionElement.getParentNode().removeChild(connectionElement);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Connection not found"
                ));
            }

            saveXmlDocument(doc, xmlFile);

            LOGGER.info("Connection deleted: " + id);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Connection deleted successfully"
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting connection", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to delete connection",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get connection details including password (for editing).
     * GET /api/v1/connections/{id}/details
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<?> getConnectionDetails(@PathVariable String id) {
        try {
            File xmlFile = getConnectionsXmlFile();

            if (!xmlFile.exists()) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Connection not found"
                ));
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList connectionNodes = doc.getElementsByTagName("connection");

            for (int i = 0; i < connectionNodes.getLength(); i++) {
                Element connectionElement = (Element) connectionNodes.item(i);
                if (connectionElement.getAttribute("id").equals(id)) {
                    Map<String, Object> connection = new HashMap<>();
                    connection.put("id", connectionElement.getAttribute("id"));
                    connection.put("name", connectionElement.getAttribute("name"));
                    connection.put("type", getElementText(connectionElement, "type"));
                    connection.put("host", getElementText(connectionElement, "host"));
                    connection.put("port", getElementText(connectionElement, "port"));
                    connection.put("database", getElementText(connectionElement, "database"));
                    connection.put("username", getElementText(connectionElement, "username"));
                    connection.put("password", getElementText(connectionElement, "password"));

                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "connection", connection
                    ));
                }
            }

            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "error", "Connection not found"
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading connection details", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to load connection details",
                "message", e.getMessage()
            ));
        }
    }

    // Helper methods

    private File getConnectionsXmlFile() throws IOException {
        // Try to load from the actual resources folder (for development)
        String resourcesPath = "src/main/resources/connections.xml";
        File resourceFile = new File(resourcesPath);

        if (resourceFile.exists() && resourceFile.canWrite()) {
            return resourceFile;
        }

        // Fallback to classpath resource (for production)
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("connections.xml");
            if (is != null) {
                is.close();
                // File exists in classpath, use it
                Path classpathFile = Paths.get(getClass().getClassLoader().getResource("connections.xml").toURI());
                return classpathFile.toFile();
            }
        } catch (Exception e) {
            // Ignore and create new file
        }

        // Create new file in resources folder
        Files.createDirectories(Paths.get("src/main/resources"));
        return new File(resourcesPath);
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return "";
    }

    private void addElement(Document doc, Element parent, String tagName, String textContent) {
        if (textContent != null) {
            Element element = doc.createElement(tagName);
            element.setTextContent(textContent);
            parent.appendChild(element);
        }
    }

    private void updateElementText(Document doc, Element parent, String tagName, String newText) {
        if (newText == null) return;

        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            nodes.item(0).setTextContent(newText);
        } else {
            addElement(doc, parent, tagName, newText);
        }
    }

    private void saveXmlDocument(Document doc, File file) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }
}
