package com.intics.metrics.dto.database;

import java.util.List;

public class SchemaDTO {
    private String schemaName;
    private List<String> tables;

    public SchemaDTO() {}

    public SchemaDTO(String schemaName, List<String> tables) {
        this.schemaName = schemaName;
        this.tables = tables;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }
}
