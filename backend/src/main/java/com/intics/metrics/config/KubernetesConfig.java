package com.intics.metrics.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class KubernetesConfig {

    @Bean
    public ApiClient apiClient() {
        try {
            ApiClient client;
            
            // First, try in-cluster configuration (when running inside a Kubernetes pod)
            try {
                client = ClientBuilder.cluster().build();
                io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
                log.info("Successfully initialized Kubernetes client using in-cluster configuration");
                return client;
            } catch (Exception inClusterException) {
                log.debug("In-cluster configuration not available: {}", inClusterException.getMessage());
                
                // Fall back to kubeconfig file (for local development)
                try {
                    client = Config.defaultClient();
                    io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
                    log.info("Successfully initialized Kubernetes client from kubeconfig file");
                    return client;
                } catch (Exception kubeconfigException) {
                    log.warn("Could not initialize Kubernetes client from kubeconfig: {}", kubeconfigException.getMessage());
                    throw kubeconfigException;
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize Kubernetes client: {}. Kubernetes monitoring will be unavailable.", e.getMessage());
            return null;
        }
    }

    @Bean
    public CoreV1Api coreV1Api(ApiClient apiClient) {
        if (apiClient == null) {
            log.warn("ApiClient is null, CoreV1Api will be unavailable");
            return null;
        }
        return new CoreV1Api(apiClient);
    }

    @Bean
    public AppsV1Api appsV1Api(ApiClient apiClient) {
        if (apiClient == null) {
            log.warn("ApiClient is null, AppsV1Api will be unavailable");
            return null;
        }
        return new AppsV1Api(apiClient);
    }
}
