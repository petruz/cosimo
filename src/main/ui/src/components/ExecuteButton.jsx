// src/main/ui/src/components/ExecuteButton.jsx

import React from 'react';
import { Button, Box, CircularProgress } from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import { useQuery } from '../hooks/useQuery';
import { useDatabase } from '../hooks/useDatabase';

export const ExecuteButton = () => {
    const { currentQueryText, executeQuery, explainQuery, isLoading } = useQuery();
    const { currentConnectionId } = useDatabase();

    const handleExecute = async () => {
        if (!currentConnectionId) {
            alert('Please establish a connection first');
            return;
        }
        if (!currentQueryText.trim()) {
            alert('Please enter a query');
            return;
        }

        console.log('ExecuteButton: Executing query');
        console.log('SQL:', currentQueryText);
        console.log('Connection ID:', currentConnectionId);

        // Execute with CORRECT parameter order: (sql, connectionId)
        await executeQuery(currentQueryText, currentConnectionId);
    };

    const handleExplain = async () => {
        if (!currentConnectionId) {
            alert('Please establish a connection first');
            return;
        }
        if (!currentQueryText.trim()) {
            alert('Please enter a query');
            return;
        }

        console.log('ExecuteButton: Executing EXPLAIN');
        await explainQuery(currentQueryText, currentConnectionId);
    };

    return (
        <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <Button
                variant="contained"
                color="primary"
                onClick={handleExecute}
                disabled={isLoading || !currentConnectionId}
                startIcon={isLoading ? <CircularProgress size={20} /> : <PlayArrowIcon />}
            >
                Execute (Ctrl+Enter)
            </Button>

            <Button
                variant="outlined"
                color="primary"
                onClick={handleExplain}
                disabled={isLoading || !currentConnectionId}
                startIcon={<TrendingUpIcon />}
            >
                Explain
            </Button>
        </Box>
    );
};
