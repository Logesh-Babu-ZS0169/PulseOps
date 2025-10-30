package com.intics.metrics.service;

import com.intics.metrics.dto.kubernetes.metrics.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class MetricsDemoDataProvider {

    private final Random random = new Random();

    public ClusterMetricsDTO generateDemoClusterMetrics() {
        log.info("Generating demo cluster metrics");

        List<NodeMetrics> nodeMetrics = generateDemoNodeMetrics();
        List<PodMetrics> podMetrics = generateDemoPodMetrics();

        long totalCpuUsage = nodeMetrics.stream()
                .mapToLong(NodeMetrics::getCpuUsageNanoCores)
                .sum();
        
        long totalCpuCapacity = nodeMetrics.stream()
                .mapToLong(NodeMetrics::getCpuCapacityNanoCores)
                .sum();

        long totalMemoryUsage = nodeMetrics.stream()
                .mapToLong(NodeMetrics::getMemoryUsageBytes)
                .sum();
        
        long totalMemoryCapacity = nodeMetrics.stream()
                .mapToLong(NodeMetrics::getMemoryCapacityBytes)
                .sum();

        long totalDiskUsage = nodeMetrics.stream()
                .mapToLong(NodeMetrics::getDiskUsageBytes)
                .sum();
        
        long totalDiskCapacity = nodeMetrics.stream()
                .mapToLong(NodeMetrics::getDiskCapacityBytes)
                .sum();

        List<PodMetrics> topPodsByCpu = podMetrics.stream()
                .sorted(Comparator.comparing(PodMetrics::getCpuUsageNanoCores).reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<PodMetrics> topPodsByMemory = podMetrics.stream()
                .sorted(Comparator.comparing(PodMetrics::getMemoryUsageBytes).reversed())
                .limit(10)
                .collect(Collectors.toList());

        Map<String, NamespaceMetrics> metricsByNamespace = calculateDemoNamespaceMetrics(podMetrics);

        return ClusterMetricsDTO.builder()
                .timestamp(LocalDateTime.now())
                .totalCpuUsageNanoCores(totalCpuUsage)
                .totalCpuCapacityNanoCores(totalCpuCapacity)
                .cpuUsagePercent(String.format("%.2f%%", (totalCpuUsage * 100.0) / totalCpuCapacity))
                .totalMemoryUsageBytes(totalMemoryUsage)
                .totalMemoryCapacityBytes(totalMemoryCapacity)
                .memoryUsagePercent(String.format("%.2f%%", (totalMemoryUsage * 100.0) / totalMemoryCapacity))
                .totalDiskUsageBytes(totalDiskUsage)
                .totalDiskCapacityBytes(totalDiskCapacity)
                .diskUsagePercent(String.format("%.2f%%", (totalDiskUsage * 100.0) / totalDiskCapacity))
                .totalPods(podMetrics.size())
                .runningPods((int) (podMetrics.size() * 0.9))
                .pendingPods((int) (podMetrics.size() * 0.05))
                .failedPods((int) (podMetrics.size() * 0.05))
                .nodeMetrics(nodeMetrics)
                .topPodsByCpu(topPodsByCpu)
                .topPodsByMemory(topPodsByMemory)
                .metricsByNamespace(metricsByNamespace)
                .build();
    }

    private List<NodeMetrics> generateDemoNodeMetrics() {
        String[] nodeNames = {"node-1", "node-2", "node-3"};
        
        return Arrays.stream(nodeNames)
                .map(nodeName -> {
                    long cpuCapacity = 4_000_000_000L;
                    long cpuUsage = (long) (cpuCapacity * (0.3 + random.nextDouble() * 0.5));
                    
                    long memoryCapacity = 16L * 1024 * 1024 * 1024;
                    long memoryUsage = (long) (memoryCapacity * (0.4 + random.nextDouble() * 0.4));
                    
                    long diskCapacity = 100L * 1024 * 1024 * 1024;
                    long diskUsage = (long) (diskCapacity * (0.3 + random.nextDouble() * 0.5));
                    
                    return NodeMetrics.builder()
                            .nodeName(nodeName)
                            .timestamp(LocalDateTime.now())
                            .cpuUsageNanoCores(cpuUsage)
                            .cpuCapacityNanoCores(cpuCapacity)
                            .cpuUsagePercent(String.format("%.2f%%", (cpuUsage * 100.0) / cpuCapacity))
                            .memoryUsageBytes(memoryUsage)
                            .memoryCapacityBytes(memoryCapacity)
                            .memoryUsagePercent(String.format("%.2f%%", (memoryUsage * 100.0) / memoryCapacity))
                            .diskUsageBytes(diskUsage)
                            .diskCapacityBytes(diskCapacity)
                            .diskUsagePercent(String.format("%.2f%%", (diskUsage * 100.0) / diskCapacity))
                            .networkReceiveBytes((long) (random.nextDouble() * 1000000000))
                            .networkTransmitBytes((long) (random.nextDouble() * 1000000000))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<PodMetrics> generateDemoPodMetrics() {
        String[] namespaces = {"default", "kube-system", "monitoring", "production"};
        String[] podPrefixes = {"web", "api", "worker", "cache", "db", "monitoring"};
        
        long podCpuCapacity = 500_000_000L;
        long podMemoryCapacity = 512L * 1024 * 1024;
        
        return IntStream.range(0, 25)
                .mapToObj(i -> {
                    String namespace = namespaces[random.nextInt(namespaces.length)];
                    String podPrefix = podPrefixes[random.nextInt(podPrefixes.length)];
                    String podName = String.format("%s-%s-%d", podPrefix, namespace, i);
                    
                    long cpuUsage = (long) (podCpuCapacity * (0.1 + random.nextDouble() * 0.7));
                    long memoryUsage = (long) (podMemoryCapacity * (0.2 + random.nextDouble() * 0.6));
                    
                    double cpuPercent = (cpuUsage * 100.0) / podCpuCapacity;
                    double memoryPercent = (memoryUsage * 100.0) / podMemoryCapacity;
                    
                    return PodMetrics.builder()
                            .podName(podName)
                            .namespace(namespace)
                            .timestamp(LocalDateTime.now())
                            .cpuUsageNanoCores(cpuUsage)
                            .cpuUsagePercent(String.format("%.2f%%", cpuPercent))
                            .memoryUsageBytes(memoryUsage)
                            .memoryUsagePercent(String.format("%.2f%%", memoryPercent))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Map<String, NamespaceMetrics> calculateDemoNamespaceMetrics(List<PodMetrics> podMetrics) {
        long namespaceCpuCapacity = 2_000_000_000L;
        long namespaceMemoryCapacity = 4L * 1024 * 1024 * 1024;
        
        return podMetrics.stream()
                .collect(Collectors.groupingBy(
                        PodMetrics::getNamespace,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                pods -> {
                                    long totalCpu = pods.stream()
                                            .mapToLong(PodMetrics::getCpuUsageNanoCores)
                                            .sum();
                                    long totalMemory = pods.stream()
                                            .mapToLong(PodMetrics::getMemoryUsageBytes)
                                            .sum();
                                    
                                    double cpuPercent = (totalCpu * 100.0) / namespaceCpuCapacity;
                                    double memoryPercent = (totalMemory * 100.0) / namespaceMemoryCapacity;
                                    
                                    return NamespaceMetrics.builder()
                                            .namespace(pods.get(0).getNamespace())
                                            .podCount(pods.size())
                                            .totalCpuUsageNanoCores(totalCpu)
                                            .totalMemoryUsageBytes(totalMemory)
                                            .cpuUsagePercent(String.format("%.2f%%", Math.min(cpuPercent, 100.0)))
                                            .memoryUsagePercent(String.format("%.2f%%", Math.min(memoryPercent, 100.0)))
                                            .build();
                                }
                        )
                ));
    }

    public List<MetricsTimeSeriesDTO> generateDemoTimeSeries(String resourceType, String resourceName, int hours) {
        List<MetricsTimeSeriesDTO> timeSeriesList = new ArrayList<>();
        
        List<String> metricTypes = Arrays.asList("cpu", "memory", "network_rx", "network_tx");
        
        for (String metricType : metricTypes) {
            List<MetricsTimeSeriesDTO.DataPoint> dataPoints = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            
            for (int i = hours * 12; i >= 0; i--) {
                LocalDateTime timestamp = now.minusMinutes(i * 5);
                double baseValue = metricType.equals("cpu") ? 50 : 
                                 metricType.equals("memory") ? 60 : 1000;
                double value = baseValue + (random.nextDouble() * 20 - 10);
                
                String unit = metricType.equals("cpu") || metricType.equals("memory") ? "%" : "MB/s";
                
                dataPoints.add(MetricsTimeSeriesDTO.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(value)
                        .unit(unit)
                        .build());
            }
            
            timeSeriesList.add(MetricsTimeSeriesDTO.builder()
                    .resourceName(resourceName)
                    .resourceType(resourceType)
                    .metricType(metricType)
                    .dataPoints(dataPoints)
                    .build());
        }
        
        return timeSeriesList;
    }
}
