// src/main/ui/src/context/AppContext.jsx

import React, { createContext, useState, useCallback } from 'react';

export const AppContext = createContext();

export const AppProvider = ({ children }) => {
    // Connection state
    const [connections, setConnections] = useState([]);
    const [currentConnectionId, setCurrentConnectionId] = useState(null);
    const [connectionMetrics, setConnectionMetrics] = useState(null);
    const [selectedConnectionId, setSelectedConnectionId] = useState(''); // For saved connections dropdown

    // Query state
    const [queries, setQueries] = useState([]);
    const [currentQueryId, setCurrentQueryId] = useState(null);
    const [currentQueryText, setCurrentQueryText] = useState('');
    const [selectedQueryInfo, setSelectedQueryInfo] = useState(null); // { folderName, queryName, originalSql }
    const [reloadQueriesCallback, setReloadQueriesCallback] = useState(null);
    const [reloadConnectionsCallback, setReloadConnectionsCallback] = useState(null);

    // Results state
    const [queryResults, setQueryResults] = useState(null);
    const [executionTime, setExecutionTime] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    // UI state
    const [showConnectionDialog, setShowConnectionDialog] = useState(false);
    const [showExplainResults, setShowExplainResults] = useState(false);

    // Update connections list
    const updateConnections = useCallback((conns) => {
        setConnections(conns);
    }, []);

    // Set current connection
    const updateCurrentConnection = useCallback((connId, metrics = null) => {
        setCurrentConnectionId(connId);
        if (metrics) {
            setConnectionMetrics(metrics);
        }
    }, []);

    // Execute query and update results
    // CRITICAL: results should be the data object, time should be a number
    const executeQuery = useCallback((results, time) => {
        console.log('AppContext.executeQuery called with:', { results, time });
        console.log('Results type:', typeof results);
        console.log('Time type:', typeof time);

        setQueryResults(results);
        setExecutionTime(time);
        setError(null);
        setIsLoading(false);
    }, []);

    // Set error
    const setErrorState = useCallback((errorMsg) => {
        setError(errorMsg);
        setIsLoading(false);
    }, []);

    const value = {
        // Connection state
        connections,
        currentConnectionId,
        connectionMetrics,
        updateConnections,
        updateCurrentConnection,
        setConnectionMetrics,
        selectedConnectionId,
        setSelectedConnectionId,

        // Query state
        queries,
        setQueries,
        currentQueryId,
        setCurrentQueryId,
        currentQueryText,
        setCurrentQueryText,
        selectedQueryInfo,
        setSelectedQueryInfo,
        reloadQueriesCallback,
        setReloadQueriesCallback,
        reloadConnectionsCallback,
        setReloadConnectionsCallback,

        // Results state
        queryResults,
        setQueryResults,
        executionTime,
        setExecutionTime,
        isLoading,
        setIsLoading,
        error,
        setErrorState,

        // UI state
        showConnectionDialog,
        setShowConnectionDialog,
        showExplainResults,
        setShowExplainResults,

        // Actions
        executeQuery,
    };

    return (
        <AppContext.Provider value={value}>
            {children}
        </AppContext.Provider>
    );
};
