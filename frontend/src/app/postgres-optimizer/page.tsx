"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import {
  Container,
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  Alert,
  CircularProgress,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Divider,
} from "@mui/material";
import {
  Speed,
  QueryStats,
  Settings,
  CleaningServices,
  Storage,
  Warning,
  CheckCircle,
  Download,
} from "@mui/icons-material";
import axios from "axios";

const API_BASE = 'http://localhost:8181/api';

interface MissingIndex {
  schema: string;
  table: string;
  sequentialScans: number;
  indexScans: number;
  suggestion: string;
  proposedIndex: string;
  priority: string;
}

interface UnusedIndex {
  schema: string;
  table: string;
  indexName: string;
  indexScans: number;
  sizeMB: number;
  suggestion: string;
  dropStatement: string;
}

interface SlowQuery {
  queryText: string;
  meanExecTimeMs: number;
  calls: number;
  totalExecTimeMs: number;
  suggestion: string;
  priority: string;
}

interface ConfigSuggestion {
  parameter: string;
  currentValue: string;
  suggestedValue: string;
  reason: string;
  priority: string;
  impact: string;
}

interface TableVacuumStatus {
  schema: string;
  table: string;
  liveTuples: number;
  deadTuples: number;
  deadTuplePercent: number;
  lastVacuum: string;
  lastAutoVacuum: string;
  suggestion: string;
  priority: string;
}

