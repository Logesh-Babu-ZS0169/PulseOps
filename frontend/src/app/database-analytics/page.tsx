'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import axios from 'axios';
import {
  Box,
  Container,
  Typography,
  Card,
  CardContent,
  Grid,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  List,
  ListItem,
  ListItemText,
  Divider,
  Button,
} from '@mui/material';
import {
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import {
  Assessment as AssessmentIcon,
  Storage as StorageIcon,
  TableChart as TableIcon,
  Analytics as AnalyticsIcon,
  TipsAndUpdates as TipsIcon,
  Warning as WarningIcon,
} from '@mui/icons-material';

const API_BASE = 'http://localhost:8181/api';

interface Connection {
  connectionId: string;
  connectionName: string;
  databaseType: string;
  databaseName: string;
}

interface DatabaseOverview {
  databaseName: string;
  totalSchemas: number;
  totalTables: number;
  totalRows: number;
  totalSizeMB: number;
  schemaStats: Array<{
    schemaName: string;
    tableCount: number;
    sizeMB: number;
  }>;
  largestTables: Array<{
    schemaName: string;
    tableName: string;
    rowCount: number;
    sizeMB: number;
  }>;
}

interface TableHealth {
  emptyTables: Array<{
    schemaName: string;
    tableName: string;
    sizeMB: number;
  }>;
  tableStatistics: Array<{
    schemaName: string;
    tableName: string;
    rowCount: number;
    columnCount: number;
    sizeMB: number;
    healthStatus: string;
  }>;
}

interface ColumnHealth {
  nullHeavyColumns: Array<{
    schemaName: string;
    tableName: string;
    columnName: string;
    dataType: string;
    nullPercentage: number;
    issue: string;
    recommendation: string;
  }>;
  totalColumnsAnalyzed: number;
  healthyColumns: number;
  problematicColumns: number;
}

interface SchemaRelationships {
  relationships: Array<{
    sourceSchema: string;
    sourceTable: string;
    sourceColumn: string;
    targetSchema: string;
    targetTable: string;
    targetColumn: string;
    constraintName: string;
  }>;
  isolatedTables: Array<{
    schemaName: string;
    tableName: string;
    rowCount: number;
    reason: string;
  }>;
  totalRelationships: number;
  connectedTables: number;
  isolatedTableCount: number;
}

interface OptimizationSuggestions {
  suggestions: Array<{
    category: string;
    priority: string;
    title: string;
    description: string;
    targetObject: string;
    potentialSavingsMB: number;
    actionSteps: string[];
  }>;
  totalSuggestions: number;
  highPriority: number;
  mediumPriority: number;
  lowPriority: number;
}

const COLORS = ['#667eea', '#764ba2', '#f093fb', '#4facfe', '#00f2fe', '#43e97b', '#fa709a'];

export default function DatabaseAnalytics() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [connections, setConnections] = useState<Connection[]>([]);
  const [selectedConnection, setSelectedConnection] = useState<string>('');

  const [overview, setOverview] = useState<DatabaseOverview | null>(null);
  const [tableHealth, setTableHealth] = useState<TableHealth | null>(null);
  const [columnHealth, setColumnHealth] = useState<ColumnHealth | null>(null);
  const [relationships, setRelationships] = useState<SchemaRelationships | null>(null);
  const [suggestions, setSuggestions] = useState<OptimizationSuggestions | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      router.push('/login');
      return;
    }
    fetchConnections();
  }, []);

  const fetchConnections = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_BASE}/database/connections`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setConnections(response.data);
    } catch (err: any) {
      setError('Failed to load connections');
    }
  };

  const analyzeDatabase = async (connectionId: string) => {
    setLoading(true);
    setError('');
    const token = localStorage.getItem('token');

    try {
      const [overviewRes, tableHealthRes, columnHealthRes, relationshipsRes, suggestionsRes] = await Promise.all([
        axios.get(`${API_BASE}/database/analytics/overview/${connectionId}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        axios.get(`${API_BASE}/database/analytics/table-health/${connectionId}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        axios.get(`${API_BASE}/database/analytics/column-health/${connectionId}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        axios.get(`${API_BASE}/database/analytics/relationships/${connectionId}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        axios.get(`${API_BASE}/database/analytics/optimization-suggestions/${connectionId}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
      ]);

      setOverview(overviewRes.data);
      setTableHealth(tableHealthRes.data);
      setColumnHealth(columnHealthRes.data);
      setRelationships(relationshipsRes.data);
      setSuggestions(suggestionsRes.data);
    } catch (err: any) {
      setError(err.response?.data || 'Failed to analyze database');
    } finally {
      setLoading(false);
    }
  };

  const handleConnectionChange = (connectionId: string) => {
    setSelectedConnection(connectionId);
    analyzeDatabase(connectionId);
  };

  const getHealthColor = (status: string) => {
    switch (status) {
      case 'HEALTHY': return 'success';
      case 'EMPTY': return 'error';
      case 'VERY_LOW': return 'warning';
      case 'LARGE': return 'info';
      case 'HIGH_VOLUME': return 'secondary';
      default: return 'default';
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'HIGH': return 'error';
      case 'MEDIUM': return 'warning';
      case 'LOW': return 'info';
      default: return 'default';
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        py: 4,
      }}
    >
      <Container maxWidth="xl">
        <Typography variant="h3" sx={{ color: 'white', mb: 3, fontWeight: 'bold' }}>
          <AnalyticsIcon sx={{ fontSize: 40, mr: 2, verticalAlign: 'middle' }} />
          Database Intelligence Analyst
        </Typography>

        <Card sx={{ mb: 3 }}>
          <CardContent>
            <FormControl fullWidth>
              <InputLabel>Select Database Connection</InputLabel>
              <Select
                value={selectedConnection}
                onChange={(e) => handleConnectionChange(e.target.value)}
                label="Select Database Connection"
              >
                {connections.map((conn) => (
                  <MenuItem key={conn.connectionId} value={conn.connectionId}>
                    {conn.connectionName} ({conn.databaseType} - {conn.databaseName})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </CardContent>
        </Card>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {loading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', my: 8 }}>
            <CircularProgress size={60} sx={{ color: 'white' }} />
          </Box>
        )}

        {!loading && overview && (
          <>
            <Grid container spacing={3} sx={{ mb: 3 }}>
              <Grid item xs={12} md={3}>
                <Card sx={{ background: 'linear-gradient(135deg, #667eea, #764ba2)', color: 'white' }}>
                  <CardContent>
                    <Typography variant="h6">Total Schemas</Typography>
                    <Typography variant="h3">{overview.totalSchemas}</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={3}>
                <Card sx={{ background: 'linear-gradient(135deg, #f093fb, #f5576c)', color: 'white' }}>
                  <CardContent>
                    <Typography variant="h6">Total Tables</Typography>
                    <Typography variant="h3">{overview.totalTables}</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={3}>
                <Card sx={{ background: 'linear-gradient(135deg, #4facfe, #00f2fe)', color: 'white' }}>
                  <CardContent>
                    <Typography variant="h6">Total Rows</Typography>
                    <Typography variant="h3">{overview.totalRows.toLocaleString()}</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={3}>
                <Card sx={{ background: 'linear-gradient(135deg, #43e97b, #38f9d7)', color: 'white' }}>
                  <CardContent>
                    <Typography variant="h6">Database Size</Typography>
                    <Typography variant="h3">{overview.totalSizeMB.toFixed(2)} MB</Typography>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>

            <Grid container spacing={3} sx={{ mb: 3 }}>
              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      <StorageIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                      Schema Distribution
                    </Typography>
                    <ResponsiveContainer width="100%" height={300}>
                      <PieChart>
                        <Pie
                          data={overview.schemaStats}
                          dataKey="tableCount"
                          nameKey="schemaName"
                          cx="50%"
                          cy="50%"
                          outerRadius={100}
                          label
                        >
                          {overview.schemaStats.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                          ))}
                        </Pie>
                        <Tooltip />
                        <Legend />
                      </PieChart>
                    </ResponsiveContainer>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      <TableIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                      Top 10 Largest Tables
                    </Typography>
                    <ResponsiveContainer width="100%" height={300}>
                      <BarChart data={overview.largestTables.slice(0, 10)}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="tableName" angle={-45} textAnchor="end" height={100} />
                        <YAxis />
                        <Tooltip />
                        <Bar dataKey="sizeMB" fill="#667eea" name="Size (MB)" />
                      </BarChart>
                    </ResponsiveContainer>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>

            {tableHealth && (
              <Card sx={{ mb: 3 }}>
                <CardContent>
                  <Typography variant="h5" gutterBottom>
                    <AssessmentIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                    Table Health Analysis
                  </Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={12} md={6}>
                      <Typography variant="h6" sx={{ mt: 2, mb: 1 }}>
                        Empty Tables ({tableHealth.emptyTables.length})
                      </Typography>
                      <TableContainer component={Paper} sx={{ maxHeight: 300 }}>
                        <Table size="small">
                          <TableHead>
                            <TableRow>
                              <TableCell>Schema</TableCell>
                              <TableCell>Table</TableCell>
                              <TableCell align="right">Size (MB)</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {tableHealth.emptyTables.map((table, idx) => (
                              <TableRow key={idx}>
                                <TableCell>{table.schemaName}</TableCell>
                                <TableCell>{table.tableName}</TableCell>
                                <TableCell align="right">{table.sizeMB.toFixed(2)}</TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <Typography variant="h6" sx={{ mt: 2, mb: 1 }}>
                        Table Statistics
                      </Typography>
                      <TableContainer component={Paper} sx={{ maxHeight: 300 }}>
                        <Table size="small">
                          <TableHead>
                            <TableRow>
                              <TableCell>Table</TableCell>
                              <TableCell align="right">Rows</TableCell>
                              <TableCell align="right">Columns</TableCell>
                              <TableCell>Status</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {tableHealth.tableStatistics.slice(0, 10).map((table, idx) => (
                              <TableRow key={idx}>
                                <TableCell>{table.tableName}</TableCell>
                                <TableCell align="right">{table.rowCount.toLocaleString()}</TableCell>
                                <TableCell align="right">{table.columnCount}</TableCell>
                                <TableCell>
                                  <Chip label={table.healthStatus} color={getHealthColor(table.healthStatus)} size="small" />
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            )}

            {columnHealth && (
              <Card sx={{ mb: 3 }}>
                <CardContent>
                  <Typography variant="h5" gutterBottom>
                    <WarningIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                    Column Health Analysis
                  </Typography>
                  <Grid container spacing={2} sx={{ mb: 2 }}>
                    <Grid item xs={4}>
                      <Card sx={{ background: '#e8f5e9' }}>
                        <CardContent>
                          <Typography variant="body2">Healthy Columns</Typography>
                          <Typography variant="h4">{columnHealth.healthyColumns}</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={4}>
                      <Card sx={{ background: '#fff3e0' }}>
                        <CardContent>
                          <Typography variant="body2">Problematic Columns</Typography>
                          <Typography variant="h4">{columnHealth.problematicColumns}</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={4}>
                      <Card sx={{ background: '#e3f2fd' }}>
                        <CardContent>
                          <Typography variant="body2">Total Analyzed</Typography>
                          <Typography variant="h4">{columnHealth.totalColumnsAnalyzed}</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                  </Grid>
                  <Typography variant="h6" sx={{ mt: 2, mb: 1 }}>
                    High NULL Percentage Columns
                  </Typography>
                  <TableContainer component={Paper}>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Table</TableCell>
                          <TableCell>Column</TableCell>
                          <TableCell>Type</TableCell>
                          <TableCell align="right">NULL %</TableCell>
                          <TableCell>Recommendation</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {columnHealth.nullHeavyColumns.map((col, idx) => (
                          <TableRow key={idx}>
                            <TableCell>{col.tableName}</TableCell>
                            <TableCell>{col.columnName}</TableCell>
                            <TableCell>{col.dataType}</TableCell>
                            <TableCell align="right">{col.nullPercentage.toFixed(1)}%</TableCell>
                            <TableCell>{col.recommendation}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </CardContent>
              </Card>
            )}

            {relationships && (
              <Card sx={{ mb: 3 }}>
                <CardContent>
                  <Typography variant="h5" gutterBottom>
                    Schema Relationships
                  </Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={12} md={4}>
                      <Card sx={{ background: 'linear-gradient(135deg, #667eea, #764ba2)', color: 'white' }}>
                        <CardContent>
                          <Typography variant="body2">Total Relationships</Typography>
                          <Typography variant="h3">{relationships.totalRelationships}</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <Card sx={{ background: 'linear-gradient(135deg, #43e97b, #38f9d7)', color: 'white' }}>
                        <CardContent>
                          <Typography variant="body2">Connected Tables</Typography>
                          <Typography variant="h3">{relationships.connectedTables}</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <Card sx={{ background: 'linear-gradient(135deg, #fa709a, #fee140)', color: 'white' }}>
                        <CardContent>
                          <Typography variant="body2">Isolated Tables</Typography>
                          <Typography variant="h3">{relationships.isolatedTableCount}</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                  </Grid>
                  {relationships.isolatedTables.length > 0 && (
                    <>
                      <Typography variant="h6" sx={{ mt: 3, mb: 1 }}>
                        Isolated Tables (No Foreign Keys)
                      </Typography>
                      <TableContainer component={Paper}>
                        <Table size="small">
                          <TableHead>
                            <TableRow>
                              <TableCell>Schema</TableCell>
                              <TableCell>Table</TableCell>
                              <TableCell align="right">Rows</TableCell>
                              <TableCell>Reason</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {relationships.isolatedTables.slice(0, 10).map((table, idx) => (
                              <TableRow key={idx}>
                                <TableCell>{table.schemaName}</TableCell>
                                <TableCell>{table.tableName}</TableCell>
                                <TableCell align="right">{table.rowCount.toLocaleString()}</TableCell>
                                <TableCell>{table.reason}</TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    </>
                  )}
                </CardContent>
              </Card>
            )}

            {suggestions && (
              <Card sx={{ mb: 3 }}>
                <CardContent>
                  <Typography variant="h5" gutterBottom>
                    <TipsIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                    Optimization Suggestions ({suggestions.totalSuggestions})
                  </Typography>
                  <Grid container spacing={2} sx={{ mb: 2 }}>
                    <Grid item xs={4}>
                      <Chip label={`High: ${suggestions.highPriority}`} color="error" />
                    </Grid>
                    <Grid item xs={4}>
                      <Chip label={`Medium: ${suggestions.mediumPriority}`} color="warning" />
                    </Grid>
                    <Grid item xs={4}>
                      <Chip label={`Low: ${suggestions.lowPriority}`} color="info" />
                    </Grid>
                  </Grid>
                  <List>
                    {suggestions.suggestions.map((suggestion, idx) => (
                      <React.Fragment key={idx}>
                        <ListItem alignItems="flex-start">
                          <ListItemText
                            primary={
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <Chip label={suggestion.priority} color={getPriorityColor(suggestion.priority)} size="small" />
                                <Typography variant="h6">{suggestion.title}</Typography>
                              </Box>
                            }
                            secondary={
                              <>
                                <Typography variant="body2" sx={{ mt: 1 }}>
                                  {suggestion.description}
                                </Typography>
                                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                                  Target: {suggestion.targetObject} | Potential Savings: {suggestion.potentialSavingsMB.toFixed(2)} MB
                                </Typography>
                                <Typography variant="body2" sx={{ mt: 1, fontWeight: 'bold' }}>
                                  Action Steps:
                                </Typography>
                                <List dense>
                                  {suggestion.actionSteps.map((step, stepIdx) => (
                                    <ListItem key={stepIdx}>
                                      <ListItemText primary={`${stepIdx + 1}. ${step}`} />
                                    </ListItem>
                                  ))}
                                </List>
                              </>
                            }
                          />
                        </ListItem>
                        {idx < suggestions.suggestions.length - 1 && <Divider />}
                      </React.Fragment>
                    ))}
                  </List>
                </CardContent>
              </Card>
            )}
          </>
        )}
      </Container>
    </Box>
  );
}
