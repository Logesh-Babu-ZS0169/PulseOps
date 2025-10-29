"use client";

import { useState, useEffect } from "react";
import axios from "axios";
import DashboardHeader from '@/components/DashboardHeader';

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
  LinearProgress,
  Tabs,
  Tab,
  FilledTextFieldProps,
  OutlinedTextFieldProps,
  StandardTextFieldProps,
  TextField,
  TextFieldVariants,
  Alert,
} from "@mui/material";
import {
  Refresh as RefreshIcon,
  Assessment as AssessmentIcon,
  Description as DescriptionIcon,
  CheckCircle as CheckCircleIcon,
  TrendingUp as TrendingUpIcon,
  Warning as WarningIcon,
  BugReport as BugReportIcon,
  CalendarToday as CalendarIcon,
  Download as DownloadIcon,
} from "@mui/icons-material";
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import dayjs, { Dayjs } from 'dayjs';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

interface HourlyMetrics {
  timeFrameStart: string;
  timeFrameEnd: string;
  totalIngestion: number;
  stagedInIngestion: number;
  inProgressCount: number;
  completedCount: number;
  failedInProcess: number;
  failedInIngestion: number;
  abortedCount: number;
}

interface DailyTotal {
  count: number;
}

interface RecoveredDocument {
  dcnId: string;
  siCaseId: string;
  documentType: string;
  downloadCompletedOn: string;
}

interface WaitingDocument {
  dcnId: string;
  siCaseId: string;
  downloadCompletedOn: string;
  status: string;
  timeRemainingToProcess: string;
}

interface InboundFailedDocument {
  downloadCompletedOn: string;
  transactionId: string;
  dcnId: string;
  siCaseId: string;
}

interface ProcessFailedDocument {
  downloadCompletedOn: string;
  dcnId: string;
  siCaseId: string;
  status: string;
  errorCode: string;
  errorMessage: string;
  rootPipelineId: string;
  actionId: string;
  log: string;
}

interface AbortedDocument {
  downloadCompletedOn: string;
  dcnId: string;
  siCaseId: string;
  status: string;
  errorCode: string;
  errorMessage: string;
}

interface PredictionData {
  predictionDate: string;
  predictedCount: number;
  sevenDayAverage: number;
  fourteenDayAverage: number;
  thirtyDayAverage: number;
  trend: string;
  confidenceScore: number;
  historicalData: Array<{
    date: string;
    count: number;
    dayOfWeek: string;
  }>;
}

interface HourlyPredictionData {
  predictionDate: string;
  totalPredictedCount: number;
  hourlyPredictions: Array<{
    hourStart: string;
    hourEnd: string;
    predictedCount: number;
    confidenceScore: number;
    hourOfDay: number;
    timeLabel: string;
  }>;
  overallConfidence: number;
  description: string;
}

interface DayWisePredictionData {
  predictions: Array<{
    date: string;
    dayOfWeek: string;
    predictedCount: number;
    trend: string;
    confidenceScore: number;
  }>;
  overallTrend: string;
  averageConfidence: number;
  description: string;
}

const formatChartData = (data: HourlyMetrics[]) => {
  return data.map((item) => ({
    time: new Date(item.timeFrameStart).toLocaleTimeString("en-US", {
      hour: "2-digit",
      minute: "2-digit",
    }),
    Total: item.totalIngestion,
    Staged: item.stagedInIngestion,
    "In Progress": item.inProgressCount,
    Completed: item.completedCount,
    "Failed (Process)": item.failedInProcess,
    "Failed (Ingestion)": item.failedInIngestion,
    Aborted: item.abortedCount,
  }));
};

