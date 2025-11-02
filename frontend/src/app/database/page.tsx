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
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
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
  IconButton,
  Tabs,
  Tab,
  Pagination,
} from '@mui/material';
import { SimpleTreeView } from '@mui/x-tree-view/SimpleTreeView';
import { TreeItem } from '@mui/x-tree-view/TreeItem';
import {
  Storage as StorageIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
  ExpandMore as ExpandMoreIcon,
  ChevronRight as ChevronRightIcon,
  Folder as FolderIcon,
  TableChart as TableIcon,
  ViewColumn as ColumnIcon,
} from '@mui/icons-material';
import DashboardHeader from '@/components/DashboardHeader';
import { useConfig } from '@/hooks/useConfig';
import axios from 'axios';

interface DatabaseConnection {
  connectionId: string;
  connectionName: string;
  databaseType: string;
  host: string;
  port: number;
  databaseName: string;
  username: string;
  connected: boolean;
  message: string;
}

interface Schema {
  schemaName: string;
  tables: string[];
}

interface TableMetadata {
  tableName: string;
  schemaName: string;
  columns: ColumnMetadata[];
  rowCount: number;
}

interface ColumnMetadata {
  columnName: string;
  dataType: string;
  columnSize: number;
  nullable: boolean;
  primaryKey: boolean;
}

interface TableData {
  tableName: string;
  schemaName: string;
  columns: string[];
  rows: Record<string, any>[];
  totalRows: number;
  pageSize: number;
  currentPage: number;
}

