/**
 * Builder.java
 */

package com.tracktopell.dao.builder;

import com.tracktopell.dao.builder.metadata.Column;
import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.metadata.Index;
import com.tracktopell.dao.builder.metadata.ReferenceTable;
import com.tracktopell.dao.builder.metadata.SimpleColumn;
import com.tracktopell.dao.builder.metadata.Table;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 *
 * @author Alfred
 */
public class Builder {
    public static boolean generateGetters=true;
    public static boolean generateSetters=true;
    public static boolean generateParseAndSetters=true;
    public static boolean generatePropertiesExtended4FK=true;
    
    private Builder() {
    }
    
    private static Hashtable getJDBCMapedTypes(){
        Hashtable mapedJavaClass = new Hashtable();
        
        Field[] f =java.sql.Types.class.getDeclaredFields();
        
        int i;        
        for(i=0;i<f.length;i++){
            try {
                mapedJavaClass.put(f[i].getName(),new Integer(f[i].getInt(java.sql.Types.class)));
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }    
        return mapedJavaClass;
    }
    
    public static void buildExtraDirs(String basePath){
        File    baseDir    = null;
        File    sourcesDir = null;
        File    classesDir = null;
        File    libDir     = null;
        File    webDir     = null;
        
        try {
            baseDir = new File(basePath);
            
            if(!baseDir.exists())
                baseDir.mkdirs();
            
            sourcesDir = new File(baseDir.getPath()+File.separator+"src");
            
            if(!sourcesDir.exists())
                sourcesDir.mkdirs();
            
            classesDir = new File(baseDir.getPath()+File.separator+"build"+File.separator+"classes");
            
            if(!classesDir.exists())
                classesDir.mkdirs();
            
            libDir     = new File(baseDir.getPath()+File.separator+"lib");
            
            if(!libDir.exists())
                libDir.mkdirs();
            
            webDir     = new File(baseDir.getPath()+File.separator+"web"+File.separator+"WEB-INF");
            
            if(!webDir.exists())
                webDir.mkdirs();
        
        } catch (Exception ex) {
            ex.printStackTrace();
        }    
    }
    
    public static void buildBeans(DBTableSet dbSet,String packageBeanMember,String basePath){        
        int i;
        
        String  fileName;        
        File    baseDir       = null;
        File    dirSourceFile = null;        
        File    sourceFile    = null;
        
        FileOutputStream fos = null;
        PrintStream      ps  = null;
        BufferedReader   br  = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        try {
            Enumeration<String> tableNames=dbSet.getTableNames();
            
            while(tableNames.hasMoreElements()){
                Table table = dbSet.getTable(tableNames.nextElement());
                //-------------------------------------------------------
                baseDir = new File(basePath);
            
                if(!baseDir.exists())
                    baseDir.mkdirs();

                fileName = packageBeanMember.replace(".",File.separator)+File.separator;
    
                dirSourceFile = new File(baseDir.getPath()+File.separator+"src"+File.separator+fileName);
                if(!dirSourceFile.exists())
                    dirSourceFile.mkdirs();
                
                fileName = dirSourceFile.getPath()+File.separator+FormatString.getCadenaHungara(table.getName())+".java";
    
                sourceFile = new File(fileName);                
                fos        = new FileOutputStream(sourceFile) ;
                ps         = new PrintStream (fos);
                
                br = new BufferedReader(new InputStreamReader(
                        fos.getClass().getResourceAsStream("/templates/TableBean.java.template"))); 
                String line=null;                
                ArrayList<String> linesToParse=null;
                int nl=0;
                while((line=br.readLine())!=null) {
                    
                    if(line.indexOf("@foreach")>=0) {
                        linesToParse=new ArrayList<String>();                        
                    } else if(line.indexOf("@endfor")>=0) {
                        Iterator<Column> columns = table.getSortedColumns();
                        Column column = null;
                        while(columns.hasNext()){
                            column = columns.next();
                            
                            for(String lineInLoop: linesToParse) {
                                if(lineInLoop.indexOf("${tablebean.member.javadocCommnet}")>=0){
                                    if(column.getComments()!=null) {
                                        ps.println("    ");
                                        ps.println("    /**");
                                        ps.println("    * "+column.getComments().replace("\n", "\n     * "));
                                        ps.println("    */");
                                    } else {
                                        String commentForced = column.getName().toLowerCase().replace("_"," ");
                                        ps.println("    ");
                                        ps.println("    /**");
                                        ps.println("    * "+commentForced);
                                        ps.println("    */");
                                    }           
                                } else if(lineInLoop.indexOf("${tablebean.member.declaration}")>=0){
                                    ps.println("    private "+column.getJavaClassType().replace("java.lang.","")+
                                            " "+FormatString.renameForJavaMethod(column.getName())+";");                                                                        
                                } else if(lineInLoop.indexOf("${tablebean.member.getter}")>=0 && generateGetters){
                                    ps.println("    public "+column.getJavaClassType().replace("java.lang.","")+
                                            " get"+FormatString.getCadenaHungara(column.getName())+"() {");
                                    ps.println("        return this."+FormatString.renameForJavaMethod(column.getName())+";");
                                    ps.println("    }");
                                } else if(lineInLoop.indexOf("${tablebean.member.setter}")>=0 && generateSetters){
                                    ps.println("    public void set"+FormatString.getCadenaHungara(column.getName())+
                                            "("+column.getJavaClassType().replace("java.lang.","")+" "+FormatString.renameForJavaMethod(column.getName())+") {");
                                    ps.println("        this."+FormatString.renameForJavaMethod(column.getName())+" = "+
                                            FormatString.renameForJavaMethod(column.getName())+";");
                                    ps.println("    }");
                                } else if(lineInLoop.indexOf("${tablebean.member.parseAndSetter}")>=0 &&generateParseAndSetters){
                                    if(column.getJavaClassType().endsWith("String")){
                                        continue;
                                    }
                                    ps.println("    public void parseAndSet"+FormatString.getCadenaHungara(column.getName())+
                                            "(String "+FormatString.renameForJavaMethod(column.getName())+"Value) throws IllegalArgumentException {");

                                    if(column.isNullable()){
                                        ps.println("        if("+FormatString.renameForJavaMethod(column.getName())+"Value == null){");
                                        ps.println("            this."+FormatString.renameForJavaMethod(column.getName())+" = null;");
                                        ps.println("            return;");
                                        ps.println("        }");                        
                                    }
                                    ps.println("        try {");                        

                                    if(column.getJavaClassType().equals("java.math.BigDecimal")||
                                            column.getJavaClassType().equals("java.lang.Integer")||
                                            column.getJavaClassType().equals("java.lang.Short")||
                                            column.getJavaClassType().equals("java.lang.Long")||
                                            column.getJavaClassType().equals("java.lang.Double")||
                                            column.getJavaClassType().equals("java.lang.Float")){
                                        ps.print  ("            this."+FormatString.renameForJavaMethod(column.getName())+" = new ");
                                        ps.println(column.getJavaClassType().replace("java.lang.","")+"("+FormatString.renameForJavaMethod(column.getName())+"Value);");                                        
                                    } else if(column.getJavaClassType().equals("java.sql.Timestamp")||
                                            column.getJavaClassType().equals("java.sql.Time")){
                                        ps.println("            java.text.SimpleDateFormat sdf=new java.text.SimpleDateFormat(\"yyyy/MM/dd hh:mm\");");
                                        ps.println("            java.sql.Timestamp ts=new java.sql.Timestamp(sdf.parse("+FormatString.renameForJavaMethod(column.getName())+"Value).getTime());");
                                        ps.println("            this."+FormatString.renameForJavaMethod(column.getName())+" = ts;");                        
                                    } else if(column.getJavaClassType().equals("java.sql.Date")){
                                        ps.println("            java.text.SimpleDateFormat sdf=new java.text.SimpleDateFormat(\"yyyy/MM/dd\");");
                                        ps.println("            java.sql.Date dd=new java.sql.Date(sdf.parse("+FormatString.renameForJavaMethod(column.getName())+"Value).getTime());");
                                        ps.println("            this."+FormatString.renameForJavaMethod(column.getName())+" = dd;");                        
                                    } else {
                                        ps.println("            throw new Exception(\"this column can't parse from String to "+column.getJavaClassType().replace("java.lang.","")+"\");//");
                                        System.err.println("-> In the Bean for "+table.getName()+": in the setter method to "+column.getName()+" can't parse from String to "+column.getJavaClassType().replace("java.lang.",""));
                                    }
                                    ps.println("        } catch (Exception ex) {");
                                    ps.println("            throw new IllegalArgumentException(ex.getMessage());");                    
                                    ps.println("        } ");                        
                                    ps.println("    }");
                                }
                                if(column.isForeignKey() && generatePropertiesExtended4FK) {
                                    Table fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
                                    
                                    if(lineInLoop.indexOf("${tablebean.member.declaration}")>=0){
                                        ps.println("    /** Maps the description of the column that's is PK in the foreign table ("+fTable.getName()+")");
                                        ps.println("    */");
                                        ps.println("    private String "+FormatString.firstLetterLowerCase(column.getFarFKDescription())+";");
                                    }
                                    else if(lineInLoop.indexOf("${tablebean.member.getter}")>=0 && generateGetters){
                                        ps.println("");
                                        ps.println("    /** Returns the description of the column that's is PK in the foreign table ("+fTable.getName()+")");
                                        ps.println("    */");
                                        ps.println("    public String get"+FormatString.renameForJavaMethod(column.getFarFKDescription())+"() {");
                                        ps.println("        return this."+FormatString.firstLetterLowerCase(column.getFarFKDescription())+";");
                                        ps.println("    }");
                                    } else if(lineInLoop.indexOf("${tablebean.member.setter}")>=0 && generateSetters){
                                        ps.println("");
                                        ps.println("    /** Set the description of the column that's is PK in the foreign table ("+fTable.getName()+")");
                                        ps.println("    */");
                                        ps.println("    public void set"+FormatString.renameForJavaMethod(column.getFarFKDescription())+"(String v) {");
                                        ps.println("        this."+FormatString.firstLetterLowerCase(column.getFarFKDescription())+" = v;");
                                        ps.println("    }");
                                    }
                                }
                            }
                        }
                        linesToParse=null;
                    } else if(linesToParse!=null){
                        linesToParse.add(line);
                    } else {
                        line = line.replace("${date}",sdf.format(new Date()));
                        line = line.replace("${tablebean.name}"   ,FormatString.getCadenaHungara(table.getName()));
                        line = line.replace("${tablebean.package}",packageBeanMember);
                        ps.println(line);
                    }
                }                
                //-------------------------------------------------------
                ps.close();                
                fos.close();
                
                sourceFile = null;
                ps         = null;
                fos        = null;
            }            
        } catch (Exception ex) {
            ex.printStackTrace();        
        }
    }
    
    public static String[] getSouportedRDBMS() {
        return new String[]{"Oracle","Informix","MySQL","SQLServer","JavaDB","Custom"};
    }
    
    public static boolean[] getAllSouportedRDBMS() {
        return new boolean[]{true,true,true,true,true};
    }
    
    public static void buildDAOFactory(String packageDAOMember,String basePath,
            DBTableSet dbSet,boolean[] buildForRDBMS){
        
        int i;
        
        String  fileName;        
        File    baseDir       = null;
        File    dirSourceFile = null;        
        File    sourceFile    = null;
        
        FileOutputStream fos  = null;
        PrintStream      ps   = null;
        BufferedReader   br   = null;
        SimpleDateFormat sdf  = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        String[]   rdbmsNames = getSouportedRDBMS();
        try {
            baseDir = new File(basePath);

            if(!baseDir.exists())
                baseDir.mkdirs();

            fileName = packageDAOMember.replace(".",File.separator)+File.separator;

            dirSourceFile = new File(baseDir.getPath()+File.separator+"src"+File.separator+fileName);
            if(!dirSourceFile.exists())
                dirSourceFile.mkdirs();

            fileName = dirSourceFile.getPath()+File.separator+"DAOFactory.java";

            sourceFile = new File(fileName);                
            fos        = new FileOutputStream(sourceFile) ;
            ps         = new PrintStream (fos);

            br = new BufferedReader(new InputStreamReader(
                    fos.getClass().getResourceAsStream("/templates/DAOFactory.java.template"))); 
            
            String line=null;            
            ArrayList<String> linesToParse=null;
            int nl=0;
            boolean rdbmsSouportedStart = false;
            boolean tablebeansStart     = false;
            
            while((line=br.readLine())!=null) {
                if(line.indexOf("@foreach")>=0) {
                    if(line.indexOf("${rdbmsSouported.names}")>=0) {
                        rdbmsSouportedStart = true;
                    } else if(line.indexOf("${tablebeans}")>=0) {
                        tablebeansStart = true;
                    }
                    
                    linesToParse=new ArrayList<String>();                        
                } else if(line.indexOf("@endfor")>=0) {
                    if(rdbmsSouportedStart){
                        rdbmsSouportedStart=false;
                        for(i=0;i<rdbmsNames.length;i++){
                            if(!buildForRDBMS[i]) {
                                continue;
                            }                        
                            for(String lineInLoop: linesToParse) {
                                lineInLoop = lineInLoop.replace("${rdbms.id.declaration}",
                                        "public static final int "+
                                        rdbmsNames[i].toUpperCase()+" = "+(i+1)+";");

                                lineInLoop = lineInLoop.replace("${rdbms.id}",
                                        rdbmsNames[i].toUpperCase());

                                lineInLoop = lineInLoop.replace("${rdbms.daoFactory.class}",
                                        packageDAOMember+"."+rdbmsNames[i].toLowerCase()+
                                        "."+rdbmsNames[i]+"DAOFactory");

                                ps.println(lineInLoop);                                                                                                    
                            }
                        }
                    } else if (tablebeansStart) {
                        tablebeansStart=false;
                        Enumeration<String> tableNames=dbSet.getTableNames();
            
                        while(tableNames.hasMoreElements()){
                            Table table = dbSet.getTable(tableNames.nextElement());

                            for(String lineInLoop: linesToParse) {
                                lineInLoop = lineInLoop.replace("${tablebean.name}"   ,
                                        FormatString.getCadenaHungara(table.getName()));
                                
                                ps.println(lineInLoop);                                                                                                    
                            }
                        }
                    }
                    linesToParse=null;
                } else if(linesToParse!=null){
                    linesToParse.add(line);
                } else {
                    line = line.replace("${date}",sdf.format(new Date()));
                    line = line.replace("${dao.package}",packageDAOMember);
                    ps.println(line);
                }
            }
            //-------------------------------------------------------
            ps.close();                
            fos.close();

            sourceFile = null;
            ps         = null;
            fos        = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void buildConcreteDAOFactoryes(String packageDAOMember,
            String basePath,String jndiDBName,DBTableSet dbSet,boolean[] buildForRDBMS){        
        String            fileName;        
        File    baseDir       = null;
        File    dirSourceFile = null;        
        File    sourceFile    = null;
        
        FileOutputStream fos = null;
        PrintStream      ps  = null;
        SimpleDateFormat sdf  = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        int i;
        try {
            String[] rdbmsNames=getSouportedRDBMS();
            for(i=0;i<rdbmsNames.length;i++){
                if(!buildForRDBMS[i])
                    continue;
                //-------------------------------------------------------
                baseDir = new File(basePath);

                if(!baseDir.exists())
                    baseDir.mkdirs();

                fileName = packageDAOMember.replace(".",File.separator)+File.separator+rdbmsNames[i].toLowerCase()+File.separator;

                dirSourceFile = new File(baseDir.getPath()+File.separator+"src"+File.separator+fileName);
                if(!dirSourceFile.exists())
                    dirSourceFile.mkdirs();

                fileName = dirSourceFile.getPath()+File.separator+rdbmsNames[i]+"DAOFactory.java";

                sourceFile = new File(fileName);                
                fos        = new FileOutputStream(sourceFile) ;
                ps         = new PrintStream (fos);
                BufferedReader   br   = null;
                
                br = new BufferedReader(new InputStreamReader(fos.getClass().
                        getResourceAsStream("/templates/RDBMSDAOFactory.java.template"))); 
            
                String line=null;                
                ArrayList<String> linesToParse=null;
                int nl=0;
                boolean tablebeansStart     = false;

                while((line=br.readLine())!=null) {
                    if(line.indexOf("@foreach")>=0) {
                        if(line.indexOf("${tablebeans}")>=0) {
                            tablebeansStart = true;
                        }
                        linesToParse=new ArrayList<String>();                        
                    } else if(line.indexOf("@endfor")>=0) {
                        if (tablebeansStart) {
                            tablebeansStart=false;
                            Enumeration<String> tableNames=dbSet.getTableNames();

                            while(tableNames.hasMoreElements()){
                                Table table = dbSet.getTable(tableNames.nextElement());

                                for(String lineInLoop: linesToParse) {
                                    lineInLoop = lineInLoop.replace("${rdbms.name}",
                                            rdbmsNames[i]);
                                    lineInLoop = lineInLoop.replace("${tablebean.name}",
                                            FormatString.getCadenaHungara(table.getName()));

                                    ps.println(lineInLoop);                                                                                                    
                                }
                            }
                        }
                        linesToParse=null;
                    } else if(linesToParse!=null){
                        linesToParse.add(line);
                    } else {
                        line = line.replace("${date}",sdf.format(new Date()));
                        line = line.replace("${dao.package}",packageDAOMember);
                        line = line.replace("${rdbms.name}",rdbmsNames[i]);
                        line = line.replace("${rdbms.jndi.name}",jndiDBName);
                        line = line.replace("${rdbms.daoFactory.package}",packageDAOMember+
                                "."+rdbmsNames[i].toLowerCase());                        
                        line = line.replace("${rdbms.name}",rdbmsNames[i]);
                        ps.println(line);
                    }                
                }                
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void buildDAOExceptions(String packageDAOMember,String basePath){
        String            fileName;        
        File    baseDir       = null;
        File    dirSourceFile = null;        
        File    sourceFile    = null;
        SimpleDateFormat sdf  = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        FileOutputStream fos = null;
        PrintStream      ps  = null;
        try {
            String[] exceptionNames = new String[]{
                    "DataAccessException","GetDataException","SetDataException"
            };
            for(String exceptionName : exceptionNames ) {
                baseDir = new File(basePath);
                if(!baseDir.exists())
                    baseDir.mkdirs();

                fileName = packageDAOMember.replace(".",File.separator)+File.separator;

                dirSourceFile = new File(baseDir.getPath()+File.separator+"src"+File.separator+fileName);
                if(!dirSourceFile.exists())
                    dirSourceFile.mkdirs();

                //System.out.println("dirSourceFile:"+dirSourceFile.getPath());
                fileName = dirSourceFile.getPath()+File.separator+exceptionName+".java";

                sourceFile = new File(fileName);                
                fos        = new FileOutputStream(sourceFile) ;
                ps         = new PrintStream (fos);
                BufferedReader   br   = null;
                
                br = new BufferedReader(new InputStreamReader(fos.getClass().
                        getResourceAsStream("/templates/"+exceptionName+".java.template"))); 
            
                String line=null;                

                while((line=br.readLine())!=null) {               
                    line = line.replace("${date}",sdf.format(new Date()));                    
                    line = line.replace("${dao.package}",packageDAOMember);
                    
                    ps.println(line);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void buildDAOs(String packageDAOMember,String packageBeanMember,
            String basePath,DBTableSet dbSet,boolean useGenrics){
        String            fileName;        
        File    baseDir       = null;
        File    dirSourceFile = null;        
        File    sourceFile    = null;
        SimpleDateFormat sdf  = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        FileOutputStream fos = null;
        PrintStream      ps  = null;
        try {
            Enumeration<Table> enumtables=dbSet.getTablesElements();
            Table tableIter;
            while(enumtables.hasMoreElements()) {
                tableIter = enumtables.nextElement();
                //-------------------------------------------------------
                baseDir = new File(basePath);
                String tnCH = FormatString.getCadenaHungara(tableIter.getName());
                if(!baseDir.exists())
                    baseDir.mkdirs();

                fileName = packageDAOMember.replace(".",File.separator)+File.separator;

                dirSourceFile = new File(baseDir.getPath()+File.separator+"src"+File.separator+fileName);
                if(!dirSourceFile.exists())
                    dirSourceFile.mkdirs();

                //System.out.println("dirSourceFile:"+dirSourceFile.getPath());
                fileName = dirSourceFile.getPath()+File.separator+tnCH+"DAO.java";

                sourceFile = new File(fileName);                
                fos        = new FileOutputStream(sourceFile) ;
                ps         = new PrintStream (fos);
                BufferedReader   br   = null;
                
                br = new BufferedReader(new InputStreamReader(fos.getClass().
                        getResourceAsStream(useGenrics?"/templates/TableBeanDAO.java5.template":
                                                       "/templates/TableBeanDAO.java.template"))); 
            
                String line=null;                
                ArrayList<String> linesToParse=null;
                int nl=0;
                boolean tablebeansStart     = false;

                while((line=br.readLine())!=null) {
                    line = line.replace("${tablebean.name}",FormatString.getCadenaHungara(tableIter.getName()));
                    line = line.replace("${date}",sdf.format(new Date()));
                    line = line.replace("${tablebean.package}",packageBeanMember);
                    line = line.replace("${dao.package}",packageDAOMember);
                    
                    ps.println(line);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void buildConcreteDAOs(String packageDAOMember,String packageBeanMember,
            String basePath,DBTableSet dbSet,boolean[] buildForRDBMS,boolean useGenrics){
        String            fileName;        
        File    baseDir       = null;
        File    dirSourceFile = null;        
        File    sourceFile    = null;
        SimpleDateFormat sdf  = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        FileOutputStream fos = null;
        PrintStream      ps  = null;
        try {
            int i;
            String[] rdbmsNames=getSouportedRDBMS();
            for(i=0;i<rdbmsNames.length;i++){
                if(!buildForRDBMS[i])
                    continue;
                //-------------------------------------------------------
            
                Enumeration<Table> enumtables=dbSet.getTablesElements();
                Table tableIter;
                while(enumtables.hasMoreElements()) {
                    tableIter = enumtables.nextElement();
                    //-------------------------------------------------------
                    baseDir = new File(basePath);
                    String tnCH = FormatString.getCadenaHungara(tableIter.getName());
                    if(!baseDir.exists())
                        baseDir.mkdirs();

                    fileName = packageDAOMember.replace(".",File.separator)+File.separator+rdbmsNames[i].toLowerCase()+File.separator;

                    dirSourceFile = new File(baseDir.getPath()+File.separator+"src"+File.separator+fileName);
                    if(!dirSourceFile.exists())
                        dirSourceFile.mkdirs();

                    fileName = dirSourceFile.getPath()+File.separator+rdbmsNames[i]+tnCH+"DAO.java";

                    sourceFile = new File(fileName);                
                    fos        = new FileOutputStream(sourceFile) ;
                    ps         = new PrintStream (fos);
                    BufferedReader   br   = null;

                    br = new BufferedReader(new InputStreamReader(fos.getClass().
                            getResourceAsStream(useGenrics?"/templates/ConcreteTableBeanDAO.java5.template":
                                                           "/templates/ConcreteTableBeanDAO.java.template"))); 

                    String line=null;                   
                    ArrayList<String> linesToParse=null;
                    boolean tablebeansStart     = false;
                    boolean tablebeansKeysStart = false;
                    boolean tablebeansPSInsertKeysStart = false;
                    boolean tablebeansInsertKeysStart = false;
                    boolean tablebeansValuesStart = false;
                    boolean tablebeansFKStart = false;
                    
                    boolean tableAutogeneratedKeysStart = false;
                    
                    while((line=br.readLine())!=null) {                        
                        if(line.indexOf("@foreach")>=0) {
                            if(line.indexOf("${tablebean.columns.select}")>=0   ) {
                                tablebeansStart = true;
                            } else if(line.indexOf("${tablebean.columns.insert}")>=0  ) {
                                tablebeansPSInsertKeysStart = true;
                            } else if(line.indexOf("${tablebean.columns.update.keys}")>=0 ) {
                                tablebeansKeysStart = true;                                
                            } else if(line.indexOf("${tablebean.columns.insert.keys}")>=0 ) {
                                tablebeansInsertKeysStart = true;
                            } else if(line.indexOf("${tablebean.fkcolumns}")>=0 ) {
                                tablebeansFKStart = true;
                            } else if(line.indexOf("${tablebean.columns.update.values}")>=0 ) {
                                tablebeansValuesStart = true;                                
                            }
                            linesToParse=new ArrayList<String>();
                        } else if(line.indexOf("@endfor")>=0) {
                            if (tablebeansKeysStart || tablebeansStart || tablebeansValuesStart || tablebeansPSInsertKeysStart || tablebeansInsertKeysStart) {
                                Iterator<Column> itCols=tableIter.getSortedColumns();
                                Column colIter;
                                int ci=0;
                                
                                while(itCols.hasNext()) {                                    
                                    colIter = itCols.next();
                                    if(tablebeansKeysStart && ! colIter.isPrimaryKey()) {
                                        continue;
                                    } else if(tablebeansInsertKeysStart  && !colIter.isAutoIncremment()){
                                        continue;
                                    } else if(tablebeansPSInsertKeysStart  && colIter.isAutoIncremment()){
                                        continue;
                                    } else if(tablebeansValuesStart && colIter.isPrimaryKey()) {
                                        continue;
                                    }
                                    
                                    ci++;
                                    for(String lineInLoop: linesToParse) {
                                        
                                        lineInLoop = lineInLoop.replace("${loop.counter}",String.valueOf(ci));
                                        
                                        lineInLoop = lineInLoop.replace("${column4insert.getter}",
                                                "get"+FormatString.getCadenaHungara(colIter.getName()));                                        
                                        lineInLoop = lineInLoop.replace("${column4update.getter}",
                                                "get"+FormatString.getCadenaHungara(colIter.getName()));
                                        
                                        lineInLoop = lineInLoop.replace("${column4insert.setter}",
                                                "set"+FormatString.getCadenaHungara(colIter.getName()));
                                        
                                        lineInLoop = lineInLoop.replace("${column4select.name}",
                                                colIter.getName());
                                        lineInLoop = lineInLoop.replace("${column4update.name}",
                                                colIter.getName());
                                        lineInLoop = lineInLoop.replace("${tablebean.sql.name}",
                                                tableIter.getName().toUpperCase());                                        
                                        lineInLoop = lineInLoop.replace("${tablebean.name}",
                                                FormatString.getCadenaHungara(tableIter.getName()));
                                        
                                        String colClass = colIter.getJavaClassType();
                                        
                                        if(colClass.equals("java.lang.Integer"))
                                            colClass = "Int";
                                        if(colClass.equals("byte[]")){
                                            lineInLoop = lineInLoop.replace("${column4insert.ps.setter}",
                                                    "setBlob");
                                            lineInLoop = lineInLoop.replace("${column4insert.rs.getter}",
                                                    "get"+colClass);
                                            lineInLoop = lineInLoop.replace("${column4select.rs.getter}",
                                                    "rs.getBlob(\""+colIter.getName()+"\").getBytes(0, (int)rs.getBlob(\""+colIter.getName()+"\").length())");
                                            lineInLoop = lineInLoop.replace("${column4insert.ps.getter}",
                                                    "new java.io.ByteArrayInputStream(bean"+FormatString.getCadenaHungara(tableIter.getName())+
                                                    ".get"+FormatString.getCadenaHungara(colIter.getName())+"())");
                                        } else if(colClass.lastIndexOf('.')>0){                       
                                            
                                            lineInLoop = lineInLoop.replace("${column4insert.ps.getter}",
                                                    "bean"+FormatString.getCadenaHungara(tableIter.getName())+
                                                    ".get"+FormatString.getCadenaHungara(colIter.getName())+"()");
                                            
                                            lineInLoop = lineInLoop.replace("${column4insert.rs.getter}",
                                                    "get"+colClass.substring(colClass.lastIndexOf('.')+1));
                                            lineInLoop = lineInLoop.replace("${column4select.rs.getter}",
                                                    "rs.get"+colClass.substring(colClass.lastIndexOf('.')+1)+"(\""+colIter.getName()+"\")");
                                            lineInLoop = lineInLoop.replace("${column4insert.ps.setter}",
                                                    "set"+colClass.substring(colClass.lastIndexOf('.')+1));
                                            
                                        } else {
                                            lineInLoop = lineInLoop.replace("${column4insert.ps.getter}",
                                                    "bean"+FormatString.getCadenaHungara(tableIter.getName())+
                                                    ".get"+FormatString.getCadenaHungara(colIter.getName())+"()");
                                            
                                            lineInLoop = lineInLoop.replace("${column4insert.rs.getter}",
                                                    "get"+colClass);
                                            lineInLoop = lineInLoop.replace("${column4select.rs.getter}",
                                                    "rs.get"+colClass+"(\""+colIter.getName()+"\")");
                                            lineInLoop = lineInLoop.replace("${column4insert.ps.setter}",
                                                    "set"+colClass);
                                        }
                                        ps.println(lineInLoop);
                                    }
                                }                                
                            } else if(tablebeansFKStart) {
                                Collection<Column> fks = tableIter.getFKs();
                                for(Column fkCol :  fks) {
                                    Table fTable = dbSet.getTable(tableIter.getFKReferenceTable(fkCol.getName()).getTableName());
                                    
                                    Collection<Column> foreignDesCols = fTable.getForeignDescriptionColumns();
                                    String descForeigCol = FormatString.getCadenaHungara(fTable.getName());
                                    int ics = 0;
                                    for(Column cfc : foreignDesCols) {
                                        descForeigCol+= "_"+FormatString.getCadenaHungara(cfc.getName());
                                        ics++;
                                    } if(ics==0){
                                        descForeigCol+= "_"+FormatString.getCadenaHungara(
                                                fTable.getPrimaryKeys().iterator().next().getName());
                                    }
                                    
                                    for(String lineInLoop: linesToParse) {
                                        
                                        lineInLoop = lineInLoop.replace("${tablebean.name}"         ,FormatString.getCadenaHungara(tableIter.getName()));
                                        lineInLoop = lineInLoop.replace("${column.fkDecSetter}"     ,"set"+descForeigCol);
                                        String psgetfkkdesc = "";
                                        ics = 0;
                                        for(Column cfc : foreignDesCols) {
                                            if(ics>0)
                                                psgetfkkdesc+= " + \" \" +";
                                            psgetfkkdesc+= "rs.getString(\""+fTable.getName()+"_"+FormatString.getCadenaHungara(cfc.getName())+"\")";
                                            ics++;
                                        } if(ics==0){
                                            psgetfkkdesc+= "rs.getString(\""+fTable.getName()+"_"+FormatString.getCadenaHungara(
                                                    fTable.getPrimaryKeys().iterator().next().getName())+"\")";
                                        }
                                        lineInLoop = lineInLoop.replace("${column.fkDecSetter.name}",psgetfkkdesc);
                                        
                                        ps.println(lineInLoop);    
                                    }
                                    
                                }
                            
                            }
                            tablebeansKeysStart         = false;
                            tablebeansValuesStart       = false;
                            tablebeansInsertKeysStart   = false;
                            tablebeansPSInsertKeysStart = false;
                            tablebeansStart             = false;
                            tablebeansFKStart           = false;
                            linesToParse                = null;
                        } else if(line.indexOf("@if")>=0) {
                            if(line.indexOf("${tablebean.autoIncrement.keys}")>=0 ) {
                                tableAutogeneratedKeysStart = true;
                            }
                            linesToParse=new ArrayList<String>();
                        } else if(line.indexOf("@endif")>=0) {
                            if(tableAutogeneratedKeysStart && tableIter.getAutoIncrementColums().hasNext()){
                                for(String lineInLoop: linesToParse) {
                                    ps.println(lineInLoop);
                                }
                            }
                            
                            tableAutogeneratedKeysStart = false;
                            linesToParse          = null;
                        } else if(linesToParse!=null){                            
                            linesToParse.add(line);
                        } else {
                            line = line.replace("${rdbms.name}",rdbmsNames[i]);
                            line = line.replace("${tablebean.sql.name}",tableIter.getName().toUpperCase());
                            line = line.replace("${tablebean.columns.insert.list_ps}",tableIter.getListMatchers4InsertPS());
                            line = line.replace("${tablebean.columns.insert.list_cols}",tableIter.getListColumnsNamesForInsert().toUpperCase());
                            line = line.replace("${tablebean.columns.select.list_cols}",tableIter.getListColumnsNames().toUpperCase());
                            
                            String sfk1 = dbSet.getListForeignDescriptionColumnNames(tableIter).toUpperCase();
                            line = line.replace("${tablebean.columns.select.list_fkrefscols}",(sfk1.trim().length()>0?" , ":"")+sfk1);
                            String sfk2 = tableIter.getListFKTableNames().toUpperCase();
                            line = line.replace("${tablebean.list_fktables}",(sfk2.trim().length()>0?", ":"")+sfk2);
                            String fkjoins = dbSet.getListFKJoinExpressions(tableIter).toUpperCase();
                            line = line.replace("${tablebean.fktables.join}",(fkjoins.trim().length()>0?" AND ":"")+fkjoins);
                            
                            line = line.replace("${concretedao.package}",packageDAOMember+"."+rdbmsNames[i].toLowerCase());
                            line = line.replace("${tablebean.name}",FormatString.getCadenaHungara(tableIter.getName()));
                            line = line.replace("${date}",sdf.format(new Date()));
                            line = line.replace("${tablebean.package}",packageBeanMember);
                            line = line.replace("${dao.package}",packageDAOMember);
                            ps.println(line);
                        }                        
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }    
    }
    
    public static DBTableSet buildDBTableSet(Connection conn,String catalogSearch,String tablePatter){
        DatabaseMetaData  dbMd = null;
        ResultSetMetaData rsMd = null;
        ResultSet         rs1  = null;
        ResultSet         rs2  = null;
        Statement         st   = null;
        
        int i,n;
        
        DBTableSet       dbTables = new DBTableSet();
        Table            iterTable;
        Column           iterColumn;
        ReferenceTable   iterreferenceFK;        
        try {
            dbMd = conn.getMetaData();
            
            String[] tablemapedJavaClass = {"TABLE"};
            
            //System.out.println("-> Connection class:"+conn.getClass());
            
            rs1  = dbMd.getTables(catalogSearch,null,tablePatter,tablemapedJavaClass);
            st = conn.createStatement();
            String tableName = null; 
            String catalog ;
            while(rs1.next()){
                tableName = rs1.getString("TABLE_NAME");
                catalog   = rs1.getString("TABLE_CAT");
                //System.out.println("-> Analysing: table="+tableName+", catalog="+catalog);
                iterTable = new Table();
                iterTable.setName(tableName);
                //---------------------------------------------------------
                rs2  = st.executeQuery("select * from "+catalog+"."+iterTable.getName()+" where 1=2");                
                rsMd = rs2.getMetaData();
                n=rsMd.getColumnCount();
                
                for(i=1;i<=n;i++){                    
                    iterColumn = new SimpleColumn();
                    
                    iterColumn.setName          (rsMd.getColumnName(i).toUpperCase());
                    iterColumn.setAutoIncremment(rsMd.isAutoIncrement(i));
                    iterColumn.setNullable      (rsMd.isNullable(i) == ResultSetMetaData.columnNullable);
                    iterColumn.setScale         (rsMd.getScale(i));
                    try{                        
                        iterColumn.setPrecision     (rsMd.getPrecision(i));
                    } catch (Exception ex){
                        System.out.print("-> Error in Precision for:"+iterTable.getName()+"."+rsMd.getColumnName(i));
                        iterColumn.setPrecision     (-1);
                    }
                    
                    
                    String classNameForColumn = rsMd.getColumnClassName(i);                    
                    if( classNameForColumn.endsWith("[B") ) {
                        iterColumn.setJavaClassType ("byte[]");
                    } else {
                        iterColumn.setJavaClassType (rsMd.getColumnClassName(i));
                    }
                    iterColumn.setSqlType       (rsMd.getColumnTypeName(i));
                    
                    iterTable.addColumn(iterColumn);
                }
                rs2.close();
                //---------------------------------------------------------
                rs2  = dbMd.getImportedKeys(catalogSearch,null,iterTable.getName());                
                while(rs2.next()){                    
                    if(iterTable.getColumn(rs2.getString("FKCOLUMN_NAME"))!=null){
                        iterreferenceFK = new ReferenceTable();
                        
                        iterreferenceFK.setColumnName(rs2.getString("PKCOLUMN_NAME"));
                        iterreferenceFK.setTableName (rs2.getString("PKTABLE_NAME"));
                    
                        iterTable.getColumn(rs2.getString("FKCOLUMN_NAME")).setForeignKey(true);
                        iterTable.addForeignKey(rs2.getString("FKCOLUMN_NAME"),iterreferenceFK);
                    }
                }
                rs2.close();       
                //---------------------------------------------------------                
                rs2  = dbMd.getPrimaryKeys(catalogSearch,null,iterTable.getName());                
                while(rs2.next()){                    
                    if(iterTable.getColumn(rs2.getString("COLUMN_NAME"))!=null){
                        iterTable.getColumn(rs2.getString("COLUMN_NAME")).setPrimaryKey(true);
                    }                        
                }
                rs2.close();     
                //---------------------------------------------------------
                rs2  = dbMd.getColumns(catalogSearch,null,iterTable.getName(),null);                
                while(rs2.next()){                    
                    if(iterTable.getColumn(rs2.getString("COLUMN_NAME"))!=null){
                        iterTable.getColumn(rs2.getString("COLUMN_NAME")).setPosition(rs2.getInt("ORDINAL_POSITION"));
                    }                        
                }
                rs2.close();     
                //---------------------------------------------------------
                rs2  = dbMd.getIndexInfo(catalogSearch,null,iterTable.getName(),false,true);                
                while(rs2.next()){                    
                    Index idx =  new Index();
                    idx.setColumnName(rs2.getString("COLUMN_NAME"));
                    idx.setName(rs2.getString("INDEX_NAME"));
                    idx.setDirectionOrder(rs2.getString("ASC_OR_DESC").toLowerCase());
                    
                    iterTable.addIndex(idx);                    
                }
                rs2.close();     
                //---------------------------------------------------------
                
                //---------------------------------------------------------
                
                dbTables.addTable(iterTable);
            }
            rs1.close();
            
            if(conn.getClass().toString().contains("mysql")) {
                Enumeration<Table> et = dbTables.getTablesElements();
                while(et.hasMoreElements()){
                    iterTable = et.nextElement();
                
                    //System.out.print("-> we are connectod to mysql, then search the comments in information_schema");
                    // we are connectod to mysql, then search the comments in information_schema
                    rs2  = st.executeQuery("select table_schema, table_name, column_name, column_comment "+
                            "from information_schema.columns where table_schema='"+catalogSearch+"' and table_name='"+iterTable.getName()+"'");
                    int numComments  = 0;
                    while(rs2.next()){
                        String columnName = rs2.getString("column_name");
                        String comment    = rs2.getString("column_comment");
                        if(comment != null && comment.trim().length()>3) {
                            //System.out.println("-> \ttable="+tableName+", column="+columnName+", comment="+comment);                        
                            iterTable.getColumn(columnName.toUpperCase()).setComments(comment);
                            numComments++;
                        }
                    } 
                    if (numComments == 0){
                        System.err.println("-> There is not comment for table "+iterTable.getName());                                
                    }
                    rs2.close();
                }
            }
            st.close();
        } catch(SQLException ex){
            ex.printStackTrace();
        }
        
        return dbTables;
    }    
    
    public static void buildDefaultANTScript(String basePath){
        File    baseDir    = null;
        FileOutputStream fos= null;
        InputStream      is = null;
        byte [] buffer=new byte[1024];
        int r;
        try {
            baseDir = new File(basePath);
            
            is = Builder.class.getResourceAsStream("/templates/build.xml.template");
            if(is==null) {
                throw new Exception("Can't find ANT template script.");
            }
            
            fos = new FileOutputStream(baseDir.getAbsolutePath()+File.separator+"build.xml");
            while((r=is.read(buffer))!=-1) {
                fos.write(buffer,0,r);
            }            
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try{
                if(fos!=null)
                    fos.close();
                if(is!=null)
                    is.close();                
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}