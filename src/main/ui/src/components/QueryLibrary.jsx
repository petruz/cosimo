// src/main/ui/src/components/QueryLibrary.jsx

import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Collapse,
    Tooltip,
    CircularProgress,
    Alert,
    IconButton,
    Button,
    Menu,
    MenuItem,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    FormControl,
    InputLabel,
    Select
} from '@mui/material';
import FolderIcon from '@mui/icons-material/Folder';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import DescriptionIcon from '@mui/icons-material/Description';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import AddIcon from '@mui/icons-material/Add';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import { useQuery } from '../hooks/useQuery';
import { useDatabase } from '../hooks/useDatabase';
import { ConnectionDialog } from './ConnectionDialog';
import axios from 'axios';

export const QueryLibrary = () => {
    const [folders, setFolders] = useState([]);
    const [openFolders, setOpenFolders] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedQuery, setSelectedQuery] = useState(null);

    // Saved connections state
    const [savedConnections, setSavedConnections] = useState([]);
    const [connectionMenuAnchor, setConnectionMenuAnchor] = useState(null);
    const [editConnectionDialog, setEditConnectionDialog] = useState(false);
    const [editingConnection, setEditingConnection] = useState(null);
    const [editError, setEditError] = useState(null);

    // Context menu state
    const [contextMenu, setContextMenu] = useState(null);
    const [contextMenuItem, setContextMenuItem] = useState(null);

    // Rename dialog state
    const [renameDialogOpen, setRenameDialogOpen] = useState(false);
    const [renameValue, setRenameValue] = useState('');
    const [renameItem, setRenameItem] = useState(null);

    // New folder/query dialog state
    const [newItemDialogOpen, setNewItemDialogOpen] = useState(false);
    const [newItemType, setNewItemType] = useState('folder'); // 'folder' or 'query'
    const [newItemName, setNewItemName] = useState('');
    const [newItemSql, setNewItemSql] = useState('');
    const [newItemDescription, setNewItemDescription] = useState('');
    const [newItemFolder, setNewItemFolder] = useState(''); // For adding query to folder

    const {
        setCurrentQueryText,
        setSelectedQueryInfo,
        setReloadQueriesCallback,
        setReloadConnectionsCallback,
        selectedConnectionId,
        setSelectedConnectionId
    } = useQuery();
    const { connect } = useDatabase();

    useEffect(() => {
        loadQueries();
        loadSavedConnections();
    }, []);

    useEffect(() => {
        // Register the reload functions in the context
        setReloadQueriesCallback(() => loadQueries);
        setReloadConnectionsCallback(() => loadSavedConnections);
    }, [setReloadQueriesCallback, setReloadConnectionsCallback]);

    const loadQueries = async () => {
        try {
            setLoading(true);
            const response = await axios.get('/api/v1/queries');

            if (response.data.success) {
                setFolders(response.data.folders);
                // Open all folders by default
                const openState = {};
                response.data.folders.forEach(folder => {
                    openState[folder.name] = true;
                });
                setOpenFolders(openState);
            } else {
                setError(response.data.message || 'Failed to load queries');
            }
        } catch (err) {
            console.error('Error loading saved queries:', err);
            setError(err.message || 'Failed to load saved queries');
        } finally {
            setLoading(false);
        }
    };

    const loadSavedConnections = async () => {
        try {
            const response = await axios.get('/api/v1/connections');
            if (response.data.success) {
                setSavedConnections(response.data.connections);
            }
        } catch (err) {
            console.error('Error loading saved connections:', err);
        }
    };

    const handleConnectionSelect = async (event) => {
        const connectionId = event.target.value;
        setSelectedConnectionId(connectionId);

        if (!connectionId) {
            // Empty selection, do nothing
            return;
        }

        try {
            // Load connection details
            const response = await axios.get(`/api/v1/connections/${connectionId}/details`);
            if (response.data.success) {
                const conn = response.data.connection;
                // Connect using the saved connection details
                await connect({
                    databaseType: conn.type,
                    host: conn.host,
                    port: parseInt(conn.port),
                    database: conn.database,
                    username: conn.username,
                    password: conn.password
                });
            }
        } catch (err) {
            console.error('Error connecting with saved connection:', err);
            alert(err.response?.data?.error || err.message || 'Failed to connect');
        }
    };

    const handleConnectionMenuOpen = (event) => {
        if (selectedConnectionId) {
            setConnectionMenuAnchor(event.currentTarget);
        }
    };

    const handleConnectionMenuClose = () => {
        setConnectionMenuAnchor(null);
    };

    const handleEditConnection = async () => {
        try {
            const response = await axios.get(`/api/v1/connections/${selectedConnectionId}/details`);
            if (response.data.success) {
                setEditingConnection(response.data.connection);
                setEditConnectionDialog(true);
            }
        } catch (err) {
            console.error('Error loading connection details:', err);
            alert('Failed to load connection details');
        }
        handleConnectionMenuClose();
    };

    const handleDeleteConnection = async () => {
        if (!window.confirm('Are you sure you want to delete this connection?')) {
            return;
        }

        try {
            await axios.delete(`/api/v1/connections/${selectedConnectionId}`);
            setSelectedConnectionId('');
            await loadSavedConnections();
        } catch (err) {
            console.error('Error deleting connection:', err);
            alert('Failed to delete connection');
        }
        handleConnectionMenuClose();
    };

    const handleSaveEditedConnection = async (formData) => {
        setEditError(null);

        try {
            await axios.put(`/api/v1/connections/${editingConnection.id}`, {
                name: formData.connectionName,
                type: formData.databaseType,
                host: formData.host,
                port: formData.port.toString(),
                database: formData.database,
                username: formData.username,
                password: formData.password
            });
            setEditConnectionDialog(false);
            setEditingConnection(null);
            await loadSavedConnections();
        } catch (err) {
            console.error('Error updating connection:', err);
            setEditError(err.response?.data?.error || 'Failed to update connection');
        }
    };

    const handleCloseEditDialog = () => {
        setEditConnectionDialog(false);
        setEditingConnection(null);
        setEditError(null);
    };

    const handleFolderClick = (folderName) => {
        setOpenFolders(prev => ({
            ...prev,
            [folderName]: !prev[folderName]
        }));
    };

    const handleQueryClick = (query, folderName) => {
        console.log('Query selected:', query);
        setSelectedQuery(query.name);
        setCurrentQueryText(query.sql);
        setSelectedQueryInfo({
            folderName: folderName,
            queryName: query.name,
            originalSql: query.sql,
            description: query.description
        });
    };

    const handleContextMenu = (event, item) => {
        event.preventDefault();
        event.stopPropagation();
        setContextMenu({
            mouseX: event.clientX - 2,
            mouseY: event.clientY - 4,
        });
        setContextMenuItem(item);
    };

    const handleCloseContextMenu = () => {
        setContextMenu(null);
        setContextMenuItem(null);
    };

    const handleRenameClick = () => {
        setRenameItem(contextMenuItem); // Save the item for rename operation
        setRenameValue(contextMenuItem.name);
        setRenameDialogOpen(true);
        handleCloseContextMenu();
    };

    const handleRenameConfirm = async () => {
        if (!renameValue.trim()) {
            alert('Name cannot be empty');
            return;
        }

        try {
            let response;

            if (renameItem.type === 'folder') {
                // Rename folder
                response = await axios.put('/api/v1/queries/folder', {
                    oldFolderName: renameItem.name,
                    newFolderName: renameValue.trim()
                });
            } else {
                // Rename query
                response = await axios.put('/api/v1/queries', {
                    folderName: renameItem.folderName,
                    oldQueryName: renameItem.name,
                    newQueryName: renameValue.trim()
                });
            }

            if (response.data.success) {
                loadQueries();
                setRenameDialogOpen(false);
                setRenameValue('');
                setRenameItem(null);

                // Update selected query if it was renamed
                if (renameItem.type === 'query' && selectedQuery === renameItem.name) {
                    setSelectedQuery(renameValue.trim());
                }
            } else {
                alert(response.data.error || 'Failed to rename');
            }
        } catch (err) {
            console.error('Error renaming:', err);
            alert(err.message || 'Failed to rename');
        }
    };

    const handleNewFolderClick = () => {
        setNewItemType('folder');
        setNewItemName('');
        setNewItemFolder('');
        setNewItemDialogOpen(true);
        handleCloseContextMenu();
    };

    const handleNewQueryClick = () => {
        setNewItemType('query');
        setNewItemName('');
        setNewItemSql('');
        setNewItemDescription('');
        setNewItemFolder(contextMenuItem?.name || ''); // Use folder name if context menu on folder
        setNewItemDialogOpen(true);
        handleCloseContextMenu();
    };

    const handleNewItemConfirm = async () => {
        if (!newItemName.trim()) {
            alert('Name cannot be empty');
            return;
        }

        try {
            let response;

            if (newItemType === 'folder') {
                // Create new folder
                response = await axios.post('/api/v1/queries/folder', {
                    folderName: newItemName.trim()
                });
            } else {
                // Create new query
                if (!newItemSql.trim()) {
                    alert('SQL cannot be empty');
                    return;
                }
                if (!newItemFolder) {
                    alert('Please select a folder');
                    return;
                }

                response = await axios.post('/api/v1/queries', {
                    folderName: newItemFolder,
                    queryName: newItemName.trim(),
                    sql: newItemSql.trim(),
                    description: newItemDescription.trim()
                });
            }

            if (response.data.success) {
                loadQueries();
                setNewItemDialogOpen(false);
                setNewItemName('');
                setNewItemSql('');
                setNewItemDescription('');
                setNewItemFolder('');
            } else {
                alert(response.data.error || 'Failed to create item');
            }
        } catch (err) {
            console.error('Error creating item:', err);
            alert(err.message || 'Failed to create item');
        }
    };

    const handleDeleteClick = () => {
        if (contextMenuItem.type === 'folder') {
            handleDeleteFolder(contextMenuItem.name);
        } else {
            handleDeleteQuery(contextMenuItem.folderName, contextMenuItem.name);
        }
        handleCloseContextMenu();
    };

    const handleDeleteQuery = async (folderName, queryName) => {
        if (!window.confirm(`Delete query "${queryName}"?`)) {
            return;
        }

        try {
            const response = await axios.delete('/api/v1/queries', {
                data: {
                    folderName: folderName,
                    queryName: queryName
                }
            });

            if (response.data.success) {
                loadQueries();
                if (selectedQuery === queryName) {
                    setSelectedQuery(null);
                }
            } else {
                alert(response.data.error || 'Failed to delete query');
            }
        } catch (err) {
            console.error('Error deleting query:', err);
            alert(err.message || 'Failed to delete query');
        }
    };

    const handleDeleteFolder = async (folderName) => {
        if (!window.confirm(`Delete folder "${folderName}" and all its queries?`)) {
            return;
        }

        try {
            const response = await axios.delete('/api/v1/queries/folder', {
                data: {
                    folderName: folderName
                }
            });

            if (response.data.success) {
                loadQueries();
            } else {
                alert(response.data.error || 'Failed to delete folder');
            }
        } catch (err) {
            console.error('Error deleting folder:', err);
            alert(err.message || 'Failed to delete folder');
        }
    };

    if (loading) {
        return (
            <Paper sx={{ p: 2, height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <CircularProgress size={24} />
            </Paper>
        );
    }

    if (error) {
        return (
            <Paper sx={{ p: 2, height: '100%' }}>
                <Alert severity="error">{error}</Alert>
            </Paper>
        );
    }

    return (
        <Paper sx={{ height: '100%', overflow: 'auto', display: 'flex', flexDirection: 'column' }}>
            <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Typography variant="h6" sx={{ mb: 1 }}>Query Library</Typography>

                {/* Saved Connections Selector */}
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <FormControl fullWidth size="small">
                        <InputLabel>Saved Connections</InputLabel>
                        <Select
                            value={selectedConnectionId}
                            onChange={handleConnectionSelect}
                            label="Saved Connections"
                        >
                            <MenuItem value="">
                                <em>None</em>
                            </MenuItem>
                            {savedConnections.map((conn) => (
                                <MenuItem key={conn.id} value={conn.id}>
                                    {conn.name} ({conn.type})
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    {selectedConnectionId && (
                        <IconButton
                            size="small"
                            onClick={handleConnectionMenuOpen}
                            title="Connection options"
                        >
                            <MoreVertIcon />
                        </IconButton>
                    )}
                </Box>

                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
                    Right-click for options
                </Typography>
            </Box>

            <Box
                sx={{ flex: 1, overflow: 'auto' }}
                onContextMenu={(e) => handleContextMenu(e, { type: 'empty' })}
            >
                <List dense>
                    {folders.map((folder) => (
                        <React.Fragment key={folder.name}>
                            <ListItemButton
                                onClick={() => handleFolderClick(folder.name)}
                                onContextMenu={(e) => handleContextMenu(e, { type: 'folder', name: folder.name })}
                            >
                                <ListItemIcon>
                                    {openFolders[folder.name] ? <FolderOpenIcon /> : <FolderIcon />}
                                </ListItemIcon>
                                <ListItemText
                                    primary={folder.name}
                                    primaryTypographyProps={{ fontWeight: 'medium' }}
                                />
                                {openFolders[folder.name] ? <ExpandLess /> : <ExpandMore />}
                            </ListItemButton>

                            <Collapse in={openFolders[folder.name]} timeout="auto" unmountOnExit>
                                <List component="div" disablePadding>
                                    {folder.queries.map((query, index) => (
                                        <Tooltip
                                            key={index}
                                            title={query.description || ''}
                                            placement="right"
                                            arrow
                                        >
                                            <ListItemButton
                                                sx={{
                                                    pl: 4,
                                                    bgcolor: selectedQuery === query.name ? 'action.selected' : 'transparent',
                                                    '&:hover': {
                                                        bgcolor: 'action.hover'
                                                    }
                                                }}
                                                onClick={() => handleQueryClick(query, folder.name)}
                                                onContextMenu={(e) => handleContextMenu(e, {
                                                    type: 'query',
                                                    name: query.name,
                                                    folderName: folder.name,
                                                    sql: query.sql,
                                                    description: query.description
                                                })}
                                            >
                                                <ListItemIcon sx={{ minWidth: 36 }}>
                                                    <DescriptionIcon fontSize="small" />
                                                </ListItemIcon>
                                                <ListItemText
                                                    primary={query.name}
                                                    primaryTypographyProps={{
                                                        variant: 'body2',
                                                        noWrap: true
                                                    }}
                                                />
                                            </ListItemButton>
                                        </Tooltip>
                                    ))}
                                </List>
                            </Collapse>
                        </React.Fragment>
                    ))}
                </List>

                {folders.length === 0 && (
                    <Box sx={{ p: 2, textAlign: 'center' }}>
                        <Typography variant="body2" color="text.secondary">
                            No saved queries available
                        </Typography>
                    </Box>
                )}
            </Box>

            {/* Context Menu - Dynamic based on context */}
            <Menu
                open={contextMenu !== null}
                onClose={handleCloseContextMenu}
                anchorReference="anchorPosition"
                anchorPosition={
                    contextMenu !== null
                        ? { top: contextMenu.mouseY, left: contextMenu.mouseX }
                        : undefined
                }
            >
                {/* Empty space - only new folder */}
                {contextMenuItem?.type === 'empty' && (
                    <MenuItem onClick={handleNewFolderClick}>
                        <ListItemIcon>
                            <AddIcon fontSize="small" />
                        </ListItemIcon>
                        <ListItemText>New Folder</ListItemText>
                    </MenuItem>
                )}

                {/* Folder - new query, rename, delete (no new folder) */}
                {contextMenuItem?.type === 'folder' && (
                    <>
                        <MenuItem onClick={handleNewQueryClick}>
                            <ListItemIcon>
                                <AddIcon fontSize="small" />
                            </ListItemIcon>
                            <ListItemText>New Query</ListItemText>
                        </MenuItem>
                        <MenuItem onClick={handleRenameClick}>
                            <ListItemIcon>
                                <EditIcon fontSize="small" />
                            </ListItemIcon>
                            <ListItemText>Rename</ListItemText>
                        </MenuItem>
                        <MenuItem onClick={handleDeleteClick}>
                            <ListItemIcon>
                                <DeleteIcon fontSize="small" />
                            </ListItemIcon>
                            <ListItemText>Delete</ListItemText>
                        </MenuItem>
                    </>
                )}

                {/* Query - only rename and delete */}
                {contextMenuItem?.type === 'query' && (
                    <>
                        <MenuItem onClick={handleRenameClick}>
                            <ListItemIcon>
                                <EditIcon fontSize="small" />
                            </ListItemIcon>
                            <ListItemText>Rename</ListItemText>
                        </MenuItem>
                        <MenuItem onClick={handleDeleteClick}>
                            <ListItemIcon>
                                <DeleteIcon fontSize="small" />
                            </ListItemIcon>
                            <ListItemText>Delete</ListItemText>
                        </MenuItem>
                    </>
                )}
            </Menu>

            {/* Rename Dialog */}
            <Dialog open={renameDialogOpen} onClose={() => setRenameDialogOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>
                    Rename {renameItem?.type === 'folder' ? 'Folder' : 'Query'}
                </DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        margin="dense"
                        label="Name"
                        fullWidth
                        value={renameValue}
                        onChange={(e) => setRenameValue(e.target.value)}
                        onKeyPress={(e) => {
                            if (e.key === 'Enter') {
                                handleRenameConfirm();
                            }
                        }}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setRenameDialogOpen(false)}>Cancel</Button>
                    <Button onClick={handleRenameConfirm} variant="contained">Rename</Button>
                </DialogActions>
            </Dialog>

            {/* New Item Dialog (Folder or Query) */}
            <Dialog open={newItemDialogOpen} onClose={() => setNewItemDialogOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>
                    New {newItemType === 'folder' ? 'Folder' : 'Query'}
                </DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        margin="dense"
                        label={newItemType === 'folder' ? 'Folder Name' : 'Query Name'}
                        fullWidth
                        value={newItemName}
                        onChange={(e) => setNewItemName(e.target.value)}
                        sx={{ mb: 2 }}
                    />

                    {newItemType === 'query' && (
                        <>
                            <TextField
                                select
                                margin="dense"
                                label="Folder"
                                fullWidth
                                value={newItemFolder}
                                onChange={(e) => setNewItemFolder(e.target.value)}
                                sx={{ mb: 2 }}
                                SelectProps={{
                                    native: true,
                                }}
                            >
                                <option value=""></option>
                                {folders.map((folder) => (
                                    <option key={folder.name} value={folder.name}>
                                        {folder.name}
                                    </option>
                                ))}
                            </TextField>

                            <TextField
                                margin="dense"
                                label="SQL"
                                fullWidth
                                multiline
                                rows={8}
                                value={newItemSql}
                                onChange={(e) => setNewItemSql(e.target.value)}
                                placeholder="SELECT * FROM table WHERE ..."
                                sx={{ mb: 2 }}
                            />

                            <TextField
                                margin="dense"
                                label="Description (optional)"
                                fullWidth
                                multiline
                                rows={2}
                                value={newItemDescription}
                                onChange={(e) => setNewItemDescription(e.target.value)}
                            />
                        </>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setNewItemDialogOpen(false)}>Cancel</Button>
                    <Button onClick={handleNewItemConfirm} variant="contained">
                        Create
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Connection Options Menu */}
            <Menu
                anchorEl={connectionMenuAnchor}
                open={Boolean(connectionMenuAnchor)}
                onClose={handleConnectionMenuClose}
            >
                <MenuItem onClick={handleEditConnection}>
                    <ListItemIcon>
                        <EditIcon fontSize="small" />
                    </ListItemIcon>
                    <ListItemText>Edit Connection</ListItemText>
                </MenuItem>
                <MenuItem onClick={handleDeleteConnection}>
                    <ListItemIcon>
                        <DeleteIcon fontSize="small" />
                    </ListItemIcon>
                    <ListItemText>Delete Connection</ListItemText>
                </MenuItem>
            </Menu>

            {/* Edit Connection Dialog */}
            {editingConnection && (
                <ConnectionDialog
                    open={editConnectionDialog}
                    onClose={handleCloseEditDialog}
                    onSave={handleSaveEditedConnection}
                    initialData={editingConnection}
                    error={editError}
                    mode="edit"
                />
            )}
        </Paper>
    );
};
