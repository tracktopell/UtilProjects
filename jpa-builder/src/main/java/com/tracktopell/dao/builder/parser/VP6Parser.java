/*
 * VP6Parser.java
 *
 */

package com.tracktopell.dao.builder.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.Hashtable;

import com.tracktopell.dao.builder.metadata.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Enumeration;

public class VP6Parser {
    private VP6Parser() {
    }
    
    public static Hashtable<String,VPModel> loadVPModels(InputStream is){
        DefaultHandler handler = new VPModelScanner();
        try {
            // Create a builder factory
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);

            SAXParser parser = factory.newSAXParser();

            // Create the builder and parse the file
            parser.parse(is, handler);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ((VPModelScanner)handler).vpModels;    
    }

    static class VPModelScanner extends DefaultHandler {
        Hashtable<String,VPModel> vpModels;
        public VPModelScanner() {
            vpModels              = new Hashtable<String,VPModel>();            
        }

        //---------------------------------------------------------------

        public void startDocument() {
            //System.err.println("->startDocument()");            
        }
        VPModel vpm = null;
        public void startElement(String uri, String localName, String qName, 
                                 Attributes attributes) {            
            try {
                
                if (qName.equals("Model") && attributes.getValue("modelType").equals("DBTable")) {
                    vpm=new VPModel();
                    vpm.setId(attributes.getValue("id"));
                    vpm.setModelType(attributes.getValue("modelType"));
                    vpm.setName(attributes.getValue("name"));
                    vpModels.put(attributes.getValue("id"),vpm);
                } else if (qName.equals("Model") && attributes.getValue("modelType").equals("DBColumn")) {
                    vpm=new VPModel();
                    vpm.setId(attributes.getValue("id"));
                    vpm.setModelType(attributes.getValue("modelType"));
                    vpm.setName(attributes.getValue("name"));
                    vpModels.put(attributes.getValue("id"),vpm);
                } else if (qName.equals("Model") && attributes.getValue("modelType").equals("DBForeignKey")) {
                    vpm=new DBForeignKey();
                    vpm.setId(attributes.getValue("id"));
                    vpm.setModelType(attributes.getValue("modelType"));
                    vpModels.put(attributes.getValue("id"),vpm);
                } else if (qName.equals("ModelRefProperty") && attributes.getValue("name").equals("from") && vpm instanceof DBForeignKey) {
                    ((DBForeignKey)vpm).setFrom("ModelRef");
                } else if (qName.equals("ModelRefProperty") && attributes.getValue("name").equals("to") && vpm instanceof DBForeignKey) {
                    ((DBForeignKey)vpm).setTo("ModelRef");
                } else if (qName.equals("ModelRef") && vpm instanceof DBForeignKey) {
                    if(((DBForeignKey)vpm).getTo()!=null && ((DBForeignKey)vpm).getTo().equals("ModelRef")){
                        ((DBForeignKey)vpm).setTo(attributes.getValue("id"));
                    } else if(((DBForeignKey)vpm).getFrom()!=null &&((DBForeignKey)vpm).getFrom().equals("ModelRef")){
                        ((DBForeignKey)vpm).setFrom(attributes.getValue("id"));
                    }
                } else if (vpm!= null && qName.equals("HTMLProperty") &&
                        attributes.getValue("name").equals("documentation") &&
                        attributes.getValue("plainTextValue")!=null &&
                        attributes.getValue("plainTextValue").trim().length()>0) {
                    vpm.setDocumentation(attributes.getValue("plainTextValue"));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void characters(char[] ch, int start, int length) {
                    
        }
        public void endElement(String uri, String localName, String qName) {            
            if (qName.equals("Model") ) {
                vpm=null;
            }
        }

        public void endDocument() {            
        }
        //---------------------------------------------------------------
    }    
    
    
    public static DBTableSet loadFromXMLWithVPModels(InputStream is,Hashtable<String,VPModel> vpModels) {
        DefaultHandler handler = new ModelPropertyScanner(vpModels);        
        try {
            // Create a builder factory
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);

            SAXParser parser = factory.newSAXParser();

            // Create the builder and parse the file
            parser.parse(is, handler);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ((ModelPropertyScanner)handler).currentDBTableSet;
    }
    
    static class ModelPropertyScanner extends DefaultHandler {
        Hashtable<String,VPModel> vpModels;
        boolean startFrom;
        boolean startTo;
        boolean fqRef;
        boolean colRef;
        boolean fkContraintsStart;
        boolean fkContraintDeclarationStart;
        DBForeignKey currentDBForeignKey;
        //Hashtable<String,DBForeignKey> foreignKeys;
        //DBForeignKey nextRefColumn;
        
        DBTableSet       currentDBTableSet;
        Table            currentTable;
        Column           currentColumn;
        int              columnPosition;
        ReferenceTable   currentReferenceTable;
        HashSet<String>             types;
        //Hashtable<String,String>    javaTypes;
        
        public ModelPropertyScanner(Hashtable<String,VPModel> vpModels) {
            this.vpModels         = vpModels;
            currentDBTableSet     = new DBTableSet();
            //foreignKeys           = new Hashtable<String,DBForeignKey>();
            currentTable          = null;
            currentColumn         = null;
            currentReferenceTable = null;
            fqRef                 = false;
            colRef                = false;
            types=new HashSet();            
        }

        //---------------------------------------------------------------

        public void startDocument() {
            //System.err.println("->startDocument()");            
        }

        public void startElement(String uri, String localName, String qName, 
                                 Attributes attributes) {            
            try {
                if (qName.equals("Model") && attributes.getValue("modelType").equals("DBTable")) {                
                    currentTable = new Table(); 
                    columnPosition = 0;
                    currentTable.setName(attributes.getValue("name"));
                    try{
                        if(this.vpModels.get(attributes.getValue("id")).getDocumentation() != null) {
                            String[] props = this.vpModels.get(attributes.getValue("id")).getDocumentation().split(";");
                            for(String prp: props) {
                                String[] keyValueProp = prp.trim().split("=");
                                if(keyValueProp[0].trim().equals("transactional") && keyValueProp[1].trim().equals("true")){
                                    currentTable.setTransactionalTable(true);
                                } else if(keyValueProp[0].trim().equals("label")){
                                    currentTable.setLabel(keyValueProp[1].trim());
                                }
                            }                        
                        }
                    } catch (Exception ex){
                        ex.printStackTrace(System.err);
                    }
                    
                    //System.err.println("->Start Table: "+attributes.getValue("name"));
                } else if (qName.equals("Model") && attributes.getValue("modelType").equals("DBColumn")) {
                    
                    if(currentTable.getColumn(attributes.getValue("name"))!=null){
                        currentColumn = null;
                        return;
                    }
                    currentColumn = new SimpleColumn();
                    columnPosition ++;
                    currentColumn.setPosition(columnPosition);
                    //System.err.println("->Start Column: "+attributes.getValue("name"));
                    currentColumn.setName(attributes.getValue("name"));
                    try{
                        if(this.vpModels.get(attributes.getValue("id")).getDocumentation() != null) {
                            String[] props = this.vpModels.get(attributes.getValue("id")).getDocumentation().split(";");
                            for(String prp: props) {
                                String[] keyValueProp = prp.trim().split("=");
                                if(keyValueProp[0].trim().equals("foreignDescription") && keyValueProp[1].trim().equals("true")){
                                    currentColumn.setForeignDescription(true);
                                } else if(keyValueProp[0].trim().equals("toStringConcatenable") && keyValueProp[1].trim().equals("true")){
                                    currentColumn.setToStringConcatenable(true);
                                } else if(keyValueProp[0].trim().equals("label")){
                                    currentColumn.setLabel(keyValueProp[1].trim());
                                } 
                            }                        
                        }
                    } catch (Exception ex){
                        ex.printStackTrace(System.err);
                    }
                } else if (qName.equals("ModelsProperty") && currentColumn!=null && attributes.getValue("name").equals("foreignKeyConstraints")) {
                    fkContraintsStart = true;
                } else if (qName.equals("Model") && attributes.getValue("modelType").equals("DBForeignKeyConstraint")) {
                    fkContraintDeclarationStart =  true;                    
                } else if (qName.equals("ModelRefProperty") && attributes.getValue("name").equals("foreignKey")&& fkContraintDeclarationStart && currentReferenceTable==null){
                    currentReferenceTable =  new ReferenceTable();
                } else if (qName.equals("ModelRef") && currentReferenceTable!=null && currentReferenceTable.getTableName()==null && currentReferenceTable.getColumnName()==null){
                    currentDBForeignKey = (DBForeignKey)vpModels.get(attributes.getValue("id"));
                    currentReferenceTable.setTableName(vpModels.get(currentDBForeignKey.getFrom()).getName());
                } else if (qName.equals("ModelRef") && currentReferenceTable!=null && currentReferenceTable.getTableName()!=null && currentReferenceTable.getColumnName()==null){
                    currentReferenceTable.setColumnName(vpModels.get(attributes.getValue("id")).getName());
                    currentColumn.setForeignKey(true);
                    currentTable.addForeignKey(currentColumn.getName(), currentReferenceTable);
                    //System.err.println("->"+currentTable.getName()+".add(FK:"+currentColumn.getName()+" -> "+currentReferenceTable+")");
                    currentReferenceTable = null;
                } else if (qName.equals("StringProperty") && currentColumn!=null && attributes.getValue("name").equals("type")){
                    
                    types.add(attributes.getValue("value"));
                    currentColumn.setSqlType(attributes.getValue("value"));
                    String jct = SQLTypesToJavaTypes.getTypeFor(attributes.getValue("value")); //javaTypes.get(attributes.getValue("value").toLowerCase());
                    if(jct == null){
                        throw new Exception("There is not java class type for : '"+attributes.getValue("value")+"'");
                    }
                    currentColumn.setJavaClassType(jct);
                } else if (qName.equals("BooleanProperty") && currentColumn!=null && attributes.getValue("name").equals("nullable")){
                    currentColumn.setNullable(attributes.getValue("value").equals("true"));
                } else if (qName.equals("StringProperty") && currentColumn!=null && attributes.getValue("name").equals("idGenerator") && attributes.getValue("value")!=null){
                    //System.err.println("\t\t("+attributes.getValue("value")+") found for "+currentTable.getName()+"."+currentColumn.getName());
                    if(attributes.getValue("value").equals("increment") || attributes.getValue("value").equals("identity")) {
                        currentColumn.setAutoIncremment(true);
                        //System.err.println("\t\t\t++ currentColumn.setAutoIncremment(true) = "+currentColumn.isAutoIncremment());
                    }
                    
                } else if (qName.equals("BooleanProperty") && currentColumn!=null && attributes.getValue("name").equals("primaryKey")){
                    currentColumn.setPrimaryKey(attributes.getValue("value").equals("true"));
                } else if (qName.equals("IntegerProperty") && currentColumn!=null && attributes.getValue("name").equals("length")){
                    currentColumn.setScale(Integer.parseInt(attributes.getValue("value")));
                } else if (qName.equals("IntegerProperty") && currentColumn!=null && attributes.getValue("name").equals("scale")){
                    currentColumn.setPrecision(Integer.parseInt(attributes.getValue("value")));
                } else if (qName.equals("ModelRefProperty") && attributes.getValue("name").equals("foreignKey") && currentColumn!=null) {
                    fqRef=true;
                } 
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void characters(char[] ch, int start, int length) {
                    
        }
        public void endElement(String uri, String localName, String qName) {
            try {
                if (qName.equals("Model") ) {
                    if ( fkContraintDeclarationStart) {
                        fkContraintDeclarationStart =  false;
                    } else if(currentTable!=null && currentColumn!=null){
                        currentColumn.fixBestJavaClassForSQLType();
                        currentTable.addColumn(currentColumn);
                        currentColumn = null;
                    } 
                } else if (qName.equals("ChildModels") && currentTable!=null) {
                    currentDBTableSet.addTable(currentTable);
                    currentTable = null;
                } else if (qName.equals("ModelsProperty") && currentColumn!=null ) {
                    fkContraintsStart = false;
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void endDocument() {
            Enumeration<Table> et = currentDBTableSet.getTablesElements();
            int nfks = 0;
            while(et.hasMoreElements()){
                Table ti = et.nextElement();
                nfks += ti.getFKs().size();
            }
            //System.err.println("->nfks ="+nfks);
            
        }
        //---------------------------------------------------------------
    }
    
    public static DBTableSet loadFromXML(InputStream is) {
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream ();
            ByteArrayInputStream  bais=null;

            byte[] buffer=new byte[1024];
            int r;
            while((r=is.read(buffer))!=-1){
                baos.write(buffer,0,r);
            }

            VP6Parser ps=new VP6Parser();
            Hashtable<String,VPModel> vpModels;
            bais=new ByteArrayInputStream (baos.toByteArray());
            vpModels = VP6Parser.loadVPModels(bais);
            DBTableSet dbSet = null;
            bais.reset();
            dbSet=VP6Parser.loadFromXMLWithVPModels(bais,vpModels);
            return dbSet;
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}