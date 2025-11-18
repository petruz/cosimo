package com.debug.queryapp.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * REST controller for managing saved queries.
 * Reads query templates from queries.xml file.
 */
@RestController
@RequestMapping("/api/v1/queries")
@CrossOrigin(origins = "*")
public class SavedQueriesController {
    private static final Logger LOGGER = Logger.getLogger(SavedQueriesController.class.getName());

    /**
     * Get all saved queries from XML file.
     * Returns a tree structure of folders and queries.
     *
     * GET /api/v1/queries
     */
    @GetMapping
    public ResponseEntity<?> getSavedQueries() {
        try {
            LOGGER.info("Loading saved queries from XML");

            File xmlFile = getQueriesXmlFile();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            List<Map<String, Object>> folders = new ArrayList<>();

            NodeList folderNodes = doc.getElementsByTagName("folder");
            for (int i = 0; i < folderNodes.getLength(); i++) {
                Node folderNode = folderNodes.item(i);
                if (folderNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element folderElement = (Element) folderNode;

                    Map<String, Object> folder = new HashMap<>();
                    folder.put("name", folderElement.getAttribute("name"));
                    folder.put("type", "folder");

                    List<Map<String, Object>> queries = new ArrayList<>();
                    NodeList queryNodes = folderElement.getElementsByTagName("query");

                    for (int j = 0; j < queryNodes.getLength(); j++) {
                        Node queryNode = queryNodes.item(j);
                        if (queryNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element queryElement = (Element) queryNode;

                            Map<String, Object> query = new HashMap<>();
                            query.put("name", queryElement.getAttribute("name"));
                            query.put("type", "query");

                            // Get SQL content from CDATA
                            NodeList sqlNodes = queryElement.getElementsByTagName("sql");
                            if (sqlNodes.getLength() > 0) {
                                String sql = sqlNodes.item(0).getTextContent().trim();
                                query.put("sql", sql);
                            }

                            // Get description
                            NodeList descNodes = queryElement.getElementsByTagName("description");
                            if (descNodes.getLength() > 0) {
                                query.put("description", descNodes.item(0).getTextContent().trim());
                            }

                            queries.add(query);
                        }
                    }

                    folder.put("queries", queries);
                    folders.add(folder);
                }
            }

            LOGGER.info("Loaded " + folders.size() + " folders with saved queries");

            return ResponseEntity.ok(Map.of(
                "success", true,
                "folders", folders
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading saved queries", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to load saved queries",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Add a new folder to the XML file.
     *
     * POST /api/v1/queries/folder
     * Body: {
     *   "folderName": "my-folder"
     * }
     */
    @PostMapping("/folder")
    public ResponseEntity<?> addFolder(@RequestBody Map<String, String> request) {
        try {
            String folderName = request.get("folderName");
            if (folderName == null || folderName.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Folder name is required"
                ));
            }

            LOGGER.info("Adding new folder: " + folderName);

            File xmlFile = getQueriesXmlFile();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Check if folder already exists
            NodeList folderNodes = doc.getElementsByTagName("folder");
            for (int i = 0; i < folderNodes.getLength(); i++) {
                Element folderElement = (Element) folderNodes.item(i);
                if (folderElement.getAttribute("name").equals(folderName)) {
                    return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "Folder already exists"
                    ));
                }
            }

            // Create new folder element
            Element newFolder = doc.createElement("folder");
            newFolder.setAttribute("name", folderName);
            doc.getDocumentElement().appendChild(newFolder);

            // Save XML
            saveXmlDocument(doc, xmlFile);

            LOGGER.info("Folder added successfully: " + folderName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Folder added successfully"
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding folder", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to add folder",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Add a new query to a folder.
     *
     * POST /api/v1/queries
     * Body: {
     *   "folderName": "test-postgres",
     *   "queryName": "My Query",
     *   "sql": "SELECT * FROM table",
     *   "description": "Optional description"
     * }
     */
    @PostMapping
    public ResponseEntity<?> addQuery(@RequestBody Map<String, String> request) {
        try {
            String folderName = request.get("folderName");
            String queryName = request.get("queryName");
            String sql = request.get("sql");
            String description = request.get("description");

            if (folderName == null || folderName.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Folder name is required"
                ));
            }
            if (queryName == null || queryName.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Query name is required"
                ));
            }
            if (sql == null || sql.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "SQL is required"
                ));
            }

            LOGGER.info("Adding new query: " + queryName + " to folder: " + folderName);

            File xmlFile = getQueriesXmlFile();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Find the folder
            Element targetFolder = null;
            NodeList folderNodes = doc.getElementsByTagName("folder");
            for (int i = 0; i < folderNodes.getLength(); i++) {
                Element folderElement = (Element) folderNodes.item(i);
                if (folderElement.getAttribute("name").equals(folderName)) {
                    targetFolder = folderElement;
                    break;
                }
            }

            if (targetFolder == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Folder not found: " + folderName
                ));
            }

            // Create new query element
            Element newQuery = doc.createElement("query");
            newQuery.setAttribute("name", queryName);

            // Add SQL with CDATA
            Element sqlElement = doc.createElement("sql");
            CDATASection cdata = doc.createCDATASection(sql);
            sqlElement.appendChild(cdata);
            newQuery.appendChild(sqlElement);

            // Add description if provided
            if (description != null && !description.trim().isEmpty()) {
                Element descElement = doc.createElement("description");
                descElement.setTextContent(description);
                newQuery.appendChild(descElement);
            }

            targetFolder.appendChild(newQuery);

            // Save XML
            saveXmlDocument(doc, xmlFile);

            LOGGER.info("Query added successfully: " + queryName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Query added successfully"
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding query", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to add query",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete a query from a folder.
     *
     * DELETE /api/v1/queries
     * Body: {
     *   "folderName": "test-postgres",
     *   "queryName": "My Query"
     * }
     */
    @DeleteMapping
    public ResponseEntity<?> deleteQuery(@RequestBody Map<String, String> request) {
        try {
            String folderName = request.get("folderName");
            String queryName = request.get("queryName");

            if (folderName == null || queryName == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Folder name and query name are required"
                ));
            }

            LOGGER.info("Deleting query: " + queryName + " from folder: " + folderName);

            File xmlFile = getQueriesXmlFile();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Find the folder
            NodeList folderNodes = doc.getElementsByTagName("folder");
            boolean found = false;

            for (int i = 0; i < folderNodes.getLength(); i++) {
                Element folderElement = (Element) folderNodes.item(i);
                if (folderElement.getAttribute("name").equals(folderName)) {
                    // Find and remove the query
                    NodeList queryNodes = folderElement.getElementsByTagName("query");
                    for (int j = 0; j < queryNodes.getLength(); j++) {
                        Element queryElement = (Element) queryNodes.item(j);
                        if (queryElement.getAttribute("name").equals(queryName)) {
                            folderElement.removeChild(queryElement);
                            found = true;
                            break;
                        }
                    }
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Query not found"
                ));
            }

            // Save XML
            saveXmlDocument(doc, xmlFile);

            LOGGER.info("Query deleted successfully: " + queryName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Query deleted successfully"
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting query", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to delete query",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete a folder.
     *
     * DELETE /api/v1/queries/folder
     * Body: {
     *   "folderName": "my-folder"
     * }
     */
    @DeleteMapping("/folder")
    public ResponseEntity<?> deleteFolder(@RequestBody Map<String, String> request) {
        try {
            String folderName = request.get("folderName");

            if (folderName == null || folderName.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Folder name is required"
                ));
            }

            LOGGER.info("Deleting folder: " + folderName);

            File xmlFile = getQueriesXmlFile();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Find and remove the folder
            NodeList folderNodes = doc.getElementsByTagName("folder");
            boolean found = false;

            for (int i = 0; i < folderNodes.getLength(); i++) {
                Element folderElement = (Element) folderNodes.item(i);
                if (folderElement.getAttribute("name").equals(folderName)) {
                    folderElement.getParentNode().removeChild(folderElement);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Folder not found"
                ));
            }

            // Save XML
            saveXmlDocument(doc, xmlFile);

            LOGGER.info("Folder deleted successfully: " + folderName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Folder deleted successfully"
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting folder", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to delete folder",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Rename a query.
     *
     * PUT /api/v1/queries
     * Body: {
     *   "folderName": "test-postgres",
     *   "oldQueryName": "My Query",
     *   "newQueryName": "Renamed Query"
     * }
     */
    @PutMapping
    public ResponseEntity<?> renameQuery(@RequestBody Map<String, String> request) {
        try {
            String folderName = request.get("folderName");
            String oldQueryName = request.get("oldQueryName");
            String newQueryName = request.get("newQueryName");

            if (folderName == null || oldQueryName == null || newQueryName == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Folder name, old query name, and new query name are required"
                ));
            }

            if (newQueryName.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "New query name cannot be empty"
                ));
            }

            LOGGER.info("Renaming query: " + oldQueryName + " to: " + newQueryName + " in folder: " + folderName);

            File xmlFile = getQueriesXmlFile();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Find the folder
            NodeList folderNodes = doc.getElementsByTagName("folder");
            boolean found = false;

            for (int i = 0; i < folderNodes.getLength(); i++) {
                Element folderElement = (Element) folderNodes.item(i);
                if (folderElement.getAttribute("name").equals(folderName)) {
                    // Find the query
                    NodeList queryNodes = folderElement.getElementsByTagName("query");
                    for (int j = 0; j < queryNodes.getLength(); j++) {
                        Element queryElement = (Element) queryNodes.item(j);
                        if (queryElement.getAttribute("name").equals(oldQueryName)) {
                            queryElement.setAttribute("name", newQueryName.trim());
                            found = true;
                            break;
                        }
                    }
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Query not found"
                ));
            }

            // Save XML
            saveXmlDocument(doc, xmlFile);

            LOGGER.info("Query renamed successfully: " + oldQueryName + " -> " + newQueryName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Query renamed successfully"
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error renaming query", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to rename query",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Rename a folder.
     *
     * PUT /api/v1/queries/folder
     * Body: {
     *   "oldFolderName": "my-folder",
     *   "newFolderName": "renamed-folder"
     * }
     */
    @PutMapping("/folder")
    public ResponseEntity<?> renameFolder(@RequestBody Map<String, String> request) {
        try {
            String oldFolderName = request.get("oldFolderName");
            String newFolderName = request.get("newFolderName");

            if (oldFolderName == null || newFolderName == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Old folder name and new folder name are required"
                ));
            }

            if (newFolderName.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "New folder name cannot be empty"
                ));
            }

            LOGGER.info("Renaming folder: " + oldFolderName + " to: " + newFolderName);

            File xmlFile = getQueriesXmlFile();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Find and rename the folder
            NodeList folderNodes = doc.getElementsByTagName("folder");
            boolean found = false;

            for (int i = 0; i < folderNodes.getLength(); i++) {
                Element folderElement = (Element) folderNodes.item(i);
                if (folderElement.getAttribute("name").equals(oldFolderName)) {
                    folderElement.setAttribute("name", newFolderName.trim());
                    found = true;
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Folder not found"
                ));
            }

            // Save XML
            saveXmlDocument(doc, xmlFile);

            LOGGER.info("Folder renamed successfully: " + oldFolderName + " -> " + newFolderName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Folder renamed successfully"
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error renaming folder", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to rename folder",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Update query content (SQL and description).
     *
     * PUT /api/v1/queries/content
     * Body: {
     *   "folderName": "test-postgres",
     *   "queryName": "My Query",
     *   "sql": "SELECT * FROM updated_table",
     *   "description": "Updated description"
     * }
     */
    @PutMapping("/content")
    public ResponseEntity<?> updateQueryContent(@RequestBody Map<String, String> request) {
        try {
            String folderName = request.get("folderName");
            String queryName = request.get("queryName");
            String sql = request.get("sql");
            String description = request.get("description");

            if (folderName == null || queryName == null || sql == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Folder name, query name, and SQL are required"
                ));
            }

            if (sql.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "SQL cannot be empty"
                ));
            }

            LOGGER.info("Updating query content: " + queryName + " in folder: " + folderName);

            File xmlFile = getQueriesXmlFile();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Find the folder and query
            NodeList folderNodes = doc.getElementsByTagName("folder");
            boolean found = false;

            for (int i = 0; i < folderNodes.getLength(); i++) {
                Element folderElement = (Element) folderNodes.item(i);
                if (folderElement.getAttribute("name").equals(folderName)) {
                    // Find the query
                    NodeList queryNodes = folderElement.getElementsByTagName("query");
                    for (int j = 0; j < queryNodes.getLength(); j++) {
                        Element queryElement = (Element) queryNodes.item(j);
                        if (queryElement.getAttribute("name").equals(queryName)) {
                            // Update SQL content
                            NodeList sqlNodes = queryElement.getElementsByTagName("sql");
                            if (sqlNodes.getLength() > 0) {
                                Element sqlElement = (Element) sqlNodes.item(0);
                                // Remove old CDATA
                                while (sqlElement.hasChildNodes()) {
                                    sqlElement.removeChild(sqlElement.getFirstChild());
                                }
                                // Add new CDATA
                                CDATASection cdata = doc.createCDATASection(sql.trim());
                                sqlElement.appendChild(cdata);
                            }

                            // Update description if provided
                            if (description != null) {
                                NodeList descNodes = queryElement.getElementsByTagName("description");
                                if (descNodes.getLength() > 0) {
                                    Element descElement = (Element) descNodes.item(0);
                                    descElement.setTextContent(description.trim());
                                } else if (!description.trim().isEmpty()) {
                                    // Create description element if it doesn't exist
                                    Element descElement = doc.createElement("description");
                                    descElement.setTextContent(description.trim());
                                    queryElement.appendChild(descElement);
                                }
                            }

                            found = true;
                            break;
                        }
                    }
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Query not found"
                ));
            }

            // Save XML
            saveXmlDocument(doc, xmlFile);

            LOGGER.info("Query content updated successfully: " + queryName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Query content updated successfully"
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating query content", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to update query content",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get the queries.xml file from the classpath or resources folder.
     */
    private File getQueriesXmlFile() throws IOException {
        // Try to get file from src/main/resources (development mode)
        File devFile = new File("src/main/resources/queries.xml");
        if (devFile.exists()) {
            return devFile;
        }

        // Try to get from target/classes (after build)
        File targetFile = new File("target/classes/queries.xml");
        if (targetFile.exists()) {
            return targetFile;
        }

        throw new IOException("queries.xml file not found");
    }

    /**
     * Save XML document to file with proper formatting.
     */
    private void saveXmlDocument(Document doc, File file) throws Exception {
        // Remove empty text nodes to avoid excessive whitespace
        removeEmptyTextNodes(doc.getDocumentElement());

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }

    /**
     * Recursively remove empty text nodes from the document.
     * This prevents accumulation of whitespace on repeated saves.
     */
    private void removeEmptyTextNodes(Node node) {
        NodeList children = node.getChildNodes();
        List<Node> nodesToRemove = new ArrayList<>();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.TEXT_NODE) {
                String text = child.getTextContent();
                if (text != null && text.trim().isEmpty()) {
                    nodesToRemove.add(child);
                }
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                removeEmptyTextNodes(child);
            }
        }

        for (Node nodeToRemove : nodesToRemove) {
            node.removeChild(nodeToRemove);
        }
    }

    /**
     * Export queries.xml file.
     * GET /api/v1/queries/xml
     */
    @GetMapping(value = "/xml", produces = "application/xml")
    public ResponseEntity<String> exportQueriesXml() {
        try {
            File xmlFile = getQueriesXmlFile();
            String content = new String(java.nio.file.Files.readAllBytes(xmlFile.toPath()));

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"queries.xml\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_XML)
                    .body(content);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting queries.xml", e);
            return ResponseEntity.status(500).body("<?xml version=\"1.0\"?><error>Failed to export</error>");
        }
    }

    /**
     * Import queries.xml file (overwrites existing).
     * POST /api/v1/queries/xml
     *
     * Accepts either:
     * - Multipart file upload (file parameter)
     * - Plain XML in request body (Content-Type: application/xml)
     */
    @PostMapping(value = "/xml", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<?> importQueriesXml(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestBody(required = false) String xmlContent) {
        try {
            InputStream inputStream;

            // Check if it's a file upload or plain XML content
            if (file != null && !file.isEmpty()) {
                inputStream = file.getInputStream();
            } else if (xmlContent != null && !xmlContent.trim().isEmpty()) {
                inputStream = new ByteArrayInputStream(xmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } else {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "No file or XML content provided"
                ));
            }

            // Validate XML structure
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);
            doc.getDocumentElement().normalize();

            // Check root element
            if (!"queries".equals(doc.getDocumentElement().getNodeName())) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Invalid XML: root element must be 'queries'"
                ));
            }

            // Save to file
            File xmlFile = getQueriesXmlFile();

            // Re-parse and save with proper formatting
            saveXmlDocument(doc, xmlFile);

            LOGGER.info("queries.xml imported successfully");

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "queries.xml imported successfully"
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error importing queries.xml", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to import queries.xml",
                "message", e.getMessage()
            ));
        }
    }
}
