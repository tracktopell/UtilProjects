/*
 * DBBuilderFactory.java
 *
 */

package com.tracktopell.dao.builder;

import com.tracktopell.dao.builder.dbschema.DBBuilder;
import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.parser.VP6Parser;
import com.tracktopell.dao.builder.parser.VPModel;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Hashtable;

/**
 *
 * @author tracktopell
 */
public class DBBuilderFactory {
    
    private DBBuilderFactory() {
    }
    
    public static DBBuilder getInstance(String classForName)
            throws InstantiationException, ClassNotFoundException, IllegalAccessException {
        Class c = DBBuilderFactory.class.forName(classForName);
        return (DBBuilder )c.newInstance();
    }
    
}
