// src/main/ui/src/components/MenuBar.jsx

import React, { useState, useRef } from 'react';
import {
    AppBar,
    Toolbar,
    Typography,
    Button,
    Menu,
    MenuItem,
    Box,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions
} from '@mui/material';
import DatabaseIcon from '@mui/icons-material/Storage';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import PaletteIcon from '@mui/icons-material/Palette';
import { useDatabase } from '../hooks/useDatabase';
import { DatabaseSelector } from './DatabaseSelector';
import { ConnectionStatus } from './ConnectionStatus';

export const MenuBar = () => {
    const [connectionMenuAnchor, setConnectionMenuAnchor] = useState(null);
    const [fileMenuAnchor, setFileMenuAnchor] = useState(null);
    const [themeMenuAnchor, setThemeMenuAnchor] = useState(null);
    const [aboutOpen, setAboutOpen] = useState(false);
    const fileInputRef = useRef();
    const { setShowConnectionDialog, isConnected } = useDatabase();

    // Salva Query Library (download XML)

    // New SQL library (creates empty queries.xml with one "New" folder)
    const handleNewSqlLibrary = async () => {
        if (!window.confirm('Create a new SQL library? Current queries will be replaced.')) {
            setFileMenuAnchor(null);
            return;
        }
        try {
            const emptyLibrary = '<?xml version="1.0" encoding="UTF-8"?>\n<queries>\n    <folder name="New"/>\n</queries>';
            const response = await fetch('/api/v1/queries/xml', {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: emptyLibrary
            });
            if (response.ok) {
                // Force complete reload with cache clear
                window.location.href = window.location.href.split('?')[0] + '?t=' + Date.now();
            } else {
                alert('Failed to create new SQL library');
            }
        } catch (err) {
            alert('Failed to create new SQL library');
        }
        setFileMenuAnchor(null);
    };

    const handleSaveQueryLibrary = async () => {
        try {
            const response = await fetch('/api/v1/queries/xml');
            const xml = await response.text();
            const blob = new Blob([xml], { type: 'application/xml' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'queries.xml';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        } catch (err) {
            alert('Failed to export queries.xml');
        }
        setFileMenuAnchor(null);
    };

    // Save as SQL library (download with custom name)
    const handleSaveAsQueryLibrary = async () => {
        const filename = prompt('Enter filename:', 'queries.xml');
        if (!filename) {
            setFileMenuAnchor(null);
            return;
        }
        try {
            const response = await fetch('/api/v1/queries/xml');
            const xml = await response.text();
            const blob = new Blob([xml], { type: 'application/xml' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename.endsWith('.xml') ? filename : filename + '.xml';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        } catch (err) {
            alert('Failed to export queries.xml');
        }
        setFileMenuAnchor(null);
    };

    // Carica Query Library (upload XML)
    const handleLoadQueryLibrary = () => {
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
            fileInputRef.current.click();
        }
        setFileMenuAnchor(null);
    };

    const handleFileChange = async (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const formData = new FormData();
        formData.append('file', file);
        try {
            const response = await fetch('/api/v1/queries/xml', {
                method: 'POST',
                body: formData
            });
            if (response.ok) {
                // Force complete reload with cache clear
                window.location.href = window.location.href.split('?')[0] + '?t=' + Date.now();
            } else {
                alert('Failed to import queries.xml');
            }
        } catch (err) {
            alert('Failed to import queries.xml');
        }
    };

    // About dialog
    const handleAboutOpen = () => setAboutOpen(true);
    const handleAboutClose = () => setAboutOpen(false);

    // Theme menu
    const handleThemeMenuOpen = (event) => setThemeMenuAnchor(event.currentTarget);
    const handleThemeMenuClose = () => setThemeMenuAnchor(null);

    const handleThemeChange = (theme) => {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('app-theme', theme);
        handleThemeMenuClose();
    };

    // File menu
    const handleFileMenuOpen = (event) => setFileMenuAnchor(event.currentTarget);
    const handleFileMenuClose = () => setFileMenuAnchor(null);

    // Connection menu
    const handleConnectionMenuOpen = (event) => setConnectionMenuAnchor(event.currentTarget);
    const handleConnectionMenuClose = () => setConnectionMenuAnchor(null);

    const handleNewConnection = () => {
        setShowConnectionDialog(true);
        handleConnectionMenuClose();
    };

    return (
        <AppBar position="static" elevation={1}>
            <Toolbar>
                <DatabaseIcon sx={{ mr: 2 }} />
                <Typography variant="h6" component="div" sx={{ flexGrow: 0, mr: 3 }}>
                    Query Debug App
                </Typography>

                {/* File Menu */}
                <Button color="inherit" onClick={handleFileMenuOpen} sx={{ mr: 2 }}>
                    File
                </Button>
                <Menu
                    anchorEl={fileMenuAnchor}
                    open={Boolean(fileMenuAnchor)}
                    onClose={handleFileMenuClose}
                >
                    <MenuItem onClick={handleNewSqlLibrary}>New</MenuItem>
                    <MenuItem onClick={handleSaveQueryLibrary}>Save</MenuItem>
                    <MenuItem onClick={handleSaveAsQueryLibrary}>Save as...</MenuItem>
                    <MenuItem onClick={handleLoadQueryLibrary}>Load</MenuItem>
                </Menu>
                <input
                    type="file"
                    accept=".xml"
                    style={{ display: 'none' }}
                    ref={fileInputRef}
                    onChange={handleFileChange}
                />

                {/* Connection Menu */}
                <Button
                    color="inherit"
                    onClick={handleConnectionMenuOpen}
                    endIcon={<KeyboardArrowDownIcon />}
                >
                    Connection
                </Button>
                <Menu
                    anchorEl={connectionMenuAnchor}
                    open={Boolean(connectionMenuAnchor)}
                    onClose={handleConnectionMenuClose}
                >
                    <MenuItem onClick={handleNewConnection}>
                        New Connection...
                    </MenuItem>
                    {isConnected && (
                        <MenuItem onClick={handleConnectionMenuClose}>
                            Disconnect
                        </MenuItem>
                    )}
                </Menu>

                {/* Theme Menu */}
                <Button
                    color="inherit"
                    onClick={handleThemeMenuOpen}
                    endIcon={<KeyboardArrowDownIcon />}
                    sx={{ ml: 2 }}
                >
                    <PaletteIcon sx={{ mr: 1, fontSize: 20 }} />
                    Theme
                </Button>
                <Menu
                    anchorEl={themeMenuAnchor}
                    open={Boolean(themeMenuAnchor)}
                    onClose={handleThemeMenuClose}
                >
                    <MenuItem onClick={() => handleThemeChange('dark')}>
                        Default Dark
                    </MenuItem>
                    <MenuItem onClick={() => handleThemeChange('floral')}>
                        Floral Garden
                    </MenuItem>
                    <MenuItem onClick={() => handleThemeChange('earth')}>
                        Warm Earth
                    </MenuItem>
                </Menu>

                <Button color="inherit" onClick={handleAboutOpen} sx={{ ml: 2 }}>
                    About
                </Button>

                <Box sx={{ flexGrow: 1 }} />

                {/* Connection Status in the right side */}
                <Box sx={{ minWidth: 200 }}>
                    <ConnectionStatus compact />
                </Box>
            </Toolbar>

            {/* About Dialog */}
            <Dialog open={aboutOpen} onClose={handleAboutClose}>
                <DialogTitle>About</DialogTitle>
                <DialogContent>
                    <Typography variant="h6">Query Debug App</Typography>
                    <Typography variant="body2" sx={{ mt: 1 }}>© 2025</Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleAboutClose}>Close</Button>
                </DialogActions>
            </Dialog>

            {/* Hidden DatabaseSelector component that provides the dialog */}
            <DatabaseSelector />
        </AppBar>
    );
};
