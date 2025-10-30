'use client';

import React, { useState, useEffect } from 'react';
import {
  Box,
  Container,
  Grid,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  CircularProgress,
  Alert,
  Button,
} from '@mui/material';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import {
  BarChart as BarChartIcon,
  Refresh as RefreshIcon,
  Memory as MemoryIcon,
  Storage as StorageIcon,
  Speed as SpeedIcon,
} from '@mui/icons-material';
import DashboardHeader from '@/components/DashboardHeader';
import { useConfig } from '@/hooks/useConfig';

export default function MetricsPage() {
  const [clusterMetrics, setClusterMetrics] = useState<any>(null);
  const [timeSeriesData, setTimeSeriesData] = useState<any>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const config = useConfig();

  useEffect(() => {
    if (config) {
      fetchMetrics();
      const interval = setInterval(fetchMetrics, 30000);
      return () => clearInterval(interval);
    }
  }, [config]);

  const fetchMetrics = async () => {
    try {
      const [metricsRes, timeSeriesRes] = await Promise.all([
        fetch(`${config.API_URL}/api/kubernetes/metrics/cluster`),
        fetch(`${config.API_URL}/api/kubernetes/metrics/timeseries?hours=6`)
      ]);

      if (!metricsRes.ok || !timeSeriesRes.ok) {
        throw new Error('Failed to fetch metrics');
      }

      const metricsData = await metricsRes.json();
      const timeSeriesData = await timeSeriesRes.json();

      setClusterMetrics(metricsData);
      setTimeSeriesData(timeSeriesData);
      setLoading(false);
    } catch (err: any) {
      setError(err.message);
      setLoading(false);
    }
  };

  if (loading && !clusterMetrics) {
    return (
      <Box sx={{ minHeight: "100vh", background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)" }}>
        <DashboardHeader />
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="80vh">
          <CircularProgress sx={{ color: "white" }} />
        </Box>
      </Box>
    );
  }

  const formatBytes = (bytes: number) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
  };

  const formatCpu = (nanoCores: number) => {
    return `${(nanoCores / 1000000000).toFixed(2)} cores`;
  };

  const cpuTimeSeries = timeSeriesData.find((ts: any) => ts.metricType === 'cpu');
  const memoryTimeSeries = timeSeriesData.find((ts: any) => ts.metricType === 'memory');

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

  return (
    <Box sx={{ minHeight: "100vh", background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)" }}>
      <DashboardHeader />

      <Container maxWidth="xl" sx={{ mt: 4, pb: 4 }}>
        {/* Header Section */}
        <Box sx={{ mb: 3, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <BarChartIcon sx={{ fontSize: 40, color: "white" }} />
            <Typography variant="h4" sx={{ color: "white", fontWeight: "bold" }}>
              System Metrics Monitoring
            </Typography>
          </Box>
          <Button
            variant="contained"
            startIcon={<RefreshIcon />}
            onClick={fetchMetrics}
            disabled={loading}
            sx={{ bgcolor: "white", color: "#667eea", "&:hover": { bgcolor: "#f5f5f5" } }}
          >
            Refresh
          </Button>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {/* Cluster Overview Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <Box>
                  <Typography variant="h3" sx={{ fontWeight: "bold" }}>
                    {clusterMetrics?.cpuUsagePercent}
                  </Typography>
                  <Typography variant="body2">CPU Usage</Typography>
                  <Typography variant="caption" sx={{ opacity: 0.9 }}>
                    {formatCpu(clusterMetrics?.totalCpuUsageNanoCores || 0)}
                  </Typography>
                </Box>
                <SpeedIcon sx={{ fontSize: 48, opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <Box>
                  <Typography variant="h3" sx={{ fontWeight: "bold" }}>
                    {clusterMetrics?.memoryUsagePercent}
                  </Typography>
                  <Typography variant="body2">Memory Usage</Typography>
                  <Typography variant="caption" sx={{ opacity: 0.9 }}>
                    {formatBytes(clusterMetrics?.totalMemoryUsageBytes || 0)}
                  </Typography>
                </Box>
                <MemoryIcon sx={{ fontSize: 48, opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <Box>
                  <Typography variant="h3" sx={{ fontWeight: "bold" }}>
                    {clusterMetrics?.diskUsagePercent}
                  </Typography>
                  <Typography variant="body2">Disk Usage</Typography>
                  <Typography variant="caption" sx={{ opacity: 0.9 }}>
                    {formatBytes(clusterMetrics?.totalDiskUsageBytes || 0)}
                  </Typography>
                </Box>
                <StorageIcon sx={{ fontSize: 48, opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ background: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)', color: 'white' }}>
            <CardContent>
              <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <Box>
                  <Typography variant="h3" sx={{ fontWeight: "bold" }}>
                    {clusterMetrics?.totalPods}
                  </Typography>
                  <Typography variant="body2">Total Pods</Typography>
                  <Typography variant="caption" sx={{ opacity: 0.9 }}>
                    Running: {clusterMetrics?.runningPods} | Failed: {clusterMetrics?.failedPods}
                  </Typography>
                </Box>
                <StorageIcon sx={{ fontSize: 48, opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Charts Section */}
      <Grid container spacing={3} sx={{ mb: 3 }}>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>CPU Usage Over Time</Typography>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={cpuTimeSeries?.dataPoints || []}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="timestamp"
                    tickFormatter={(timestamp) => new Date(timestamp).toLocaleTimeString()}
                  />
                  <YAxis />
                  <Tooltip labelFormatter={(timestamp) => new Date(timestamp).toLocaleString()} />
                  <Legend />
                  <Area type="monotone" dataKey="value" stroke="#8884d8" fill="#8884d8" />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Memory Usage Over Time</Typography>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={memoryTimeSeries?.dataPoints || []}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="timestamp"
                    tickFormatter={(timestamp) => new Date(timestamp).toLocaleTimeString()}
                  />
                  <YAxis />
                  <Tooltip labelFormatter={(timestamp) => new Date(timestamp).toLocaleString()} />
                  <Legend />
                  <Area type="monotone" dataKey="value" stroke="#82ca9d" fill="#82ca9d" />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

      </Grid>

      {/* Data Tables Section */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ fontWeight: "bold" }}>
                Top Pods by CPU
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Pod Name</TableCell>
                      <TableCell>Namespace</TableCell>
                      <TableCell align="right">CPU Usage</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(clusterMetrics?.topPodsByCpu || []).slice(0, 5).map((pod: any) => (
                      <TableRow key={pod.podName}>
                        <TableCell>{pod.podName}</TableCell>
                        <TableCell><Chip label={pod.namespace} size="small" /></TableCell>
                        <TableCell align="right">{pod.cpuUsagePercent}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ fontWeight: "bold" }}>
                Top Pods by Memory
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Pod Name</TableCell>
                      <TableCell>Namespace</TableCell>
                      <TableCell align="right">Memory Usage</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(clusterMetrics?.topPodsByMemory || []).slice(0, 5).map((pod: any) => (
                      <TableRow key={pod.podName}>
                        <TableCell>{pod.podName}</TableCell>
                        <TableCell><Chip label={pod.namespace} size="small" /></TableCell>
                        <TableCell align="right">{formatBytes(pod.memoryUsageBytes)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ fontWeight: "bold" }}>
                Node Metrics
              </Typography>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Node Name</TableCell>
                      <TableCell align="right">CPU Usage</TableCell>
                      <TableCell align="right">Memory Usage</TableCell>
                      <TableCell align="right">Disk Usage</TableCell>
                      <TableCell align="right">Network RX</TableCell>
                      <TableCell align="right">Network TX</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(clusterMetrics?.nodeMetrics || []).map((node: any) => (
                      <TableRow key={node.nodeName}>
                        <TableCell>{node.nodeName}</TableCell>
                        <TableCell align="right">{node.cpuUsagePercent}</TableCell>
                        <TableCell align="right">{node.memoryUsagePercent}</TableCell>
                        <TableCell align="right">{node.diskUsagePercent}</TableCell>
                        <TableCell align="right">{formatBytes(node.networkReceiveBytes)}</TableCell>
                        <TableCell align="right">{formatBytes(node.networkTransmitBytes)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      </Container>
    </Box>
  );
}
