// src/main/ui/src/components/ResultsGrid.jsx

import React, { useMemo, useEffect } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { Box, Paper, Typography, CircularProgress, Alert } from '@mui/material';
import { useQuery } from '../hooks/useQuery';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-quartz.css';

export const ResultsGrid = () => {
    const { queryResults, isLoading, error, executionTime } = useQuery();

    // Debug logging
    useEffect(() => {
        console.log('ResultsGrid state:', {
            queryResults,
            isLoading,
            error,
            executionTime
        });
    }, [queryResults, isLoading, error, executionTime]);

    const columnDefs = useMemo(() => {
        if (!queryResults || !queryResults.columns || queryResults.columns.length === 0) {
            console.log('No columns available');
            return [];
        }

        console.log('Creating column definitions for:', queryResults.columns);

        return queryResults.columns.map(col => ({
            field: col,
            headerName: col,
            sortable: true,
            filter: true,
            resizable: true,
            flex: 1,
            minWidth: 100,
        }));
    }, [queryResults]);

    const rowData = useMemo(() => {
        const rows = queryResults?.rows || [];
        console.log('Row data:', rows.length, 'rows');
        return rows;
    }, [queryResults]);

    return (
        <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="subtitle2">
                    Results
                </Typography>
                {executionTime !== null && (
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                        Executed in {executionTime}ms • {rowData.length} rows
                    </Typography>
                )}
            </Box>

            {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                    {error}
                </Alert>
            )}

            {isLoading && (
                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1 }}>
                    <CircularProgress />
                    <Typography sx={{ ml: 2 }}>Executing query...</Typography>
                </Box>
            )}

            {!isLoading && !error && rowData.length === 0 && !queryResults && (
                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1 }}>
                    <Typography color="text.secondary">
                        No query executed yet. Enter a query and click Execute.
                    </Typography>
                </Box>
            )}

            {!isLoading && !error && queryResults && rowData.length === 0 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1 }}>
                    <Typography color="text.secondary">
                        Query executed successfully but returned no rows.
                    </Typography>
                </Box>
            )}

            {!isLoading && !error && rowData.length > 0 && (
                <Box
                    sx={{
                        flex: 1,
                        '& .ag-root': {
                            fontFamily: 'Roboto, sans-serif',
                        }
                    }}
                    className="ag-theme-quartz"
                >
                    <AgGridReact
                        columnDefs={columnDefs}
                        rowData={rowData}
                        pagination={true}
                        paginationPageSize={50}
                        domLayout="normal"
                    />
                </Box>
            )}
        </Box>
    );
};
