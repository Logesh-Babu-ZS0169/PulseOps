package com.intics.metrics.service;

import com.intics.metrics.model.DailyTotalResponse;
import com.intics.metrics.model.HourlyMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class MetricsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public DailyTotalResponse getDailyTotalInbound(LocalDate date, LocalTime startTime, LocalTime endTime) {
        String sql;
        Object[] params;
        
        if (startTime != null && endTime != null) {
            sql = "SELECT COUNT(*) AS total_count_inbound " +
                  "FROM inbound_config.ingestion_file_details AS idf " +
                  "LEFT JOIN inbound_config.ingestion_downloaded_file_details AS idfd " +
                  "ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                  "WHERE idfd.pipeline_initiated_on >= (?::date + ?::time) " +
                  "AND idfd.pipeline_initiated_on < (?::date + ?::time)";
            params = new Object[]{date.toString(), startTime.toString(), date.toString(), endTime.toString()};
        } else {
            sql = "SELECT COUNT(*) AS total_count_inbound " +
                  "FROM inbound_config.ingestion_file_details AS idf " +
                  "LEFT JOIN inbound_config.ingestion_downloaded_file_details AS idfd " +
                  "ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                  "WHERE idfd.pipeline_initiated_on >= ?::date " +
                  "AND idfd.pipeline_initiated_on < (?::date + interval '1 day')";
            params = new Object[]{date.toString(), date.toString()};
        }

        Long count = jdbcTemplate.queryForObject(sql, Long.class, params);
        return new DailyTotalResponse(count != null ? count : 0L);
    }

    public List<HourlyMetrics> getHourlyMetricsCommercial(LocalDate date, LocalTime startTime, LocalTime endTime) {
        String sql;
        Object[] params;
        
        if (startTime != null && endTime != null) {
            sql = "SELECT " +
                  "    date_trunc('hour', idf.request_completed_on) AS time_frame_start, " +
                  "    date_trunc('hour', idf.request_completed_on) + interval '1 hour' AS time_frame_end, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND idfd.status IN ('STAGED', 'COMPLETED', 'FAILED', 'IN_PROGRESS') THEN 1 END) AS total_ingestion, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND idfd.status = 'STAGED' THEN 1 END) AS staged_in_ingestion, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND a.file_extension = 'pdf' AND pqsa.stage <> 'PRODUCT' AND pqsa.status <> 'COMPLETED' THEN 1 END) AS in_progress_count, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'COMPLETED' THEN 1 END) AS completed_count, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'FAILED' THEN 1 END) AS failed_in_process, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND idfd.status = 'FAILED' THEN 1 END) AS failed_in_ingestion, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'ABORTED' THEN 1 END) AS aborted_count " +
                  "FROM inbound_config.ingestion_file_details AS idf " +
                  "JOIN inbound_config.ingestion_downloaded_file_details AS idfd ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                  "LEFT JOIN info.source_of_origin AS soo ON soo.transaction_id = idfd.transaction_id " +
                  "LEFT JOIN info.asset AS a ON a.asset_id = soo.asset_id AND idfd.file_name = a.file_name " +
                  "LEFT JOIN batching.sub_batching_process_payload_queue AS pqsa ON pqsa.origin_id = soo.origin_id " +
                  "LEFT JOIN alchemy_response.pipeline_error_details AS ped ON ped.origin_id = soo.origin_id AND ped.transaction_id = soo.transaction_id " +
                  "LEFT JOIN product_outbound.product_response_details AS prd ON prd.origin_id = soo.origin_id " +
                  "WHERE idf.request_completed_on >= (?::date + ?::time) " +
                  "AND idf.request_completed_on < (?::date + ?::time) " +
                  "AND idfd.document_type = 'MEDICAL_COMMERCIAL' " +
                  "GROUP BY date_trunc('hour', idf.request_completed_on) " +
                  "ORDER BY time_frame_start";
            params = new Object[]{date.toString(), startTime.toString(), date.toString(), endTime.toString()};
        } else {
            sql = "SELECT " +
                  "    date_trunc('hour', idf.request_completed_on) AS time_frame_start, " +
                  "    date_trunc('hour', idf.request_completed_on) + interval '1 hour' AS time_frame_end, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND idfd.status IN ('STAGED', 'COMPLETED', 'FAILED', 'IN_PROGRESS') THEN 1 END) AS total_ingestion, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND idfd.status = 'STAGED' THEN 1 END) AS staged_in_ingestion, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND a.file_extension = 'pdf' AND pqsa.stage <> 'PRODUCT' AND pqsa.status <> 'COMPLETED' THEN 1 END) AS in_progress_count, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'COMPLETED' THEN 1 END) AS completed_count, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'FAILED' THEN 1 END) AS failed_in_process, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND idfd.status = 'FAILED' THEN 1 END) AS failed_in_ingestion, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'ABORTED' THEN 1 END) AS aborted_count " +
                  "FROM inbound_config.ingestion_file_details AS idf " +
                  "JOIN inbound_config.ingestion_downloaded_file_details AS idfd ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                  "LEFT JOIN info.source_of_origin AS soo ON soo.transaction_id = idfd.transaction_id " +
                  "LEFT JOIN info.asset AS a ON a.asset_id = soo.asset_id AND idfd.file_name = a.file_name " +
                  "LEFT JOIN batching.sub_batching_process_payload_queue AS pqsa ON pqsa.origin_id = soo.origin_id " +
                  "LEFT JOIN alchemy_response.pipeline_error_details AS ped ON ped.origin_id = soo.origin_id AND ped.transaction_id = soo.transaction_id " +
                  "LEFT JOIN product_outbound.product_response_details AS prd ON prd.origin_id = soo.origin_id " +
                  "WHERE idf.request_completed_on >= ?::date " +
                  "AND idf.request_completed_on < (?::date + interval '1 day') " +
                  "AND idfd.document_type = 'MEDICAL_COMMERCIAL' " +
                  "GROUP BY date_trunc('hour', idf.request_completed_on) " +
                  "ORDER BY time_frame_start";
            params = new Object[]{date.toString(), date.toString()};
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new HourlyMetrics(
            rs.getTimestamp("time_frame_start").toLocalDateTime(),
            rs.getTimestamp("time_frame_end").toLocalDateTime(),
            rs.getLong("total_ingestion"),
            rs.getLong("staged_in_ingestion"),
            rs.getLong("in_progress_count"),
            rs.getLong("completed_count"),
            rs.getLong("failed_in_process"),
            rs.getLong("failed_in_ingestion"),
            rs.getLong("aborted_count")
        ));
    }

    public List<HourlyMetrics> getHourlyMetricsGBD(LocalDate date, LocalTime startTime, LocalTime endTime) {
        String sql;
        Object[] params;
        
        if (startTime != null && endTime != null) {
            sql = "SELECT " +
                  "    date_trunc('hour', idf.request_completed_on) AS time_frame_start, " +
                  "    date_trunc('hour', idf.request_completed_on) + interval '1 hour' AS time_frame_end, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND idfd.status IN ('STAGED', 'COMPLETED', 'FAILED', 'IN_PROGRESS') THEN 1 END) AS total_ingestion, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND idfd.status = 'STAGED' THEN 1 END) AS staged_in_ingestion, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND a.file_extension = 'pdf' AND pqsa.stage <> 'PRODUCT' AND pqsa.status <> 'COMPLETED' THEN 1 END) AS in_progress_count, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'COMPLETED' THEN 1 END) AS completed_count, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'FAILED' THEN 1 END) AS failed_in_process, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND idfd.status = 'FAILED' THEN 1 END) AS failed_in_ingestion, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'ABORTED' THEN 1 END) AS aborted_count " +
                  "FROM inbound_config.ingestion_file_details AS idf " +
                  "JOIN inbound_config.ingestion_downloaded_file_details AS idfd ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                  "LEFT JOIN info.source_of_origin AS soo ON soo.transaction_id = idfd.transaction_id " +
                  "LEFT JOIN info.asset AS a ON a.asset_id = soo.asset_id AND idfd.file_name = a.file_name " +
                  "LEFT JOIN batching.sub_batching_process_payload_queue AS pqsa ON pqsa.origin_id = soo.origin_id " +
                  "LEFT JOIN alchemy_response.pipeline_error_details AS ped ON ped.origin_id = soo.origin_id AND ped.transaction_id = soo.transaction_id " +
                  "LEFT JOIN product_outbound.product_response_details AS prd ON prd.origin_id = soo.origin_id " +
                  "WHERE idf.request_completed_on >= (?::date + ?::time) " +
                  "AND idf.request_completed_on < (?::date + ?::time) " +
                  "AND idfd.document_type = 'MEDICAL_GBD' " +
                  "GROUP BY date_trunc('hour', idf.request_completed_on) " +
                  "ORDER BY time_frame_start";
            params = new Object[]{date.toString(), startTime.toString(), date.toString(), endTime.toString()};
        } else {
            sql = "SELECT " +
                  "    date_trunc('hour', idf.request_completed_on) AS time_frame_start, " +
                  "    date_trunc('hour', idf.request_completed_on) + interval '1 hour' AS time_frame_end, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND idfd.status IN ('STAGED', 'COMPLETED', 'FAILED', 'IN_PROGRESS') THEN 1 END) AS total_ingestion, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND idfd.status = 'STAGED' THEN 1 END) AS staged_in_ingestion, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND a.file_extension = 'pdf' AND pqsa.stage <> 'PRODUCT' AND pqsa.status <> 'COMPLETED' THEN 1 END) AS in_progress_count, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'COMPLETED' THEN 1 END) AS completed_count, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'FAILED' THEN 1 END) AS failed_in_process, " +
                  "    COUNT(CASE WHEN ped.status IS NULL AND idfd.status = 'FAILED' THEN 1 END) AS failed_in_ingestion, " +
                  "    COUNT(CASE WHEN a.file_extension = 'pdf' AND ped.status = 'ABORTED' THEN 1 END) AS aborted_count " +
                  "FROM inbound_config.ingestion_file_details AS idf " +
                  "JOIN inbound_config.ingestion_downloaded_file_details AS idfd ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                  "LEFT JOIN info.source_of_origin AS soo ON soo.transaction_id = idfd.transaction_id " +
                  "LEFT JOIN info.asset AS a ON a.asset_id = soo.asset_id AND idfd.file_name = a.file_name " +
                  "LEFT JOIN batching.sub_batching_process_payload_queue AS pqsa ON pqsa.origin_id = soo.origin_id " +
                  "LEFT JOIN alchemy_response.pipeline_error_details AS ped ON ped.origin_id = soo.origin_id AND ped.transaction_id = soo.transaction_id " +
                  "LEFT JOIN product_outbound.product_response_details AS prd ON prd.origin_id = soo.origin_id " +
                  "WHERE idf.request_completed_on >= ?::date " +
                  "AND idf.request_completed_on < (?::date + interval '1 day') " +
                  "AND idfd.document_type = 'MEDICAL_GBD' " +
                  "GROUP BY date_trunc('hour', idf.request_completed_on) " +
                  "ORDER BY time_frame_start";
            params = new Object[]{date.toString(), date.toString()};
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new HourlyMetrics(
            rs.getTimestamp("time_frame_start").toLocalDateTime(),
            rs.getTimestamp("time_frame_end").toLocalDateTime(),
            rs.getLong("total_ingestion"),
            rs.getLong("staged_in_ingestion"),
            rs.getLong("in_progress_count"),
            rs.getLong("completed_count"),
            rs.getLong("failed_in_process"),
            rs.getLong("failed_in_ingestion"),
            rs.getLong("aborted_count")
        ));
    }
}
