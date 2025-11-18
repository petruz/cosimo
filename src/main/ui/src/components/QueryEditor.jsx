// src/components/QueryEditor.jsx

import React, { useState, useEffect } from 'react';
import { Box, Typography, IconButton, Tooltip, CircularProgress } from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import Editor from '@monaco-editor/react';
import { useQuery } from '../hooks/useQuery';
import axios from 'axios';

export const QueryEditor = () => {
    const { currentQueryText, setCurrentQueryText, selectedQueryInfo, setSelectedQueryInfo, reloadQueriesCallback } = useQuery();
    const [isSaving, setIsSaving] = useState(false);
    const [currentTheme, setCurrentTheme] = useState('vs-dark');

    // Detect theme changes
    useEffect(() => {
        const updateTheme = () => {
            const theme = document.documentElement.getAttribute('data-theme') || 'dark';
            setCurrentTheme(theme === 'earth' ? 'warm-earth' : theme === 'floral' ? 'floral-garden' : 'vs-dark');
        };

        updateTheme();

        // Listen for theme changes
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'attributes' && mutation.attributeName === 'data-theme') {
                    updateTheme();
                }
            });
        });

        observer.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ['data-theme']
        });

        return () => observer.disconnect();
    }, []);

    // Check if the query has been modified
    const isModified = selectedQueryInfo &&
                      selectedQueryInfo.originalSql !== currentQueryText;

    const handleSave = async () => {
        if (!selectedQueryInfo || !isModified) return;

        try {
            setIsSaving(true);
            const response = await axios.put('/api/v1/queries/content', {
                folderName: selectedQueryInfo.folderName,
                queryName: selectedQueryInfo.queryName,
                sql: currentQueryText,
                description: selectedQueryInfo.description
            });

            if (response.data.success) {
                // Update the original SQL to mark as saved
                setSelectedQueryInfo({
                    ...selectedQueryInfo,
                    originalSql: currentQueryText
                });

                // Reload queries from backend to refresh the library
                if (reloadQueriesCallback) {
                    await reloadQueriesCallback();
                }
            } else {
                alert(response.data.error || 'Failed to save query');
            }
        } catch (err) {
            console.error('Error saving query:', err);
            alert(err.message || 'Failed to save query');
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Box
            sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                backgroundColor: 'var(--editor-bg)',
                p: 2
            }}
        >
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Typography variant="subtitle2" sx={{ color: 'var(--editor-text)' }}>
                    SQL Query {selectedQueryInfo && `- ${selectedQueryInfo.queryName}`}
                </Typography>
                {isModified && (
                    <Tooltip title="Save Query (Ctrl+S)">
                        <IconButton
                            size="small"
                            onClick={handleSave}
                            disabled={isSaving}
                            sx={{ color: 'var(--accent-warm)' }}
                        >
                            {isSaving ? <CircularProgress size={20} /> : <SaveIcon />}
                        </IconButton>
                    </Tooltip>
                )}
            </Box>
            <Box sx={{ flex: 1, minHeight: 0 }}>
                <Editor
                    height="100%"
                    language="sql"
                    value={currentQueryText}
                    onChange={(value) => setCurrentQueryText(value || '')}
                    theme={currentTheme}
                    options={{
                        minimap: { enabled: false },
                        fontSize: 13,
                        wordWrap: 'on',
                    }}
                    onMount={(editor, monaco) => {
                        // Define custom themes
                        monaco.editor.defineTheme('warm-earth', {
                            base: 'vs',
                            inherit: true,
                            rules: [
                                { token: 'keyword', foreground: '714329', fontStyle: 'bold' },
                                { token: 'string', foreground: '8a7355' },
                                { token: 'number', foreground: '714329' },
                                { token: 'comment', foreground: '9d8b7a', fontStyle: 'italic' }
                            ],
                            colors: {
                                'editor.background': '#B08463',
                                'editor.foreground': '#2c2c2c',
                                'editorLineNumber.foreground': '#6d5d4f',
                                'editor.selectionBackground': '#d0b9a799',
                                'editor.lineHighlightBackground': '#9d806555'
                            }
                        });

                        monaco.editor.defineTheme('floral-garden', {
                            base: 'vs',
                            inherit: true,
                            rules: [
                                { token: 'keyword', foreground: '677156', fontStyle: 'bold' },
                                { token: 'string', foreground: 'DBA48F' },
                                { token: 'number', foreground: 'D7C393' },
                                { token: 'comment', foreground: '8a8a8a', fontStyle: 'italic' }
                            ],
                            colors: {
                                'editor.background': '#EFEBCE',
                                'editor.foreground': '#2c2c2c',
                                'editorLineNumber.foreground': '#8a8a8a',
                                'editor.selectionBackground': '#D7C39355',
                                'editor.lineHighlightBackground': '#DBA48F22'
                            }
                        });

                        // Add keyboard shortcut for save (Ctrl+S)
                        editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => {
                            // Check modification state and save
                            handleSave();
                        });
                    }}
                />
            </Box>
        </Box>
    );
};
