// src/App.jsx

import React, { useState } from 'react';
import { Box, IconButton, Tooltip } from '@mui/material';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { AppProvider } from './context/AppContext';
import { MenuBar } from './components/MenuBar';
import { QueryEditor } from './components/QueryEditor';
import { ExecuteButton } from './components/ExecuteButton';
import { ResultsGrid } from './components/ResultsGrid';
import { QueryLibrary } from './components/QueryLibrary';
import './styles/App.css';
// Definizione tema earth per Material-UI
const earthTheme = createTheme({
    palette: {
        primary: { main: '#714329' },
        secondary: { main: '#B08463' },
        background: {
            default: '#D0B9A7',
            paper: '#f5f5f5'
        },
        text: {
            primary: '#2c2c2c',
            secondary: '#f5f5f5'
        },
        divider: '#B08463',
        error: { main: '#c97777' },
        warning: { main: '#d4a574' },
        info: { main: '#8aa8c4' },
        success: { main: '#6b9b6b' }
    }
});

function AppContent() {
    const [leftPanelWidth, setLeftPanelWidth] = useState(300); // Query Library width
    const [editorHeight, setEditorHeight] = useState(300); // Query Editor height
    const [showEditor, setShowEditor] = useState(true);
    const [isResizingLeft, setIsResizingLeft] = useState(false);
    const [isResizingEditor, setIsResizingEditor] = useState(false);

    const handleMouseDownLeft = (e) => {
        e.preventDefault();
        setIsResizingLeft(true);
    };

    const handleMouseDownEditor = (e) => {
        e.preventDefault();
        setIsResizingEditor(true);
    };

    const handleMouseMove = (e) => {
        if (isResizingLeft) {
            const newWidth = e.clientX;
            if (newWidth > 200 && newWidth < 600) {
                setLeftPanelWidth(newWidth);
            }
        }
        if (isResizingEditor) {
            const menuBarHeight = 64; // AppBar height
            const newHeight = e.clientY - menuBarHeight;
            if (newHeight > 100 && newHeight < window.innerHeight - 300) {
                setEditorHeight(newHeight);
            }
        }
    };

    const handleMouseUp = () => {
        setIsResizingLeft(false);
        setIsResizingEditor(false);
    };

    React.useEffect(() => {
        if (isResizingLeft || isResizingEditor) {
            document.addEventListener('mousemove', handleMouseMove);
            document.addEventListener('mouseup', handleMouseUp);
            return () => {
                document.removeEventListener('mousemove', handleMouseMove);
                document.removeEventListener('mouseup', handleMouseUp);
            };
        }
    }, [isResizingLeft, isResizingEditor]);

    return (
        <Box sx={{
            display: 'flex',
            flexDirection: 'column',
            height: '100vh',
            overflow: 'hidden',
            backgroundColor: 'var(--results-bg)'
        }}>
            <MenuBar />

            <Box sx={{
                display: 'flex',
                flex: 1,
                overflow: 'hidden',
                backgroundColor: 'var(--results-bg)'
            }}>
                {/* Left Panel: Query Library */}
                <Box
                    sx={{
                        width: `${leftPanelWidth}px`,
                        minWidth: '200px',
                        maxWidth: '600px',
                        display: 'flex',
                        flexDirection: 'column',
                        borderRight: 1,
                        borderColor: 'divider',
                        overflow: 'hidden'
                    }}
                >
                    <QueryLibrary />
                </Box>

                {/* Vertical Resizer for Left Panel */}
                <Box
                    onMouseDown={handleMouseDownLeft}
                    sx={{
                        width: '4px',
                        cursor: 'col-resize',
                        backgroundColor: 'divider',
                        '&:hover': {
                            backgroundColor: 'primary.main',
                        },
                        userSelect: 'none'
                    }}
                />

                {/* Right Panel: Query Editor and Results */}
                <Box sx={{
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden',
                    backgroundColor: 'var(--results-bg)'
                }}>
                    {/* Query Editor Section (collapsible) */}
                    {showEditor && (
                        <>
                            <Box
                                sx={{
                                    height: `${editorHeight}px`,
                                    minHeight: '100px',
                                    display: 'flex',
                                    flexDirection: 'column',
                                    overflow: 'hidden',
                                    borderBottom: 1,
                                    borderColor: 'divider',
                                    backgroundColor: 'var(--results-bg)'
                                }}
                            >
                                <Box sx={{
                                    p: 1,
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'center',
                                    backgroundColor: 'var(--results-bg)'
                                }}>
                                    <ExecuteButton />
                                    <Tooltip title="Hide Editor">
                                        <IconButton size="small" onClick={() => setShowEditor(false)}>
                                            <ExpandLessIcon />
                                        </IconButton>
                                    </Tooltip>
                                </Box>
                                <Box sx={{ flex: 1, overflow: 'hidden' }}>
                                    <QueryEditor />
                                </Box>
                            </Box>

                            {/* Horizontal Resizer for Editor */}
                            <Box
                                onMouseDown={handleMouseDownEditor}
                                sx={{
                                    height: '4px',
                                    cursor: 'row-resize',
                                    backgroundColor: 'divider',
                                    '&:hover': {
                                        backgroundColor: 'primary.main',
                                    },
                                    userSelect: 'none'
                                }}
                            />
                        </>
                    )}

                    {/* Show Editor Button (when collapsed) */}
                    {!showEditor && (
                        <Box sx={{
                            p: 1,
                            borderBottom: 1,
                            borderColor: 'divider',
                            display: 'flex',
                            alignItems: 'center',
                            backgroundColor: 'var(--results-bg)'
                        }}>
                            <ExecuteButton />
                            <Tooltip title="Show Editor">
                                <IconButton size="small" onClick={() => setShowEditor(true)} sx={{ ml: 1 }}>
                                    <ExpandMoreIcon />
                                </IconButton>
                            </Tooltip>
                        </Box>
                    )}

                    {/* Results Grid Section */}
                    <Box sx={{
                        flex: 1,
                        overflow: 'hidden',
                        backgroundColor: 'var(--results-bg)'
                    }}>
                        <ResultsGrid />
                    </Box>
                </Box>
            </Box>
        </Box>
    );
}

export default function App() {
    return (
        <ThemeProvider theme={earthTheme}>
            <AppProvider>
                <AppContent />
            </AppProvider>
        </ThemeProvider>
    );
}
