/*
 * SmartColumnNameRelation.java
 *
 */

package com.tracktopell.dao.builder.metadata;

import java.util.Enumeration;

/**
 *
 * @author Tracktopell
 */
public class SmartColumnNameRelation {
    private boolean ignoreCase;    
    private String joinColumnName;
    private String targetColumnName;            
    /** Creates a new instance of SmartColumnNameRelation */
    public SmartColumnNameRelation(String joinColumnName,String targetColumnName,boolean ignoreCase,boolean replace) {
        this.joinColumnName   = joinColumnName;
        this.targetColumnName = targetColumnName;
        this.ignoreCase       = ignoreCase;        
    }

    public String getJoinColumnName() {
        return joinColumnName;
    }

    public void setJoinColumnName(String joinColumnName) {
        this.joinColumnName = joinColumnName;
    }

    public String getTargetColumnName() {
        return targetColumnName;
    }

    public void setTargetColumnName(String targetColumnName) {
        this.targetColumnName = targetColumnName;
    }
    
    public Column match(Table table){
        Column targetColumn = null;
        Enumeration<String> ec=table.getColumNames();
        Column col=null;
        boolean matchJoinNamePattern = false;
        while(ec.hasMoreElements()){            
            col=table.getColumn(ec.nextElement());
            if( ignoreCase ){
                if(col.getName().toUpperCase().matches(this.joinColumnName.toUpperCase())){
                    matchJoinNamePattern = true;
                }
                if(col.getName().toUpperCase().matches(this.targetColumnName.toUpperCase())){
                    targetColumn = col;
                }
            }
            else{
                if(col.getName().matches(this.joinColumnName)){
                    matchJoinNamePattern = true;
                }
                if(col.getName().matches(this.targetColumnName)){
                    targetColumn = col;
                }
            }
        }
        if(targetColumn!=null && matchJoinNamePattern)
            return targetColumn;
        
        return null;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }
}