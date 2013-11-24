/*
 * Table.java
 */

package com.tracktopell.dao.builder.metadata;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 *
 * @author Usuario
 */
public class EmbeddeableColumn extends Table implements Column{
    private String sqlType;
    private String javaClassType;
    private boolean autoIncremment;
    private boolean primaryKey;
    private boolean foreignKey;
    private boolean nullable;
    private boolean foreignDescription;
    private boolean toStringConcatenable;
    private int precision;
    private int scale;
    private int position;
    private int typeFormatingNumber;
    private String comments;
    private String farFKDescription;


    /** Creates a new instance of Table */
    public EmbeddeableColumn() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }

    public String getJavaClassType() {
        return javaClassType;
    }

    public void setJavaClassType(String javaClassType) {
        this.javaClassType = javaClassType;
    }

    public boolean isAutoIncremment() {
        return autoIncremment;
    }

    public void setAutoIncremment(boolean autoIncremment) {
        this.autoIncremment = autoIncremment;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public boolean isForeignKey() {
        return foreignKey;
    }

    public void setForeignKey(boolean foreignKey) {
        this.foreignKey = foreignKey;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getTypeFormatingNumber() {
        return typeFormatingNumber;
    }

    public void setTypeFormatingNumber(int typeFormatingNumber) {
        this.typeFormatingNumber = typeFormatingNumber;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isForeignDescription() {
        return foreignDescription;
    }

    public void setForeignDescription(boolean foreignDescription) {
        this.foreignDescription = foreignDescription;
    }

    public void buildPosibleLabel() {
        String[] nameParts = name.split("_");
        StringBuffer sb = new StringBuffer();
        for(String sn : nameParts) {
            sb.append(sn.substring(0, 1).toUpperCase());
            sb.append(sn.substring(1).toLowerCase());
            sb.append(" ");
        }
        label = sb.toString().trim();
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getFarFKDescription() {
        return farFKDescription;
    }

    public void setFarFKDescription(String farFKDescription) {
        this.farFKDescription = farFKDescription;
    }


    //--------------------------------------------------------------------------

    public Iterator<Column> getSortedColumnsForJPA() {
        return getSortedColumns();
    }

    public void fixBestJavaClassForSQLType() {
        throw new IllegalStateException("This methos it's not to retrieve the JAva Class here, because is not Primitive Column");
    }

	@Override
	public boolean isIntegerJavaType() {
		return	javaClassType.equals("int") || javaClassType.equals("java.lang.Integer") ||
				javaClassType.equals("long") || javaClassType.equals("java.lang.Long");
	}

    /**
     * @return the toStringConcatenable
     */
    public boolean isToStringConcatenable() {
        return toStringConcatenable;
    }

    /**
     * @param toStringConcatenable the toStringConcatenable to set
     */
    public void setToStringConcatenable(boolean toStringConcatenable) {
        this.toStringConcatenable = toStringConcatenable;
    }
}
