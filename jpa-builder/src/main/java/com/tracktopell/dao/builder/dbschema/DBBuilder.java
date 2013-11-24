/*
 * DBBuilder.java
 *
 */

package com.tracktopell.dao.builder.dbschema;

import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.metadata.Table;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.List;

/**
 *
 * @author tracktopell
 */
public abstract class DBBuilder {
    
    /** Creates a new instance of DBBuilder */
    protected DBBuilder() {
    }
    
    public void createDBSchema(String schemaName, DBTableSet dbSet,PrintStream out) {        
        List<Table> lt=dbSet.getTablesSortedForCreation();
        
        out.println("-- ============================= CREACION DEL ESQUEMA DE LA BASE DE DATOS ====================");        
        printDefinitionSchema(schemaName,dbSet,out);
        out.println("-- ============================= TABLES ("+lt.size()+") =======================");
        
        for(Table t: lt) {            
            printDefinitionTable(t,out);                     
        }
        
        out.println("-- =================================== CONSTRAINTS ==============================");
        
        for(Table t: lt) {
            printAddPKContraints(t,out);            
        }
        
        for(Table t: lt) {
            printAddFKContraints(t,out);            
        }
        
        for(Table t: lt) {
            printAddIndexes(t,out);            
        }
    }
    
    protected abstract void printDefinitionSchema(String schemaName,DBTableSet dbSet,PrintStream out) ;

    protected abstract void printDefinitionTable(Table currentTable, PrintStream out) ;
    
    protected abstract void printAddPKContraints(Table currentTable, PrintStream out) ;    

    protected abstract void printAddFKContraints(Table currentTable, PrintStream out) ;

    protected abstract void printAddIndexes(Table currentTable, PrintStream out);
}
