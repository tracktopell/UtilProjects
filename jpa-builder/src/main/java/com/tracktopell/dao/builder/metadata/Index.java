/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tracktopell.dao.builder.metadata;

/**
 *
 * @author aegonzalez
 */
public class Index {
    private String name;
    private String columnName;
    private String directionOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDirectionOrder() {
        return directionOrder;
    }

    public void setDirectionOrder(String directionOrder) {
        this.directionOrder = directionOrder;
    }
    
    public String toString() {
        return "INDEX "+name+"("+columnName+" "+directionOrder+")";
    }
}
