package com.intics.metrics.dto.database;

import java.util.List;

public class TableMetadataDTO {
    private String tableName;
    private String schemaName;
    private List<ColumnMetadata> columns;
    private Long rowCount;

    public TableMetadataDTO() {}

    public TableMetadataDTO(String tableName, String schemaName, List<ColumnMetadata> columns, Long rowCount) {
        this.tableName = tableName;
        this.schemaName = schemaName;
        this.columns = columns;
        this.rowCount = rowCount;
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

    public List<ColumnMetadata> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnMetadata> columns) {
        this.columns = columns;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }

    public static class ColumnMetadata {
        private String columnName;
        private String dataType;
        private Integer columnSize;
        private boolean nullable;
        private boolean primaryKey;

        public ColumnMetadata() {}

        public ColumnMetadata(String columnName, String dataType, Integer columnSize, boolean nullable, boolean primaryKey) {
            this.columnName = columnName;
            this.dataType = dataType;
            this.columnSize = columnSize;
            this.nullable = nullable;
            this.primaryKey = primaryKey;
        }

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public Integer getColumnSize() {
            return columnSize;
        }

        public void setColumnSize(Integer columnSize) {
            this.columnSize = columnSize;
        }

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        public void setPrimaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
        }
    }
}
