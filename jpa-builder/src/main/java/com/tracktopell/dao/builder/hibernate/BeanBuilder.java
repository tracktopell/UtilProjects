/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tracktopell.dao.builder.hibernate;

import com.tracktopell.dao.builder.FormatString;
import com.tracktopell.dao.builder.metadata.Column;
import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.metadata.Table;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

/**
 *
 * @author aegonzalez
 */
public class BeanBuilder {
    public static void buildMappingBeans(DBTableSet dbSet,String packageBeanMember,String basePath){
        
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
                        fos.getClass().getResourceAsStream("/templates/TableBean4Hibernate.java.template"))); 
                String line=null;                
                ArrayList<String> linesToParse=null;
                int nl=0;
                while((line=br.readLine())!=null) {
                    
                    if(line.indexOf("%foreach")>=0) {
                        linesToParse=new ArrayList<String>();                        
                    } else if(line.indexOf("%endfor")>=0) {
                        Iterator<Column> columns = table.getSortedColumns();
                        Column column = null;
                        while(columns.hasNext()){
                            column = columns.next();
                            Table fTable    = null;
                            String refObjFK = null;
                            
                            
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
                                    
                                    if(column.isForeignKey() ) {         
                                        fTable    = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
                                        refObjFK = FormatString.getCadenaHungara(fTable.getName());
                                        
                                        ps.println("    private "+refObjFK+" "+FormatString.firstLetterLowerCase(refObjFK)+";");
                                    } else {                                    
                                        ps.println("    private "+column.getJavaClassType().replace("java.lang.","")+
                                                " "+FormatString.renameForJavaMethod(column.getName())+";");                                                                        
                                    }
                                } else if(lineInLoop.indexOf("${tablebean.member.getter}")>=0 ){
                                    if(column.isForeignKey() ) {
                                        fTable    = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
                                        refObjFK = FormatString.getCadenaHungara(fTable.getName());
                                        
                                        ps.println("    public "+refObjFK+" get"+refObjFK+"() {");
                                        ps.println("        return this."+FormatString.firstLetterLowerCase(refObjFK)+";");
                                        ps.println("    }");
                                    } else {
                                        ps.println("    public "+column.getJavaClassType().replace("java.lang.","")+
                                                " get"+FormatString.getCadenaHungara(column.getName())+"() {");
                                        ps.println("        return this."+FormatString.renameForJavaMethod(column.getName())+";");
                                        ps.println("    }");
                                    }
                                } else if(lineInLoop.indexOf("${tablebean.member.setter}")>=0 ){
                                    if(column.isForeignKey() ) {
                                        fTable    = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
                                        refObjFK = FormatString.getCadenaHungara(fTable.getName());
                                        
                                        ps.println("    public void set"+refObjFK+"("+refObjFK+" v) {");
                                        ps.println("        this."+FormatString.firstLetterLowerCase(refObjFK)+" = v;");
                                        ps.println("    }");
                                    } else {
                                        ps.println("    public void set"+FormatString.getCadenaHungara(column.getName())+
                                                "("+column.getJavaClassType().replace("java.lang.","")+" "+FormatString.renameForJavaMethod(column.getName())+") {");
                                        ps.println("        this."+FormatString.renameForJavaMethod(column.getName())+" = "+
                                                FormatString.renameForJavaMethod(column.getName())+";");
                                        ps.println("    }");
                                    }
                                } else if(lineInLoop.indexOf("${tablebean.member_getter}")>=0 ){
                                    if(column.isForeignKey() ) {
                                        fTable    = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
                                        refObjFK = FormatString.getCadenaHungara(fTable.getName());
                                        
                                        lineInLoop = lineInLoop.replace("${tablebean.member_getter}",
                                                "get"+refObjFK+"().get"+FormatString.getCadenaHungara(table.getFKReferenceTable(column.getName()).getColumnName())+"()");                                        
                                    } else {
                                        lineInLoop = lineInLoop.replace("${tablebean.member_getter}",
                                                "get"+FormatString.getCadenaHungara(column.getName())+"()");                                        
                                    }
                                    ps.println(lineInLoop);
                                }
                            }
                        }
                        linesToParse=null;
                    } else if(linesToParse!=null){
                        linesToParse.add(line);
                    } else {
                        line = line.replace("${date}",sdf.format(new Date()));
                        line = line.replace("${tablebean.serialId}",String.valueOf(table.hashCode()));
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
}
