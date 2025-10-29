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
} from "@mui/material";
import {
  Refresh as RefreshIcon,
  Cloud as CloudIcon,
  Storage as StorageIcon,
  Memory as MemoryIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Warning as WarningIcon,
  Pending as PendingIcon,
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
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

interface PodMetrics {
  name: string;
  namespace: string;
  status: string;
  phase: string;
  restartCount: number;
  age: string;
  node: string;
  healthStatus: string;
  warnings: string[];
  resources?: {
    cpuUsage?: string;
    memoryUsage?: string;
    cpuRequest?: string;
    cpuLimit?: string;
    memoryRequest?: string;
    memoryLimit?: string;
  };
}

interface NodeMetrics {
  name: string;
  status: string;
  capacity: Record<string, string>;
  allocatable: Record<string, string>;
  version: string;
  osImage: string;
}

interface ClusterOverview {
  totalNodes: number;
  healthyNodes: number;
  totalPods: number;
  podsByStatus: Record<string, number>;
  podsByNamespace: Record<string, number>;
}

export default function KubernetesMonitoring() {
  const [pods, setPods] = useState<PodMetrics[]>([]);
  console.log("pods", pods)
  const [nodes, setNodes] = useState<NodeMetrics[]>([]);
  const [clusterOverview, setClusterOverview] = useState<ClusterOverview | null>(null);
  const [namespaces, setNamespaces] = useState<string[]>([]);
  const [selectedNamespace, setSelectedNamespace] = useState<string>("all");
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const config = useConfig()
  const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8181';

  const fetchData = async () => {

    if (!config) return;

    setLoading(true);
    setError(null);

    try {

      const token = localStorage.getItem('token');
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
      
      console.log("headers", headers)
      console.log("token", token)

      const [podsResponse, nodesResponse, clusterResponse, namespacesResponse] = await Promise.all([
        axios.get<PodMetrics[]>(`${config.API_URL}/api/kubernetes/pods`, {
          params: selectedNamespace !== "all" ? { namespace: selectedNamespace } : {},
           headers,
        }),
        axios.get<NodeMetrics[]>(`${config.API_URL}/api/kubernetes/nodes`, {headers}),
        axios.get<ClusterOverview>(`${config.API_URL}/api/kubernetes/cluster/overview`, {headers}),
        axios.get(`${config.API_URL}/api/kubernetes/namespaces`, {headers}),
      ]);
console.log("111111",podsResponse)
      setPods(podsResponse.data);
      setNodes(nodesResponse.data);
      setClusterOverview(clusterResponse.data);
      
      const namespaceList = namespacesResponse.data.map((ns: any) => ns.name);
      setNamespaces(namespaceList);
    } catch (err: any) {
      console.error("Error fetching Kubernetes data:", err);
      setError(err.response?.data || "Failed to connect to Kubernetes API. Ensure kubeconfig is properly configured.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 15000);
    return () => clearInterval(interval);
  }, [selectedNamespace,config]);

  const filteredPods = pods.filter((pod) =>
    pod.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    pod.namespace.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getStatusColor = (status: string) => {
    if (status === "Running") return "success";
    if (status === "Pending" || status === "ContainerCreating") return "warning";
    if (status === "Error" || status === "CrashLoopBackOff" || status === "ImagePullBackOff") return "error";
    return "default";
  };

  const getHealthIcon = (healthStatus: string) => {
    if (healthStatus === "Healthy") return <CheckCircleIcon color="success" />;
    if (healthStatus === "Warning") return <WarningIcon color="warning" />;
    if (healthStatus === "Pending") return <PendingIcon color="info" />;
    return <ErrorIcon color="error" />;
  };

  const podStatusData = clusterOverview?.podsByStatus
    ? Object.entries(clusterOverview.podsByStatus).map(([name, value]) => ({
        name,
        value,
      }))
    : [];

  const COLORS = ["#9c27b0", "#673ab7", "#3f51b5", "#f50057", "#ff9800"];

  return (
    <Box sx={{ minHeight: "100vh", background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)" }}>
      <DashboardHeader />

      <Container maxWidth="xl" sx={{ mt: 4, pb: 4 }}>
        {/* Header Section */}
        <Box sx={{ mb: 3, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <CloudIcon sx={{ fontSize: 40, color: "white" }} />
            <Typography variant="h4" sx={{ color: "white", fontWeight: "bold" }}>
              Kubernetes Cluster Monitoring
            </Typography>
          </Box>
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

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {/* Cluster Overview Cards */}
        {clusterOverview && (
          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)", color: "white" }}>
                <CardContent>
                  <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <Box>
                      <Typography variant="h3" sx={{ fontWeight: "bold" }}>
                        {clusterOverview.totalPods}
                      </Typography>
                      <Typography variant="body2">Total Pods</Typography>
                    </Box>
                    <StorageIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ background: "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)", color: "white" }}>
                <CardContent>
                  <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <Box>
                      <Typography variant="h3" sx={{ fontWeight: "bold" }}>
                        {clusterOverview.totalNodes}
                      </Typography>
                      <Typography variant="body2">Total Nodes</Typography>
                    </Box>
                    <MemoryIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ background: "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)", color: "white" }}>
                <CardContent>
                  <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <Box>
                      <Typography variant="h3" sx={{ fontWeight: "bold" }}>
                        {clusterOverview.healthyNodes}
                      </Typography>
                      <Typography variant="body2">Healthy Nodes</Typography>
                    </Box>
                    <CheckCircleIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ background: "linear-gradient(135deg, #fa709a 0%, #fee140 100%)", color: "white" }}>
                <CardContent>
                  <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <Box>
                      <Typography variant="h3" sx={{ fontWeight: "bold" }}>
                        {Object.keys(clusterOverview.podsByNamespace).length}
                      </Typography>
                      <Typography variant="body2">Namespaces</Typography>
                    </Box>
                    <CloudIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Charts Section */}
        {clusterOverview && podStatusData.length > 0 && (
          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Pod Status Distribution
                  </Typography>
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie
                        data={podStatusData}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="value"
                      >
                        {podStatusData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Pods by Namespace
                  </Typography>
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart
                      data={Object.entries(clusterOverview.podsByNamespace).map(([name, count]) => ({
                        name,
                        count,
                      }))}
                    >
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" />
                      <YAxis />
                      <Tooltip />
                      <Bar dataKey="count" fill="#667eea" />
                    </BarChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Filters Section */}
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6} md={4}>
                <FormControl fullWidth>
                  <InputLabel>Namespace</InputLabel>
                  <Select
                    value={selectedNamespace}
                    label="Namespace"
                    onChange={(e) => setSelectedNamespace(e.target.value)}
                  >
                    <MenuItem value="all">All Namespaces</MenuItem>
                    {namespaces.map((ns) => (
                      <MenuItem key={ns} value={ns}>
                        {ns}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6} md={8}>
                <TextField
                  fullWidth
                  label="Search Pods"
                  placeholder="Search by pod name or namespace..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </Grid>
            </Grid>
          </CardContent>
        </Card>

        {/* Pods Table */}
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <StorageIcon /> Pod Metrics
              {loading && <CircularProgress size={20} />}
            </Typography>

            <TableContainer component={Paper} sx={{ mt: 2, maxHeight: 600 }}>
              <Table stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell><strong>Status</strong></TableCell>
                    <TableCell><strong>Name</strong></TableCell>
                    <TableCell><strong>Namespace</strong></TableCell>
                    <TableCell><strong>Phase</strong></TableCell>
                    <TableCell><strong>Restarts</strong></TableCell>
                    <TableCell><strong>Age</strong></TableCell>
                    <TableCell><strong>Node</strong></TableCell>
                    <TableCell><strong>CPU Request</strong></TableCell>
                    <TableCell><strong>Memory Request</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredPods.map((pod) => (
                    <TableRow
                      key={`${pod.namespace}-${pod.name}`}
                      sx={{
                        "&:hover": { backgroundColor: "#f5f5f5" },
                        backgroundColor: pod.warnings.length > 0 ? "#fff3e0" : "inherit",
                      }}
                    >
                      <TableCell>{getHealthIcon(pod.healthStatus)}</TableCell>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: "bold" }}>
                          {pod.name}
                        </Typography>
                        {pod.warnings.length > 0 && (
                          <Box sx={{ mt: 0.5 }}>
                            {pod.warnings.map((warning, idx) => (
                              <Chip
                                key={idx}
                                label={warning}
                                size="small"
                                color="warning"
                                sx={{ mr: 0.5, mt: 0.5 }}
                              />
                            ))}
                          </Box>
                        )}
                      </TableCell>
                      <TableCell>
                        <Chip label={pod.namespace} size="small" color="primary" variant="outlined" />
                      </TableCell>
                      <TableCell>
                        <Chip label={pod.phase} size="small" color={getStatusColor(pod.phase)} />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={pod.restartCount}
                          size="small"
                          color={pod.restartCount > 3 ? "error" : "default"}
                        />
                      </TableCell>
                      <TableCell>{pod.age}</TableCell>
                      <TableCell>{pod.node || "N/A"}</TableCell>
                      <TableCell>{pod.resources?.cpuRequest || "N/A"}</TableCell>
                      <TableCell>{pod.resources?.memoryRequest || "N/A"}</TableCell>
                    </TableRow>
                  ))}
                  {filteredPods.length === 0 && !loading && (
                    <TableRow>
                      <TableCell colSpan={9} align="center">
                        <Typography variant="body2" color="textSecondary">
                          No pods found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>

        {/* Nodes Table */}
        {nodes.length > 0 && (
          <Card sx={{ mt: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <MemoryIcon /> Node Metrics
              </Typography>

              <TableContainer component={Paper} sx={{ mt: 2 }}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell><strong>Name</strong></TableCell>
                      <TableCell><strong>Status</strong></TableCell>
                      <TableCell><strong>CPU Capacity</strong></TableCell>
                      <TableCell><strong>Memory Capacity</strong></TableCell>
                      <TableCell><strong>Version</strong></TableCell>
                      <TableCell><strong>OS Image</strong></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {nodes.map((node) => (
                      <TableRow key={node.name}>
                        <TableCell sx={{ fontWeight: "bold" }}>{node.name}</TableCell>
                        <TableCell>
                          <Chip
                            label={node.status}
                            size="small"
                            color={node.status === "True" ? "success" : "error"}
                          />
                        </TableCell>
                        <TableCell>{node.capacity?.cpu || "N/A"}</TableCell>
                        <TableCell>{node.capacity?.memory || "N/A"}</TableCell>
                        <TableCell>{node.version}</TableCell>
                        <TableCell>{node.osImage}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        )}
      </Container>
    </Box>
  );
}