export default function PostgresOptimizer() {
  const router = useRouter();
  const [connectionId, setConnectionId] = useState("");
  const [connections, setConnections] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [indexData, setIndexData] = useState<any>(null);
  const [queryData, setQueryData] = useState<any>(null);
  const [configData, setConfigData] = useState<any>(null);
  const [vacuumData, setVacuumData] = useState<any>(null);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      router.push("/login");
      return;
    }
    fetchConnections();
  }, []);

  const fetchConnections = async () => {
    try {
      const token = localStorage.getItem("token");
      if (!token) {
        setError("Please log in to continue");
        return;
      }
      const response = await axios.get(`${API_BASE}/database/connections`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log("Fetched connections:", response.data);
      setConnections(response.data);
    } catch (error: any) {
      console.error("Error fetching connections:", error);
      setError(
        error.response?.data ||
          "Failed to load connections. Please try logging in again.",
      );
    }
  };

  const analyzeAll = async (connId: string) => {
    setLoading(true);
    setError("");

    const token = localStorage.getItem("token");
    const headers = { Authorization: `Bearer ${token}` };

    try {
      const [indexRes, queryRes, configRes, vacuumRes] = await Promise.all([
        axios.get(
          `${API_BASE}/database/analytics/postgres/index-analysis/${connId}`,
          { headers },
        ),
        axios.get(
          `${API_BASE}/database/analytics/postgres/query-analysis/${connId}`,
          { headers },
        ),
        axios.get(
          `${API_BASE}/database/analytics/postgres/config-analysis/${connId}`,
          { headers },
        ),
        axios.get(
          `${API_BASE}/database/analytics/postgres/vacuum-analysis/${connId}`,
          { headers },
        ),
      ]);

      setIndexData(indexRes.data);
      setQueryData(queryRes.data);
      setConfigData(configRes.data);
      setVacuumData(vacuumRes.data);
    } catch (error: any) {
      setError(
        error.response?.data ||
          "Error analyzing database. Make sure you selected a PostgreSQL connection.",
      );
    } finally {
      setLoading(false);
    }
  };

  const handleConnectionChange = (connId: string) => {
    setConnectionId(connId);
    analyzeAll(connId);
  };

  const exportReport = async () => {
    if (!connectionId) {
      setError("Please select a database connection first");
      return;
    }

    try {
      const token = localStorage.getItem("token");
      const response = await axios.get(
        `${API_BASE}/database/analytics/postgres/export-report/${connectionId}`,
        {
          headers: { Authorization: `Bearer ${token}` },
          responseType: "blob",
        }
      );

      const blob = new Blob([response.data], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `PostgreSQL_Optimization_Report_${new Date().toISOString()}.xlsx`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error: any) {
      console.error("Export error:", error);
      setError("Failed to export report. Please try again.");
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case "HIGH":
        return "error";
      case "MEDIUM":
        return "warning";
      case "LOW":
        return "success";
      default:
        return "default";
    }
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
        py: 4,
      }}
    >
      <Container maxWidth="xl">
        <Box sx={{ mb: 4 }}>
          <Typography
            variant="h3"
            sx={{ color: "#fff", fontWeight: 700, mb: 1 }}
          >
            <Speed sx={{ fontSize: 40, mr: 2, verticalAlign: "middle" }} />
            PostgreSQL AI Optimizer
          </Typography>
          <Typography variant="body1" sx={{ color: "rgba(255,255,255,0.9)" }}>
            Advanced database optimization analysis powered by AI algorithms
          </Typography>
        </Box>

        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} md={9}>
                <FormControl fullWidth>
                  <InputLabel>Select Database Connection</InputLabel>
                  <Select
                    value={connectionId}
                    onChange={(e) => handleConnectionChange(e.target.value)}
                    label="Select Database Connection"
                  >
                    {connections.map((conn) => (
                      <MenuItem key={conn.connectionId} value={conn.connectionId}>
                        {conn.connectionName} ({conn.databaseType} -{" "}
                        {conn.databaseName})
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} md={3}>
                <Button
                  variant="contained"
                  startIcon={<Download />}
                  onClick={exportReport}
                  disabled={!connectionId || loading || !indexData}
                  fullWidth
                  sx={{
                    bgcolor: "#667eea",
                    color: "#fff",
                    "&:hover": { bgcolor: "#5568d3" },
                    height: "56px",
                  }}
                >
                  Export Report
                </Button>
              </Grid>
            </Grid>
          </CardContent>
        </Card>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {loading && (
          <Box sx={{ display: "flex", justifyContent: "center", my: 8 }}>
            <CircularProgress size={60} sx={{ color: "white" }} />
          </Box>
        )}

        {!loading && indexData && (
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography
                variant="h5"
                gutterBottom
                sx={{ display: "flex", alignItems: "center", color: "#667eea" }}
              >
                <Storage sx={{ mr: 1 }} /> Index Analysis
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={6}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      Missing Indexes
                    </Typography>
                    <Typography variant="h4" sx={{ color: "#f44336" }}>
                      {indexData.missingIndexes?.length || 0}
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      Unused Indexes (Wasted Space)
                    </Typography>
                    <Typography variant="h4" sx={{ color: "#ff9800" }}>
                      {indexData.unusedIndexes?.length || 0} (
                      {indexData.totalWastedSpaceMB?.toFixed(2) || 0} MB)
                    </Typography>
                  </Paper>
                </Grid>
              </Grid>

              <Divider sx={{ my: 2 }} />

              <Typography
                variant="h6"
                gutterBottom
                sx={{ color: "#f44336", mt: 2 }}
              >
                Missing Indexes (High Sequential Scans)
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Priority</TableCell>
                      <TableCell>Table</TableCell>
                      <TableCell>Sequential Scans</TableCell>
                      <TableCell>Suggestion</TableCell>
                      <TableCell>Proposed Index</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {indexData.missingIndexes
                      ?.slice(0, 10)
                      .map((idx: MissingIndex, i: number) => (
                        <TableRow key={i}>
                          <TableCell>
                            <Chip
                              label={idx.priority}
                              color={getPriorityColor(idx.priority)}
                              size="small"
                            />
                          </TableCell>
                          <TableCell>
                            {idx.schema}.{idx.table}
                          </TableCell>
                          <TableCell>
                            {idx.sequentialScans.toLocaleString()}
                          </TableCell>
                          <TableCell>{idx.suggestion}</TableCell>
                          <TableCell>
                            <Typography
                              variant="caption"
                              sx={{
                                fontFamily: "monospace",
                                fontSize: "0.75rem",
                              }}
                            >
                              {idx.proposedIndex}
                            </Typography>
                          </TableCell>
                        </TableRow>
                      ))}
                  </TableBody>
                </Table>
              </TableContainer>

              <Typography
                variant="h6"
                gutterBottom
                sx={{ color: "#ff9800", mt: 3 }}
              >
                Unused Indexes (Wasting Disk Space)
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Table</TableCell>
                      <TableCell>Index Name</TableCell>
                      <TableCell>Size (MB)</TableCell>
                      <TableCell>Scans</TableCell>
                      <TableCell>Recommendation</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {indexData.unusedIndexes
                      ?.slice(0, 10)
                      .map((idx: UnusedIndex, i: number) => (
                        <TableRow key={i}>
                          <TableCell>
                            {idx.schema}.{idx.table}
                          </TableCell>
                          <TableCell>{idx.indexName}</TableCell>
                          <TableCell>{idx.sizeMB.toFixed(2)}</TableCell>
                          <TableCell>{idx.indexScans}</TableCell>
                          <TableCell>
                            <Typography
                              variant="caption"
                              sx={{
                                fontFamily: "monospace",
                                fontSize: "0.75rem",
                              }}
                            >
                              {idx.dropStatement}
                            </Typography>
                          </TableCell>
                        </TableRow>
                      ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        )}

        {queryData && (
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography
                variant="h5"
                gutterBottom
                sx={{ display: "flex", alignItems: "center", color: "#667eea" }}
              >
                <QueryStats sx={{ mr: 1 }} /> Query Optimization
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={4}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      Slow Queries Found
                    </Typography>
                    <Typography variant="h4" sx={{ color: "#f44336" }}>
                      {queryData.slowQueries?.length || 0}
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      Total Queries Analyzed
                    </Typography>
                    <Typography variant="h4">
                      {queryData.totalQueriesAnalyzed || 0}
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      Avg Query Time
                    </Typography>
                    <Typography variant="h4">
                      {queryData.avgQueryTimeMs?.toFixed(2) || 0} ms
                    </Typography>
                  </Paper>
                </Grid>
              </Grid>

              {queryData.slowQueries?.length > 0 ? (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Priority</TableCell>
                        <TableCell>Mean Time (ms)</TableCell>
                        <TableCell>Calls</TableCell>
                        <TableCell>Query</TableCell>
                        <TableCell>Recommendation</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {queryData.slowQueries
                        .slice(0, 10)
                        .map((query: SlowQuery, i: number) => (
                          <TableRow key={i}>
                            <TableCell>
                              <Chip
                                label={query.priority}
                                color={getPriorityColor(query.priority)}
                                size="small"
                              />
                            </TableCell>
                            <TableCell
                              sx={{
                                color:
                                  query.meanExecTimeMs > 1000
                                    ? "#f44336"
                                    : "#ff9800",
                                fontWeight: 600,
                              }}
                            >
                              {query.meanExecTimeMs.toFixed(2)}
                            </TableCell>
                            <TableCell>
                              {query.calls.toLocaleString()}
                            </TableCell>
                            <TableCell>
                              <Typography
                                variant="caption"
                                sx={{
                                  fontFamily: "monospace",
                                  fontSize: "0.75rem",
                                }}
                              >
                                {query.queryText.substring(0, 100)}...
                              </Typography>
                            </TableCell>
                            <TableCell>{query.suggestion}</TableCell>
                          </TableRow>
                        ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : (
                <Alert severity="info">
                  No slow queries detected or pg_stat_statements extension not
                  enabled.
                </Alert>
              )}
            </CardContent>
          </Card>
        )}

        {configData && (
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography
                variant="h5"
                gutterBottom
                sx={{ display: "flex", alignItems: "center", color: "#667eea" }}
              >
                <Settings sx={{ mr: 1 }} /> Configuration Optimization
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={3}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      Memory (MB)
                    </Typography>
                    <Typography variant="h5">
                      {configData.totalMemoryMB?.toLocaleString()}
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} md={3}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      CPU Cores
                    </Typography>
                    <Typography variant="h5">{configData.cpuCores}</Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} md={3}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      Current Connections
                    </Typography>
                    <Typography variant="h5">
                      {configData.currentConnections}
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} md={3}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      Max Connections
                    </Typography>
                    <Typography variant="h5">
                      {configData.maxConnections}
                    </Typography>
                  </Paper>
                </Grid>
              </Grid>

              {configData.suggestions?.length > 0 ? (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Priority</TableCell>
                        <TableCell>Parameter</TableCell>
                        <TableCell>Current Value</TableCell>
                        <TableCell>Suggested Value</TableCell>
                        <TableCell>Reason & Impact</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {configData.suggestions.map(
                        (suggestion: ConfigSuggestion, i: number) => (
                          <TableRow key={i}>
                            <TableCell>
                              <Chip
                                label={suggestion.priority}
                                color={getPriorityColor(suggestion.priority)}
                                size="small"
                              />
                            </TableCell>
                            <TableCell sx={{ fontWeight: 600 }}>
                              {suggestion.parameter}
                            </TableCell>
                            <TableCell>{suggestion.currentValue}</TableCell>
                            <TableCell
                              sx={{ color: "#4caf50", fontWeight: 600 }}
                            >
                              {suggestion.suggestedValue}
                            </TableCell>
                            <TableCell>
                              <Typography variant="body2">
                                {suggestion.reason}
                              </Typography>
                              <Typography
                                variant="caption"
                                color="text.secondary"
                              >
                                {suggestion.impact}
                              </Typography>
                            </TableCell>
                          </TableRow>
                        ),
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : (
                <Alert severity="success" icon={<CheckCircle />}>
                  Your PostgreSQL configuration is well-optimized! No changes
                  recommended.
                </Alert>
              )}
            </CardContent>
          </Card>
        )}

        {vacuumData && (
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography
                variant="h5"
                gutterBottom
                sx={{ display: "flex", alignItems: "center", color: "#667eea" }}
              >
                <CleaningServices sx={{ mr: 1 }} /> VACUUM Analysis
              </Typography>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={12} md={4}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      Overall Health
                    </Typography>
                    <Chip
                      label={vacuumData.overallHealth}
                      color={
                        vacuumData.overallHealth === "GOOD"
                          ? "success"
                          : vacuumData.overallHealth === "FAIR"
                            ? "info"
                            : vacuumData.overallHealth === "WARNING"
                              ? "warning"
                              : "error"
                      }
                      sx={{ mt: 1, fontSize: "1rem", fontWeight: 600 }}
                    />
                  </Paper>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      Total Dead Tuples
                    </Typography>
                    <Typography variant="h4" sx={{ color: "#f44336" }}>
                      {vacuumData.totalDeadTuples?.toLocaleString() || 0}
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Paper sx={{ p: 2, background: "#f5f5f5" }}>
                    <Typography variant="body2" color="text.secondary">
                      Tables Needing VACUUM
                    </Typography>
                    <Typography variant="h4" sx={{ color: "#ff9800" }}>
                      {vacuumData.tablesNeedingVacuum?.length || 0}
                    </Typography>
                  </Paper>
                </Grid>
              </Grid>

              {vacuumData.tablesNeedingVacuum?.length > 0 ? (
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Priority</TableCell>
                        <TableCell>Table</TableCell>
                        <TableCell>Live Tuples</TableCell>
                        <TableCell>Dead Tuples</TableCell>
                        <TableCell>Dead %</TableCell>
                        <TableCell>Last Vacuum</TableCell>
                        <TableCell>Recommendation</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {vacuumData.tablesNeedingVacuum
                        .slice(0, 15)
                        .map((table: TableVacuumStatus, i: number) => (
                          <TableRow key={i}>
                            <TableCell>
                              <Chip
                                label={table.priority}
                                color={getPriorityColor(table.priority)}
                                size="small"
                              />
                            </TableCell>
                            <TableCell>
                              {table.schema}.{table.table}
                            </TableCell>
                            <TableCell>
                              {table.liveTuples.toLocaleString()}
                            </TableCell>
                            <TableCell
                              sx={{ color: "#f44336", fontWeight: 600 }}
                            >
                              {table.deadTuples.toLocaleString()}
                            </TableCell>
                            <TableCell
                              sx={{
                                color:
                                  table.deadTuplePercent > 20
                                    ? "#f44336"
                                    : "#ff9800",
                                fontWeight: 600,
                              }}
                            >
                              {table.deadTuplePercent.toFixed(2)}%
                            </TableCell>
                            <TableCell>
                              <Typography variant="caption">
                                {table.lastVacuum === "Never"
                                  ? "❌ Never"
                                  : `✅ ${table.lastVacuum}`}
                              </Typography>
                            </TableCell>
                            <TableCell>{table.suggestion}</TableCell>
                          </TableRow>
                        ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : (
                <Alert severity="success" icon={<CheckCircle />}>
                  All tables are healthy! No VACUUM needed at this time.
                </Alert>
              )}
            </CardContent>
          </Card>
        )}
      </Container>
    </Box>
  );
}
