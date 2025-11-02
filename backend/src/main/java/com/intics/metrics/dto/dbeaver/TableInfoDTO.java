package com.intics.metrics.dto.dbeaver;

import java.util.List;

public class TableInfoDTO {
    private String tableName;
    private String tableType;
    private Long rowCount;
    private List<ColumnInfoDTO> columns;

    public TableInfoDTO() {}

    public TableInfoDTO(String tableName, String tableType, Long rowCount, List<ColumnInfoDTO> columns) {
        this.tableName = tableName;
        this.tableType = tableType;
        this.rowCount = rowCount;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }

    public List<ColumnInfoDTO> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnInfoDTO> columns) {
        this.columns = columns;
    }
}
