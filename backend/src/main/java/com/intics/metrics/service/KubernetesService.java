package com.intics.metrics.service;

import com.intics.metrics.dto.kubernetes.*;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KubernetesService {

    @Autowired(required = false)
    private CoreV1Api coreV1Api;

    @Autowired(required = false)
    private ApiClient apiClient;

    public boolean isKubernetesAvailable() {
        return coreV1Api != null && apiClient != null;
    }

    public void healthCheck() throws ApiException {
        if (coreV1Api == null) {
            throw new IllegalStateException("Kubernetes CoreV1Api not initialized");
        }
        
        try {
            // Limit to 1 namespace to verify API connectivity
            coreV1Api.listNamespace(null, null, null, null, null, 1, null, null, null, null);
        } catch (ApiException e) {
            log.error("Kubernetes health check failed: {}", e.getMessage());
            throw e;
        }
    }

    public List<PodMetricsDTO> getAllPods(String namespace) {
        if (coreV1Api == null) {
            log.error("Kubernetes CoreV1Api not initialized");
            throw new IllegalStateException("Kubernetes client not initialized. Please configure kubeconfig.");
        }

        try {
            V1PodList podList;
            if (namespace != null && !namespace.isEmpty()) {
                podList = coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null);
            } else {
                podList = coreV1Api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
            }

            return podList.getItems().stream()
                    .map(pod -> convertToPodMetricsDTO(pod))
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            log.error("Error fetching pods - HTTP Status: {}, Response Body: {}, Message: {}", 
                     e.getCode(), e.getResponseBody(), e.getMessage(), e);
            throw new RuntimeException(String.format(
                "Failed to fetch pods from Kubernetes - HTTP %d: %s", 
                e.getCode(), 
                e.getResponseBody() != null ? e.getResponseBody() : e.getMessage()), e);
        }
    }

    public List<NodeMetricsDTO> getAllNodes() {
        if (coreV1Api == null) {
            log.error("Kubernetes CoreV1Api not initialized");
            throw new IllegalStateException("Kubernetes client not initialized. Please configure kubeconfig.");
        }

        try {
            V1NodeList nodeList = coreV1Api.listNode(null, null, null, null, null, null, null, null, null, null);

            return nodeList.getItems().stream()
                    .map(node -> convertToNodeMetricsDTO(node))
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            log.error("Error fetching nodes - HTTP Status: {}, Response Body: {}, Message: {}", 
                     e.getCode(), e.getResponseBody(), e.getMessage(), e);
            throw new RuntimeException(String.format(
                "Failed to fetch nodes from Kubernetes - HTTP %d: %s", 
                e.getCode(), 
                e.getResponseBody() != null ? e.getResponseBody() : e.getMessage()), e);
        }
    }

    public List<NamespaceDTO> getAllNamespaces() {
        if (coreV1Api == null) {
            log.error("Kubernetes CoreV1Api not initialized");
            throw new IllegalStateException("Kubernetes client not initialized. Please configure kubeconfig.");
        }

        try {
            V1NamespaceList namespaceList = coreV1Api.listNamespace(null, null, null, null, null, null, null, null, null, null);

            return namespaceList.getItems().stream()
                    .map(this::convertToNamespaceDTO)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            log.error("Error fetching namespaces - HTTP Status: {}, Response Body: {}, Message: {}", 
                     e.getCode(), e.getResponseBody(), e.getMessage(), e);
            throw new RuntimeException(String.format(
                "Failed to fetch namespaces from Kubernetes - HTTP %d: %s", 
                e.getCode(), 
                e.getResponseBody() != null ? e.getResponseBody() : e.getMessage()), e);
        }
    }

    public ClusterOverviewDTO getClusterOverview() {
        if (coreV1Api == null) {
            log.error("Kubernetes CoreV1Api not initialized");
            throw new IllegalStateException("Kubernetes client not initialized. Please configure kubeconfig.");
        }

        try {
            V1NodeList nodeList = coreV1Api.listNode(null, null, null, null, null, null, null, null, null, null);
            V1PodList podList = coreV1Api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null);

            int totalNodes = nodeList.getItems().size();
            int healthyNodes = (int) nodeList.getItems().stream()
                    .filter(this::isNodeHealthy)
                    .count();

            Map<String, Integer> podsByStatus = podList.getItems().stream()
                    .collect(Collectors.groupingBy(
                            pod -> pod.getStatus().getPhase(),
                            Collectors.summingInt(p -> 1)
                    ));

            Map<String, Integer> podsByNamespace = podList.getItems().stream()
                    .collect(Collectors.groupingBy(
                            pod -> pod.getMetadata().getNamespace(),
                            Collectors.summingInt(p -> 1)
                    ));

            return ClusterOverviewDTO.builder()
                    .totalNodes(totalNodes)
                    .healthyNodes(healthyNodes)
                    .totalPods(podList.getItems().size())
                    .podsByStatus(podsByStatus)
                    .podsByNamespace(podsByNamespace)
                    .build();
        } catch (ApiException e) {
            log.error("Error fetching cluster overview - HTTP Status: {}, Response Body: {}, Message: {}", 
                     e.getCode(), e.getResponseBody(), e.getMessage(), e);
            throw new RuntimeException(String.format(
                "Failed to fetch cluster overview from Kubernetes - HTTP %d: %s", 
                e.getCode(), 
                e.getResponseBody() != null ? e.getResponseBody() : e.getMessage()), e);
        }
    }

    private PodMetricsDTO convertToPodMetricsDTO(V1Pod pod) {
        V1PodStatus status = pod.getStatus();
        V1ObjectMeta metadata = pod.getMetadata();

        int totalRestarts = pod.getStatus().getContainerStatuses() != null ?
                pod.getStatus().getContainerStatuses().stream()
                        .mapToInt(V1ContainerStatus::getRestartCount)
                        .sum() : 0;

        String age = calculateAge(metadata.getCreationTimestamp());
        String healthStatus = determineHealthStatus(pod);
        List<String> warnings = collectWarnings(pod);

        List<PodMetricsDTO.ContainerMetrics> containers = new ArrayList<>();
        if (status.getContainerStatuses() != null) {
            containers = status.getContainerStatuses().stream()
                    .map(cs -> PodMetricsDTO.ContainerMetrics.builder()
                            .name(cs.getName())
                            .image(cs.getImage())
                            .status(getContainerStatus(cs))
                            .restartCount(cs.getRestartCount())
                            .build())
                    .collect(Collectors.toList());
        }

        // Extract resource requests and limits from pod spec
        PodMetricsDTO.ResourceMetrics resources = extractResourcesFromPod(pod);

        return PodMetricsDTO.builder()
                .name(metadata.getName())
                .namespace(metadata.getNamespace())
                .status(getContainerStatus(status.getContainerStatuses() != null && !status.getContainerStatuses().isEmpty() ?
                        status.getContainerStatuses().get(0) : null))
                .phase(status.getPhase())
                .restartCount(totalRestarts)
                .age(age)
                .createdAt(metadata.getCreationTimestamp() != null ?
                        LocalDateTime.ofInstant(metadata.getCreationTimestamp().toInstant(), ZoneId.systemDefault()) : null)
                .node(pod.getSpec().getNodeName())
                .resources(resources)
                .containers(containers)
                .labels(metadata.getLabels())
                .healthStatus(healthStatus)
                .warnings(warnings)
                .build();
    }

    private NodeMetricsDTO convertToNodeMetricsDTO(V1Node node) {
        V1NodeStatus status = node.getStatus();
        V1ObjectMeta metadata = node.getMetadata();

        String nodeStatus = status.getConditions() != null ?
                status.getConditions().stream()
                        .filter(c -> "Ready".equals(c.getType()))
                        .map(c -> c.getStatus())
                        .findFirst()
                        .orElse("Unknown") : "Unknown";

        Map<String, String> capacity = status.getCapacity() != null ?
                status.getCapacity().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().toSuffixedString()
                        )) : Collections.emptyMap();

        Map<String, String> allocatable = status.getAllocatable() != null ?
                status.getAllocatable().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().toSuffixedString()
                        )) : Collections.emptyMap();

        return NodeMetricsDTO.builder()
                .name(metadata.getName())
                .status(nodeStatus)
                .capacity(capacity)
                .allocatable(allocatable)
                .version(status.getNodeInfo().getKubeletVersion())
                .osImage(status.getNodeInfo().getOsImage())
                .labels(metadata.getLabels())
                .build();
    }

    private NamespaceDTO convertToNamespaceDTO(V1Namespace namespace) {
        V1ObjectMeta metadata = namespace.getMetadata();

        return NamespaceDTO.builder()
                .name(metadata.getName())
                .status(namespace.getStatus() != null ? namespace.getStatus().getPhase() : "Active")
                .createdAt(metadata.getCreationTimestamp() != null ?
                        LocalDateTime.ofInstant(metadata.getCreationTimestamp().toInstant(), ZoneId.systemDefault()) : null)
                .build();
    }

    private String calculateAge(java.time.OffsetDateTime creationTimestamp) {
        if (creationTimestamp == null) return "Unknown";

        Duration duration = Duration.between(
                creationTimestamp.toInstant(),
                java.time.Instant.now()
        );

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return days + "d";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return minutes + "m";
        }
    }

    private String getContainerStatus(V1ContainerStatus cs) {
        if (cs == null) return "Unknown";

        if (cs.getState() != null) {
            if (cs.getState().getRunning() != null) {
                return "Running";
            } else if (cs.getState().getWaiting() != null) {
                return cs.getState().getWaiting().getReason();
            } else if (cs.getState().getTerminated() != null) {
                return "Terminated";
            }
        }
        return "Unknown";
    }

    private String determineHealthStatus(V1Pod pod) {
        if (pod.getStatus().getPhase().equals("Running")) {
            if (pod.getStatus().getContainerStatuses() != null) {
                boolean anyContainerFailed = pod.getStatus().getContainerStatuses().stream()
                        .anyMatch(cs -> cs.getRestartCount() > 5);
                if (anyContainerFailed) return "Warning";
            }
            return "Healthy";
        } else if (pod.getStatus().getPhase().equals("Pending")) {
            return "Pending";
        } else {
            return "Unhealthy";
        }
    }

    private List<String> collectWarnings(V1Pod pod) {
        List<String> warnings = new ArrayList<>();

        if (pod.getStatus().getContainerStatuses() != null) {
            for (V1ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                if (cs.getRestartCount() > 3) {
                    warnings.add("High restart count: " + cs.getRestartCount());
                }
                if (cs.getState() != null && cs.getState().getWaiting() != null) {
                    warnings.add("Container waiting: " + cs.getState().getWaiting().getReason());
                }
            }
        }

        return warnings;
    }

    private boolean isNodeHealthy(V1Node node) {
        if (node.getStatus() == null || node.getStatus().getConditions() == null) {
            return false;
        }

        return node.getStatus().getConditions().stream()
                .anyMatch(c -> "Ready".equals(c.getType()) && "True".equals(c.getStatus()));
    }

    private PodMetricsDTO.ResourceMetrics extractResourcesFromPod(V1Pod pod) {
        // Extract resource requests and limits from containers
        PodMetricsDTO.ResourceMetrics.ResourceMetricsBuilder builder = PodMetricsDTO.ResourceMetrics.builder();
        
        if (pod.getSpec() != null && pod.getSpec().getContainers() != null && !pod.getSpec().getContainers().isEmpty()) {
            V1Container firstContainer = pod.getSpec().getContainers().get(0);
            V1ResourceRequirements resources = firstContainer.getResources();
            
            if (resources != null) {
                // Extract requests
                if (resources.getRequests() != null) {
                    if (resources.getRequests().containsKey("cpu")) {
                        builder.cpuRequest(resources.getRequests().get("cpu").toSuffixedString());
                    }
                    if (resources.getRequests().containsKey("memory")) {
                        builder.memoryRequest(resources.getRequests().get("memory").toSuffixedString());
                    }
                }
                
                // Extract limits
                if (resources.getLimits() != null) {
                    if (resources.getLimits().containsKey("cpu")) {
                        builder.cpuLimit(resources.getLimits().get("cpu").toSuffixedString());
                    }
                    if (resources.getLimits().containsKey("memory")) {
                        builder.memoryLimit(resources.getLimits().get("memory").toSuffixedString());
                    }
                }
            }
        }
        
        return builder
                .cpuUsage("N/A")  // Will be available with metrics server
                .memoryUsage("N/A")
                .build();
    }
}
