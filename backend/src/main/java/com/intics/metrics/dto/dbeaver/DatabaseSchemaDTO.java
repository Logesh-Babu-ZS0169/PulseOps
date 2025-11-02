package com.intics.metrics.dto.dbeaver;

import java.util.List;

public class DatabaseSchemaDTO {
    private String schemaName;
    private List<TableInfoDTO> tables;

    public DatabaseSchemaDTO() {}

    public DatabaseSchemaDTO(String schemaName, List<TableInfoDTO> tables) {
        this.schemaName = schemaName;
        this.tables = tables;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public List<TableInfoDTO> getTables() {
        return tables;
    }

    public void setTables(List<TableInfoDTO> tables) {
        this.tables = tables;
    }
}
