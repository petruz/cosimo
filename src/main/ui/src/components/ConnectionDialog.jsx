// src/main/ui/src/components/ConnectionDialog.jsx

import React, { useState, useEffect } from 'react';
import {
    Button,
    Dialog,
    TextField,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    CircularProgress,
    Alert,
    Checkbox,
    FormControlLabel,
    Grid,
    DialogTitle,
    DialogContent,
    DialogActions,
} from '@mui/material';

export const ConnectionDialog = ({
    open,
    onClose,
    onSave,
    initialData = null,
    isLoading = false,
    error = null,
    mode = 'new' // 'new' or 'edit'
}) => {
    const [formData, setFormData] = useState({
        connectionName: '',
        databaseType: 'postgres',
        host: 'localhost',
        port: 5432,
        database: 'postgres',
        username: 'postgres',
        password: '',
    });

    const [saveConnection, setSaveConnection] = useState(mode === 'edit');

    useEffect(() => {
        if (initialData) {
            setFormData({
                connectionName: initialData.name || '',
                databaseType: initialData.type || 'postgres',
                host: initialData.host || 'localhost',
                port: initialData.port || 5432,
                database: initialData.database || 'postgres',
                username: initialData.username || 'postgres',
                password: initialData.password || '',
            });
        } else {
            // Reset to defaults for new connection
            setFormData({
                connectionName: '',
                databaseType: 'postgres',
                host: 'localhost',
                port: 5432,
                database: 'postgres',
                username: 'postgres',
                password: '',
            });
            setSaveConnection(false);
        }
    }, [initialData, open]);

    const handleChange = (field) => (event) => {
        let value = event.target.value;

        // Automatically adjust port based on database type
        if (field === 'databaseType') {
            if (value === 'postgres') {
                setFormData({
                    ...formData,
                    databaseType: value,
                    port: 5432,
                });
                return;
            } else if (value === 'clickhouse') {
                setFormData({
                    ...formData,
                    databaseType: value,
                    port: 8123,
                });
                return;
            }
        }

        setFormData({
            ...formData,
            [field]: value,
        });
    };

    const handleSubmit = () => {
        onSave(formData, saveConnection);
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit();
        }
    };

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="md"
            fullWidth
        >
            <DialogTitle>
                {mode === 'edit' ? 'Edit Connection' : 'New Database Connection'}
            </DialogTitle>
            <DialogContent>
                {error && (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {error}
                    </Alert>
                )}

                {mode === 'new' && (
                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={saveConnection}
                                onChange={(e) => setSaveConnection(e.target.checked)}
                                disabled={isLoading}
                            />
                        }
                        label="Save this connection"
                        sx={{ mb: 2 }}
                    />
                )}

                {(saveConnection || mode === 'edit') && (
                    <TextField
                        fullWidth
                        label="Connection Name"
                        value={formData.connectionName}
                        onChange={handleChange('connectionName')}
                        onKeyPress={handleKeyPress}
                        margin="dense"
                        disabled={isLoading}
                        required
                        sx={{ mb: 2 }}
                    />
                )}

                <Grid container spacing={2}>
                    <Grid item xs={6}>
                        <FormControl fullWidth margin="dense">
                            <InputLabel>Database Type</InputLabel>
                            <Select
                                value={formData.databaseType}
                                onChange={handleChange('databaseType')}
                                label="Database Type"
                                disabled={isLoading}
                            >
                                <MenuItem value="postgres">PostgreSQL</MenuItem>
                                <MenuItem value="clickhouse">ClickHouse</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={6}>
                        <TextField
                            fullWidth
                            label="Database"
                            value={formData.database}
                            onChange={handleChange('database')}
                            onKeyPress={handleKeyPress}
                            margin="dense"
                            disabled={isLoading}
                        />
                    </Grid>
                    <Grid item xs={8}>
                        <TextField
                            fullWidth
                            label="Host"
                            value={formData.host}
                            onChange={handleChange('host')}
                            onKeyPress={handleKeyPress}
                            margin="dense"
                            disabled={isLoading}
                        />
                    </Grid>
                    <Grid item xs={4}>
                        <TextField
                            fullWidth
                            label="Port"
                            type="number"
                            value={formData.port}
                            onChange={handleChange('port')}
                            onKeyPress={handleKeyPress}
                            margin="dense"
                            disabled={isLoading}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <TextField
                            fullWidth
                            label="Username"
                            value={formData.username}
                            onChange={handleChange('username')}
                            onKeyPress={handleKeyPress}
                            margin="dense"
                            disabled={isLoading}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <TextField
                            fullWidth
                            label="Password"
                            type="password"
                            value={formData.password}
                            onChange={handleChange('password')}
                            onKeyPress={handleKeyPress}
                            margin="dense"
                            disabled={isLoading}
                        />
                    </Grid>
                </Grid>
            </DialogContent>
            <DialogActions>
                <Button
                    onClick={onClose}
                    disabled={isLoading}
                >
                    Cancel
                </Button>
                <Button
                    variant="contained"
                    onClick={handleSubmit}
                    disabled={isLoading}
                >
                    {isLoading ? (
                        <CircularProgress size={24} color="inherit" />
                    ) : (
                        mode === 'edit' ? 'Save' : 'Connect'
                    )}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
