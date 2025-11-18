// src/main/ui/src/components/AddQueryDialog.jsx

import React, { useState } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    TextField,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Box,
    Alert,
    Tabs,
    Tab
} from '@mui/material';

export const AddQueryDialog = ({ open, onClose, folders, onQueryAdded }) => {
    const [tabIndex, setTabIndex] = useState(0);

    // Query form state
    const [selectedFolder, setSelectedFolder] = useState('');
    const [queryName, setQueryName] = useState('');
    const [querySql, setQuerySql] = useState('');
    const [queryDescription, setQueryDescription] = useState('');

    // Folder form state
    const [folderName, setFolderName] = useState('');

    const [error, setError] = useState(null);
    const [saving, setSaving] = useState(false);

    const handleClose = () => {
        setQueryName('');
        setQuerySql('');
        setQueryDescription('');
        setSelectedFolder('');
        setFolderName('');
        setError(null);
        setTabIndex(0);
        onClose();
    };

    const handleAddQuery = async () => {
        if (!selectedFolder || !queryName.trim() || !querySql.trim()) {
            setError('Please fill in all required fields');
            return;
        }

        try {
            setSaving(true);
            setError(null);

            const response = await fetch('/api/v1/queries', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    folderName: selectedFolder,
                    queryName: queryName.trim(),
                    sql: querySql.trim(),
                    description: queryDescription.trim()
                })
            });

            const data = await response.json();

            if (data.success) {
                onQueryAdded();
                handleClose();
            } else {
                setError(data.error || data.message || 'Failed to add query');
            }
        } catch (err) {
            console.error('Error adding query:', err);
            setError(err.message || 'Failed to add query');
        } finally {
            setSaving(false);
        }
    };

    const handleAddFolder = async () => {
        if (!folderName.trim()) {
            setError('Please enter a folder name');
            return;
        }

        try {
            setSaving(true);
            setError(null);

            const response = await fetch('/api/v1/queries/folder', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    folderName: folderName.trim()
                })
            });

            const data = await response.json();

            if (data.success) {
                onQueryAdded();
                handleClose();
            } else {
                setError(data.error || data.message || 'Failed to add folder');
            }
        } catch (err) {
            console.error('Error adding folder:', err);
            setError(err.message || 'Failed to add folder');
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
            <DialogTitle>Add to Query Library</DialogTitle>

            <Tabs value={tabIndex} onChange={(e, v) => setTabIndex(v)} sx={{ borderBottom: 1, borderColor: 'divider', px: 3 }}>
                <Tab label="Add Query" />
                <Tab label="Add Folder" />
            </Tabs>

            <DialogContent>
                {error && (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {error}
                    </Alert>
                )}

                {tabIndex === 0 && (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
                        <FormControl fullWidth required>
                            <InputLabel>Folder</InputLabel>
                            <Select
                                value={selectedFolder}
                                label="Folder"
                                onChange={(e) => setSelectedFolder(e.target.value)}
                            >
                                {folders.map((folder) => (
                                    <MenuItem key={folder.name} value={folder.name}>
                                        {folder.name}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>

                        <TextField
                            label="Query Name"
                            value={queryName}
                            onChange={(e) => setQueryName(e.target.value)}
                            required
                            fullWidth
                        />

                        <TextField
                            label="SQL"
                            value={querySql}
                            onChange={(e) => setQuerySql(e.target.value)}
                            required
                            multiline
                            rows={8}
                            fullWidth
                            placeholder="SELECT * FROM table WHERE ..."
                        />

                        <TextField
                            label="Description (optional)"
                            value={queryDescription}
                            onChange={(e) => setQueryDescription(e.target.value)}
                            multiline
                            rows={2}
                            fullWidth
                        />
                    </Box>
                )}

                {tabIndex === 1 && (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
                        <TextField
                            label="Folder Name"
                            value={folderName}
                            onChange={(e) => setFolderName(e.target.value)}
                            required
                            fullWidth
                            placeholder="my-queries"
                        />
                    </Box>
                )}
            </DialogContent>

            <DialogActions>
                <Button onClick={handleClose} disabled={saving}>
                    Cancel
                </Button>
                {tabIndex === 0 ? (
                    <Button
                        onClick={handleAddQuery}
                        variant="contained"
                        disabled={saving}
                    >
                        Add Query
                    </Button>
                ) : (
                    <Button
                        onClick={handleAddFolder}
                        variant="contained"
                        disabled={saving}
                    >
                        Add Folder
                    </Button>
                )}
            </DialogActions>
        </Dialog>
    );
};
