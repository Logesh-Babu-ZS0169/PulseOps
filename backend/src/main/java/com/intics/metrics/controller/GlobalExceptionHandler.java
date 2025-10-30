package com.intics.metrics.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice(annotations = RestController.class)
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        
        // Check if it's a Kubernetes connection error
        if (ex.getMessage() != null && ex.getMessage().contains("Kubernetes")) {
            response.put("error", "Kubernetes Not Connected");
            response.put("message", "Kubernetes cluster is not configured. Please add a kubeconfig file at ~/.kube/config to connect to your cluster.");
            response.put("details", "The Kubernetes monitoring features require access to a Kubernetes cluster. Configure your kubeconfig file and restart the application.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        
        response.put("error", "Service Unavailable");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        
        // Check if it's a Kubernetes connection error
        if (ex.getMessage() != null && ex.getMessage().contains("Failed to connect to localhost")) {
            response.put("error", "Kubernetes Connection Failed");
            response.put("message", "Unable to connect to Kubernetes cluster. The cluster may not be running or the kubeconfig is misconfigured.");
            response.put("details", "Please ensure your Kubernetes cluster is running and accessible, or configure a valid kubeconfig file at ~/.kube/config");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        
        if (ex.getMessage() != null && ex.getMessage().contains("Kubernetes")) {
            response.put("error", "Kubernetes Error");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        
        response.put("error", "Internal Server Error");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
