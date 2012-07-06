/*
 * VPModel.java
 *
 */

package com.tracktopell.dao.builder.parser;

/**
 *
 * @author tracktopell
 */
public class VPModel{
    private String id;
    private String modelType;
    private String name;
    private String documentation;
    public String toString(){
        return "VPModel{id="+getId()+";modelType="+getModelType()+"; name="+getName()+"; documentation=\""+getDocumentation()+"\";}\r\n";
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the modelType
     */
    public String getModelType() {
        return modelType;
    }

    /**
     * @param modelType the modelType to set
     */
    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the documentation
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * @param documentation the documentation to set
     */
    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}

