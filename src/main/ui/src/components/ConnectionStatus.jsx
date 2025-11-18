// src/components/ConnectionStatus.jsx

import React, { useEffect } from 'react';
import { Box, Chip, CircularProgress, Paper, Typography } from '@mui/material';
import { useDatabase } from '../hooks/useDatabase';

export const ConnectionStatus = ({ compact = false }) => {
    const { currentConnectionId, connectionMetrics, connections } = useDatabase();

    // Removed automatic metrics polling - not needed for debug app
    // useEffect(() => {
    //     if (currentConnectionId) {
    //         getMetrics();
    //         const interval = setInterval(getMetrics, 5000);
    //         return () => clearInterval(interval);
    //     }
    // }, [currentConnectionId, getMetrics]);

    if (!currentConnectionId) {
        return (
            <Chip
                label="No Connection"
                color="error"
                variant="outlined"
                size={compact ? "small" : "medium"}
            />
        );
    }

    const connection = connections.find(c => c.id === currentConnectionId);

    // Compact mode for menu bar
    if (compact) {
        return (
            <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                <Chip
                    label={`${connection?.databaseType?.toUpperCase() || 'DB'}: ${connection?.database}`}
                    color="success"
                    size="small"
                />
                <Chip
                    label={`${connectionMetrics?.activeConnections || 0}/${connectionMetrics?.maximumPoolSize || '?'}`}
                    variant="outlined"
                    size="small"
                />
            </Box>
        );
    }

    // Full mode for main page
    return (
        <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
                Connection Status
            </Typography>

            <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
                <Chip
                    label={connection?.database}
                    color="success"
                    variant="outlined"
                />
                <Chip
                    label={`${connectionMetrics?.activeConnections || 0}/${connectionMetrics?.maximumPoolSize || '?'} active`}
                    variant="outlined"
                />
            </Box>

            {connectionMetrics && (
                <Box sx={{ fontSize: '0.85rem', color: 'text.secondary' }}>
                    <div>Pool: {connectionMetrics.poolName}</div>
                    <div>Idle: {connectionMetrics.idleConnections}</div>
                    <div>Pending: {connectionMetrics.pendingConnections}</div>
                </Box>
            )}
        </Paper>
    );
};
