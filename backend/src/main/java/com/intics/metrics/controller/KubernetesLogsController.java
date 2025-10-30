package com.intics.metrics.controller;

import com.intics.metrics.dto.kubernetes.logs.*;
import com.intics.metrics.service.KubernetesConnectivityService;
import com.intics.metrics.service.KubernetesDemoDataProvider;
import com.intics.metrics.service.KubernetesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/kubernetes/logs")
@CrossOrigin(origins = "*")
@Tag(name = "Kubernetes Logs", description = "APIs for monitoring and searching Kubernetes pod logs")
@Slf4j
public class KubernetesLogsController {

    @Autowired
    private KubernetesService kubernetesService;

    @Autowired
    private KubernetesConnectivityService connectivityService;

    @Autowired
    private KubernetesDemoDataProvider demoDataProvider;

    @PostMapping("/pod")
    @Operation(summary = "Get pod logs",
            description = "Retrieve logs for a specific pod with filtering options")
    public ResponseEntity<LogSearchResultDTO> getPodLogs(@RequestBody PodLogRequestDTO request) {
        log.info("Fetching logs for pod: {}/{}", request.getNamespace(), request.getPodName());
        
        if (!connectivityService.isConnected()) {
            log.info("Kubernetes not connected, returning demo data");
            LogSearchResultDTO demoLogs = demoDataProvider.generateDemoLogs(
                request.getNamespace(), 
                request.getSearchTerm(), 
                request.getLogLevel(), 
                request.getSinceSeconds()
            );
            return ResponseEntity.ok(demoLogs);
        }
        
        LogSearchResultDTO logs = kubernetesService.getPodLogs(request);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/search")
    @Operation(summary = "Search logs across namespace",
            description = "Search for logs across all pods in a namespace with filters")
    public ResponseEntity<LogSearchResultDTO> searchLogs(
            @Parameter(description = "Namespace to search (optional, defaults to all)")
            @RequestParam(required = false) String namespace,
            @Parameter(description = "Search term to filter logs")
            @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Log level filter (ERROR, WARN, INFO, DEBUG, ALL)")
            @RequestParam(required = false, defaultValue = "ALL") String logLevel,
            @Parameter(description = "Time range in seconds (default: 3600)")
            @RequestParam(required = false, defaultValue = "3600") Integer sinceSeconds) {
        
        log.info("Searching logs - namespace: {}, searchTerm: {}, logLevel: {}, sinceSeconds: {}", 
                namespace, searchTerm, logLevel, sinceSeconds);
        
        if (!connectivityService.isConnected()) {
            log.info("Kubernetes not connected, returning demo data");
            LogSearchResultDTO demoLogs = demoDataProvider.generateDemoLogs(
                namespace, searchTerm, logLevel, sinceSeconds
            );
            return ResponseEntity.ok(demoLogs);
        }
        
        LogSearchResultDTO logs = kubernetesService.searchLogsAcrossNamespace(
            namespace, searchTerm, logLevel, sinceSeconds
        );
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get log statistics",
            description = "Get aggregated statistics about logs in a namespace")
    public ResponseEntity<LogStatisticsDTO> getLogStatistics(
            @Parameter(description = "Namespace to analyze (optional, defaults to all)")
            @RequestParam(required = false) String namespace,
            @Parameter(description = "Time range in seconds (default: 3600)")
            @RequestParam(required = false, defaultValue = "3600") Integer sinceSeconds) {
        
        log.info("Fetching log statistics for namespace: {}, sinceSeconds: {}", namespace, sinceSeconds);
        
        if (!connectivityService.isConnected()) {
            log.info("Kubernetes not connected, returning demo statistics");
            LogStatisticsDTO demoStats = demoDataProvider.generateDemoStatistics(namespace, sinceSeconds);
            return ResponseEntity.ok(demoStats);
        }
        
        LogStatisticsDTO statistics = kubernetesService.getLogStatistics(namespace, sinceSeconds);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check",
            description = "Check if Kubernetes logs API is available and connection status")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("available", connectivityService.isAvailable());
        health.put("connected", connectivityService.isConnected());
        health.put("status", connectivityService.getConnectionStatus());
        health.put("demoMode", !connectivityService.isConnected());
        
        if (connectivityService.isConnected()) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.status(200).body(health);
        }
    }

    @GetMapping("/namespaces")
    @Operation(summary = "Get available namespaces",
            description = "Get list of available Kubernetes namespaces")
    public ResponseEntity<?> getNamespaces() {
        if (!connectivityService.isConnected()) {
            log.info("Kubernetes not connected, returning demo namespaces");
            Map<String, Object> response = new HashMap<>();
            response.put("namespaces", demoDataProvider.getDemoNamespaces());
            response.put("demoMode", true);
            return ResponseEntity.ok(response);
        }
        
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("namespaces", kubernetesService.getAllNamespaces());
            response.put("demoMode", false);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching namespaces", e);
            Map<String, Object> response = new HashMap<>();
            response.put("namespaces", demoDataProvider.getDemoNamespaces());
            response.put("demoMode", true);
            return ResponseEntity.ok(response);
        }
    }
}
