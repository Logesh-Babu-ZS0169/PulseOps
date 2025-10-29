package com.intics.metrics.controller;

import com.intics.metrics.model.DailyTotalResponse;
import com.intics.metrics.model.HourlyMetrics;
import com.intics.metrics.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "Endpoints for retrieving daily and hourly ingestion metrics")
public class MetricsController {

    @Autowired
    private MetricsService metricsService;

    @Operation(
            summary = "Get Daily Total Metrics",
            description = "Retrieves total inbound document counts for a specific date with optional time range filtering. " +
                    "Returns counts for MEDICAL_COMMERCIAL and MEDICAL_GBD document types with status breakdown.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved daily metrics"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/daily-total")
    public ResponseEntity<DailyTotalResponse> getDailyTotal(
            @Parameter(description = "Target date (defaults to today)", example = "2025-10-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Start time for filtering (optional)", example = "09:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @Parameter(description = "End time for filtering (optional)", example = "17:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(metricsService.getDailyTotalInbound(targetDate, startTime, endTime));
    }

    @Operation(
            summary = "Get Hourly MEDICAL_COMMERCIAL Metrics",
            description = "Retrieves hourly breakdown of MEDICAL_COMMERCIAL document ingestion with status counts for a specific date",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved hourly commercial metrics"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/hourly/commercial")
    public ResponseEntity<List<HourlyMetrics>> getHourlyCommercial(
            @Parameter(description = "Target date (defaults to today)", example = "2025-10-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Start time for filtering (optional)", example = "09:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @Parameter(description = "End time for filtering (optional)", example = "17:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(metricsService.getHourlyMetricsCommercial(targetDate, startTime, endTime));
    }

    @Operation(
            summary = "Get Hourly MEDICAL_GBD Metrics",
            description = "Retrieves hourly breakdown of MEDICAL_GBD document ingestion with status counts for a specific date",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved hourly GBD metrics"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/hourly/gbd")
    public ResponseEntity<List<HourlyMetrics>> getHourlyGBD(
            @Parameter(description = "Target date (defaults to today)", example = "2025-10-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Start time for filtering (optional)", example = "09:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @Parameter(description = "End time for filtering (optional)", example = "17:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(metricsService.getHourlyMetricsGBD(targetDate, startTime, endTime));
    }
}
