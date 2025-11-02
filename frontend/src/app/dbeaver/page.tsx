'use client';

import React, { useState, useEffect } from 'react';
import {
  Box,
  Container,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Alert,
  CircularProgress,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  Storage as StorageIcon,
  Refresh as RefreshIcon,
  Download as DownloadIcon,
  ContentCopy as CopyIcon,
  ExpandMore as ExpandMoreIcon,
  TableChart as TableIcon,
  Key as KeyIcon,
} from '@mui/icons-material';
import DashboardHeader from '@/components/DashboardHeader';
import { useConfig } from '@/hooks/useConfig';

interface DatabaseConnection {
  connectionName: string;
  provider: string;
  driver: string;
  host: string;
  port: string;
  database: string;
  username: string;
  jdbcUrl: string;
  sslMode: string;
  hasPassword: boolean;
}

interface ColumnInfo {
  columnName: string;
  dataType: string;
  nullable: boolean;
  defaultValue: string | null;
  primaryKey: boolean;
}

interface TableInfo {
  tableName: string;
  tableType: string;
  rowCount: number | null;
  columns: ColumnInfo[];
}

interface DatabaseSchema {
  schemaName: string;
  tables: TableInfo[];
}

export default function DBeaverPage() {
  const [connection, setConnection] = useState<DatabaseConnection | null>(null);
  const [schemas, setSchemas] = useState<DatabaseSchema[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [copySuccess, setCopySuccess] = useState('');
  const config = useConfig();

  useEffect(() => {
    if (config) {
      fetchData();
    }
  }, [config]);

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const token = localStorage.getItem('token');
      const headers: HeadersInit = token ? { 'Authorization': `Bearer ${token}` } : {};

      const [connectionRes, schemasRes] = await Promise.all([
        fetch(`${config.API_URL}/api/dbeaver/connection`, { headers }),
        fetch(`${config.API_URL}/api/dbeaver/schemas`, { headers }),
      ]);

      if (connectionRes.ok) {
        const connData = await connectionRes.json();
        setConnection(connData);
      }

      if (schemasRes.ok) {
        const schemasData = await schemasRes.json();
        setSchemas(schemasData);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load database information');
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadConfig = async () => {
    try {
      const token = localStorage.getItem('token');
      const headers: HeadersInit = token ? { 'Authorization': `Bearer ${token}` } : {};

      const response = await fetch(`${config.API_URL}/api/dbeaver/connection/export`, { headers });
      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'pulseops-connection.xml';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      }
    } catch (err) {
      setError('Failed to download connection config');
    }
  };

  const handleCopy = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    setCopySuccess(`${label} copied to clipboard!`);
    setTimeout(() => setCopySuccess(''), 3000);
  };

  if (loading && !connection) {
    return (
      <Box sx={{ minHeight: "100vh", background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)" }}>
        <DashboardHeader />
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="80vh">
          <CircularProgress sx={{ color: "white" }} />
        </Box>
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: "100vh", background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)" }}>
      <DashboardHeader />

      <Container maxWidth="xl" sx={{ mt: 4, pb: 4 }}>
        {/* Header Section */}
        <Box sx={{ mb: 3, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <StorageIcon sx={{ fontSize: 40, color: "white" }} />
            <Typography variant="h4" sx={{ color: "white", fontWeight: "bold" }}>
              DBeaver Database Manager
            </Typography>
          </Box>
          <Box sx={{ display: "flex", gap: 2 }}>
            <Button
              variant="contained"
              startIcon={<DownloadIcon />}
              onClick={handleDownloadConfig}
              disabled={!connection}
              sx={{ bgcolor: "white", color: "#667eea", "&:hover": { bgcolor: "#f5f5f5" } }}
            >
              Download Config
            </Button>
            <Button
              variant="contained"
              startIcon={<RefreshIcon />}
              onClick={fetchData}
              disabled={loading}
              sx={{ bgcolor: "white", color: "#667eea", "&:hover": { bgcolor: "#f5f5f5" } }}
            >
              Refresh
            </Button>
          </Box>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {copySuccess && (
          <Alert severity="success" sx={{ mb: 3 }}>
            {copySuccess}
          </Alert>
        )}

        {/* Connection Info Cards */}
        {connection && (
          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} md={4}>
              <Card sx={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', color: 'white', height: '100%' }}>
                <CardContent>
                  <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <Box>
                      <Typography variant="h6" sx={{ mb: 1 }}>Connection Name</Typography>
                      <Typography variant="h5" sx={{ fontWeight: "bold" }}>
                        {connection.connectionName}
                      </Typography>
                      <Typography variant="caption" sx={{ opacity: 0.9 }}>
                        {connection.provider.toUpperCase()} Database
                      </Typography>
                    </Box>
                    <StorageIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={4}>
              <Card sx={{ background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', color: 'white', height: '100%' }}>
                <CardContent>
                  <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <Box>
                      <Typography variant="h6" sx={{ mb: 1 }}>Database Host</Typography>
                      <Typography variant="h5" sx={{ fontWeight: "bold" }}>
                        {connection.host}
                      </Typography>
                      <Typography variant="caption" sx={{ opacity: 0.9 }}>
                        Port: {connection.port}
                      </Typography>
                    </Box>
                    <StorageIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={4}>
              <Card sx={{ background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', color: 'white', height: '100%' }}>
                <CardContent>
                  <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <Box>
                      <Typography variant="h6" sx={{ mb: 1 }}>Schema Count</Typography>
                      <Typography variant="h5" sx={{ fontWeight: "bold" }}>
                        {schemas.length}
                      </Typography>
                      <Typography variant="caption" sx={{ opacity: 0.9 }}>
                        Total Tables: {schemas.reduce((sum, s) => sum + s.tables.length, 0)}
                      </Typography>
                    </Box>
                    <TableIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Connection Details */}
        {connection && (
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ fontWeight: "bold", mb: 2 }}>
                Connection Details
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">JDBC URL</Typography>
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <Typography variant="body1" sx={{ fontFamily: 'monospace', fontSize: '0.85rem', wordBreak: 'break-all' }}>
                        {connection.jdbcUrl}
                      </Typography>
                      <Tooltip title="Copy JDBC URL">
                        <IconButton size="small" onClick={() => handleCopy(connection.jdbcUrl, 'JDBC URL')}>
                          <CopyIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  </Box>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">Username</Typography>
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                        {connection.username}
                      </Typography>
                      <Tooltip title="Copy Username">
                        <IconButton size="small" onClick={() => handleCopy(connection.username, 'Username')}>
                          <CopyIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  </Box>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary">Database Name</Typography>
                    <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>{connection.database}</Typography>
                  </Box>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary">Driver</Typography>
                    <Typography variant="body1">{connection.driver}</Typography>
                  </Box>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary">SSL Mode</Typography>
                    <Chip label={connection.sslMode} color="success" size="small" />
                  </Box>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        )}

        {/* Schema Browser */}
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom sx={{ fontWeight: "bold", mb: 2 }}>
              Database Schema Explorer
            </Typography>
            {schemas.map((schema) => (
              <Accordion key={schema.schemaName} sx={{ mb: 1 }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 2, width: '100%' }}>
                    <StorageIcon color="primary" />
                    <Typography variant="subtitle1" sx={{ fontWeight: 500 }}>
                      {schema.schemaName}
                    </Typography>
                    <Chip label={`${schema.tables.length} tables`} size="small" color="primary" variant="outlined" />
                  </Box>
                </AccordionSummary>
                <AccordionDetails>
                  {schema.tables.map((table) => (
                    <Accordion key={table.tableName} sx={{ mb: 1 }}>
                      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                          <TableIcon fontSize="small" />
                          <Typography variant="body1">{table.tableName}</Typography>
                          <Chip label={table.tableType} size="small" />
                          {table.rowCount !== null && (
                            <Chip label={`${table.rowCount} rows`} size="small" variant="outlined" />
                          )}
                        </Box>
                      </AccordionSummary>
                      <AccordionDetails>
                        <TableContainer>
                          <Table size="small">
                            <TableHead>
                              <TableRow>
                                <TableCell><strong>Column</strong></TableCell>
                                <TableCell><strong>Type</strong></TableCell>
                                <TableCell><strong>Nullable</strong></TableCell>
                                <TableCell><strong>Default</strong></TableCell>
                                <TableCell><strong>Primary Key</strong></TableCell>
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {table.columns.map((column) => (
                                <TableRow key={column.columnName}>
                                  <TableCell>
                                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                                      {column.primaryKey && <KeyIcon fontSize="small" color="primary" />}
                                      {column.columnName}
                                    </Box>
                                  </TableCell>
                                  <TableCell>
                                    <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                                      {column.dataType}
                                    </Typography>
                                  </TableCell>
                                  <TableCell>
                                    <Chip 
                                      label={column.nullable ? 'YES' : 'NO'} 
                                      size="small" 
                                      color={column.nullable ? 'default' : 'secondary'}
                                    />
                                  </TableCell>
                                  <TableCell>
                                    <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                                      {column.defaultValue || '-'}
                                    </Typography>
                                  </TableCell>
                                  <TableCell>
                                    {column.primaryKey && <Chip label="PK" size="small" color="primary" />}
                                  </TableCell>
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      </AccordionDetails>
                    </Accordion>
                  ))}
                </AccordionDetails>
              </Accordion>
            ))}
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
}
