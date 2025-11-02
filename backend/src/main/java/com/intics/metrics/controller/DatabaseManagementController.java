package com.intics.metrics.controller;

import com.intics.metrics.dto.database.*;
import com.intics.metrics.service.DatabaseConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = "*")
public class DatabaseManagementController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManagementController.class);

    @Autowired
    private DatabaseConnectionService databaseConnectionService;

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {
            throw new SecurityException("Authentication required");
        }
        return authentication.getName();
    }

    @PostMapping("/connections/test")
    public ResponseEntity<DatabaseConnectionResponse> testConnection(@RequestBody DatabaseConnectionRequest request) {
        try {
            String username = getAuthenticatedUsername();
            logger.info("User {} testing database connection to {}", username, request.getHost());
            DatabaseConnectionResponse response = databaseConnectionService.testConnection(request);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            logger.warn("Unauthenticated test connection attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new DatabaseConnectionResponse(null, request.getConnectionName(),
                            request.getDatabaseType(), request.getHost(), request.getPort(),
                            request.getDatabaseName(), request.getUsername(),
                            false, "Authentication required"));
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DatabaseConnectionResponse(null, request.getConnectionName(),
                            request.getDatabaseType(), request.getHost(), request.getPort(),
                            request.getDatabaseName(), request.getUsername(),
                            false, "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/connections")
    public ResponseEntity<DatabaseConnectionResponse> createConnection(@RequestBody DatabaseConnectionRequest request) {
        try {
            String username = getAuthenticatedUsername();
            DatabaseConnectionResponse response = databaseConnectionService.createAndSaveConnection(request, username);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            logger.warn("Unauthenticated connection creation attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new DatabaseConnectionResponse(null, request.getConnectionName(),
                            request.getDatabaseType(), request.getHost(), request.getPort(),
                            request.getDatabaseName(), request.getUsername(),
                            false, "Authentication required"));
        } catch (Exception e) {
            logger.error("Error creating connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DatabaseConnectionResponse(null, request.getConnectionName(),
                            request.getDatabaseType(), request.getHost(), request.getPort(),
                            request.getDatabaseName(), request.getUsername(),
                            false, "Error: " + e.getMessage()));
        }
    }

    @GetMapping("/connections")
    public ResponseEntity<List<DatabaseConnectionResponse>> getAllConnections() {
        try {
            String username = getAuthenticatedUsername();
            List<DatabaseConnectionResponse> connections = databaseConnectionService.getAllConnections(username);
            return ResponseEntity.ok(connections);
        } catch (SecurityException e) {
            logger.warn("Unauthenticated getAllConnections attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            logger.error("Error getting connections", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/connections/{connectionId}")
    public ResponseEntity<Void> deleteConnection(@PathVariable String connectionId) {
        try {
            String username = getAuthenticatedUsername();
            databaseConnectionService.closeConnection(connectionId, username);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            if (e.getMessage().equals("Authentication required")) {
                logger.warn("Unauthenticated deleteConnection attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            logger.warn("Unauthorized access attempt to connection {}", connectionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error deleting connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/connections/{connectionId}/schemas")
    public ResponseEntity<List<SchemaDTO>> getSchemas(@PathVariable String connectionId) {
        try {
            String username = getAuthenticatedUsername();
            List<SchemaDTO> schemas = databaseConnectionService.getSchemas(connectionId, username);
            return ResponseEntity.ok(schemas);
        } catch (SecurityException e) {
            if (e.getMessage().equals("Authentication required")) {
                logger.warn("Unauthenticated getSchemas attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            logger.warn("Unauthorized access attempt to connection {}", connectionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error getting schemas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/connections/{connectionId}/schemas/{schemaName}/tables")
    public ResponseEntity<List<String>> getTables(
            @PathVariable String connectionId,
            @PathVariable String schemaName) {
        try {
            String username = getAuthenticatedUsername();
            List<String> tables = databaseConnectionService.getTables(connectionId, schemaName, username);
            return ResponseEntity.ok(tables);
        } catch (SecurityException e) {
            if (e.getMessage().equals("Authentication required")) {
                logger.warn("Unauthenticated getTables attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            logger.warn("Unauthorized access attempt to connection {}", connectionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error getting tables", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/connections/{connectionId}/schemas/{schemaName}/tables/{tableName}/metadata")
    public ResponseEntity<TableMetadataDTO> getTableMetadata(
            @PathVariable String connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName) {
        try {
            String username = getAuthenticatedUsername();
            TableMetadataDTO metadata = databaseConnectionService.getTableMetadata(
                    connectionId, schemaName, tableName, username);
            return ResponseEntity.ok(metadata);
        } catch (SecurityException e) {
            if (e.getMessage().equals("Authentication required")) {
                logger.warn("Unauthenticated getTableMetadata attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            logger.warn("Unauthorized access attempt to connection {}", connectionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error getting table metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/connections/{connectionId}/schemas/{schemaName}/tables/{tableName}/data")
    public ResponseEntity<TableDataDTO> getTableData(
            @PathVariable String connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize) {
        try {
            String username = getAuthenticatedUsername();
            TableDataDTO data = databaseConnectionService.getTableData(
                    connectionId, schemaName, tableName, page, pageSize, username);
            return ResponseEntity.ok(data);
        } catch (SecurityException e) {
            if (e.getMessage().equals("Authentication required")) {
                logger.warn("Unauthenticated getTableData attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            logger.warn("Unauthorized access attempt to connection {}", connectionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error getting table data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
