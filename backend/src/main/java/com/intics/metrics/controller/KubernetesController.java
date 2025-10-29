package com.intics.metrics.controller;

import com.intics.metrics.annotation.RequiresMFA;
import com.intics.metrics.dto.kubernetes.*;
import com.intics.metrics.service.KubernetesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kubernetes")
@CrossOrigin(origins = "*")
@Tag(name = "Kubernetes Monitoring", description = "APIs for monitoring Kubernetes clusters, pods, and nodes")
@Slf4j
public class KubernetesController {

    @Autowired
    private KubernetesService kubernetesService;

    @GetMapping("/pods")
    @Operation(summary = "Get all pods",
            description = "Retrieve metrics for all pods in the cluster or a specific namespace")
    public ResponseEntity<List<PodMetricsDTO>> getAllPods(
            @Parameter(description = "Namespace to filter pods (optional)")
            @RequestParam(required = false) String namespace) {
        log.info("Fetching pods for namespace: {}", namespace != null ? namespace : "all");
        List<PodMetricsDTO> pods = kubernetesService.getAllPods(namespace);
        return ResponseEntity.ok(pods);
    }

    @GetMapping("/pods/namespace/{namespace}")
    @Operation(summary = "Get pods by namespace",
            description = "Retrieve all pods in a specific namespace")
    public ResponseEntity<List<PodMetricsDTO>> getPodsByNamespace(
            @Parameter(description = "Namespace name")
            @PathVariable String namespace) {
        log.info("Fetching pods for namespace: {}", namespace);
        List<PodMetricsDTO> pods = kubernetesService.getAllPods(namespace);
        return ResponseEntity.ok(pods);
    }

    @GetMapping("/nodes")
    @Operation(summary = "Get all nodes",
            description = "Retrieve metrics for all nodes in the cluster")
    public ResponseEntity<List<NodeMetricsDTO>> getAllNodes() {
        log.info("Fetching all nodes");
        List<NodeMetricsDTO> nodes = kubernetesService.getAllNodes();
        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/namespaces")
    @Operation(summary = "Get all namespaces",
            description = "Retrieve all namespaces in the cluster")
    public ResponseEntity<List<NamespaceDTO>> getAllNamespaces() {
        log.info("Fetching all namespaces");
        List<NamespaceDTO> namespaces = kubernetesService.getAllNamespaces();
        return ResponseEntity.ok(namespaces);
    }

    @GetMapping("/cluster/overview")
    @Operation(summary = "Get cluster overview",
            description = "Retrieve high-level cluster metrics including node and pod counts")
    public ResponseEntity<ClusterOverviewDTO> getClusterOverview() {
        log.info("Fetching cluster overview");
        ClusterOverviewDTO overview = kubernetesService.getClusterOverview();
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/health")
    @Operation(summary = "Check Kubernetes API health",
            description = "Verify connection to Kubernetes cluster")
    public ResponseEntity<String> checkHealth() {
        try {
            if (!kubernetesService.isKubernetesAvailable()) {
                return ResponseEntity.status(503).body("Kubernetes API client not initialized. Please configure kubeconfig.");
            }
            kubernetesService.healthCheck();
            return ResponseEntity.ok("Kubernetes API connection healthy");
        } catch (Exception e) {
            log.error("Kubernetes API health check failed", e);
            return ResponseEntity.status(503).body("Kubernetes API unavailable: " + e.getMessage());
        }
    }
}
