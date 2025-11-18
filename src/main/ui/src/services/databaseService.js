// src/services/databaseService.js

import api from './api';

/**
 * Database connection management service
 */
export const databaseService = {
    /**
     * Connect to a database
     */
    connect: async (connectionConfig) => {
        const response = await api.post('/database/connect', {
            databaseType: connectionConfig.databaseType,
            host: connectionConfig.host,
            port: connectionConfig.port,
            database: connectionConfig.database,
            username: connectionConfig.username,
            password: connectionConfig.password,
        });
        return response.data;
    },

    /**
     * Get connection metrics
     */
    getMetrics: async () => {
        const response = await api.get('/database/metrics');
        return response.data;
    },

    /**
     * List all active connections
     */
    listConnections: async () => {
        const response = await api.get('/database/connections');
        return response.data;
    },

    /**
     * Select a connection to be current
     */
    selectConnection: async (connectionId) => {
        const response = await api.post(`/database/select/${connectionId}`);
        return response.data;
    },

    /**
     * Close a connection
     */
    disconnectConnection: async (connectionId) => {
        const response = await api.post(`/database/disconnect/${connectionId}`);
        return response.data;
    },

    /**
     * Close all connections
     */
    disconnectAll: async () => {
        const response = await api.post('/database/disconnect-all');
        return response.data;
    },
};
