package com.intics.metrics.service;

import com.intics.metrics.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FailureRecoveryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<RecoveredDocument> getRecoveredFromInboundFailed(String date, String startTime, String endTime) {
        String sql;
        List<Object> params = new ArrayList<>();
        
        if (startTime != null && endTime != null) {
            sql = "SELECT " +
                    "idfd.document_id AS dcn_id, " +
                    "idfd.request_txn_id AS si_case_id, " +
                    "idfd.document_type, " +
                    "idfd.download_completed_on " +
                    "FROM inbound_config.ingestion_downloaded_file_details AS idfd " +
                    "JOIN info.source_of_origin AS soo ON soo.transaction_id = idfd.transaction_id " +
                    "JOIN info.asset AS a ON a.asset_id = soo.asset_id AND a.file_name = idfd.file_name " +
                    "JOIN alchemy_response.pipeline_error_details AS ped ON ped.origin_id = soo.origin_id AND ped.transaction_id = soo.transaction_id " +
                    "JOIN product_outbound.product_response_details AS prd ON soo.origin_id = prd.origin_id " +
                    "WHERE idfd.download_completed_on >= (CAST(? AS DATE) + CAST(? AS TIME)) " +
                    "AND idfd.download_completed_on < (CAST(? AS DATE) + CAST(? AS TIME)) " +
                    "AND idfd.status = 'FAILED' " +
                    "AND ped.status = 'COMPLETED'";
            params.add(date);
            params.add(startTime);
            params.add(date);
            params.add(endTime);
        } else {
            sql = "SELECT " +
                    "idfd.document_id AS dcn_id, " +
                    "idfd.request_txn_id AS si_case_id, " +
                    "idfd.document_type, " +
                    "idfd.download_completed_on " +
                    "FROM inbound_config.ingestion_downloaded_file_details AS idfd " +
                    "JOIN info.source_of_origin AS soo ON soo.transaction_id = idfd.transaction_id " +
                    "JOIN info.asset AS a ON a.asset_id = soo.asset_id AND a.file_name = idfd.file_name " +
                    "JOIN alchemy_response.pipeline_error_details AS ped ON ped.origin_id = soo.origin_id AND ped.transaction_id = soo.transaction_id " +
                    "JOIN product_outbound.product_response_details AS prd ON soo.origin_id = prd.origin_id " +
                    "WHERE idfd.download_completed_on >= CAST(? AS DATE) " +
                    "AND idfd.download_completed_on < CAST(? AS DATE) + INTERVAL '1 day' " +
                    "AND idfd.status = 'FAILED' " +
                    "AND ped.status = 'COMPLETED'";
            params.add(date);
            params.add(date);
        }

        return jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> new RecoveredDocument(
                rs.getString("dcn_id"),
                rs.getString("si_case_id"),
                rs.getString("document_type"),
                rs.getString("download_completed_on")
        ));
    }

    public List<WaitingDocument> getDocumentsWaitingToProcess(String date, String startTime, String endTime) {
        String sql;
        List<Object> params = new ArrayList<>();
        
        if (startTime != null && endTime != null) {
            sql = "SELECT " +
                    "idfd.document_id AS dcn_id, " +
                    "idfd.request_txn_id AS si_case_id, " +
                    "idfd.download_completed_on, " +
                    "idfd.status, " +
                    "(idfd.download_completed_on + INTERVAL '1 hour' - NOW()) AS time_remaining_to_process " +
                    "FROM inbound_config.ingestion_downloaded_file_details AS idfd " +
                    "WHERE idfd.download_completed_on >= (CAST(? AS DATE) + CAST(? AS TIME)) " +
                    "AND idfd.download_completed_on < (CAST(? AS DATE) + CAST(? AS TIME)) " +
                    "AND idfd.status IN ('STAGED', 'IN_PROGRESS') " +
                    "ORDER BY time_remaining_to_process ASC";
            params.add(date);
            params.add(startTime);
            params.add(date);
            params.add(endTime);
        } else {
            sql = "SELECT " +
                    "idfd.document_id AS dcn_id, " +
                    "idfd.request_txn_id AS si_case_id, " +
                    "idfd.download_completed_on, " +
                    "idfd.status, " +
                    "(idfd.download_completed_on + INTERVAL '1 hour' - NOW()) AS time_remaining_to_process " +
                    "FROM inbound_config.ingestion_downloaded_file_details AS idfd " +
                    "WHERE idfd.download_completed_on >= CAST(? AS DATE) " +
                    "AND idfd.download_completed_on < CAST(? AS DATE) + INTERVAL '1 day' " +
                    "AND idfd.status IN ('STAGED', 'IN_PROGRESS') " +
                    "ORDER BY time_remaining_to_process ASC";
            params.add(date);
            params.add(date);
        }

        return jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> new WaitingDocument(
                rs.getString("dcn_id"),
                rs.getString("si_case_id"),
                rs.getString("download_completed_on"),
                rs.getString("status"),
                rs.getString("time_remaining_to_process")
        ));
    }

    public List<InboundFailedDocument> getInboundFailedStatus(String date, String startTime, String endTime) {
        String sql;
        List<Object> params = new ArrayList<>();
        
        if (startTime != null && endTime != null) {
            sql = "SELECT " +
                    "idf.download_completed_on, " +
                    "idfd.transaction_id, " +
                    "idf.document_id AS dcn_id, " +
                    "idf.request_txn_id AS si_case_id " +
                    "FROM inbound_config.ingestion_file_details idf " +
                    "LEFT JOIN inbound_config.ingestion_downloaded_file_details idfd ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                    "WHERE idf.download_completed_on >= (CAST(? AS DATE) + CAST(? AS TIME)) " +
                    "AND idf.download_completed_on < (CAST(? AS DATE) + CAST(? AS TIME)) " +
                    "AND idfd.status = 'FAILED' " +
                    "ORDER BY idfd.id ASC";
            params.add(date);
            params.add(startTime);
            params.add(date);
            params.add(endTime);
        } else {
            sql = "SELECT " +
                    "idf.download_completed_on, " +
                    "idfd.transaction_id, " +
                    "idf.document_id AS dcn_id, " +
                    "idf.request_txn_id AS si_case_id " +
                    "FROM inbound_config.ingestion_file_details idf " +
                    "LEFT JOIN inbound_config.ingestion_downloaded_file_details idfd ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                    "WHERE idf.download_completed_on >= CAST(? AS DATE) " +
                    "AND idf.download_completed_on < CAST(? AS DATE) + INTERVAL '1 day' " +
                    "AND idfd.status = 'FAILED' " +
                    "ORDER BY idfd.id ASC";
            params.add(date);
            params.add(date);
        }

        return jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> new InboundFailedDocument(
                rs.getString("download_completed_on"),
                rs.getString("transaction_id"),
                rs.getString("dcn_id"),
                rs.getString("si_case_id")
        ));
    }

    public List<ProcessFailedDocument> getProcessFailedStatus(String date, String startTime, String endTime) {
        String sql;
        List<Object> params = new ArrayList<>();
        
        if (startTime != null && endTime != null) {
            sql = "SELECT DISTINCT ON (idf.document_id, idf.request_txn_id, soo.root_pipeline_id, hea.action_id) " +
                    "idf.download_completed_on, " +
                    "idf.document_id AS dcn_id, " +
                    "idf.request_txn_id AS si_case_id, " +
                    "ped.status, " +
                    "ped.error_code, " +
                    "ped.error_message, " +
                    "soo.root_pipeline_id, " +
                    "hea.action_id, " +
                    "aea.log " +
                    "FROM inbound_config.ingestion_file_details AS idf " +
                    "JOIN inbound_config.ingestion_downloaded_file_details AS idfd ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                    "JOIN info.source_of_origin AS soo ON soo.transaction_id = idfd.transaction_id " +
                    "JOIN info.asset AS a ON a.asset_id = soo.asset_id AND idfd.file_name = a.file_name " +
                    "JOIN alchemy_response.pipeline_error_details AS ped ON ped.origin_id = soo.origin_id AND ped.transaction_id = soo.transaction_id " +
                    "JOIN product_outbound.product_response_details AS prd ON prd.origin_id = soo.origin_id " +
                    "LEFT JOIN audit.handyman_exception_audit AS hea ON hea.root_pipeline_id = soo.root_pipeline_id " +
                    "LEFT JOIN audit.action_execution_audit AS aea ON aea.action_id = hea.action_id " +
                    "WHERE idf.download_completed_on >= (CAST(? AS DATE) + CAST(? AS TIME)) " +
                    "AND idf.download_completed_on < (CAST(? AS DATE) + CAST(? AS TIME)) " +
                    "AND ped.status = 'FAILED' " +
                    "ORDER BY idf.document_id, idf.request_txn_id, soo.root_pipeline_id, hea.action_id, idf.download_completed_on DESC";
            params.add(date);
            params.add(startTime);
            params.add(date);
            params.add(endTime);
        } else {
            sql = "SELECT DISTINCT ON (idf.document_id, idf.request_txn_id, soo.root_pipeline_id, hea.action_id) " +
                    "idf.download_completed_on, " +
                    "idf.document_id AS dcn_id, " +
                    "idf.request_txn_id AS si_case_id, " +
                    "ped.status, " +
                    "ped.error_code, " +
                    "ped.error_message, " +
                    "soo.root_pipeline_id, " +
                    "hea.action_id, " +
                    "aea.log " +
                    "FROM inbound_config.ingestion_file_details AS idf " +
                    "JOIN inbound_config.ingestion_downloaded_file_details AS idfd ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                    "JOIN info.source_of_origin AS soo ON soo.transaction_id = idfd.transaction_id " +
                    "JOIN info.asset AS a ON a.asset_id = soo.asset_id AND idfd.file_name = a.file_name " +
                    "JOIN alchemy_response.pipeline_error_details AS ped ON ped.origin_id = soo.origin_id AND ped.transaction_id = soo.transaction_id " +
                    "JOIN product_outbound.product_response_details AS prd ON prd.origin_id = soo.origin_id " +
                    "LEFT JOIN audit.handyman_exception_audit AS hea ON hea.root_pipeline_id = soo.root_pipeline_id " +
                    "LEFT JOIN audit.action_execution_audit AS aea ON aea.action_id = hea.action_id " +
                    "WHERE idf.download_completed_on >= CAST(? AS DATE) " +
                    "AND idf.download_completed_on < CAST(? AS DATE) + INTERVAL '1 day' " +
                    "AND ped.status = 'FAILED' " +
                    "ORDER BY idf.document_id, idf.request_txn_id, soo.root_pipeline_id, hea.action_id, idf.download_completed_on DESC";
            params.add(date);
            params.add(date);
        }

        return jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> new ProcessFailedDocument(
                rs.getString("download_completed_on"),
                rs.getString("dcn_id"),
                rs.getString("si_case_id"),
                rs.getString("status"),
                rs.getString("error_code"),
                rs.getString("error_message"),
                rs.getString("root_pipeline_id"),
                rs.getString("action_id"),
                rs.getString("log")
        ));
    }

    public List<AbortedDocument> getAbortedStatus(String date, String startTime, String endTime) {
        String sql;
        List<Object> params = new ArrayList<>();
        
        if (startTime != null && endTime != null) {
            sql = "SELECT " +
                    "idf.download_completed_on, " +
                    "idf.document_id AS dcn_id, " +
                    "idf.request_txn_id AS si_case_id, " +
                    "ped.status, " +
                    "ped.error_code, " +
                    "ped.error_message " +
                    "FROM inbound_config.ingestion_file_details AS idf " +
                    "JOIN inbound_config.ingestion_downloaded_file_details AS idfd ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                    "JOIN info.source_of_origin AS soo ON soo.transaction_id = idfd.transaction_id " +
                    "JOIN info.asset AS a ON a.asset_id = soo.asset_id AND idfd.file_name = a.file_name " +
                    "JOIN alchemy_response.pipeline_error_details AS ped ON ped.origin_id = soo.origin_id AND ped.transaction_id = soo.transaction_id " +
                    "JOIN product_outbound.product_response_details AS prd ON prd.origin_id = soo.origin_id " +
                    "WHERE idf.download_completed_on >= (CAST(? AS DATE) + CAST(? AS TIME)) " +
                    "AND idf.download_completed_on < (CAST(? AS DATE) + CAST(? AS TIME)) " +
                    "AND ped.status = 'ABORTED'";
            params.add(date);
            params.add(startTime);
            params.add(date);
            params.add(endTime);
        } else {
            sql = "SELECT " +
                    "idf.download_completed_on, " +
                    "idf.document_id AS dcn_id, " +
                    "idf.request_txn_id AS si_case_id, " +
                    "ped.status, " +
                    "ped.error_code, " +
                    "ped.error_message " +
                    "FROM inbound_config.ingestion_file_details AS idf " +
                    "JOIN inbound_config.ingestion_downloaded_file_details AS idfd ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                    "JOIN info.source_of_origin AS soo ON soo.transaction_id = idfd.transaction_id " +
                    "JOIN info.asset AS a ON a.asset_id = soo.asset_id AND idfd.file_name = a.file_name " +
                    "JOIN alchemy_response.pipeline_error_details AS ped ON ped.origin_id = soo.origin_id AND ped.transaction_id = soo.transaction_id " +
                    "JOIN product_outbound.product_response_details AS prd ON prd.origin_id = soo.origin_id " +
                    "WHERE idf.download_completed_on >= CAST(? AS DATE) " +
                    "AND idf.download_completed_on < CAST(? AS DATE) + INTERVAL '1 day' " +
                    "AND ped.status = 'ABORTED'";
            params.add(date);
            params.add(date);
        }

        return jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> new AbortedDocument(
                rs.getString("download_completed_on"),
                rs.getString("dcn_id"),
                rs.getString("si_case_id"),
                rs.getString("status"),
                rs.getString("error_code"),
                rs.getString("error_message")
        ));
    }
}