export default function Dashboard() {
  const [tabValue, setTabValue] = useState(0);
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
  const [startTime, setStartTime] = useState<Dayjs | null>(null);
  const [endTime, setEndTime] = useState<Dayjs | null>(null);
  const [failureRecoveryDate, setFailureRecoveryDate] = useState<Dayjs>(dayjs());
  const [failureStartTime, setFailureStartTime] = useState<Dayjs | null>(null);
  const [failureEndTime, setFailureEndTime] = useState<Dayjs | null>(null);
  const [dailyTotal, setDailyTotal] = useState<DailyTotal>({ count: 0 });
  const [commercialData, setCommercialData] = useState<HourlyMetrics[]>([]);
  const [gbdData, setGbdData] = useState<HourlyMetrics[]>([]);
  const [lastUpdated, setLastUpdated] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(true);
  
  const [recoveredDocs, setRecoveredDocs] = useState<RecoveredDocument[]>([]);
  const [waitingDocs, setWaitingDocs] = useState<WaitingDocument[]>([]);
  const [inboundFailedDocs, setInboundFailedDocs] = useState<InboundFailedDocument[]>([]);
  const [processFailedDocs, setProcessFailedDocs] = useState<ProcessFailedDocument[]>([]);
  const [abortedDocs, setAbortedDocs] = useState<AbortedDocument[]>([]);
  const [failureLoading, setFailureLoading] = useState<boolean>(false);
  
  const [predictionData, setPredictionData] = useState<PredictionData | null>(null);
  const [predictionLoading, setPredictionLoading] = useState<boolean>(false);
  const [hourlyPredictionData, setHourlyPredictionData] = useState<HourlyPredictionData | null>(null);
  const [hourlyPredictionLoading, setHourlyPredictionLoading] = useState<boolean>(false);
  const [dayWisePredictionData, setDayWisePredictionData] = useState<DayWisePredictionData | null>(null);
  const [dayWisePredictionLoading, setDayWisePredictionLoading] = useState<boolean>(false);

  const config = useConfig()
  const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8181';

  const fetchData = async (date?: Dayjs, start?: Dayjs | null, end?: Dayjs | null) => {

    if (!config) return;

    try {
      setLoading(true);
      const targetDate = (date || selectedDate).format('YYYY-MM-DD');
      const params: any = { date: targetDate };
      
      const useStartTime = start !== undefined ? start : startTime;
      const useEndTime = end !== undefined ? end : endTime;
      
      if (useStartTime && useEndTime) {
        params.startTime = useStartTime.format('HH:mm:ss');
        params.endTime = useEndTime.format('HH:mm:ss');
      }
      
      const [dailyRes, commercialRes, gbdRes] = await Promise.all([
        axios.get(`${config.API_URL}/api/metrics/daily-total`, { params }),
        axios.get(`${config.API_URL}/api/metrics/hourly/commercial`, { params }),
        axios.get(`${config.API_URL}/api/metrics/hourly/gbd`, { params }),
      ]);

      setDailyTotal(dailyRes.data);
      setCommercialData(commercialRes.data);
      setGbdData(gbdRes.data);
      setLastUpdated(new Date().toLocaleString());
    } catch (error) {
      console.error("Error fetching metrics:", error);
    } finally {
      setLoading(false);
    }
  };
  
  const handleDateChange = (newDate: Dayjs | null) => {
    if (newDate) {
      setSelectedDate(newDate);
      fetchData(newDate, startTime, endTime);
    }
  };
  
  const handleStartTimeChange = (newTime: Dayjs | null) => {
    setStartTime(newTime);
    if (newTime && endTime) {
      fetchData(selectedDate, newTime, endTime);
    }
  };
  
  const handleEndTimeChange = (newTime: Dayjs | null) => {
    setEndTime(newTime);
    if (startTime && newTime) {
      fetchData(selectedDate, startTime, newTime);
    }
  };
  
  const handleClearTimeFilter = () => {
    setStartTime(null);
    setEndTime(null);
    fetchData(selectedDate, null, null);
  };

  const fetchFailureRecoveryData = async (date?: Dayjs, start?: Dayjs | null, end?: Dayjs | null) => {
    try {
      setFailureLoading(true);
      const targetDate = (date || failureRecoveryDate).format('YYYY-MM-DD');
      const params: any = { date: targetDate };
      
      const useStartTime = start !== undefined ? start : failureStartTime;
      const useEndTime = end !== undefined ? end : failureEndTime;
      
      if (useStartTime && useEndTime) {
        params.startTime = useStartTime.format('HH:mm:ss');
        params.endTime = useEndTime.format('HH:mm:ss');
      }
      
      const [recovered, waiting, inboundFailed, processFailed, aborted] = await Promise.all([
        axios.get(`${config.API_URL}/api/failure-recovery/recovered`, { params }),
        axios.get(`${config.API_URL}/api/failure-recovery/waiting`, { params }),
        axios.get(`${config.API_URL}/api/failure-recovery/inbound-failed`, { params }),
        axios.get(`${config.API_URL}/api/failure-recovery/process-failed`, { params }),
        axios.get(`${config.API_URL}/api/failure-recovery/aborted`, { params }),
      ]);
      
      setRecoveredDocs(recovered.data);
      setWaitingDocs(waiting.data);
      setInboundFailedDocs(inboundFailed.data);
      setProcessFailedDocs(processFailed.data);
      setAbortedDocs(aborted.data);
    } catch (error) {
      console.error("Error fetching failure recovery data:", error);
    } finally {
      setFailureLoading(false);
    }
  };
  
  const handleFailureRecoveryDateChange = (newDate: Dayjs | null) => {
    if (newDate) {
      setFailureRecoveryDate(newDate);
      fetchFailureRecoveryData(newDate, failureStartTime, failureEndTime);
    }
  };
  
  const handleFailureStartTimeChange = (newTime: Dayjs | null) => {
    setFailureStartTime(newTime);
    if (newTime && failureEndTime) {
      fetchFailureRecoveryData(failureRecoveryDate, newTime, failureEndTime);
    }
  };
  
  const handleFailureEndTimeChange = (newTime: Dayjs | null) => {
    setFailureEndTime(newTime);
    if (failureStartTime && newTime) {
      fetchFailureRecoveryData(failureRecoveryDate, failureStartTime, newTime);
    }
  };
  
  const handleFailureClearTimeFilter = () => {
    setFailureStartTime(null);
    setFailureEndTime(null);
    fetchFailureRecoveryData(failureRecoveryDate, null, null);
  };

  const fetchPredictionData = async () => {
    try {
      setPredictionLoading(true);
      const response = await axios.get(`${config.API_URL}/api/prediction/next-day`);
      setPredictionData(response.data);
    } catch (error) {
      console.error("Error fetching prediction data:", error);
    } finally {
      setPredictionLoading(false);
    }
  };
  
  const fetchHourlyPredictionData = async () => {
    try {
      setHourlyPredictionLoading(true);
      const response = await axios.get(`${config.API_URL}/api/prediction/hourly`);
      setHourlyPredictionData(response.data);
    } catch (error) {
      console.error("Error fetching hourly prediction data:", error);
    } finally {
      setHourlyPredictionLoading(false);
    }
  };
  
  const fetchDayWisePredictionData = async () => {
    try {
      setDayWisePredictionLoading(true);
      const response = await axios.get(`${config.API_URL}/api/prediction/day-wise`, { params: { days: 7 } });
      setDayWisePredictionData(response.data);
    } catch (error) {
      console.error("Error fetching day-wise prediction data:", error);
    } finally {
      setDayWisePredictionLoading(false);
    }
  };
  
  const fetchAllPredictions = async () => {
    await Promise.all([
      fetchPredictionData(),
      fetchHourlyPredictionData(),
      fetchDayWisePredictionData()
    ]);
  };

  useEffect(() => {
    fetchData();
  }, [selectedDate, config]);
  
  useEffect(() => {
    if (tabValue === 1) {
      fetchFailureRecoveryData();
    } else if (tabValue === 2) {
      fetchAllPredictions();
      const interval = setInterval(() => {
        fetchAllPredictions();
      }, 3600000);
      return () => clearInterval(interval);
    }
  }, [tabValue, failureRecoveryDate]);

  const calculateTotals = (data: HourlyMetrics[]) => {
    return data.reduce(
      (acc, curr) => ({
        total: acc.total + curr.totalIngestion,
        completed: acc.completed + curr.completedCount,
        failed: acc.failed + curr.failedInProcess + curr.failedInIngestion,
        inProgress: acc.inProgress + curr.inProgressCount,
      }),
      { total: 0, completed: 0, failed: 0, inProgress: 0 },
    );
  };

  const commercialTotals = calculateTotals(commercialData);
  const gbdTotals = calculateTotals(gbdData);

  return (
    <LocalizationProvider dateAdapter={AdapterDayjs}>
      <Box sx={{ bgcolor: "#f5f5f5", minHeight: "100vh", pb: 4 }}>
        <DashboardHeader />
        {/* Header */}
        <Paper elevation={3} sx={{ mb: 3, borderRadius: 0 }}>
          <Container maxWidth="xl">
            <Box
              sx={{
                py: 3,
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <Box>
                <Typography
                  variant="h4"
                  component="h1"
                  fontWeight="bold"
                  color="primary"
                >
                  Inbound Ingestion Metrics
                </Typography>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ mt: 0.5 }}
                >
                  Real-time monitoring dashboard
                </Typography>
              </Box>
              <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                <Box>
                  <Typography variant="caption" color="text.secondary" display="block">
                    Select Date
                  </Typography>
                  <DatePicker
                    value={tabValue === 0 ? selectedDate : failureRecoveryDate}
                    onChange={tabValue === 0 ? handleDateChange : handleFailureRecoveryDateChange}
                    inputFormat="YYYY-MM-DD"
        renderInput={(params: JSX.IntrinsicAttributes & { variant?: TextFieldVariants | undefined; } & Omit<FilledTextFieldProps | OutlinedTextFieldProps | StandardTextFieldProps, "variant">) => (
          <TextField {...params} size="small" sx={{ width: 160 }} />
        )}
                  />
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary" display="block">
                    Start Time
                  </Typography>
                  <TimePicker
                      value={tabValue === 0 ? startTime : failureStartTime}
                      onChange={tabValue === 0 ? handleStartTimeChange : handleFailureStartTimeChange}
                      inputFormat="HH:mm"
                      renderInput={(params: JSX.IntrinsicAttributes & { variant?: TextFieldVariants | undefined; } & Omit<
                        FilledTextFieldProps | OutlinedTextFieldProps | StandardTextFieldProps,
                        "variant"
                      >) => (
                        <TextField {...params} size="small" sx={{ width: 130 }} />
                      )}
                    />
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary" display="block">
                    End Time
                  </Typography>
 <TimePicker
          value={tabValue === 0 ? endTime : failureEndTime}
          onChange={tabValue === 0 ? handleEndTimeChange : handleFailureEndTimeChange}
          inputFormat="HH:mm"
          renderInput={(params: any) => (
            <TextField {...params} size="small" sx={{ width: 130 }} />
          )}
        />
                </Box>
                {((tabValue === 0 && (startTime || endTime)) || (tabValue === 1 && (failureStartTime || failureEndTime))) && (
                  <Box>
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ visibility: "hidden" }}>
                      Clear
                    </Typography>
                    <Button
                      variant="outlined"
                      color="secondary"
                      onClick={tabValue === 0 ? handleClearTimeFilter : handleFailureClearTimeFilter}
                      size="small"
                    >
                      Clear Time
                    </Button>
                  </Box>
                )}
                <Box sx={{ textAlign: "right" }}>
                  <Typography variant="caption" color="text.secondary">
                    Last Updated
                  </Typography>
                  <Typography variant="body2" fontWeight="medium" sx={{ mb: 1 }}>
                    {lastUpdated}
                  </Typography>
                  <Button
                    variant="contained"
                    color="primary"
                    startIcon={
                      (tabValue === 0 ? loading : failureLoading) ? (
                        <CircularProgress size={20} color="inherit" />
                      ) : (
                        <RefreshIcon />
                      )
                    }
                    onClick={tabValue === 0 ? () => fetchData() : () => fetchFailureRecoveryData()}
                    disabled={tabValue === 0 ? loading : failureLoading}
                    size="small"
                  >
                    {(tabValue === 0 ? loading : failureLoading) ? "Refreshing..." : "Refresh Now"}
                  </Button>
                </Box>
              </Box>
            </Box>
          
          <Tabs value={tabValue} onChange={(e: any, newValue: any) => setTabValue(newValue)} sx={{ borderTop: 1, borderColor: "divider" }}>
            <Tab label="Main Metrics" icon={<AssessmentIcon />} iconPosition="start" />
            <Tab label="Failure Recovery Reports" icon={<BugReportIcon />} iconPosition="start" />
            <Tab label="Traffic Prediction" icon={<TrendingUpIcon />} iconPosition="start" />
          </Tabs>
        </Container>
      </Paper>

      <Container maxWidth="xl">
        {/* Tab 0: Main Metrics */}
        {tabValue === 0 && (
          <>
        {/* Summary Cards */}
        <Grid container spacing={3} sx={{ mb: 4 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Card
              sx={{
                background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
                color: "white",
              }}
            >
              <CardContent>
                <Box
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <Box>
                    <Typography variant="body2" sx={{ opacity: 0.9, mb: 1 }}>
                      Total Inbound Documents
                    </Typography>
                    <Typography variant="h4" fontWeight="bold">
                      {dailyTotal.count}
                    </Typography>
                  </Box>
                  <AssessmentIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card
              sx={{
                background: "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)",
                color: "white",
              }}
            >
              <CardContent>
                <Box
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <Box>
                    <Typography variant="body2" sx={{ opacity: 0.9, mb: 1 }}>
                      Commercial Processed
                    </Typography>
                    <Typography variant="h4" fontWeight="bold">
                      {commercialTotals.total.toLocaleString()}
                    </Typography>
                  </Box>
                  <DescriptionIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card
              sx={{
                background: "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)",
                color: "white",
              }}
            >
              <CardContent>
                <Box
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <Box>
                    <Typography variant="body2" sx={{ opacity: 0.9, mb: 1 }}>
                      GBD Processed
                    </Typography>
                    <Typography variant="h4" fontWeight="bold">
                      {gbdTotals.total.toLocaleString()}
                    </Typography>
                  </Box>
                  <TrendingUpIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card
              sx={{
                background: "linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)",
                color: "white",
              }}
            >
              <CardContent>
                <Box
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <Box>
                    <Typography variant="body2" sx={{ opacity: 0.9, mb: 1 }}>
                      Total Completed
                    </Typography>
                    <Typography variant="h4" fontWeight="bold">
                      {(
                        commercialTotals.completed + gbdTotals.completed
                      ).toLocaleString()}
                    </Typography>
                  </Box>
                  <CheckCircleIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* Next Day Prediction Card */}
        {predictionData && !predictionLoading && (
          <Card sx={{ mb: 4, background: "linear-gradient(135deg, #FA8BFF 0%, #2BD2FF 50%, #2BFF88 100%)" }}>
            <CardContent>
              <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
                <TrendingUpIcon sx={{ fontSize: 32, mr: 1, color: "white" }} />
                <Typography variant="h5" fontWeight="bold" sx={{ color: "white" }}>
                  Next Day Traffic Prediction
                </Typography>
              </Box>
              
              <Grid container spacing={3}>
                <Grid item xs={12} md={3}>
                  <Card sx={{ background: "rgba(255,255,255,0.95)" }}>
                    <CardContent>
                      <Typography variant="caption" color="text.secondary">
                        Predicted Count for {new Date(predictionData.predictionDate).toLocaleDateString()}
                      </Typography>
                      <Typography variant="h3" fontWeight="bold" color="primary" sx={{ mt: 1 }}>
                        {predictionData.predictedCount.toLocaleString()}
                      </Typography>
                      <Box sx={{ display: "flex", alignItems: "center", mt: 1 }}>
                        <Typography variant="body2" color="text.secondary" sx={{ mr: 1 }}>
                          Trend:
                        </Typography>
                        <Chip
                          label={predictionData.trend}
                          color={
                            predictionData.trend === "INCREASING" ? "success" :
                            predictionData.trend === "DECREASING" ? "error" :
                            "default"
                          }
                          size="small"
                        />
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
                
                <Grid item xs={12} md={3}>
                  <Card sx={{ background: "rgba(255,255,255,0.95)" }}>
                    <CardContent>
                      <Typography variant="caption" color="text.secondary">
                        Confidence Score
                      </Typography>
                      <Box sx={{ display: "flex", alignItems: "baseline", mt: 1 }}>
                        <Typography variant="h3" fontWeight="bold" color="primary">
                          {(predictionData.confidenceScore * 100).toFixed(0)}
                        </Typography>
                        <Typography variant="h5" color="text.secondary" sx={{ ml: 0.5 }}>
                          %
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={predictionData.confidenceScore * 100}
                        sx={{ mt: 2, height: 8, borderRadius: 4 }}
                      />
                    </CardContent>
                  </Card>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <Card sx={{ background: "rgba(255,255,255,0.95)" }}>
                    <CardContent>
                      <Typography variant="caption" color="text.secondary" sx={{ mb: 2, display: "block" }}>
                        Moving Averages
                      </Typography>
                      <Grid container spacing={2}>
                        <Grid item xs={4}>
                          <Box sx={{ textAlign: "center" }}>
                            <Typography variant="caption" color="text.secondary">
                              7-Day Avg
                            </Typography>
                            <Typography variant="h5" fontWeight="bold" color="primary">
                              {predictionData.sevenDayAverage.toLocaleString()}
                            </Typography>
                          </Box>
                        </Grid>
                        <Grid item xs={4}>
                          <Box sx={{ textAlign: "center" }}>
                            <Typography variant="caption" color="text.secondary">
                              14-Day Avg
                            </Typography>
                            <Typography variant="h5" fontWeight="bold" color="primary">
                              {predictionData.fourteenDayAverage.toLocaleString()}
                            </Typography>
                          </Box>
                        </Grid>
                        <Grid item xs={4}>
                          <Box sx={{ textAlign: "center" }}>
                            <Typography variant="caption" color="text.secondary">
                              30-Day Avg
                            </Typography>
                            <Typography variant="h5" fontWeight="bold" color="primary">
                              {predictionData.thirtyDayAverage.toLocaleString()}
                            </Typography>
                          </Box>
                        </Grid>
                      </Grid>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>

              {predictionData.historicalData && predictionData.historicalData.length > 0 && (
                <Box sx={{ mt: 3, background: "rgba(255,255,255,0.95)", borderRadius: 2, p: 2 }}>
                  <Typography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
                    Historical Trend (Last 30 Days)
                  </Typography>
                  <ResponsiveContainer width="100%" height={250}>
                    <LineChart data={[...predictionData.historicalData].reverse()}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis 
                        dataKey="date" 
                        tickFormatter={(value: string | number | Date) => new Date(value).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                      />
                      <YAxis />
                      <Tooltip 
                        labelFormatter={(value: string | number | Date) => new Date(value).toLocaleDateString()}
                        formatter={(value: any) => [value.toLocaleString(), 'Count']}
                      />
                      <Legend />
                      <Line 
                        type="monotone" 
                        dataKey="count" 
                        stroke="#667eea" 
                        strokeWidth={2} 
                        dot={{ r: 4 }}
                        name="Daily Count"
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </Box>
              )}
            </CardContent>
          </Card>
        )}

        {/* MEDICAL_COMMERCIAL Section */}
        <MetricsSection
          title="MEDICAL_COMMERCIAL - Hourly Distribution Summary"
          data={commercialData}
          totals={commercialTotals}
          color="#667eea"
        />

        {/* MEDICAL_GBD Section */}
        <Box sx={{ mt: 4 }}>
          <MetricsSection
            title="MEDICAL_GBD - Hourly Distribution Summary"
            data={gbdData}
            totals={gbdTotals}
            color="#4facfe"
          />
        </Box>
          </>
        )}
        
        {/* Tab 1: Failure Recovery Reports */}
        {tabValue === 1 && (
          <FailureRecoveryReports
            recoveredDocs={recoveredDocs}
            waitingDocs={waitingDocs}
            inboundFailedDocs={inboundFailedDocs}
            processFailedDocs={processFailedDocs}
            abortedDocs={abortedDocs}
            loading={failureLoading}
            selectedDate={failureRecoveryDate}
            startTime={failureStartTime}
            endTime={failureEndTime}
          />
        )}

        {/* Tab 2: Traffic Prediction */}
        {tabValue === 2 && (
          <>
            {/* Header */}


            {/* Next-Day Prediction */}
            {predictionData && !predictionLoading && (
              <Card sx={{ 
                mb: 4,
                background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
                color: "white"
              }}>
                <CardContent sx={{ p: 4 }}>
                  <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    <TrendingUpIcon /> Next-Day Traffic Prediction
                  </Typography>
                  <Grid container spacing={3} sx={{ mt: 2 }}>
                    <Grid item xs={12} md={4}>
                      <Card sx={{ bgcolor: "rgba(255,255,255,0.15)", backdropFilter: "blur(10px)" }}>
                        <CardContent>
                          <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.9)" }}>
                            Predicted Count for {new Date(predictionData.predictionDate).toLocaleDateString()}
                          </Typography>
                          <Typography variant="h3" fontWeight="bold" sx={{ color: "white", my: 1 }}>
                            {predictionData.predictedCount.toLocaleString()}
                          </Typography>
                          <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                            <Typography variant="body2">Trend:</Typography>
                            <Chip 
                              label={predictionData.trend} 
                              size="small"
                              color={predictionData.trend === "INCREASING" ? "success" : predictionData.trend === "DECREASING" ? "error" : "default"}
                              sx={{ fontWeight: "bold" }}
                            />
                          </Box>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <Card sx={{ bgcolor: "rgba(255,255,255,0.15)", backdropFilter: "blur(10px)" }}>
                        <CardContent>
                          <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.9)" }}>
                            Confidence Score
                          </Typography>
                          <Typography variant="h3" fontWeight="bold" sx={{ color: "white", my: 1 }}>
                            {(predictionData.confidenceScore * 100).toFixed(0)}%
                          </Typography>
                          <LinearProgress 
                            variant="determinate" 
                            value={predictionData.confidenceScore * 100} 
                            sx={{ 
                              height: 8, 
                              borderRadius: 4,
                              bgcolor: "rgba(255,255,255,0.3)",
                              '& .MuiLinearProgress-bar': { bgcolor: "white" }
                            }}
                          />
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <Card sx={{ bgcolor: "rgba(255,255,255,0.15)", backdropFilter: "blur(10px)" }}>
                        <CardContent>
                          <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.9)", display: "block" }}>
                            Moving Averages
                          </Typography>
                          <Grid container spacing={2} sx={{ mt: 0.5 }}>
                            <Grid item xs={4}>
                              <Typography variant="caption">7-Day Avg</Typography>
                              <Typography variant="h6" fontWeight="bold" sx={{ color: "white" }}>
                                {predictionData.sevenDayAverage}
                              </Typography>
                            </Grid>
                            <Grid item xs={4}>
                              <Typography variant="caption">14-Day Avg</Typography>
                              <Typography variant="h6" fontWeight="bold" sx={{ color: "white" }}>
                                {predictionData.fourteenDayAverage}
                              </Typography>
                            </Grid>
                            <Grid item xs={4}>
                              <Typography variant="caption">30-Day Avg</Typography>
                              <Typography variant="h6" fontWeight="bold" sx={{ color: "white" }}>
                                {predictionData.thirtyDayAverage}
                              </Typography>
                            </Grid>
                          </Grid>
                        </CardContent>
                      </Card>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            )}

            {/* Hourly Prediction */}
            {hourlyPredictionData && !hourlyPredictionLoading && (
              <Card sx={{ mb: 4 }}>
                <CardContent sx={{ p: 3 }}>
                  <Typography variant="h5" fontWeight="bold" color="primary" gutterBottom sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    🕐 Hourly Traffic Prediction - {new Date(hourlyPredictionData.predictionDate).toLocaleDateString()}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                    {hourlyPredictionData.description}
                  </Typography>
                  <Grid container spacing={2} sx={{ mb: 3 }}>
                    <Grid item xs={12} sm={6}>
                      <Card variant="outlined" sx={{ bgcolor: "#e3f2fd" }}>
                        <CardContent>
                          <Typography variant="caption" color="text.secondary">Total Predicted</Typography>
                          <Typography variant="h4" fontWeight="bold" color="primary">
                            {hourlyPredictionData.totalPredictedCount.toLocaleString()}
                          </Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Card variant="outlined" sx={{ bgcolor: "#f3e5f5" }}>
                        <CardContent>
                          <Typography variant="caption" color="text.secondary">Overall Confidence</Typography>
                          <Typography variant="h4" fontWeight="bold" color="secondary">
                            {(hourlyPredictionData.overallConfidence * 100).toFixed(0)}%
                          </Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                  </Grid>
                  <Box sx={{ width: "100%", height: 400 }}>
                    <ResponsiveContainer>
                      <BarChart data={hourlyPredictionData.hourlyPredictions.map((h: { timeLabel: any; predictedCount: any; hourOfDay: any; }) => ({
                        time: h.timeLabel,
                        count: h.predictedCount,
                        hour: h.hourOfDay
                      }))}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="time" angle={-45} textAnchor="end" height={80} />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="count" fill="#667eea" name="Predicted Count" />
                      </BarChart>
                    </ResponsiveContainer>
                  </Box>
                </CardContent>
              </Card>
            )}

            {/* Day-wise Prediction */}
            {dayWisePredictionData && !dayWisePredictionLoading && (
              <Card sx={{ mb: 4 }}>
                <CardContent sx={{ p: 3 }}>
                  <Typography variant="h5" fontWeight="bold" color="primary" gutterBottom sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    📅 7-Day Traffic Forecast
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    {dayWisePredictionData.description}
                  </Typography>
                  <Grid container spacing={2} sx={{ mb: 3 }}>
                    <Grid item xs={12} sm={6}>
                      <Card variant="outlined" sx={{ bgcolor: "#fff3e0" }}>
                        <CardContent>
                          <Typography variant="caption" color="text.secondary">Overall Trend</Typography>
                          <Chip 
                            label={dayWisePredictionData.overallTrend} 
                            color={dayWisePredictionData.overallTrend === "INCREASING" ? "success" : dayWisePredictionData.overallTrend === "DECREASING" ? "error" : "default"}
                            sx={{ fontWeight: "bold", fontSize: "1rem", height: 36 }}
                          />
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Card variant="outlined" sx={{ bgcolor: "#e0f2f1" }}>
                        <CardContent>
                          <Typography variant="caption" color="text.secondary">Average Confidence</Typography>
                          <Typography variant="h4" fontWeight="bold" color="primary">
                            {(dayWisePredictionData.averageConfidence * 100).toFixed(0)}%
                          </Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                  </Grid>
                  <Box sx={{ width: "100%", height: 400, mb: 3 }}>
                    <ResponsiveContainer>
                      <LineChart data={dayWisePredictionData.predictions.map((p: { date: string | number | Date; predictedCount: any; dayOfWeek: any; }) => ({
                        date: new Date(p.date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
                        count: p.predictedCount,
                        dayOfWeek: p.dayOfWeek
                      }))}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="date" />
                        <YAxis />
                        <Tooltip />
                        <Legend />
                        <Line 
                          type="monotone" 
                          dataKey="count" 
                          stroke="#4facfe" 
                          strokeWidth={3}
                          dot={{ r: 6, fill: "#4facfe" }}
                          name="Predicted Count"
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </Box>
                  <TableContainer component={Paper} variant="outlined">
                    <Table>
                      <TableHead>
                        <TableRow sx={{ bgcolor: "#f5f5f5" }}>
                          <TableCell><strong>Date</strong></TableCell>
                          <TableCell><strong>Day</strong></TableCell>
                          <TableCell align="right"><strong>Predicted Count</strong></TableCell>
                          <TableCell><strong>Trend</strong></TableCell>
                          <TableCell align="right"><strong>Confidence</strong></TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {dayWisePredictionData.predictions.map((pred: { date: string | number | Date; dayOfWeek: any; predictedCount: { toLocaleString: () => any; }; trend: string | string[]; confidenceScore: number; }, idx: any) => (
                          <TableRow key={idx} hover>
                            <TableCell>{new Date(pred.date).toLocaleDateString()}</TableCell>
                            <TableCell>{pred.dayOfWeek}</TableCell>
                            <TableCell align="right"><strong>{pred.predictedCount.toLocaleString()}</strong></TableCell>
                            <TableCell>
                              <Chip 
                                label={pred.trend} 
                                size="small"
                                color={pred.trend.includes("INCREASING") ? "success" : pred.trend.includes("DECREASING") ? "error" : "default"}
                              />
                            </TableCell>
                            <TableCell align="right">{(pred.confidenceScore * 100).toFixed(0)}%</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </CardContent>
              </Card>
            )}

            {/* Loading States */}
            {(predictionLoading || hourlyPredictionLoading || dayWisePredictionLoading) && (
              <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: 400 }}>
                <CircularProgress size={60} />
              </Box>
            )}
          </>
        )}
      </Container>
    </Box>
  </LocalizationProvider>
  );
}

