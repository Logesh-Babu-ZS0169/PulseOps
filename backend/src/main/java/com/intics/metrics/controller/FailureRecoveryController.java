package com.intics.metrics.controller;

import com.intics.metrics.model.*;
import com.intics.metrics.service.ExcelExportService;
import com.intics.metrics.service.FailureRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/failure-recovery")
@CrossOrigin(origins = "*")
@Tag(name = "Failure Recovery", description = "Endpoints for retrieving failure recovery reports and document status tracking")
public class FailureRecoveryController {

    @Autowired
    private FailureRecoveryService failureRecoveryService;

    @Autowired
    private ExcelExportService excelExportService;

    @Operation(
            summary = "Get Recovered Documents",
            description = "Retrieves documents that initially failed during inbound ingestion but were successfully processed later",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved recovered documents"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/recovered")
    public List<RecoveredDocument> getRecoveredFromInboundFailed(
            @Parameter(description = "Target date (defaults to today)", example = "2025-10-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Start time for filtering (optional)", example = "09:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @Parameter(description = "End time for filtering (optional)", example = "17:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        String dateStr = (date != null) ? date.toString() : LocalDate.now().toString();
        String startTimeStr = (startTime != null) ? startTime.toString() : null;
        String endTimeStr = (endTime != null) ? endTime.toString() : null;
        return failureRecoveryService.getRecoveredFromInboundFailed(dateStr, startTimeStr, endTimeStr);
    }

    @Operation(
            summary = "Get Kill Switch - Documents Waiting",
            description = "Retrieves documents staged or in-progress that exceed waiting threshold",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved waiting documents"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/waiting")
    public List<WaitingDocument> getDocumentsWaitingToProcess(
            @Parameter(description = "Target date (defaults to today)", example = "2025-10-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Start time for filtering (optional)", example = "09:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @Parameter(description = "End time for filtering (optional)", example = "17:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        String dateStr = (date != null) ? date.toString() : LocalDate.now().toString();
        String startTimeStr = (startTime != null) ? startTime.toString() : null;
        String endTimeStr = (endTime != null) ? endTime.toString() : null;
        return failureRecoveryService.getDocumentsWaitingToProcess(dateStr, startTimeStr, endTimeStr);
    }

    @Operation(
            summary = "Get Inbound Failed Documents",
            description = "Retrieves documents that failed during inbound ingestion phase",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved inbound failed documents"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/inbound-failed")
    public List<InboundFailedDocument> getInboundFailedStatus(
            @Parameter(description = "Target date (defaults to today)", example = "2025-10-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Start time for filtering (optional)", example = "09:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @Parameter(description = "End time for filtering (optional)", example = "17:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        String dateStr = (date != null) ? date.toString() : LocalDate.now().toString();
        String startTimeStr = (startTime != null) ? startTime.toString() : null;
        String endTimeStr = (endTime != null) ? endTime.toString() : null;
        return failureRecoveryService.getInboundFailedStatus(dateStr, startTimeStr, endTimeStr);
    }

    @Operation(
            summary = "Get Process Failed Documents",
            description = "Retrieves documents that failed during processing phase with audit logs including root_pipeline_id, action_id, and log details. Uses DISTINCT ON to prevent duplicate rows.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved process failed documents"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/process-failed")
    public List<ProcessFailedDocument> getProcessFailedStatus(
            @Parameter(description = "Target date (defaults to today)", example = "2025-10-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Start time for filtering (optional)", example = "09:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @Parameter(description = "End time for filtering (optional)", example = "17:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        String dateStr = (date != null) ? date.toString() : LocalDate.now().toString();
        String startTimeStr = (startTime != null) ? startTime.toString() : null;
        String endTimeStr = (endTime != null) ? endTime.toString() : null;
        return failureRecoveryService.getProcessFailedStatus(dateStr, startTimeStr, endTimeStr);
    }

    @Operation(
            summary = "Get Aborted Documents",
            description = "Retrieves documents where pipeline aborted mid-way during processing",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved aborted documents"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/aborted")
    public List<AbortedDocument> getAbortedStatus(
            @Parameter(description = "Target date (defaults to today)", example = "2025-10-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Start time for filtering (optional)", example = "09:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @Parameter(description = "End time for filtering (optional)", example = "17:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        String dateStr = (date != null) ? date.toString() : LocalDate.now().toString();
        String startTimeStr = (startTime != null) ? startTime.toString() : null;
        String endTimeStr = (endTime != null) ? endTime.toString() : null;
        return failureRecoveryService.getAbortedStatus(dateStr, startTimeStr, endTimeStr);
    }

    @Operation(
            summary = "Download All Failure Recovery Reports (XLSX)",
            description = "Exports all five failure recovery reports (Recovered, Waiting, Inbound Failed, Process Failed, Aborted) to a single Excel file with multiple sheets",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully generated Excel file"),
                    @ApiResponse(responseCode = "500", description = "Error generating Excel file")
            }
    )
    @GetMapping("/download-excel")
    public ResponseEntity<byte[]> downloadFailureRecoveryReportsExcel(
            @Parameter(description = "Target date (defaults to today)", example = "2025-10-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Start time for filtering (optional)", example = "09:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @Parameter(description = "End time for filtering (optional)", example = "17:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        try {
            String dateStr = (date != null) ? date.toString() : LocalDate.now().toString();
            String startTimeStr = (startTime != null) ? startTime.toString() : null;
            String endTimeStr = (endTime != null) ? endTime.toString() : null;
            
            List<RecoveredDocument> recovered = failureRecoveryService.getRecoveredFromInboundFailed(dateStr, startTimeStr, endTimeStr);
            List<WaitingDocument> waiting = failureRecoveryService.getDocumentsWaitingToProcess(dateStr, startTimeStr, endTimeStr);
            List<InboundFailedDocument> inboundFailed = failureRecoveryService.getInboundFailedStatus(dateStr, startTimeStr, endTimeStr);
            List<ProcessFailedDocument> processFailed = failureRecoveryService.getProcessFailedStatus(dateStr, startTimeStr, endTimeStr);
            List<AbortedDocument> aborted = failureRecoveryService.getAbortedStatus(dateStr, startTimeStr, endTimeStr);

            byte[] excelBytes = excelExportService.generateFailureRecoveryReportsExcel(
                    recovered, waiting, inboundFailed, processFailed, aborted);

            String filename = "failure_recovery_reports_" + dateStr + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
