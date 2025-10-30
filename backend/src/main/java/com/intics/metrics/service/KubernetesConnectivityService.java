package com.intics.metrics.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class KubernetesConnectivityService {

    @Autowired(required = false)
    private CoreV1Api coreV1Api;

    @Autowired(required = false)
    private ApiClient apiClient;

    private boolean connectionValidated = false;
    private String connectionError = null;

    @PostConstruct
    public void validateConnection() {
        if (coreV1Api == null || apiClient == null) {
            connectionError = "Kubernetes client not initialized. No kubeconfig file found at ~/.kube/config";
            log.warn("Kubernetes connectivity check: {}", connectionError);
            return;
        }

        try {
            // Try to list namespaces to verify connectivity
            coreV1Api.listNamespace(null, null, null, null, null, 1, null, null, null, null);
            connectionValidated = true;
            connectionError = null;
            log.info("Kubernetes connectivity validated successfully");
        } catch (ApiException e) {
            connectionError = String.format("Failed to connect to Kubernetes cluster: %s (HTTP %d)", 
                    e.getMessage(), e.getCode());
            log.warn("Kubernetes connectivity check failed: {}", connectionError);
        } catch (Exception e) {
            connectionError = "Failed to connect to Kubernetes cluster: " + e.getMessage();
            log.warn("Kubernetes connectivity check failed: {}", connectionError);
        }
    }

    public boolean isConnected() {
        return connectionValidated && coreV1Api != null && apiClient != null;
    }

    public boolean isAvailable() {
        return coreV1Api != null && apiClient != null;
    }

    public String getConnectionStatus() {
        if (isConnected()) {
            return "Connected";
        } else if (connectionError != null) {
            return "Disconnected: " + connectionError;
        } else {
            return "Disconnected: Kubernetes client not configured";
        }
    }

    public String getConnectionError() {
        return connectionError;
    }

    public CoreV1Api getCoreV1Api() {
        return coreV1Api;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }
}
