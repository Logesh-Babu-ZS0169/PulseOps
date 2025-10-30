package com.intics.metrics.service;

import com.intics.metrics.dto.kubernetes.*;
import com.intics.metrics.dto.kubernetes.logs.*;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public LogSearchResultDTO getPodLogs(PodLogRequestDTO request) {
        if (coreV1Api == null) {
            log.error("Kubernetes CoreV1Api not initialized");
            throw new IllegalStateException("Kubernetes client not initialized. Please configure kubeconfig.");
        }

        try {
            String namespace = request.getNamespace();
            String podName = request.getPodName();
            String containerName = request.getContainerName();
            Integer tailLines = request.getTailLines() != null ? request.getTailLines() : 1000;
            Integer sinceSeconds = request.getSinceSeconds() != null ? request.getSinceSeconds() : 3600;
            Boolean timestamps = request.getTimestamps() != null ? request.getTimestamps() : true;

            String logs = coreV1Api.readNamespacedPodLog(
                    podName,
                    namespace,
                    containerName,
                    null,
                    null,
                    null,
                    null,
                    null,
                    sinceSeconds,
                    tailLines,
                    timestamps
            );

            List<LogEntryDTO> logEntries = parseLogLines(logs, namespace, podName, containerName);

            if (request.getSearchTerm() != null && !request.getSearchTerm().isEmpty()) {
                logEntries = logEntries.stream()
                        .filter(log -> log.getMessage().toLowerCase().contains(request.getSearchTerm().toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (request.getLogLevel() != null && !request.getLogLevel().equals("ALL")) {
                logEntries = logEntries.stream()
                        .filter(log -> request.getLogLevel().equals(log.getLogLevel()))
                        .collect(Collectors.toList());
            }

            return buildLogSearchResult(logEntries, request.getSearchTerm(), String.format("Last %d seconds", sinceSeconds));

        } catch (ApiException e) {
            log.error("Error fetching pod logs - HTTP Status: {}, Response Body: {}",
                    e.getCode(), e.getMessage(), e);
            throw new RuntimeException(String.format(
                    "Failed to fetch pod logs from Kubernetes - HTTP %d: %s",
                    e.getCode(),
                    e.getMessage()), e);
        }
    }

    public LogSearchResultDTO searchLogsAcrossNamespace(String namespace, String searchTerm, String logLevel, Integer sinceSeconds) {
        if (coreV1Api == null) {
            log.error("Kubernetes CoreV1Api not initialized");
            throw new IllegalStateException("Kubernetes client not initialized. Please configure kubeconfig.");
        }

        List<LogEntryDTO> allLogs = new ArrayList<>();

        try {
            List<PodMetricsDTO> pods = getAllPods(namespace);

            for (PodMetricsDTO pod : pods) {
                try {
                    PodLogRequestDTO request = PodLogRequestDTO.builder()
                            .namespace(pod.getNamespace())
                            .podName(pod.getName())
                            .tailLines(500)
                            .sinceSeconds(sinceSeconds != null ? sinceSeconds : 3600)
                            .timestamps(true)
                            .searchTerm(searchTerm)
                            .logLevel(logLevel)
                            .build();

                    LogSearchResultDTO podLogs = getPodLogs(request);
                    allLogs.addAll(podLogs.getLogs());
                } catch (Exception e) {
                    log.debug("Could not fetch logs for pod {}: {}", pod.getName(), e.getMessage());
                }
            }

            allLogs.sort(Comparator.comparing(LogEntryDTO::getTimestamp).reversed());

            return buildLogSearchResult(allLogs, searchTerm, String.format("Last %d seconds", sinceSeconds != null ? sinceSeconds : 3600));

        } catch (Exception e) {
            log.error("Error searching logs across namespace: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search logs: " + e.getMessage(), e);
        }
    }

    public LogStatisticsDTO getLogStatistics(String namespace, Integer sinceSeconds) {
        if (coreV1Api == null) {
            log.error("Kubernetes CoreV1Api not initialized");
            throw new IllegalStateException("Kubernetes client not initialized. Please configure kubeconfig.");
        }

        LogSearchResultDTO allLogs = searchLogsAcrossNamespace(namespace, null, "ALL", sinceSeconds);

        Map<String, Integer> errorsByPod = allLogs.getLogs().stream()
                .filter(log -> "ERROR".equals(log.getLogLevel()))
                .collect(Collectors.groupingBy(
                        LogEntryDTO::getPodName,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        int errorCount = allLogs.getLogLevelCounts().getOrDefault("ERROR", 0);
        int warningCount = allLogs.getLogLevelCounts().getOrDefault("WARN", 0);
        int infoCount = allLogs.getLogLevelCounts().getOrDefault("INFO", 0);

        return LogStatisticsDTO.builder()
                .totalLogs(allLogs.getTotalCount())
                .logsByLevel(allLogs.getLogLevelCounts())
                .logsByNamespace(allLogs.getNamespaceCounts())
                .logsByPod(allLogs.getPodCounts())
                .errorsByPod(errorsByPod)
                .errorCount(errorCount)
                .warningCount(warningCount)
                .infoCount(infoCount)
                .timeRange(String.format("Last %d seconds", sinceSeconds != null ? sinceSeconds : 3600))
                .build();
    }

    private List<LogEntryDTO> parseLogLines(String logs, String namespace, String podName, String containerName) {
        List<LogEntryDTO> logEntries = new ArrayList<>();

        if (logs == null || logs.isEmpty()) {
            return logEntries;
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(logs))) {
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                LogEntryDTO logEntry = parseLogLine(line, namespace, podName, containerName, lineNumber);
                logEntries.add(logEntry);
                lineNumber++;
            }
        } catch (Exception e) {
            log.error("Error parsing log lines: {}", e.getMessage(), e);
        }

        return logEntries;
    }

    private LogEntryDTO parseLogLine(String line, String namespace, String podName, String containerName, int lineNumber) {
        String timestamp = extractTimestamp(line);
        String logLevel = extractLogLevel(line);
        String message = line;

        if (timestamp != null) {
            message = line.substring(timestamp.length()).trim();
        }

        return LogEntryDTO.builder()
                .timestamp(timestamp != null ? timestamp : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .podName(podName)
                .namespace(namespace)
                .containerName(containerName)
                .logLevel(logLevel)
                .message(message)
                .lineNumber(lineNumber)
                .build();
    }

    private String extractTimestamp(String line) {
        Pattern timestampPattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z|\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})");
        Matcher matcher = timestampPattern.matcher(line);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String extractLogLevel(String line) {
        String upperLine = line.toUpperCase();

        if (upperLine.contains("ERROR") || upperLine.contains("FATAL") || upperLine.contains("SEVERE")) {
            return "ERROR";
        } else if (upperLine.contains("WARN") || upperLine.contains("WARNING")) {
            return "WARN";
        } else if (upperLine.contains("INFO")) {
            return "INFO";
        } else if (upperLine.contains("DEBUG") || upperLine.contains("TRACE")) {
            return "DEBUG";
        }

        return "INFO";
    }

    private LogSearchResultDTO buildLogSearchResult(List<LogEntryDTO> logs, String searchQuery, String timeRange) {
        Map<String, Integer> logLevelCounts = logs.stream()
                .collect(Collectors.groupingBy(
                        LogEntryDTO::getLogLevel,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> namespaceCounts = logs.stream()
                .collect(Collectors.groupingBy(
                        LogEntryDTO::getNamespace,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> podCounts = logs.stream()
                .collect(Collectors.groupingBy(
                        LogEntryDTO::getPodName,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        return LogSearchResultDTO.builder()
                .logs(logs)
                .totalCount(logs.size())
                .logLevelCounts(logLevelCounts)
                .namespaceCounts(namespaceCounts)
                .podCounts(podCounts)
                .searchQuery(searchQuery)
                .timeRange(timeRange)
                .build();
    }
}
