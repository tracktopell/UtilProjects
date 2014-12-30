/**
 * Archivo: SQLTypesToJavaTypes.java
 * 
 * Fecha de Creaci&oacute;n: 1/06/2011
 *
 * 2H Software - Bursatec 2011
 */
package com.tracktopell.dao.builder.metadata;

import java.util.Hashtable;

/**
 * 
 * @author Alfredo Estrada Gonz&aacute;lez.
 * @version 1.0
 *
 */
public class SQLTypesToJavaTypes {

    private static Hashtable<String, String> javaTypes;

    static {        
        javaTypes = new Hashtable<String, String>();

        javaTypes.put("numeric", Double.class.toString().replace("class ", ""));
        javaTypes.put("decimal", Double.class.toString().replace("class ", ""));
        javaTypes.put("double", Double.class.toString().replace("class ", ""));
        javaTypes.put("integer", Integer.class.toString().replace("class ", ""));
        javaTypes.put("int", Integer.class.toString().replace("class ", ""));
        javaTypes.put("float", Float.class.toString().replace("class ", ""));
        javaTypes.put("long", Long.class.toString().replace("class ", ""));
        
        javaTypes.put("numeric_not_null", "double");
        javaTypes.put("decimal_not_null", "double");
        javaTypes.put("double_not_null", "double");
        javaTypes.put("integer_not_null", "int");
        javaTypes.put("int_not_null", "int");
        javaTypes.put("float_not_null", "float");
        javaTypes.put("long_not_null", "long");
        
        javaTypes.put("varchar", String.class.toString().replace("class ", ""));
        javaTypes.put("date", java.sql.Date.class.toString().replace("class ", ""));
        javaTypes.put("datetime", java.sql.Date.class.toString().replace("class ", ""));
		javaTypes.put("timestamp", java.sql.Timestamp.class.toString().replace("class ", ""));
        javaTypes.put("blob", "byte[]");

    }
    
    public static String getTypeFor(String sqlType){
        return javaTypes.get(sqlType.toLowerCase());        
    }
    
    public static boolean isBindableDoubleSQLType(String sqlType) {
        return  sqlType.startsWith("numeric") ||
                sqlType.startsWith("decimal") ;
                
    }
}