function MetricsSection({
  title,
  data,
  totals,
  color,
}: {
  title: string;
  data: HourlyMetrics[];
  totals: {
    total: number;
    completed: number;
    failed: number;
    inProgress: number;
  };
  color: string;
}) {
  const chartData = formatChartData(data);

  return (
    <Paper elevation={2} sx={{ p: 3 }}>
      <Typography variant="h5" fontWeight="bold" color="primary" sx={{ mb: 3 }}>
        {title}
      </Typography>

      {/* Mini Stats */}
      <Grid container spacing={2} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined" sx={{ bgcolor: "#e3f2fd" }}>
            <CardContent sx={{ py: 2 }}>
              <Typography
                variant="caption"
                color="text.secondary"
                fontWeight="medium"
              >
                Total Ingestion
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="primary">
                {totals.total}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined" sx={{ bgcolor: "#e8f5e9" }}>
            <CardContent sx={{ py: 2 }}>
              <Typography
                variant="caption"
                color="text.secondary"
                fontWeight="medium"
              >
                Completed
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="success.main">
                {totals.completed}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined" sx={{ bgcolor: "#fff3e0" }}>
            <CardContent sx={{ py: 2 }}>
              <Typography
                variant="caption"
                color="text.secondary"
                fontWeight="medium"
              >
                In Progress
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="warning.main">
                {totals.inProgress}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined" sx={{ bgcolor: "#ffebee" }}>
            <CardContent sx={{ py: 2 }}>
              <Typography
                variant="caption"
                color="text.secondary"
                fontWeight="medium"
              >
                Failed
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="error.main">
                {totals.failed}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Line Chart */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h6" fontWeight="medium" sx={{ mb: 2 }}>
          Hourly Trend
        </Typography>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line
              type="monotone"
              dataKey="Total"
              stroke={color}
              strokeWidth={2}
            />
            <Line
              type="monotone"
              dataKey="Completed"
              stroke="#4caf50"
              strokeWidth={2}
            />
            <Line
              type="monotone"
              dataKey="Failed (Process)"
              stroke="#f44336"
              strokeWidth={2}
            />
          </LineChart>
        </ResponsiveContainer>
      </Box>

      {/* Bar Chart */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h6" fontWeight="medium" sx={{ mb: 2 }}>
          Status Breakdown
        </Typography>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar dataKey="Staged" fill="#2196f3" />
            <Bar dataKey="In Progress" fill="#ff9800" />
            <Bar dataKey="Completed" fill="#4caf50" />
            <Bar dataKey="Failed (Process)" fill="#f44336" />
            <Bar dataKey="Aborted" fill="#9e9e9e" />
          </BarChart>
        </ResponsiveContainer>
      </Box>

      {/* Data Table */}
      <Box>
        <Typography variant="h6" fontWeight="medium" sx={{ mb: 2 }}>
          Detailed Hourly Data
        </Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: "#f5f5f5" }}>
                <TableCell sx={{ fontWeight: "bold" }}>
                  Time Frame Start
                </TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>
                  Time Frame End
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: "bold" }}>
                  Total
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: "bold" }}>
                  Staged
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: "bold" }}>
                  In Progress
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: "bold" }}>
                  Completed
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: "bold" }}>
                  Failed (Process)
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: "bold" }}>
                  Failed (Ingestion)
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: "bold" }}>
                  Aborted
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {data.map((row, idx) => (
                <TableRow key={idx} hover>
                  <TableCell>
                    {new Date(row.timeFrameStart).toLocaleString()}
                  </TableCell>
                  <TableCell>
                    {new Date(row.timeFrameEnd).toLocaleString()}
                  </TableCell>
                  <TableCell align="right">
                    <Chip
                      label={row.totalIngestion}
                      size="small"
                      color="primary"
                    />
                  </TableCell>
                  <TableCell align="right">{row.stagedInIngestion}</TableCell>
                  <TableCell align="right">{row.inProgressCount}</TableCell>
                  <TableCell align="right">
                    <Typography color="success.main" fontWeight="medium">
                      {row.completedCount}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography color="error.main">
                      {row.failedInProcess}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography color="error.main">
                      {row.failedInIngestion}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography color="text.secondary">
                      {row.abortedCount}
                    </Typography>
                  </TableCell>
                </TableRow>
              ))}
              {data.length === 0 && (
                <TableRow>
                  <TableCell colSpan={9} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">
                      No data available for today
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Box>
    </Paper>
  );
}