export default function DatabaseManagementPage() {
  const [connections, setConnections] = useState<DatabaseConnection[]>([]);
  const [selectedConnection, setSelectedConnection] = useState<DatabaseConnection | null>(null);
  const [schemas, setSchemas] = useState<Schema[]>([]);
  const [selectedSchema, setSelectedSchema] = useState<string>('');
  const [selectedTable, setSelectedTable] = useState<string>('');
  const [tableMetadata, setTableMetadata] = useState<TableMetadata | null>(null);
  const [tableData, setTableData] = useState<TableData | null>(null);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [tabValue, setTabValue] = useState(0);
  const [token, setToken] = useState<string | null>(null);

  const [newConnection, setNewConnection] = useState({
    connectionName: '',
    databaseType: 'postgresql',
    host: '',
    port: 5432,
    databaseName: '',
    username: '',
    password: '',
    schema: 'public',
  });

  const config = useConfig();

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    setToken(storedToken);
  }, []);

  useEffect(() => {
    if (config && token) {
      fetchConnections();
    }
  }, [config, token]);

  const fetchConnections = async () => {
    try {
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
      const res = await axios.get(`${config?.API_URL}/api/database/connections`, { headers });
      setConnections(res.data);
    } catch (err: any) {
      console.error('Error fetching connections:', err);
    }
  };

  const handleCreateConnection = async () => {
    setLoading(true);
    setError('');
    try {
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
      const res = await axios.post(
        `${config?.API_URL}/api/database/connections`,
        newConnection,
        { headers }
      );
      if (res.data.connected) {
        setConnections([...connections, res.data]);
        setSelectedConnection(res.data);
        setOpenDialog(false);
        fetchSchemas(res.data.connectionId);
        setNewConnection({
          connectionName: '',
          databaseType: 'postgresql',
          host: '',
          port: 5432,
          databaseName: '',
          username: '',
          password: '',
          schema: 'public',
        });
      } else {
        setError(res.data.message);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create connection');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteConnection = async (connectionId: string) => {
    try {
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
      await axios.delete(`${config?.API_URL}/api/database/connections/${connectionId}`, { headers });
      setConnections(connections.filter(c => c.connectionId !== connectionId));
      if (selectedConnection?.connectionId === connectionId) {
        setSelectedConnection(null);
        setSchemas([]);
        setTableData(null);
        setTableMetadata(null);
      }
    } catch (err: any) {
      console.error('Error deleting connection:', err);
    }
  };

  const fetchSchemas = async (connectionId: string) => {
    setLoading(true);
    try {
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
      const res = await axios.get(
        `${config?.API_URL}/api/database/connections/${connectionId}/schemas`,
        { headers }
      );
      setSchemas(res.data);
    } catch (err: any) {
      setError('Failed to fetch schemas');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectTable = async (schemaName: string, tableName: string) => {
    setSelectedSchema(schemaName);
    setSelectedTable(tableName);
    setPage(1);
    await Promise.all([
      fetchTableMetadata(schemaName, tableName),
      fetchTableData(schemaName, tableName, 1),
    ]);
  };

  const fetchTableMetadata = async (schemaName: string, tableName: string) => {
    try {
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
      const res = await axios.get(
        `${config?.API_URL}/api/database/connections/${selectedConnection?.connectionId}/schemas/${schemaName}/tables/${tableName}/metadata`,
        { headers }
      );
      setTableMetadata(res.data);
      setTabValue(0);
    } catch (err: any) {
      console.error('Error fetching table metadata:', err);
    }
  };

  const fetchTableData = async (schemaName: string, tableName: string, pageNum: number) => {
    setLoading(true);
    try {
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
      const res = await axios.get(
        `${config?.API_URL}/api/database/connections/${selectedConnection?.connectionId}/schemas/${schemaName}/tables/${tableName}/data`,
        { params: { page: pageNum, pageSize: 100 }, headers }
      );
      setTableData(res.data);
    } catch (err: any) {
      console.error('Error fetching table data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handlePageChange = (event: React.ChangeEvent<unknown>, value: number) => {
    setPage(value);
    if (selectedSchema && selectedTable) {
      fetchTableData(selectedSchema, selectedTable, value);
    }
  };

  const handleConnectionSelect = (connection: DatabaseConnection) => {
    setSelectedConnection(connection);
    fetchSchemas(connection.connectionId);
    setTableData(null);
    setTableMetadata(null);
  };

  return (
    <Box sx={{ minHeight: '100vh', background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
      <DashboardHeader />

      <Container maxWidth="xl" sx={{ mt: 4, pb: 4 }}>
        {/* Header */}
        <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <StorageIcon sx={{ fontSize: 40, color: 'white' }} />
            <Typography variant="h4" sx={{ color: 'white', fontWeight: 'bold' }}>
              Database Management
            </Typography>
          </Box>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
            sx={{ bgcolor: 'white', color: '#667eea', '&:hover': { bgcolor: '#f5f5f5' } }}
          >
            New Connection
          </Button>
        </Box>

        <Grid container spacing={3}>
          {/* Left Panel - Connections & Navigator */}
          <Grid item xs={12} md={3}>
            <Card sx={{ height: 'calc(100vh - 220px)', overflow: 'auto' }}>
              <CardContent>
                <Typography variant="h6" gutterBottom sx={{ fontWeight: 'bold' }}>
                  Connections
                </Typography>
                
                {connections.map((conn) => (
                  <Box
                    key={conn.connectionId}
                    sx={{
                      p: 1.5,
                      mb: 1,
                      borderRadius: 1,
                      cursor: 'pointer',
                      bgcolor: selectedConnection?.connectionId === conn.connectionId ? '#f0f0f0' : 'transparent',
                      '&:hover': { bgcolor: '#f5f5f5' },
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                    onClick={() => handleConnectionSelect(conn)}
                  >
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                        {conn.connectionName}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {conn.databaseType} - {conn.databaseName}
                      </Typography>
                    </Box>
                    <IconButton
                      size="small"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDeleteConnection(conn.connectionId);
                      }}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Box>
                ))}

                {selectedConnection && schemas.length > 0 && (
                  <>
                    <Typography variant="h6" sx={{ mt: 3, mb: 1, fontWeight: 'bold' }}>
                      Database Navigator
                    </Typography>
                    <SimpleTreeView>
                      {schemas.map((schema) => (
                        <TreeItem
                          key={schema.schemaName}
                          itemId={schema.schemaName}
                          label={
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.5 }}>
                              <FolderIcon fontSize="small" />
                              <Typography variant="body2">{schema.schemaName}</Typography>
                            </Box>
                          }
                        >
                          {schema.tables.map((table) => (
                            <TreeItem
                              key={`${schema.schemaName}.${table}`}
                              itemId={`${schema.schemaName}.${table}`}
                              label={
                                <Box
                                  sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.5 }}
                                  onClick={() => handleSelectTable(schema.schemaName, table)}
                                >
                                  <TableIcon fontSize="small" color="primary" />
                                  <Typography variant="body2">{table}</Typography>
                                </Box>
                              }
                            />
                          ))}
                        </TreeItem>
                      ))}
                    </SimpleTreeView>
                  </>
                )}
              </CardContent>
            </Card>
          </Grid>

          {/* Right Panel - Table View */}
          <Grid item xs={12} md={9}>
            <Card sx={{ height: 'calc(100vh - 220px)', overflow: 'auto' }}>
              <CardContent>
                {!selectedConnection ? (
                  <Box sx={{ textAlign: 'center', py: 8 }}>
                    <StorageIcon sx={{ fontSize: 80, color: '#ccc', mb: 2 }} />
                    <Typography variant="h6" color="text.secondary">
                      Select a connection to get started
                    </Typography>
                  </Box>
                ) : !selectedTable ? (
                  <Box sx={{ textAlign: 'center', py: 8 }}>
                    <TableIcon sx={{ fontSize: 80, color: '#ccc', mb: 2 }} />
                    <Typography variant="h6" color="text.secondary">
                      Select a table to view its data
                    </Typography>
                  </Box>
                ) : (
                  <>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                      <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                        {selectedSchema}.{selectedTable}
                      </Typography>
                      <Button
                        startIcon={<RefreshIcon />}
                        onClick={() => handleSelectTable(selectedSchema, selectedTable)}
                        size="small"
                      >
                        Refresh
                      </Button>
                    </Box>

                    <Tabs value={tabValue} onChange={(e, v) => setTabValue(v)} sx={{ mb: 2 }}>
                      <Tab label="Data" />
                      <Tab label="Columns" />
                    </Tabs>

                    {tabValue === 0 && tableData && (
                      <>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                          Showing {tableData.rows.length} of {tableData.totalRows} rows
                        </Typography>
                        <TableContainer sx={{ maxHeight: 'calc(100vh - 450px)' }}>
                          <Table stickyHeader size="small">
                            <TableHead>
                              <TableRow>
                                {tableData.columns.map((col) => (
                                  <TableCell key={col} sx={{ fontWeight: 'bold', bgcolor: '#f5f5f5' }}>
                                    {col}
                                  </TableCell>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {tableData.rows.map((row, idx) => (
                                <TableRow key={idx} hover>
                                  {tableData.columns.map((col) => (
                                    <TableCell key={col}>
                                      {row[col] !== null && row[col] !== undefined
                                        ? String(row[col])
                                        : <em style={{ color: '#999' }}>NULL</em>}
                                    </TableCell>
                                  ))}
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                        {tableData.totalRows > tableData.pageSize && (
                          <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                            <Pagination
                              count={Math.ceil(tableData.totalRows / tableData.pageSize)}
                              page={page}
                              onChange={handlePageChange}
                              color="primary"
                            />
                          </Box>
                        )}
                      </>
                    )}

                    {tabValue === 1 && tableMetadata && (
                      <TableContainer>
                        <Table size="small">
                          <TableHead>
                            <TableRow>
                              <TableCell sx={{ fontWeight: 'bold', bgcolor: '#f5f5f5' }}>Column Name</TableCell>
                              <TableCell sx={{ fontWeight: 'bold', bgcolor: '#f5f5f5' }}>Data Type</TableCell>
                              <TableCell sx={{ fontWeight: 'bold', bgcolor: '#f5f5f5' }}>Size</TableCell>
                              <TableCell sx={{ fontWeight: 'bold', bgcolor: '#f5f5f5' }}>Nullable</TableCell>
                              <TableCell sx={{ fontWeight: 'bold', bgcolor: '#f5f5f5' }}>Key</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {tableMetadata.columns.map((col) => (
                              <TableRow key={col.columnName} hover>
                                <TableCell sx={{ fontWeight: col.primaryKey ? 'bold' : 'normal' }}>
                                  {col.columnName}
                                </TableCell>
                                <TableCell>{col.dataType}</TableCell>
                                <TableCell>{col.columnSize}</TableCell>
                                <TableCell>
                                  <Chip
                                    label={col.nullable ? 'Yes' : 'No'}
                                    size="small"
                                    color={col.nullable ? 'default' : 'primary'}
                                  />
                                </TableCell>
                                <TableCell>
                                  {col.primaryKey && (
                                    <Chip label="PK" size="small" color="secondary" />
                                  )}
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  </>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Container>

      {/* New Connection Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create New Database Connection</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          
          <TextField
            fullWidth
            label="Connection Name"
            value={newConnection.connectionName}
            onChange={(e) => setNewConnection({ ...newConnection, connectionName: e.target.value })}
            margin="normal"
          />
          
          <FormControl fullWidth margin="normal">
            <InputLabel>Database Type</InputLabel>
            <Select
              value={newConnection.databaseType}
              label="Database Type"
              onChange={(e) => setNewConnection({ ...newConnection, databaseType: e.target.value })}
            >
              <MenuItem value="postgresql">PostgreSQL</MenuItem>
              <MenuItem value="mysql">MySQL</MenuItem>
              <MenuItem value="mariadb">MariaDB</MenuItem>
              <MenuItem value="oracle">Oracle</MenuItem>
              <MenuItem value="sqlserver">SQL Server</MenuItem>
            </Select>
          </FormControl>

          <Grid container spacing={2}>
            <Grid item xs={8}>
              <TextField
                fullWidth
                label="Host"
                value={newConnection.host}
                onChange={(e) => setNewConnection({ ...newConnection, host: e.target.value })}
                margin="normal"
              />
            </Grid>
            <Grid item xs={4}>
              <TextField
                fullWidth
                label="Port"
                type="number"
                value={newConnection.port}
                onChange={(e) => setNewConnection({ ...newConnection, port: parseInt(e.target.value) })}
                margin="normal"
              />
            </Grid>
          </Grid>

          <TextField
            fullWidth
            label="Database Name"
            value={newConnection.databaseName}
            onChange={(e) => setNewConnection({ ...newConnection, databaseName: e.target.value })}
            margin="normal"
          />

          <TextField
            fullWidth
            label="Username"
            value={newConnection.username}
            onChange={(e) => setNewConnection({ ...newConnection, username: e.target.value })}
            margin="normal"
          />

          <TextField
            fullWidth
            label="Password"
            type="password"
            value={newConnection.password}
            onChange={(e) => setNewConnection({ ...newConnection, password: e.target.value })}
            margin="normal"
          />

          <TextField
            fullWidth
            label="Schema (optional)"
            value={newConnection.schema}
            onChange={(e) => setNewConnection({ ...newConnection, schema: e.target.value })}
            margin="normal"
            helperText="Default schema to use (e.g., public for PostgreSQL)"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button
            onClick={handleCreateConnection}
            variant="contained"
            disabled={loading || !newConnection.connectionName || !newConnection.host || !newConnection.databaseName}
          >
            {loading ? <CircularProgress size={24} /> : 'Connect'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
