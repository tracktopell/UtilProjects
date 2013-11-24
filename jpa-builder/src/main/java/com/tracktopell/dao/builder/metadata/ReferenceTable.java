/*
 * ReferenceTable.java
 */

package com.tracktopell.dao.builder.metadata;

/**
 *
 * @author Usuario
 */
public class ReferenceTable {
    private String tableName;
    private String columnName;
    /** Creates a new instance of ReferenceTable */
    public ReferenceTable() {
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    public String toString() {
        return "ReferenceTable{ tableName='"+tableName+"', columnName='"+columnName+"'}";
    }
}
