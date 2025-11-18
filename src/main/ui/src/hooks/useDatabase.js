// src/main/ui/src/hooks/useDatabase.js

import { useContext } from 'react';
import { AppContext } from '../context/AppContext';
import { databaseService } from '../services/databaseService';

export const useDatabase = () => {
    const context = useContext(AppContext);

    const connect = async (config) => {
        try {
            console.log('Starting connection...', config);
            context.setIsLoading(true);
            context.setErrorState(null);

            const result = await databaseService.connect(config);
            console.log('Connection result:', result);

            context.updateCurrentConnection(result.connectionId);
            await refreshConnections();

            console.log('Resetting loading state...');
            context.setIsLoading(false);
            console.log('Loading state reset complete');

            return result;
        } catch (error) {
            console.error('Connection error:', error);
            context.setIsLoading(false);
            context.setErrorState(error.response?.data?.message || error.message);
            throw error;
        }
    };

    const refreshConnections = async () => {
        try {
            const data = await databaseService.listConnections();
            context.updateConnections(data.connections);
            context.updateCurrentConnection(data.currentConnectionId);
        } catch (error) {
            console.error('Error refreshing connections:', error);
        }
    };

    const getMetrics = async () => {
        try {
            const metrics = await databaseService.getMetrics();
            context.setConnectionMetrics(metrics);
            return metrics;
        } catch (error) {
            console.error('Error getting metrics:', error);
            return null;
        }
    };

    const disconnect = async (connectionId) => {
        try {
            await databaseService.disconnectConnection(connectionId);
            await refreshConnections();
        } catch (error) {
            context.setErrorState(error.message);
            throw error;
        }
    };

    return {
        connect,
        refreshConnections,
        getMetrics,
        disconnect,
        ...context,
    };
};
