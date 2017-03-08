/*
 * MySQLDBBuilder.java
 *
 */

package com.tracktopell.dao.builder.dbschema.mysql;

import com.tracktopell.dao.builder.dbschema.DBBuilder;
import com.tracktopell.dao.builder.metadata.Column;
import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.metadata.ReferenceTable;
import com.tracktopell.dao.builder.metadata.Table;
import java.io.PrintStream;
import java.util.Iterator;

/**
 *
 * @author tracktopell
 */
public class MySQLDBBuilder extends DBBuilder{
    
    public MySQLDBBuilder() {
    }

    protected void printDefinitionSchema(String schemaName,DBTableSet dbSet,PrintStream out) {
		out.println("-- SCHEMMA COMPATIBLE WITH  MySQL SERVER 5.7.x+ ");
        out.println("DROP DATABASE IF EXISTS ${schemaName};".replace("${schemaName}",schemaName.toLowerCase()));
        out.println("CREATE DATABASE ${schemaName};".replace("${schemaName}",schemaName.toLowerCase()));
        out.println("USE ${schemaName};".replace("${schemaName}",schemaName.toLowerCase()));        
    }

    protected void printDefinitionTable(Table currentTable, PrintStream out) {
        Iterator<Column> it = currentTable.getSortedColumns();
        Column col = null;
        StringBuffer pkBuffer = new StringBuffer("PRIMARY KEY (");
        int pkConter=0;
        out.println("CREATE TABLE ${table.name}".replace("${table.name}",currentTable.getName().toUpperCase())+" (");
        while(it.hasNext()) {
            col = it.next();
            out.print("    ");
            out.print(col.getName().toUpperCase());
            out.print(" ");
            if( col.getSqlType().toUpperCase().equals("BLOB")) {
                out.print("MEDIUMBLOB");
            } else {
                out.print(col.getSqlType().toUpperCase());
            }
            out.print(" ");
            if(col.getSqlType().equals("integer") || 
                    col.getSqlType().equals("varchar") || 
                    col.getSqlType().equals("decimal")){
                out.print(" (");
                out.print(col.getScale());
                if(col.getSqlType().equals("decimal")){
                    out.print(",");            
                    out.print(col.getPrecision());
                }
                out.print(")");                
            }
            out.print(" ");
            if(!col.isNullable()) {
				if(col.getSqlType().equalsIgnoreCase("timestamp")){
					out.print(" NOT NULL DEFAULT CURRENT_TIMESTAMP");
				} else{
					out.print(" NOT NULL");
				}
            } else if(col.isNullable()) {
				if(col.getSqlType().equalsIgnoreCase("timestamp")){
					out.print(" NULL DEFAULT NULL");
				} else{
					
				}                
            }
            if(col.isAutoIncremment()) {
                out.print(" AUTO_INCREMENT");
            }
            out.println(",");                      
            
            if(col.isPrimaryKey()) {
                pkConter++;
                if(pkConter>1)
                    pkBuffer.append(", ");
                pkBuffer.append("");                
                pkBuffer.append(col.getName().toUpperCase());                
                pkBuffer.append("");
            }
        }
        pkBuffer.append(")");
        
        out.print("    ");
        out.println(pkBuffer);        
        out.println(")ENGINE=InnoDB;");        
        out.println("");
        out.println("-- ===============================================================================");
    }
    /**
     * prints the alter talble for add constraints
     */
    protected void printAddPKContraints(Table currentTable, PrintStream out) {
    }
    /**
     * prints the alter talble for add constraints
     */
    protected void printAddFKContraints(Table currentTable, PrintStream out) {
        Iterator<Column> it = currentTable.getSortedColumns();
        Column col = null;
        while(it.hasNext()) {
            col = it.next();
            
            if(col.isForeignKey()) {                
                ReferenceTable rt = currentTable.getFKReferenceTable(col.getName());
                
                out.print("ALTER TABLE ");
                out.print(currentTable.getName().toUpperCase());
                out.print(" ADD CONSTRAINT FOREIGN KEY (");
                out.print(col.getName().toUpperCase());                
                out.print(")    REFERENCES ");
                out.print(rt.getTableName().toUpperCase());
                out.print("(");
                out.print(rt.getColumnName().toUpperCase());
                out.println(");");                                
            }
        }        
    }

    @Override
    protected void printAddIndexes(Table currentTable, PrintStream out) {
        
    }
}
