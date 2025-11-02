package com.intics.metrics.dto.database;

import java.util.List;
import java.util.Map;

public class TableDataDTO {
    private String tableName;
    private String schemaName;
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private int totalRows;
    private int pageSize;
    private int currentPage;

    public TableDataDTO() {}

    public TableDataDTO(String tableName, String schemaName, List<String> columns, 
                       List<Map<String, Object>> rows, int totalRows, int pageSize, int currentPage) {
        this.tableName = tableName;
        this.schemaName = schemaName;
        this.columns = columns;
        this.rows = rows;
        this.totalRows = totalRows;
        this.pageSize = pageSize;
        this.currentPage = currentPage;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
}
