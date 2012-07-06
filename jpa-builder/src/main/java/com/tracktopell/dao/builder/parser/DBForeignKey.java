/*
 * DBForeignKey.java
 *
 */

package com.tracktopell.dao.builder.parser;

/**
 *
 * @author tracktopell
 */
class DBForeignKey extends VPModel{
    private String from;
    private String to;

    DBForeignKey() {
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
    
    public String toString() {
        return "DBForeignKey{id="+getId()+"; from="+from+"; to="+to+"; }\r\n";
    }
}