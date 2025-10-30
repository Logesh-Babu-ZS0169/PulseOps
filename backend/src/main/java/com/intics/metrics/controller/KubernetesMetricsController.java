package com.intics.metrics.controller;

import com.intics.metrics.dto.kubernetes.metrics.*;
import com.intics.metrics.service.KubernetesConnectivityService;
import com.intics.metrics.service.KubernetesMetricsService;
import com.intics.metrics.service.MetricsDemoDataProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kubernetes/metrics")
@Tag(name = "Kubernetes Metrics", description = "System metrics for Kubernetes cluster (CPU, Memory, Disk, Network)")
@Slf4j
@CrossOrigin(origins = "*")
public class KubernetesMetricsController {

    @Autowired(required = false)
    private KubernetesMetricsService kubernetesMetricsService;

    @Autowired
    private MetricsDemoDataProvider demoDataProvider;

    @Autowired
    private KubernetesConnectivityService connectivityService;

    @GetMapping("/cluster")
    @Operation(summary = "Get cluster-wide metrics",
            description = "Retrieve CPU, memory, disk, and network metrics for the entire cluster")
    public ResponseEntity<ClusterMetricsDTO> getClusterMetrics() {
        log.info("Fetching cluster metrics");

        if (!connectivityService.isConnected()) {
            log.info("Kubernetes not available, returning demo data");
            ClusterMetricsDTO demoMetrics = demoDataProvider.generateDemoClusterMetrics();
            return ResponseEntity.ok(demoMetrics);
        }

        try {
            ClusterMetricsDTO metrics = kubernetesMetricsService.getClusterMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error fetching cluster metrics: {}", e.getMessage(), e);
            ClusterMetricsDTO demoMetrics = demoDataProvider.generateDemoClusterMetrics();
            return ResponseEntity.ok(demoMetrics);
        }
    }

    @GetMapping("/timeseries")
    @Operation(summary = "Get time-series metrics",
            description = "Retrieve historical time-series metrics for a specific resource")
    public ResponseEntity<List<MetricsTimeSeriesDTO>> getTimeSeriesMetrics(
            @RequestParam(defaultValue = "cluster") String resourceType,
            @RequestParam(defaultValue = "default") String resourceName,
            @RequestParam(defaultValue = "6") int hours) {
        
        log.info("Fetching time-series metrics for {} {} over {} hours", resourceType, resourceName, hours);

        List<MetricsTimeSeriesDTO> demoTimeSeries = demoDataProvider.generateDemoTimeSeries(resourceType, resourceName, hours);
        return ResponseEntity.ok(demoTimeSeries);
    }

    @GetMapping("/nodes")
    @Operation(summary = "Get node metrics",
            description = "Retrieve metrics for all nodes in the cluster")
    public ResponseEntity<List<NodeMetrics>> getNodeMetrics() {
        log.info("Fetching node metrics");

        if (!connectivityService.isConnected()) {
            log.info("Kubernetes not available, returning demo node metrics");
            ClusterMetricsDTO demoMetrics = demoDataProvider.generateDemoClusterMetrics();
            return ResponseEntity.ok(demoMetrics.getNodeMetrics());
        }

        try {
            ClusterMetricsDTO metrics = kubernetesMetricsService.getClusterMetrics();
            return ResponseEntity.ok(metrics.getNodeMetrics());
        } catch (Exception e) {
            log.error("Error fetching node metrics: {}", e.getMessage(), e);
            ClusterMetricsDTO demoMetrics = demoDataProvider.generateDemoClusterMetrics();
            return ResponseEntity.ok(demoMetrics.getNodeMetrics());
        }
    }

    @GetMapping("/pods/top")
    @Operation(summary = "Get top resource-consuming pods",
            description = "Retrieve the top 10 pods by CPU or memory usage")
    public ResponseEntity<List<PodMetrics>> getTopPods(
            @RequestParam(defaultValue = "cpu") String metric) {
        
        log.info("Fetching top pods by {}", metric);

        if (!connectivityService.isConnected()) {
            log.info("Kubernetes not available, returning demo top pods");
            ClusterMetricsDTO demoMetrics = demoDataProvider.generateDemoClusterMetrics();
            List<PodMetrics> topPods = metric.equals("memory") ? 
                    demoMetrics.getTopPodsByMemory() : 
                    demoMetrics.getTopPodsByCpu();
            return ResponseEntity.ok(topPods);
        }

        try {
            ClusterMetricsDTO metrics = kubernetesMetricsService.getClusterMetrics();
            List<PodMetrics> topPods = metric.equals("memory") ? 
                    metrics.getTopPodsByMemory() : 
                    metrics.getTopPodsByCpu();
            return ResponseEntity.ok(topPods);
        } catch (Exception e) {
            log.error("Error fetching top pods: {}", e.getMessage(), e);
            ClusterMetricsDTO demoMetrics = demoDataProvider.generateDemoClusterMetrics();
            List<PodMetrics> topPods = metric.equals("memory") ? 
                    demoMetrics.getTopPodsByMemory() : 
                    demoMetrics.getTopPodsByCpu();
            return ResponseEntity.ok(topPods);
        }
    }

    @GetMapping("/namespaces")
    @Operation(summary = "Get metrics by namespace",
            description = "Retrieve aggregated metrics for each namespace")
    public ResponseEntity<List<NamespaceMetrics>> getNamespaceMetrics() {
        log.info("Fetching namespace metrics");

        if (!connectivityService.isConnected()) {
            log.info("Kubernetes not available, returning demo namespace metrics");
            ClusterMetricsDTO demoMetrics = demoDataProvider.generateDemoClusterMetrics();
            return ResponseEntity.ok(List.copyOf(demoMetrics.getMetricsByNamespace().values()));
        }

        try {
            ClusterMetricsDTO metrics = kubernetesMetricsService.getClusterMetrics();
            return ResponseEntity.ok(List.copyOf(metrics.getMetricsByNamespace().values()));
        } catch (Exception e) {
            log.error("Error fetching namespace metrics: {}", e.getMessage(), e);
            ClusterMetricsDTO demoMetrics = demoDataProvider.generateDemoClusterMetrics();
            return ResponseEntity.ok(List.copyOf(demoMetrics.getMetricsByNamespace().values()));
        }
    }
}
