"use client";

import { useState, useEffect } from "react";
import axios from "axios";
import DashboardHeader from "../../components/DashboardHeader";
import { useConfig } from '@/hooks/useConfig';
import {
  Box,
  Container,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  CircularProgress,
  Chip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Alert,
  InputAdornment,
  IconButton,
  Tooltip,
  Pagination,
} from "@mui/material";
import {
  Refresh as RefreshIcon,
  Search as SearchIcon,
  Storage as StorageIcon,
  Error as ErrorIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  BugReport as BugReportIcon,
  FilterList as FilterListIcon,
  Download as DownloadIcon,
} from "@mui/icons-material";
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  Legend,
  ResponsiveContainer,
  LineChart,
  Line,
} from "recharts";

interface LogEntry {
  timestamp: string;
  podName: string;
  namespace: string;
  containerName: string;
  logLevel: string;
  message: string;
  node?: string;
  lineNumber: number;
}

interface LogSearchResult {
  logs: LogEntry[];
  totalCount: number;
  logLevelCounts: Record<string, number>;
  namespaceCounts: Record<string, number>;
  podCounts: Record<string, number>;
  searchQuery?: string;
  timeRange: string;
  demoMode?: boolean;
}

interface LogStatistics {
  totalLogs: number;
  logsByLevel: Record<string, number>;
  logsByNamespace: Record<string, number>;
  logsByPod: Record<string, number>;
  errorsByPod: Record<string, number>;
  errorCount: number;
  warningCount: number;
  infoCount: number;
  timeRange: string;
  demoMode?: boolean;
}

