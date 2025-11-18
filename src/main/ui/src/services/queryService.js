// src/main/ui/src/services/queryService.js

import api from './api';

/**
 * Query execution service
 */
export const queryService = {
    /**
     * Execute a SELECT query without modification
     */
    executeQuery: async (sql, connectionId = null) => {
        console.log('queryService.executeQuery called with:', { sql, connectionId });

        const requestBody = {
            sql,
            connectionId,
        };

        console.log('Request body:', requestBody);

        const response = await api.post('/query/execute', requestBody);

        console.log('API Response:', response.data);
        console.log('Response structure:', {
            success: response.data.success,
            hasData: !!response.data.data,
            dataColumns: response.data.data?.columns,
            dataRowCount: response.data.data?.rowCount,
            executionTimeMs: response.data.executionTimeMs,
            databaseType: response.data.databaseType
        });

        return response.data;
    },

    /**
     * Execute an INSERT/UPDATE/DELETE statement
     */
    executeUpdate: async (sql, connectionId = null) => {
        console.log('queryService.executeUpdate called with:', { sql, connectionId });

        const response = await api.post('/query/update', {
            sql,
            connectionId,
        });

        console.log('Update API Response:', response.data);
        return response.data;
    },

    /**
     * Execute EXPLAIN query (PostgreSQL EXPLAIN ANALYZE or ClickHouse EXPLAIN)
     */
    explainQuery: async (sql, connectionId = null) => {
        console.log('queryService.explainQuery called with:', { sql, connectionId });

        const response = await api.post('/query/explain', {
            sql,
            connectionId,
        });

        console.log('EXPLAIN API Response:', response.data);
        return response.data;
    },
};
