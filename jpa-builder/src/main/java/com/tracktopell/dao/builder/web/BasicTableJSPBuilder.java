/*
 * BasicTableJSPBuilder.java
 *
 */

package com.tracktopell.dao.builder.web;

import com.tracktopell.dao.builder.FormatString;
import com.tracktopell.dao.builder.metadata.Column;
import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.metadata.SmartColumnNameRelation;
import com.tracktopell.dao.builder.metadata.Table;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

/**
 *
 * @author tracktopell
 */
public class BasicTableJSPBuilder {

    private BasicTableJSPBuilder() {
    }
    
    public static void buildListTableJSP(DBTableSet dbSet,String packageDAOMember,String packageBeansMember,String basePath){        
        int i;
        
        String            fileName;        
        File    baseDir       = null;
        File    dirSourceFile = null;        
        File    sourceFile    = null;
        
        FileOutputStream  fos = null;
        PrintStream       ps  = null;
        InputStream       is  = null;
        InputStreamReader isr = null; 
        BufferedReader    br  = null;
        try {

            Enumeration<String> tableNames=dbSet.getTableNames();
            
            while(tableNames.hasMoreElements()){
                Table table = dbSet.getTable(tableNames.nextElement());
                //-------------------------------------------------------
                baseDir = new File(basePath);
            
                if(!baseDir.exists())
                    baseDir.mkdirs();

                dirSourceFile = new File(baseDir.getPath()+File.separator+"web");
                if(!dirSourceFile.exists())
                    dirSourceFile.mkdirs();
                
                fileName = dirSourceFile.getPath()+File.separator+"list"+FormatString.getCadenaHungara(table.getName())+".jsp";
    
                sourceFile = new File(fileName);                
                fos        = new FileOutputStream(sourceFile) ;
                ps         = new PrintStream (fos);
                //System.out.println("fileName:"+fileName);
                //-------------------------------------------------------

                ps.println("<%@taglib uri=\"http://java.sun.com/jsp/jstl/core\" prefix=\"c\"%>");
                ps.println("<%@taglib uri=\"http://java.sun.com/jsp/jstl/fmt\"  prefix=\"fmt\"%>"); 
                ps.println("<%@page import=\""+packageDAOMember+".*\"%>");
                ps.println("<%@page import=\""+packageBeansMember+".*\"%>");
                ps.println("<%@page import=\"java.util.*\"%>");
                ps.println("<%");
                ps.println("    Collection lista = null;");
                ps.println("    String exDescription=null;");
                ps.println("    try {");
                ps.println("        lista = DAOFactory.getDAO().get"+FormatString.getCadenaHungara(table.getName())+"By(new "+FormatString.getCadenaHungara(table.getName())+"());");
                ps.println("    }");
                ps.println("    catch( Exception ex){");
                ps.println("        ex.printStackTrace();");
                ps.println("        exDescription = ex.toString();");
                ps.println("        lista = null;");
                ps.println("    }");
                ps.println("    pageContext.setAttribute(\"lista\",lista);");
                ps.println("%>");                
                ps.println("");
                
                is = BasicTableJSPBuilder.class.getResourceAsStream("/templates/list.jsp");
                if(is==null)
                    throw new IOException("Can't find JSP List template.");
                isr = new InputStreamReader (is); 
                br  = new BufferedReader (isr);
                String lineReaded = null;
                
                boolean headStared = false;
                boolean headEnd    = false;
                boolean bodyStared = false;
                boolean bodyEnd    = false;
                
                Iterator<Column> iteratorColumns=null;
                Column column = null;
                StringBuffer finalValue= new StringBuffer();
                while((lineReaded = br.readLine())!=null){
                    if(lineReaded.indexOf("${table.name}")!=-1){
                        ps.println(lineReaded.replace("${table.name}",table.getName()));
                    }
                    else if(lineReaded.indexOf("${columns.header.start}")!=-1){
                        headStared = true;
                    }
                    else if(lineReaded.indexOf("${column.name}")!=-1 && headStared && !headEnd){
                        iteratorColumns = table.getSortedColumns();
                        while(iteratorColumns.hasNext()){
                            column = iteratorColumns.next();
                            ps.println(lineReaded.replace("${column.name}",column.getName()));
                        }
                    }
                    else if(lineReaded.indexOf("${columns.header.end}")!=-1 && headStared){
                        headEnd = true;
                    }
                    else if(lineReaded.indexOf("${columns.body.start}")!=-1 && headEnd){
                        bodyStared = true;
                        ps.println("<%");
                        ps.println("    if(lista!=null) {");
                        ps.println("%>");
                        ps.println("    <c:forEach items=\"${lista}\" var=\"bean\">");
                    }
                    else if(lineReaded.indexOf("${column.value}")!=-1 && bodyStared && !bodyEnd){
                        
                        iteratorColumns = table.getSortedColumns();
                        while(iteratorColumns.hasNext()){
                            column = iteratorColumns.next();
                        
                            if(column.getJavaClassType().equals("java.math.BigDecimal")){
                                finalValue=new StringBuffer("<fmt:formatNumber value=\"");
                                finalValue.append("${bean."+FormatString.renameForJavaMethod(column.getName())+"}");
                                finalValue.append("\" pattern=\"###,###,###,###.0#\"/>");
                                
                                ps.println(lineReaded.replace("${column.value}",finalValue.toString()));                                    
                            } else if(column.getJavaClassType().equals("java.sql.Timestamp")||
                                    column.getJavaClassType().equals("java.sql.Date")){
                                finalValue=new StringBuffer("<fmt:formatDate value=\"");
                                finalValue.append("${bean."+FormatString.renameForJavaMethod(column.getName())+"}");
                                finalValue.append("\" pattern=\"yyyy/MM/dd hh:mm\"/>");
                                
                                ps.println(lineReaded.replace("${column.value}",finalValue.toString()));                                    
                            } else {
                                ps.println(lineReaded.replace("${column.value}",
                                        "${bean."+FormatString.renameForJavaMethod(column.getName())+"}"));    
                            }                            
                        }
                    }
                    else if(lineReaded.indexOf("${columns.body.end}")!=-1 && bodyStared && !bodyEnd){
                        bodyEnd = true;
                        ps.println("    </c:forEach>");
                        ps.println("<%");
                        ps.println("    }");
                        ps.println("%>");
                    }
                    else {
                        ps.println(lineReaded);
                    }
                }
                isr.close();
                ps.close();                
                fos.close();
                
                sourceFile = null;
                ps         = null;
                fos        = null;
            }            
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }    
    
    
    public static void buildListTableExpandedJSP(DBTableSet dbSet,String packageDAOMember,String packageBeansMember,String basePath,SmartColumnNameRelation smartPattern){        
        int i;
        
        String            fileName;        
        File    baseDir       = null;
        File    dirSourceFile = null;        
        File    sourceFile    = null;
        
        FileOutputStream  fos = null;
        PrintStream       ps  = null;
        InputStream       is  = null;
        InputStreamReader isr = null; 
        BufferedReader    br  = null;
        try {
            Enumeration<String> tableNames=dbSet.getTableNames();
            
            while(tableNames.hasMoreElements()){
                Table table = dbSet.getTable(tableNames.nextElement());
                //-------------------------------------------------------
                baseDir = new File(basePath);
            
                if(!baseDir.exists())
                    baseDir.mkdirs();

                dirSourceFile = new File(baseDir.getPath()+File.separator+"web");
                if(!dirSourceFile.exists())
                    dirSourceFile.mkdirs();
                
                fileName = dirSourceFile.getPath()+File.separator+"listExpanded"+FormatString.getCadenaHungara(table.getName())+".jsp";
    
                sourceFile = new File(fileName);                
                fos        = new FileOutputStream(sourceFile) ;
                ps         = new PrintStream (fos);
                //System.out.println("fileName:"+fileName);
                //-------------------------------------------------------

                ps.println("<%@taglib uri=\"http://java.sun.com/jsp/jstl/core\" prefix=\"c\"%>");
                ps.println("<%@taglib uri=\"http://java.sun.com/jsp/jstl/fmt\"  prefix=\"fmt\"%>"); 
                ps.println("<%@page import=\""+packageDAOMember+".*\"%>");
                ps.println("<%@page import=\""+packageBeansMember+".*\"%>");
                ps.println("<%@page import=\"java.util.*\"%>");
                ps.println("<%");
                ps.println("    Collection<Object[]> lista = null;");
                ps.println("    List<String> columnsNames = null;");
                ps.println("    List<String> columnsClass = null;");                
                ps.println("    String exDescription=null;");
                ps.println("    try {");
                ps.println("        columnsNames = new ArrayList<String>();");
                ps.println("        columnsClass = new ArrayList<String>();");
                ps.println("        lista = DAOFactory.getDAO().getExpanded"+FormatString.getCadenaHungara(table.getName())+"By(new "+FormatString.getCadenaHungara(table.getName())+"(),columnsNames,columnsClass,true);");
                ps.println("    }");
                ps.println("    catch( Exception ex){");
                ps.println("        ex.printStackTrace();");
                ps.println("        exDescription = ex.toString();");
                ps.println("        columnsNames = null;");
                ps.println("        columnsClass = null;");
                ps.println("        lista = null;");
                ps.println("    }");
                ps.println("    pageContext.setAttribute(\"lista\",lista);");
                ps.println("%>");                
                ps.println("");
                
                is = BasicTableJSPBuilder.class.getResourceAsStream("/templates/list.jsp");
                if(is==null)
                    throw new IOException("Can't find JSP List template.");
                isr = new InputStreamReader (is); 
                br  = new BufferedReader (isr);
                String lineReaded = null;
                
                boolean headStared = false;
                boolean headEnd    = false;
                boolean bodyStared = false;
                boolean bodyEnd    = false;

                ArrayList<String> columnNameResult  = new ArrayList<String>();
                ArrayList<String> columnClassResult = new ArrayList<String>();

                //dbSet.getExpandedQueryFor(table,smartPattern,columnNameResult,columnClassResult,true);

                
                Iterator<Column> iteratorColumns=null;
                Column column = null;
                StringBuffer finalValue= new StringBuffer();
                while((lineReaded = br.readLine())!=null){
                    if(lineReaded.indexOf("${table.name}")!=-1){
                        ps.println(lineReaded.replace("${table.name}",table.getName()));
                    }
                    else if(lineReaded.indexOf("${columns.header.start}")!=-1){
                        headStared = true;
                    }
                    else if(lineReaded.indexOf("${column.name}")!=-1 && headStared && !headEnd){
                        for(i=0;i<columnNameResult.size();i++){
                            ps.println(lineReaded.replace("${column.name}",columnNameResult.get(i)));
                        }
                    }
                    else if(lineReaded.indexOf("${columns.header.end}")!=-1 && headStared){
                        headEnd = true;
                    }
                    else if(lineReaded.indexOf("${columns.body.start}")!=-1 && headEnd){
                        bodyStared = true;
                        ps.println("<%");
                        ps.println("    if(lista!=null) {");
                        ps.println("%>");
                        ps.println("    <c:forEach items=\"${lista}\" var=\"bean\">");
                    }
                    else if(lineReaded.indexOf("${column.value}")!=-1 && bodyStared && !bodyEnd){
                        
                        for(i=0;i<columnNameResult.size();i++){
                        
                            if(columnClassResult.get(i).equals("java.math.BigDecimal")){
                                finalValue=new StringBuffer("<fmt:formatNumber value=\"");
                                finalValue.append("${bean["+i+"]}");
                                finalValue.append("\" pattern=\"###,###,###,###.0#\"/>");
                                
                                ps.println(lineReaded.replace("${column.value}",finalValue.toString()));                                    
                            } else if(columnClassResult.get(i).equals("java.sql.Timestamp")||
                                    columnClassResult.get(i).equals("java.sql.Date")){
                                finalValue=new StringBuffer("<fmt:formatDate value=\"");
                                finalValue.append("${bean["+i+"]}");
                                finalValue.append("\" pattern=\"yyyy/MM/dd hh:mm\"/>");
                                
                                ps.println(lineReaded.replace("${column.value}",finalValue.toString()));                                    
                            } else {
                                ps.println(lineReaded.replace("${column.value}","${bean["+i+"]}"));    
                            }                            
                        }
                    }
                    else if(lineReaded.indexOf("${columns.body.end}")!=-1 && bodyStared && !bodyEnd){
                        bodyEnd = true;
                        ps.println("    </c:forEach>");
                        ps.println("<%");
                        ps.println("    }");
                        ps.println("%>");
                    }
                    else {
                        ps.println(lineReaded);
                    }
                }
                isr.close();
                ps.close();                
                fos.close();
                
                sourceFile = null;
                ps         = null;
                fos        = null;
            }            
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }    
}