function FailureRecoveryReports({
  recoveredDocs,
  waitingDocs,
  inboundFailedDocs,
  processFailedDocs,
  abortedDocs,
  loading,
  selectedDate,
  startTime,
  endTime,
}: {
  recoveredDocs: RecoveredDocument[];
  waitingDocs: WaitingDocument[];
  inboundFailedDocs: InboundFailedDocument[];
  processFailedDocs: ProcessFailedDocument[];
  abortedDocs: AbortedDocument[];
  loading: boolean;
  selectedDate: Dayjs;
  startTime: Dayjs | null;
  endTime: Dayjs | null;
}) {
  const handleDownloadExcel = async () => {
    try {
      const dateParam = selectedDate.format('YYYY-MM-DD');
      const params: any = { date: dateParam };
      
      if (startTime && endTime) {
        params.startTime = startTime.format('HH:mm:ss');
        params.endTime = endTime.format('HH:mm:ss');
      }
      
      const response = await axios.get(`http://localhost:8181/api/failure-recovery/download-excel`, {
        params,
        responseType: 'blob',
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `failure_recovery_reports_${dateParam}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Error downloading Excel file:', error);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
        <Button
          variant="contained"
          color="primary"
          startIcon={<DownloadIcon />}
          onClick={handleDownloadExcel}
          sx={{
            background: 'linear-gradient(45deg, #667eea 30%, #764ba2 90%)',
            color: 'white',
            fontWeight: 'bold',
          }}
        >
          Download All Reports (XLSX)
        </Button>
      </Box>
      {/* Recovered from Inbound Failed */}
      <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
        <Typography variant="h5" fontWeight="bold" color="success.main" sx={{ mb: 2 }}>
          🔄 Recovered from Inbound Failed
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Documents that failed during inbound ingestion but were successfully processed later
        </Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: "#e8f5e9" }}>
                <TableCell sx={{ fontWeight: "bold" }}>DCN ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>SI Case ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Document Type</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Download Completed</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {recoveredDocs.slice(-5).map((doc, idx) => (
                <TableRow key={idx} hover>
                  <TableCell>{doc.dcnId}</TableCell>
                  <TableCell>{doc.siCaseId}</TableCell>
                  <TableCell><Chip label={doc.documentType} size="small" color="primary" /></TableCell>
                  <TableCell>{new Date(doc.downloadCompletedOn).toLocaleString()}</TableCell>
                </TableRow>
              ))}
              {recoveredDocs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">No recovered documents found</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Kill Switch - Documents Waiting */}
      <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
        <Typography variant="h5" fontWeight="bold" color="warning.main" sx={{ mb: 2 }}>
          ⏱️ Kill Switch - Documents Waiting to Process
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Documents staged or in-progress exceeding waiting threshold
        </Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: "#fff3e0" }}>
                <TableCell sx={{ fontWeight: "bold" }}>DCN ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>SI Case ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Download Completed</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Status</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Time Remaining</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {waitingDocs.slice(-5).map((doc, idx) => (
                <TableRow key={idx} hover>
                  <TableCell>{doc.dcnId}</TableCell>
                  <TableCell>{doc.siCaseId}</TableCell>
                  <TableCell>{new Date(doc.downloadCompletedOn).toLocaleString()}</TableCell>
                  <TableCell>
                    <Chip 
                      label={doc.status} 
                      size="small" 
                      color={doc.status === "STAGED" ? "info" : "warning"} 
                    />
                  </TableCell>
                  <TableCell>{doc.timeRemainingToProcess}</TableCell>
                </TableRow>
              ))}
              {waitingDocs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">No waiting documents found</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Inbound Failed Status */}
      <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
        <Typography variant="h5" fontWeight="bold" color="error.main" sx={{ mb: 2 }}>
          ❌ Inbound Failed Status
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Files that failed during download phase
        </Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: "#ffebee" }}>
                <TableCell sx={{ fontWeight: "bold" }}>Download Completed</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Transaction ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>DCN ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>SI Case ID</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {inboundFailedDocs.slice(-5).map((doc, idx) => (
                <TableRow key={idx} hover>
                  <TableCell>{new Date(doc.downloadCompletedOn).toLocaleString()}</TableCell>
                  <TableCell>{doc.transactionId}</TableCell>
                  <TableCell>{doc.dcnId}</TableCell>
                  <TableCell>{doc.siCaseId}</TableCell>
                </TableRow>
              ))}
              {inboundFailedDocs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">No inbound failed documents</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Process Failed Status */}
      <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
        <Typography variant="h5" fontWeight="bold" color="error.main" sx={{ mb: 2 }}>
          🔴 Process Failed Status
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Documents that failed during processing phase
        </Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: "#ffebee" }}>
                <TableCell sx={{ fontWeight: "bold" }}>Download Completed</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>DCN ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>SI Case ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Status</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Error Code</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Error Message</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Root Pipeline ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Action ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Log</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {processFailedDocs.slice(-5).map((doc, idx) => (
                <TableRow key={idx} hover>
                  <TableCell>{new Date(doc.downloadCompletedOn).toLocaleString()}</TableCell>
                  <TableCell>{doc.dcnId}</TableCell>
                  <TableCell>{doc.siCaseId}</TableCell>
                  <TableCell><Chip label={doc.status} size="small" color="error" /></TableCell>
                  <TableCell>{doc.errorCode || "N/A"}</TableCell>
                  <TableCell sx={{ maxWidth: 300 }}>{doc.errorMessage || "N/A"}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                    {doc.rootPipelineId || "N/A"}
                  </TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                    {doc.actionId || "N/A"}
                  </TableCell>
                  <TableCell sx={{ maxWidth: 400, fontSize: '0.85rem', fontFamily: 'monospace' }}>
                    {doc.log ? (
                      <Box sx={{ maxHeight: 100, overflow: 'auto', bgcolor: '#f5f5f5', p: 1, borderRadius: 1 }}>
                        {doc.log}
                      </Box>
                    ) : (
                      "N/A"
                    )}
                  </TableCell>
                </TableRow>
              ))}
              {processFailedDocs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={9} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">No process failed documents</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Aborted Status */}
      <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
        <Typography variant="h5" fontWeight="bold" color="text.secondary" sx={{ mb: 2 }}>
          ⛔ Aborted Status
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Documents where pipeline aborted mid-way
        </Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: "#f5f5f5" }}>
                <TableCell sx={{ fontWeight: "bold" }}>Download Completed</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>DCN ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>SI Case ID</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Status</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Error Code</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Error Message</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {abortedDocs.slice(-5).map((doc, idx) => (
                <TableRow key={idx} hover>
                  <TableCell>{new Date(doc.downloadCompletedOn).toLocaleString()}</TableCell>
                  <TableCell>{doc.dcnId}</TableCell>
                  <TableCell>{doc.siCaseId}</TableCell>
                  <TableCell><Chip label={doc.status} size="small" /></TableCell>
                  <TableCell>{doc.errorCode || "N/A"}</TableCell>
                  <TableCell sx={{ maxWidth: 300 }}>{doc.errorMessage || "N/A"}</TableCell>
                </TableRow>
              ))}
              {abortedDocs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">No aborted documents</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Box>
  );
}
