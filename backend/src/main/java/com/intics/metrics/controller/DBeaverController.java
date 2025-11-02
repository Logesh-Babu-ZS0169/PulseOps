package com.intics.metrics.controller;

import com.intics.metrics.dto.dbeaver.DatabaseConnectionDTO;
import com.intics.metrics.dto.dbeaver.DatabaseSchemaDTO;
import com.intics.metrics.service.DBeaverService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dbeaver")
public class DBeaverController {

    private final DBeaverService dbeaverService;

    public DBeaverController(DBeaverService dbeaverService) {
        this.dbeaverService = dbeaverService;
    }

    @GetMapping("/connection")
    public ResponseEntity<DatabaseConnectionDTO> getConnectionInfo() {
        try {
            DatabaseConnectionDTO connection = dbeaverService.getConnectionInfo();
            return ResponseEntity.ok(connection);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/connection/export")
    public ResponseEntity<String> exportConnectionConfig() {
        try {
            String xml = dbeaverService.generateDBeaverXML();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDispositionFormData("attachment", "pulseops-connection.xml");
            
            return new ResponseEntity<>(xml, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/schemas")
    public ResponseEntity<List<DatabaseSchemaDTO>> getDatabaseSchemas() {
        try {
            List<DatabaseSchemaDTO> schemas = dbeaverService.getDatabaseSchemas();
            return ResponseEntity.ok(schemas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
