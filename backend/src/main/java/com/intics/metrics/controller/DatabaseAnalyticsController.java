package com.intics.metrics.controller;

import com.intics.metrics.dto.analytics.*;
import com.intics.metrics.security.JwtUtil;
import com.intics.metrics.service.DatabaseAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/database/analytics")
@CrossOrigin(origins = "*")
public class DatabaseAnalyticsController {

    @Autowired
    private DatabaseAnalyticsService analyticsService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/overview/{connectionId}")
    public ResponseEntity<?> getDatabaseOverview(
            @PathVariable String connectionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
            DatabaseOverviewDTO overview = analyticsService.getDatabaseOverview(connectionId, username);
            return ResponseEntity.ok(overview);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving database overview: " + e.getMessage());
        }
    }

    @GetMapping("/table-health/{connectionId}")
    public ResponseEntity<?> getTableHealth(
            @PathVariable String connectionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
            TableHealthDTO health = analyticsService.getTableHealth(connectionId, username);
            return ResponseEntity.ok(health);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving table health: " + e.getMessage());
        }
    }

    @GetMapping("/column-health/{connectionId}")
    public ResponseEntity<?> getColumnHealth(
            @PathVariable String connectionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
            ColumnHealthDTO health = analyticsService.getColumnHealth(connectionId, username);
            return ResponseEntity.ok(health);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving column health: " + e.getMessage());
        }
    }

    @GetMapping("/relationships/{connectionId}")
    public ResponseEntity<?> getSchemaRelationships(
            @PathVariable String connectionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
            SchemaRelationshipsDTO relationships = analyticsService.getSchemaRelationships(connectionId, username);
            return ResponseEntity.ok(relationships);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving schema relationships: " + e.getMessage());
        }
    }

    @GetMapping("/optimization-suggestions/{connectionId}")
    public ResponseEntity<?> getOptimizationSuggestions(
            @PathVariable String connectionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
            OptimizationSuggestionsDTO suggestions = analyticsService.getOptimizationSuggestions(connectionId, username);
            return ResponseEntity.ok(suggestions);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving optimization suggestions: " + e.getMessage());
        }
    }

    @GetMapping("/postgres/index-analysis/{connectionId}")
    public ResponseEntity<?> analyzeIndexes(
            @PathVariable String connectionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
            IndexRecommendationDTO result = analyticsService.analyzeIndexes(connectionId, username);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error analyzing indexes: " + e.getMessage());
        }
    }

    @GetMapping("/postgres/query-analysis/{connectionId}")
    public ResponseEntity<?> analyzeQueries(
            @PathVariable String connectionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
            QueryOptimizationDTO result = analyticsService.analyzeQueries(connectionId, username);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error analyzing queries: " + e.getMessage());
        }
    }

    @GetMapping("/postgres/config-analysis/{connectionId}")
    public ResponseEntity<?> analyzeConfig(
            @PathVariable String connectionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
            ConfigOptimizationDTO result = analyticsService.analyzeConfig(connectionId, username);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error analyzing configuration: " + e.getMessage());
        }
    }

    @GetMapping("/postgres/vacuum-analysis/{connectionId}")
    public ResponseEntity<?> analyzeVacuum(
            @PathVariable String connectionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
            VacuumAnalysisDTO result = analyticsService.analyzeVacuum(connectionId, username);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error analyzing vacuum status: " + e.getMessage());
        }
    }

    @GetMapping("/postgres/export-report/{connectionId}")
    public ResponseEntity<?> exportOptimizationReport(
            @PathVariable String connectionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
            byte[] excelData = analyticsService.exportOptimizationReport(connectionId, username);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "PostgreSQL_Optimization_Report_" + System.currentTimeMillis() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating report: " + e.getMessage());
        }
    }
}
