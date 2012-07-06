/*
 * OracleDBBuilder.java
 *
 */

package com.tracktopell.dao.builder.dbschema.oracle;

import com.tracktopell.dao.builder.dbschema.DBBuilder;
import com.tracktopell.dao.builder.metadata.Column;
import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.metadata.Index;
import com.tracktopell.dao.builder.metadata.ReferenceTable;
import com.tracktopell.dao.builder.metadata.Table;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;

/**
 *
 * @author tracktopell
 */
public class OracleDBBuilder extends DBBuilder{
    
    public OracleDBBuilder() {
    }

    protected void printDefinitionSchema(String schemaName,DBTableSet dbSet,PrintStream out) {        
    }

    protected void printDefinitionTable(Table currentTable, PrintStream out) {
        Iterator<Column> it = currentTable.getSortedColumns();
        Column col = null;
        
        out.println("CREATE TABLE \"${table.name}\"".replace("${table.name}",currentTable.getName().toUpperCase())+" (");
        for(int counter=0;it.hasNext();counter++) {
            col = it.next();
            if(counter>0){
                out.println(" ,");
            }
            out.print("\t\"");
            out.print(col.getName().toUpperCase());
            out.print("\"\t\t");
            if(col.getSqlType().toLowerCase().equals("varchar")) {
                out.print("VARCHAR2");
            } else if(col.getSqlType().toLowerCase().equals("double") ||
                    col.getSqlType().toLowerCase().equals("float")) {
                out.print("NUMBER");
            } else if(col.getSqlType().toLowerCase().startsWith("int") ||
                    col.getSqlType().toLowerCase().startsWith("tinyint")) {
                out.print("NUMBER");
            } else if(col.getSqlType().toLowerCase().startsWith("datetime") ||
                    col.getSqlType().toLowerCase().startsWith("timestamp")) {
                out.print("DATE");
            } else {
                out.print(col.getSqlType().toUpperCase());
            }
            
            out.print("\t");
            if(col.getSqlType().toLowerCase().startsWith("varchar") || 
                    col.getSqlType().toLowerCase().startsWith("int")){
                out.print(" (");                
                out.print(col.getPrecision());
                out.print(")");                
            } else if(col.getSqlType().toLowerCase().startsWith("decimal") ||
                    col.getSqlType().toLowerCase().startsWith("double")  ||
                    col.getSqlType().toLowerCase().startsWith("float")   ){
                out.print(" (");
                out.print(col.getScale());
                if(col.getSqlType().toLowerCase().startsWith("decimal") ||
                    col.getSqlType().toLowerCase().startsWith("double")  ||
                    col.getSqlType().toLowerCase().startsWith("float")  ){
                    out.print(",");            
                    out.print(col.getPrecision());    
                }
                out.print(")");                
            }
            
            out.print("\t");
            if(!col.isNullable()) {
                out.print(" NOT NULL");
            }            
        }
        out.println("");
        out.println(");");        
        out.println("");
        it = currentTable.getSortedColumns();
        
        for(int counter=0;it.hasNext();counter++) {
            col = it.next();
            if(! col.isAutoIncremment()){
                continue;
            }
            String seqName = "SQ_"+currentTable.getName().toUpperCase();
            if(seqName.length()>=28) {
                seqName = seqName.substring(0, 28)+col.getPosition();
            }
            String nines = "9999999999999999999999999999999999999999999999999999999999999999999999";
            
            out.println("CREATE SEQUENCE "+seqName+" INCREMENT BY 1 START WITH 1 MINVALUE 1 "+
                    " MAXVALUE "+nines.substring(0,col.getPrecision())+
                    " NOCACHE;");
        }
        out.println("-- ===============================================================================");
    }
    
    /**
     * prints the alter talble for add constraints
     */
    protected void printAddPKContraints(Table currentTable, PrintStream out) {
        Iterator<Column> it = currentTable.getSortedColumns();
        Column col = null;
        StringBuilder sbPK = new StringBuilder ();
        int counterPK =0;
        ReferenceTable rt = null;
        while(it.hasNext()) {
            col = it.next();
            
            if(col.isPrimaryKey()) {                
                if(counterPK>0) {
                    sbPK.append(", ");
                }
                
                rt = currentTable.getFKReferenceTable(col.getName());
                sbPK.append("\"");
                sbPK.append(col.getName().toUpperCase());                
                sbPK.append("\"");
                counterPK++;
            }
        }
        
        if(counterPK > 0) {
            String pkName = "PK_"+currentTable.getName().toUpperCase();
            if(pkName.length()>25){
                pkName = pkName.substring(0, 25);
            }
            pkName = pkName + String.valueOf(col.getPosition());
            
            out.print("ALTER TABLE \"");
            out.print(currentTable.getName().toUpperCase());
            out.print("\" ADD CONSTRAINT ");
            out.print("\""+pkName+"\"");
            out.print(" PRIMARY KEY (");            
            out.print(sbPK.toString());                
            out.println(" );");
        }
        out.println("-- ===============================================================================");        
    }
    
    
    /**
     * prints the alter talble for add constraints
     */
    protected void printAddFKContraints(Table currentTable, PrintStream out) {
        Iterator<Column> it = currentTable.getSortedColumns();
        Column col = null;
        ReferenceTable rt = null;
        
        it = currentTable.getSortedColumns();
        int counterFK = 0;
        while(it.hasNext()) {
            col = it.next();
            
            if(col.isForeignKey()) {                
                rt = currentTable.getFKReferenceTable(col.getName());
                counterFK++;
                
                String fkName = "FK_"+currentTable.getName().toUpperCase();
                if(fkName.length()>25){
                    fkName = fkName.substring(0, 25);
                }
                fkName = fkName + String.valueOf(col.getPosition());
                
                out.print("ALTER TABLE \"");
                out.print(currentTable.getName().toUpperCase());
                out.print("\" ADD CONSTRAINT ");
                out.print("\""+fkName+"\"");
                out.print(" FOREIGN KEY (\"");
                out.print(col.getName().toUpperCase());                
                out.print("\")\tREFERENCES \"");
                out.print(rt.getTableName().toUpperCase());
                out.print("\"(\"");
                out.print(rt.getColumnName().toUpperCase());
                out.println("\");");                                
            }
        }
        out.println("-- ===============================================================================");
    }

    protected void printAddIndexes(Table currentTable, PrintStream out) {
        Iterator<Index> it = currentTable.getIndexes();
        int ni = 1;
        Hashtable<String, Boolean> indexedColumn = new Hashtable<String, Boolean>();
        while(it.hasNext()) {
            Index currIndex = it.next();
            if(indexedColumn.get(currIndex.getColumnName())==null) {
                indexedColumn.put(currIndex.getColumnName(),true);
                
                String indexName = currentTable.getName().toUpperCase();
                if(indexName.length() > 26) {
                    indexName = indexName.substring(0, 26);
                }
                indexName = indexName + "_" + (currentTable.getColumn(currIndex.getColumnName()).isPrimaryKey()?"I":"C");
                indexName = indexName + (ni++);

                out.print("CREATE INDEX \"");
                out.print(indexName);
                out.print("\" ON \"");
                out.print(currentTable.getName().toUpperCase());
                out.print("\" (\"");
                out.print(currIndex.getColumnName().toUpperCase());                
                out.print("\" ");
                out.print(currIndex.getDirectionOrder().equalsIgnoreCase("A")?"ASC":"DESC");
                out.println(");");
            }
        }
        //out.println("-- CREATED "+indexedColumn.size()+" INDEXES");
    }
}
