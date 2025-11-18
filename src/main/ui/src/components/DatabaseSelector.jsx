// src/main/ui/src/components/DatabaseSelector.jsx

import React, { useState } from 'react';
import { useDatabase } from '../hooks/useDatabase';
import { useQuery } from '../hooks/useQuery';
import { ConnectionDialog } from './ConnectionDialog';
import axios from 'axios';

export const DatabaseSelector = () => {
    const { connect, isLoading, showConnectionDialog, setShowConnectionDialog } = useDatabase();
    const { reloadConnectionsCallback, setSelectedConnectionId } = useQuery();
    const [localError, setLocalError] = useState(null);

    const handleSave = async (formData, shouldSave) => {
        setLocalError(null);

        // Validate connection name if saving
        if (shouldSave && !formData.connectionName.trim()) {
            setLocalError('Connection name is required when saving');
            return;
        }

        try {
            let savedConnectionId = null;

            // First, save the connection if requested
            if (shouldSave) {
                const response = await axios.post('/api/v1/connections', {
                    name: formData.connectionName,
                    type: formData.databaseType,
                    host: formData.host,
                    port: formData.port.toString(),
                    database: formData.database,
                    username: formData.username,
                    password: formData.password
                });

                savedConnectionId = response.data.connectionId;

                // Reload saved connections to show in the dropdown
                if (reloadConnectionsCallback) {
                    await reloadConnectionsCallback();
                }
            }

            // Then connect
            await connect(formData);

            // If connection was saved, select it in the dropdown
            if (savedConnectionId && setSelectedConnectionId) {
                setSelectedConnectionId(savedConnectionId);
            }

            // Connection successful - close dialog
            setShowConnectionDialog(false);

        } catch (error) {
            setLocalError(error.response?.data?.message || error.response?.data?.error || error.message);
        }
    };

    const handleClose = () => {
        setShowConnectionDialog(false);
        setLocalError(null);
    };

    return (
        <ConnectionDialog
            open={showConnectionDialog}
            onClose={handleClose}
            onSave={handleSave}
            isLoading={isLoading}
            error={localError}
            mode="new"
        />
    );
};