export default function KubernetesLogsMonitoring() {
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [statistics, setStatistics] = useState<LogStatistics | null>(null);
  const [namespaces, setNamespaces] = useState<string[]>([]);
  const [selectedNamespace, setSelectedNamespace] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [logLevel, setLogLevel] = useState<string>("ALL");
  const [timeRange, setTimeRange] = useState<number>(3600);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [demoMode, setDemoMode] = useState<boolean>(false);
  const logsPerPage = 50;

  const config = useConfig();

  const COLORS = {
    ERROR: '#f44336',
    WARN: '#ff9800',
    INFO: '#2196f3',
    DEBUG: '#9c27b0',
  };

  const PIE_COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8'];

  useEffect(() => {
    if (config?.API_URL) {
      fetchNamespaces();
      fetchData();
    }
  }, [config]);

  const fetchNamespaces = async () => {
    if (!config) return;
    
    try {
      const response = await axios.get(`${config.API_URL}/api/kubernetes/logs/namespaces`);
      if (response.data.demoMode) {
        setDemoMode(true);
      }
      // Handle both formats: array of namespace objects or simple array of strings
      if (Array.isArray(response.data.namespaces)) {
        setNamespaces(response.data.namespaces);
      } else if (Array.isArray(response.data)) {
        setNamespaces(response.data.map((ns: any) => ns.name));
      }
    } catch (err: any) {
      console.error("Error fetching namespaces:", err);
    }
  };

  const fetchData = async () => {
    if (!config) return;
    
    setLoading(true);
    setError(null);
    try {
      const [logsResponse, statsResponse] = await Promise.all([
        axios.get(`${config.API_URL}/api/kubernetes/logs/search`, {
          params: {
            namespace: selectedNamespace || undefined,
            searchTerm: searchTerm || undefined,
            logLevel: logLevel,
            sinceSeconds: timeRange,
          },
        }),
        axios.get(`${config.API_URL}/api/kubernetes/logs/statistics`, {
          params: {
            namespace: selectedNamespace || undefined,
            sinceSeconds: timeRange,
          },
        }),
      ]);

      setLogs(logsResponse.data.logs || []);
      setStatistics(statsResponse.data);
      setDemoMode(logsResponse.data.demoMode || statsResponse.data.demoMode || false);
    } catch (err: any) {
      console.error("Error fetching logs:", err);
      const errorData = err.response?.data;
      const errorMessage = errorData?.message || err.message || "Failed to fetch logs";
      const errorDetails = errorData?.details;
      
      setError(errorDetails ? `${errorMessage}\n\n${errorDetails}` : errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setCurrentPage(1);
    fetchData();
  };

  const handleReset = async () => {
    if (!config) return;
    
    const resetFilters = {
      namespace: "",
      searchTerm: "",
      logLevel: "ALL",
      timeRange: 3600,
    };
    
    setSelectedNamespace(resetFilters.namespace);
    setSearchTerm(resetFilters.searchTerm);
    setLogLevel(resetFilters.logLevel);
    setTimeRange(resetFilters.timeRange);
    setCurrentPage(1);
    
    setLoading(true);
    setError(null);
    try {
      const [logsResponse, statsResponse] = await Promise.all([
        axios.get(`${config.API_URL}/api/kubernetes/logs/search`, {
          params: {
            namespace: resetFilters.namespace || undefined,
            searchTerm: resetFilters.searchTerm || undefined,
            logLevel: resetFilters.logLevel,
            sinceSeconds: resetFilters.timeRange,
          },
        }),
        axios.get(`${config.API_URL}/api/kubernetes/logs/statistics`, {
          params: {
            namespace: resetFilters.namespace || undefined,
            sinceSeconds: resetFilters.timeRange,
          },
        }),
      ]);

      setLogs(logsResponse.data.logs || []);
      setStatistics(statsResponse.data);
      setDemoMode(logsResponse.data.demoMode || statsResponse.data.demoMode || false);
    } catch (err: any) {
      console.error("Error fetching logs:", err);
      setError(err.response?.data?.message || err.message || "Failed to fetch logs");
    } finally {
      setLoading(false);
    }
  };

  const exportLogs = () => {
    const dataStr = JSON.stringify(logs, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `k8s-logs-${new Date().toISOString()}.json`;
    link.click();
  };

  const getLogLevelIcon = (level: string) => {
    switch (level) {
      case 'ERROR':
        return <ErrorIcon sx={{ color: COLORS.ERROR, fontSize: 18 }} />;
      case 'WARN':
        return <WarningIcon sx={{ color: COLORS.WARN, fontSize: 18 }} />;
      case 'INFO':
        return <InfoIcon sx={{ color: COLORS.INFO, fontSize: 18 }} />;
      case 'DEBUG':
        return <BugReportIcon sx={{ color: COLORS.DEBUG, fontSize: 18 }} />;
      default:
        return <InfoIcon sx={{ color: COLORS.INFO, fontSize: 18 }} />;
    }
  };

  const getLogLevelChipColor = (level: string): "error" | "warning" | "info" | "default" => {
    switch (level) {
      case 'ERROR':
        return 'error';
      case 'WARN':
        return 'warning';
      case 'INFO':
        return 'info';
      default:
        return 'default';
    }
  };

  const logLevelData = statistics ? Object.entries(statistics.logsByLevel).map(([name, value]) => ({
    name,
    value,
  })) : [];

  const namespaceData = statistics ? Object.entries(statistics.logsByNamespace)
    .slice(0, 10)
    .map(([name, value]) => ({
      name,
      value,
    })) : [];

  const topErrorPodsData = statistics ? Object.entries(statistics.errorsByPod)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 10)
    .map(([name, value]) => ({
      name: name.length > 20 ? name.substring(0, 20) + '...' : name,
      errors: value,
    })) : [];

  const paginatedLogs = logs.slice(
    (currentPage - 1) * logsPerPage,
    currentPage * logsPerPage
  );

  const totalPages = Math.ceil(logs.length / logsPerPage);

  return (
    <Box
      sx={{
        background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
        minHeight: "100vh",
        pb: 4,
      }}
    >
      <DashboardHeader />

      <Container maxWidth="xl" sx={{ mt: 4 }}>
        {demoMode && (
          <Alert severity="info" sx={{ mb: 3 }}>
            <strong>Demo Mode Active:</strong> Showing sample Kubernetes logs data. 
            To connect to a real cluster, add your kubeconfig file at <code>~/.kube/config</code> and restart the application.
          </Alert>
        )}
        
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <Typography variant="h4" sx={{ color: "white", fontWeight: "bold", display: "flex", alignItems: "center", gap: 1 }}>
              <StorageIcon /> Kubernetes Logs Monitoring
            </Typography>
            {demoMode && (
              <Chip 
                label="DEMO" 
                color="warning" 
                size="small"
                sx={{ fontWeight: "bold" }}
              />
            )}
          </Box>
          <Box>
            <Button
              variant="contained"
              startIcon={<RefreshIcon />}
              onClick={fetchData}
              disabled={loading}
              sx={{
                bgcolor: "white",
                color: "#667eea",
                mr: 2,
                "&:hover": { bgcolor: "#f5f5f5" },
              }}
            >
              Refresh
            </Button>
            <Button
              variant="contained"
              startIcon={<DownloadIcon />}
              onClick={exportLogs}
              disabled={logs.length === 0}
              sx={{
                bgcolor: "white",
                color: "#667eea",
                "&:hover": { bgcolor: "#f5f5f5" },
              }}
            >
              Export
            </Button>
          </Box>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <Grid container spacing={3} sx={{ mb: 3 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ height: "100%", background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)", color: "white" }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>Total Logs</Typography>
                <Typography variant="h3">{statistics?.totalLogs || 0}</Typography>
                <Typography variant="body2" sx={{ mt: 1, opacity: 0.9 }}>
                  {statistics?.timeRange || "Last hour"}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ height: "100%", bgcolor: "#f44336", color: "white" }}>
              <CardContent>
                <Typography variant="h6" gutterBottom sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <ErrorIcon /> Errors
                </Typography>
                <Typography variant="h3">{statistics?.errorCount || 0}</Typography>
                <Typography variant="body2" sx={{ mt: 1, opacity: 0.9 }}>
                  Critical issues
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ height: "100%", bgcolor: "#ff9800", color: "white" }}>
              <CardContent>
                <Typography variant="h6" gutterBottom sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <WarningIcon /> Warnings
                </Typography>
                <Typography variant="h3">{statistics?.warningCount || 0}</Typography>
                <Typography variant="body2" sx={{ mt: 1, opacity: 0.9 }}>
                  Attention needed
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ height: "100%", bgcolor: "#2196f3", color: "white" }}>
              <CardContent>
                <Typography variant="h6" gutterBottom sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <InfoIcon /> Info Logs
                </Typography>
                <Typography variant="h3">{statistics?.infoCount || 0}</Typography>
                <Typography variant="body2" sx={{ mt: 1, opacity: 0.9 }}>
                  Informational
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <FilterListIcon /> Search & Filter
            </Typography>
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} sm={6} md={3}>
                <FormControl fullWidth>
                  <InputLabel>Namespace</InputLabel>
                  <Select
                    value={selectedNamespace}
                    label="Namespace"
                    onChange={(e) => setSelectedNamespace(e.target.value)}
                  >
                    <MenuItem value="">All Namespaces</MenuItem>
                    {namespaces.map((ns) => (
                      <MenuItem key={ns} value={ns}>{ns}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <FormControl fullWidth>
                  <InputLabel>Log Level</InputLabel>
                  <Select
                    value={logLevel}
                    label="Log Level"
                    onChange={(e) => setLogLevel(e.target.value)}
                  >
                    <MenuItem value="ALL">All Levels</MenuItem>
                    <MenuItem value="ERROR">Error</MenuItem>
                    <MenuItem value="WARN">Warning</MenuItem>
                    <MenuItem value="INFO">Info</MenuItem>
                    <MenuItem value="DEBUG">Debug</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <FormControl fullWidth>
                  <InputLabel>Time Range</InputLabel>
                  <Select
                    value={timeRange}
                    label="Time Range"
                    onChange={(e) => setTimeRange(Number(e.target.value))}
                  >
                    <MenuItem value={300}>Last 5 minutes</MenuItem>
                    <MenuItem value={900}>Last 15 minutes</MenuItem>
                    <MenuItem value={1800}>Last 30 minutes</MenuItem>
                    <MenuItem value={3600}>Last hour</MenuItem>
                    <MenuItem value={10800}>Last 3 hours</MenuItem>
                    <MenuItem value={21600}>Last 6 hours</MenuItem>
                    <MenuItem value={86400}>Last 24 hours</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <TextField
                  fullWidth
                  label="Search logs"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton onClick={handleSearch} edge="end">
                          <SearchIcon />
                        </IconButton>
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
            </Grid>
            <Box sx={{ mt: 2, display: "flex", gap: 2 }}>
              <Button
                variant="contained"
                startIcon={<SearchIcon />}
                onClick={handleSearch}
                disabled={loading}
              >
                Search
              </Button>
              <Button variant="outlined" onClick={handleReset}>
                Reset Filters
              </Button>
            </Box>
          </CardContent>
        </Card>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: 300 }}>
            <CircularProgress size={60} sx={{ color: "white" }} />
          </Box>
        ) : (
          <>
            <Grid container spacing={3} sx={{ mb: 3 }}>
              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>Log Distribution by Level</Typography>
                    {logLevelData.length > 0 ? (
                      <ResponsiveContainer width="100%" height={300}>
                        <PieChart>
                          <Pie
                            data={logLevelData}
                            cx="50%"
                            cy="50%"
                            labelLine={false}
                            label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                            outerRadius={80}
                            fill="#8884d8"
                            dataKey="value"
                          >
                            {logLevelData.map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={COLORS[entry.name as keyof typeof COLORS] || PIE_COLORS[index % PIE_COLORS.length]} />
                            ))}
                          </Pie>
                          <RechartsTooltip />
                        </PieChart>
                      </ResponsiveContainer>
                    ) : (
                      <Typography>No data available</Typography>
                    )}
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>Top Pods with Errors</Typography>
                    {topErrorPodsData.length > 0 ? (
                      <ResponsiveContainer width="100%" height={300}>
                        <BarChart data={topErrorPodsData}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="name" angle={-45} textAnchor="end" height={100} />
                          <YAxis />
                          <RechartsTooltip />
                          <Bar dataKey="errors" fill="#f44336" />
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <Typography>No errors found</Typography>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            </Grid>

            <Card>
              <CardContent>
                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}>
                  <Typography variant="h6">
                    Log Entries ({logs.length} results)
                  </Typography>
                  {totalPages > 1 && (
                    <Pagination
                      count={totalPages}
                      page={currentPage}
                      onChange={(_, page) => setCurrentPage(page)}
                      color="primary"
                    />
                  )}
                </Box>
                
                <TableContainer component={Paper} sx={{ maxHeight: 600 }}>
                  <Table stickyHeader size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Timestamp</TableCell>
                        <TableCell>Level</TableCell>
                        <TableCell>Namespace</TableCell>
                        <TableCell>Pod</TableCell>
                        <TableCell>Container</TableCell>
                        <TableCell>Message</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {paginatedLogs.length > 0 ? (
                        paginatedLogs.map((log, index) => (
                          <TableRow key={`${log.podName}-${log.lineNumber}-${index}`} hover>
                            <TableCell sx={{ fontSize: '0.75rem', whiteSpace: 'nowrap' }}>
                              {log.timestamp}
                            </TableCell>
                            <TableCell>
                              <Chip
                                icon={getLogLevelIcon(log.logLevel)}
                                label={log.logLevel}
                                color={getLogLevelChipColor(log.logLevel)}
                                size="small"
                              />
                            </TableCell>
                            <TableCell sx={{ fontSize: '0.8rem' }}>{log.namespace}</TableCell>
                            <TableCell sx={{ fontSize: '0.8rem' }}>
                              <Tooltip title={log.podName}>
                                <span>{log.podName.length > 25 ? log.podName.substring(0, 25) + '...' : log.podName}</span>
                              </Tooltip>
                            </TableCell>
                            <TableCell sx={{ fontSize: '0.8rem' }}>{log.containerName || 'N/A'}</TableCell>
                            <TableCell sx={{ fontSize: '0.8rem', maxWidth: 500, wordBreak: 'break-word' }}>
                              {log.message}
                            </TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell colSpan={6} align="center">
                            <Typography color="textSecondary">No logs found</Typography>
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>

                {totalPages > 1 && (
                  <Box sx={{ display: "flex", justifyContent: "center", mt: 2 }}>
                    <Pagination
                      count={totalPages}
                      page={currentPage}
                      onChange={(_, page) => setCurrentPage(page)}
                      color="primary"
                    />
                  </Box>
                )}
              </CardContent>
            </Card>
          </>
        )}
      </Container>
    </Box>
  );
}
