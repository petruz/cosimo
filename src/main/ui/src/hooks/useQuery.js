// src/main/ui/src/hooks/useQuery.js

import { useContext } from 'react';
import { AppContext } from '../context/AppContext';
import { queryService } from '../services/queryService';

export const useQuery = () => {
    const context = useContext(AppContext);

    const executeQuery = async (sql, connectionId = null) => {
        try {
            console.log('=== useQuery.executeQuery START ===');
            console.log('SQL:', sql);
            console.log('Connection ID:', connectionId);

            context.setIsLoading(true);
            context.setErrorState(null);

            // Call the API
            const result = await queryService.executeQuery(sql, connectionId);

            console.log('=== useQuery.executeQuery RECEIVED RESULT ===');
            console.log('Full result object:', result);
            console.log('result.success:', result.success);
            console.log('result.data:', result.data);
            console.log('result.data TYPE:', typeof result.data);
            console.log('result.data is string?', typeof result.data === 'string');
            console.log('result.executionTimeMs:', result.executionTimeMs);
            console.log('result.executionTimeMs TYPE:', typeof result.executionTimeMs);

            if (result.success) {
                console.log('Query successful, calling context.executeQuery with:');
                console.log('- data:', result.data);
                console.log('- data type:', typeof result.data);
                console.log('- executionTimeMs:', result.executionTimeMs);
                console.log('- executionTimeMs type:', typeof result.executionTimeMs);

                // CRITICAL: Pass data object and execution time in correct order
                context.executeQuery(result.data, result.executionTimeMs);

                console.log('Context updated successfully');
            } else {
                console.error('Query failed:', result.message);
                context.setErrorState(result.message || 'Query execution failed');
            }

            console.log('=== useQuery.executeQuery END ===');
            return result;

        } catch (error) {
            console.error('=== useQuery.executeQuery ERROR ===');
            console.error('Error object:', error);
            console.error('Error response:', error.response);
            console.error('Error response data:', error.response?.data);

            const errorMessage = error.response?.data?.message
                || error.response?.data?.error
                || error.message
                || 'Unknown error occurred';

            console.error('Final error message:', errorMessage);
            context.setErrorState(errorMessage);
            throw error;
        }
    };

    const explainQuery = async (sql, connectionId = null) => {
        try {
            console.log('=== useQuery.explainQuery START ===');
            console.log('SQL:', sql);

            context.setIsLoading(true);
            context.setErrorState(null);

            const result = await queryService.explainQuery(sql, connectionId);

            console.log('EXPLAIN result:', result);

            if (result.success) {
                context.executeQuery(result.data, result.executionTimeMs);
                context.setShowExplainResults(true);
            } else {
                context.setErrorState(result.message || 'EXPLAIN execution failed');
            }

            return result;
        } catch (error) {
            console.error('EXPLAIN execution error:', error);

            const errorMessage = error.response?.data?.message
                || error.response?.data?.error
                || error.message
                || 'Unknown error occurred';

            context.setErrorState(errorMessage);
            throw error;
        }
    };

    return {
        ...context,
        executeQuery,  // Override context.executeQuery with our wrapper
        explainQuery,
    };
};
