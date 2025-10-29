package com.intics.metrics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ScheduledMetricsChecker {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledMetricsChecker.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmailService emailService;

    private LocalDateTime lastCommercialCheck;
    private LocalDateTime lastGbdCheck;

    @PostConstruct
    public void init() {
        lastCommercialCheck = LocalDateTime.now().minusHours(1);
        lastGbdCheck = LocalDateTime.now().minusHours(1);
        logger.info("ScheduledMetricsChecker initialized. Starting checks every 5 minutes...");
    }

    // Runs every 5 minutes
    @Scheduled(cron = "${SCHEDULER_CRON_COMMERCIAL:0 */5 * * * *}")
    public void checkCommercialNewRows() {
        logger.info("Checking for new MEDICAL_COMMERCIAL rows for last hour...");
        checkForNewRows("MEDICAL_COMMERCIAL", lastCommercialCheck);
        lastCommercialCheck = LocalDateTime.now().minusHours(1);
    }

    // Runs every 5 minutes
    @Scheduled(cron = "${SCHEDULER_CRON_COMMERCIAL:0 */5 * * * *}")
    public void checkGbdNewRows() {
        logger.info("Checking for new MEDICAL_GBD rows for last hour...");
        checkForNewRows("MEDICAL_GBD", lastGbdCheck);
        lastGbdCheck = LocalDateTime.now().minusHours(1);
    }

    private void checkForNewRows(String documentType, LocalDateTime lastCheckTime) {
        try {
            // Calculate last completed hour
            LocalDateTime endHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0); // current hour start
            LocalDateTime startHour = endHour.minusHours(1); // previous hour

            String sql = "SELECT " +
                    "date_trunc('hour', idf.request_completed_on) AS time_frame_start, " +
                    "date_trunc('hour', idf.request_completed_on) + interval '1 hour' AS time_frame_end, " +
                    "COUNT(CASE WHEN a.file_extension = 'pdf' AND idfd.status IN ('STAGED','COMPLETED','FAILED','IN_PROGRESS') THEN 1 END) AS total_ingestion, " +
                    "COUNT(CASE WHEN ped.status IS NULL AND idfd.status='STAGED' THEN 1 END) AS staged_in_ingestion, " +
                    "COUNT(CASE WHEN ped.status IS NULL AND a.file_extension='pdf' AND pqsa.stage<>'PRODUCT' AND pqsa.status<>'COMPLETED' THEN 1 END) AS in_progress_count, " +
                    "COUNT(CASE WHEN a.file_extension='pdf' AND ped.status='COMPLETED' THEN 1 END) AS completed_count, " +
                    "COUNT(CASE WHEN a.file_extension='pdf' AND ped.status='FAILED' THEN 1 END) AS failed_in_process, " +
                    "COUNT(CASE WHEN ped.status IS NULL AND idfd.status='FAILED' THEN 1 END) AS failed_in_ingestion, " +
                    "COUNT(CASE WHEN a.file_extension='pdf' AND ped.status='ABORTED' THEN 1 END) AS aborted_count " +
                    "FROM inbound_config.ingestion_file_details AS idf " +
                    "JOIN inbound_config.ingestion_downloaded_file_details AS idfd ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                    "LEFT JOIN info.source_of_origin AS soo ON soo.transaction_id = idfd.transaction_id " +
                    "LEFT JOIN info.asset AS a ON a.asset_id = soo.asset_id AND idfd.file_name = a.file_name " +
                    "LEFT JOIN batching.sub_batching_process_payload_queue AS pqsa ON pqsa.origin_id = soo.origin_id " +
                    "LEFT JOIN alchemy_response.pipeline_error_details AS ped ON ped.origin_id = soo.origin_id AND ped.transaction_id = soo.transaction_id " +
                    "WHERE idf.request_completed_on >= ? AND idf.request_completed_on < ? " +
                    "AND idfd.document_type = ? " +
                    "GROUP BY date_trunc('hour', idf.request_completed_on) " +
                    "ORDER BY time_frame_start";

            List<Map<String, Object>> newRows = jdbcTemplate.queryForList(sql, startHour, endHour, documentType);

            if (newRows != null && !newRows.isEmpty()) {
                logger.info("Found {} new row(s) for {} in last hour", newRows.size(), documentType);

                StringBuilder metricsDetails = new StringBuilder();
                metricsDetails.append("Hourly Breakdown for last hour:\n");
                metricsDetails.append("=".repeat(80)).append("\n\n");

                for (Map<String, Object> row : newRows) {
                    String hour = row.get("time_frame_start").toString();
                    Long totalCount = ((Number) row.get("total_ingestion")).longValue();
                    Long staged = ((Number) row.get("staged_in_ingestion")).longValue();
                    Long inProgress = ((Number) row.get("in_progress_count")).longValue();
                    Long completed = ((Number) row.get("completed_count")).longValue();
                    Long failed = ((Number) row.get("failed_in_process")).longValue();
                    Long failedInIngestion = ((Number) row.get("failed_in_ingestion")).longValue();
                    Long aborted = ((Number) row.get("aborted_count")).longValue();

                    metricsDetails.append("Hour: ").append(hour).append("\n")
                            .append("  Total Documents: ").append(totalCount).append("\n")
                            .append("  Staged: ").append(staged).append("\n")
                            .append("  In Progress: ").append(inProgress).append("\n")
                            .append("  Completed: ").append(completed).append("\n")
                            .append("  Failed: ").append(failed).append("\n")
                            .append("  Failed in Ingestion: ").append(failedInIngestion).append("\n")
                            .append("  Aborted: ").append(aborted).append("\n")
                            .append("-".repeat(80)).append("\n\n");
                }

                String hourRange = formatDateTime(startHour) + " to " + formatDateTime(endHour);
                emailService.sendCombinedMetricsEmail(documentType, newRows.size(), hourRange, metricsDetails.toString());

            } else {
                logger.info("No new rows found for {} in last hour", documentType);
            }

        } catch (Exception e) {
            logger.error("Error checking for new rows in {}: {}", documentType, e.getMessage(), e);
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }
}
