package com.intics.metrics.service;

import com.intics.metrics.dto.kubernetes.logs.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class KubernetesDemoDataProvider {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    private static final String[] DEMO_NAMESPACES = {"default", "kube-system", "production", "staging"};
    private static final String[] DEMO_PODS = {
        "app-deployment-7d8f6c9b5d-x7k2p",
        "api-server-6b5d8f7c9d-m4n3l",
        "database-postgres-0",
        "redis-cache-5f8c9b7d6e-q9w2r",
        "nginx-ingress-controller-8c7d9f6e5b-t5y6u"
    };
    
    private static final String[] LOG_TEMPLATES = {
        "Starting application on port 8080",
        "Database connection established successfully",
        "Processing request for user: %s",
        "Cache hit for key: %s",
        "Request completed in %dms",
        "Health check passed",
        "Connecting to Redis cluster at redis://redis-service:6379",
        "Executing database query: SELECT * FROM users WHERE id = %d",
        "API request received: GET /api/v1/users",
        "Authentication successful for user: %s",
        "ERROR: Failed to connect to database - connection timeout",
        "WARN: High memory usage detected: %d%%",
        "ERROR: Unable to process request - null pointer exception",
        "WARN: Slow query detected, took %dms",
        "INFO: Successfully processed %d items",
        "DEBUG: Cache statistics - hits: %d, misses: %d",
        "ERROR: Service unavailable - maximum retry attempts exceeded",
        "WARN: Deprecated API endpoint called: /api/v1/old-endpoint"
    };

    public LogSearchResultDTO generateDemoLogs(String namespace, String searchTerm, String logLevel, Integer sinceSeconds) {
        log.info("Generating demo logs for namespace: {}, searchTerm: {}, logLevel: {}", namespace, searchTerm, logLevel);
        
        List<LogEntryDTO> logs = new ArrayList<>();
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();
        
        // Generate 50-100 demo log entries
        int logCount = 50 + random.nextInt(51);
        
        for (int i = 0; i < logCount; i++) {
            String pod = DEMO_PODS[random.nextInt(DEMO_PODS.length)];
            String ns = namespace != null ? namespace : DEMO_NAMESPACES[random.nextInt(DEMO_NAMESPACES.length)];
            String level = generateLogLevel(random, logLevel);
            String message = generateLogMessage(random, level);
            
            // Skip if doesn't match search term
            if (searchTerm != null && !searchTerm.isEmpty() 
                    && !message.toLowerCase().contains(searchTerm.toLowerCase())) {
                continue;
            }
            
            LocalDateTime timestamp = now.minusSeconds(random.nextInt(sinceSeconds != null ? sinceSeconds : 3600));
            
            LogEntryDTO entry = new LogEntryDTO();
            entry.setTimestamp(timestamp.format(TIMESTAMP_FORMATTER));
            entry.setPodName(pod);
            entry.setNamespace(ns);
            entry.setContainerName("main");
            entry.setLogLevel(level);
            entry.setMessage(message);
            
            logs.add(entry);
        }
        
        // Sort by timestamp descending
        logs.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        
        // Calculate aggregations
        Map<String, Integer> logLevelCounts = new HashMap<>();
        Map<String, Integer> namespaceCounts = new HashMap<>();
        Map<String, Integer> podCounts = new HashMap<>();
        
        for (LogEntryDTO log : logs) {
            logLevelCounts.put(log.getLogLevel(), logLevelCounts.getOrDefault(log.getLogLevel(), 0) + 1);
            namespaceCounts.put(log.getNamespace(), namespaceCounts.getOrDefault(log.getNamespace(), 0) + 1);
            podCounts.put(log.getPodName(), podCounts.getOrDefault(log.getPodName(), 0) + 1);
        }
        
        LogSearchResultDTO result = new LogSearchResultDTO();
        result.setLogs(logs);
        result.setTotalCount(logs.size());
        result.setLogLevelCounts(logLevelCounts);
        result.setNamespaceCounts(namespaceCounts);
        result.setPodCounts(podCounts);
        result.setNamespace(namespace != null ? namespace : "all");
        result.setSearchTerm(searchTerm);
        result.setLogLevel(logLevel);
        result.setDemoMode(true);
        
        return result;
    }

    public LogStatisticsDTO generateDemoStatistics(String namespace, Integer sinceSeconds) {
        log.info("Generating demo statistics for namespace: {}", namespace);
        
        Random random = new Random();
        
        LogStatisticsDTO stats = new LogStatisticsDTO();
        stats.setNamespace(namespace != null ? namespace : "all");
        stats.setTimeRangeSeconds(sinceSeconds != null ? sinceSeconds : 3600);
        
        // Generate log level counts (for both old and new fields)
        Map<String, Integer> levelCounts = new HashMap<>();
        levelCounts.put("ERROR", 15 + random.nextInt(20));
        levelCounts.put("WARN", 30 + random.nextInt(40));
        levelCounts.put("INFO", 100 + random.nextInt(150));
        levelCounts.put("DEBUG", 50 + random.nextInt(100));
        stats.setLogLevelCounts(levelCounts);
        stats.setLogsByLevel(levelCounts);  // For backwards compatibility
        
        // Generate pod statistics
        List<PodLogStatDTO> podStats = new ArrayList<>();
        Map<String, Integer> logsByNamespace = new HashMap<>();
        Map<String, Integer> logsByPod = new HashMap<>();
        Map<String, Integer> errorsByPod = new HashMap<>();
        
        for (String pod : DEMO_PODS) {
            String ns = namespace != null ? namespace : DEMO_NAMESPACES[random.nextInt(DEMO_NAMESPACES.length)];
            int totalLogs = 50 + random.nextInt(100);
            int errorCount = random.nextInt(10);
            
            PodLogStatDTO podStat = new PodLogStatDTO();
            podStat.setPodName(pod);
            podStat.setNamespace(ns);
            podStat.setTotalLogs(totalLogs);
            podStat.setErrorCount(errorCount);
            podStat.setWarningCount(5 + random.nextInt(20));
            podStats.add(podStat);
            
            logsByNamespace.put(ns, logsByNamespace.getOrDefault(ns, 0) + totalLogs);
            logsByPod.put(pod, totalLogs);
            errorsByPod.put(pod, errorCount);
        }
        stats.setPodStatistics(podStats);
        stats.setLogsByNamespace(logsByNamespace);
        stats.setLogsByPod(logsByPod);
        stats.setErrorsByPod(errorsByPod);
        
        // Calculate totals
        int totalLogs = levelCounts.values().stream().mapToInt(Integer::intValue).sum();
        stats.setTotalLogs(totalLogs);
        stats.setErrorCount(levelCounts.get("ERROR"));
        stats.setWarningCount(levelCounts.get("WARN"));
        stats.setInfoCount(levelCounts.get("INFO"));
        
        // Top error pods
        List<PodLogStatDTO> topErrorPods = podStats.stream()
                .sorted((a, b) -> Integer.compare(b.getErrorCount(), a.getErrorCount()))
                .limit(5)
                .collect(Collectors.toList());
        stats.setTopErrorPods(topErrorPods);
        
        stats.setDemoMode(true);
        
        return stats;
    }

    private String generateLogLevel(Random random, String filterLevel) {
        if (filterLevel != null && !filterLevel.equals("ALL")) {
            return filterLevel;
        }
        
        int rand = random.nextInt(100);
        if (rand < 10) return "ERROR";
        if (rand < 30) return "WARN";
        if (rand < 70) return "INFO";
        return "DEBUG";
    }

    private String generateLogMessage(Random random, String level) {
        String template = LOG_TEMPLATES[random.nextInt(LOG_TEMPLATES.length)];
        
        try {
            // Count format specifiers
            long stringCount = template.chars().filter(ch -> ch == '%').count() - template.chars().filter(ch -> ch == 'd' || ch == 's').count();
            boolean hasString = template.contains("%s");
            boolean hasInt = template.contains("%d");
            
            if (hasString && hasInt) {
                String user = "user_" + (1000 + random.nextInt(9000));
                int value = random.nextInt(1000);
                if (template.contains("ms")) {
                    value = 10 + random.nextInt(5000);
                } else if (template.contains("%")) {
                    value = 50 + random.nextInt(50);
                }
                return String.format(template, user, value);
            } else if (hasString) {
                return String.format(template, "user_" + (1000 + random.nextInt(9000)));
            } else if (hasInt) {
                // Count how many %d there are
                long intCount = template.length() - template.replace("%d", "").length() / 2;
                if (intCount == 2) {
                    int val1 = 100 + random.nextInt(900);
                    int val2 = 10 + random.nextInt(90);
                    return String.format(template, val1, val2);
                } else {
                    int value = random.nextInt(1000);
                    if (template.contains("ms")) {
                        value = 10 + random.nextInt(5000);
                    } else if (template.contains("%")) {
                        value = 50 + random.nextInt(50);
                    }
                    return String.format(template, value);
                }
            }
            
            return template;
        } catch (Exception e) {
            // If formatting fails, return a simple message
            return template.replaceAll("%[sd]", "").trim();
        }
    }

    public List<String> getDemoNamespaces() {
        return Arrays.asList(DEMO_NAMESPACES);
    }
}
