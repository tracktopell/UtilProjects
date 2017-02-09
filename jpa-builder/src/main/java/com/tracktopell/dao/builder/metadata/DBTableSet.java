/*
 * DBTableSet.java
 *
 */

package com.tracktopell.dao.builder.metadata;

import com.tracktopell.dao.builder.FormatString;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Usuario
 */
public class DBTableSet {
    private Hashtable<String,Table> tables;
    
    /** Creates a new instance of DBTableSet */
    public DBTableSet() {
    }

    public DBTableSet copyJustSelectedNames(String[] namesSelected){
        DBTableSet copy;
        
        copy= new DBTableSet();
        int i;
        for(i=0;i<namesSelected.length;i++){
			Collection<Table> tv = tables.values();
			for(Table t: tv) {
				if(t.getName().trim().equalsIgnoreCase(namesSelected[i])){
					copy.addTable(t);
				}
			}
            //if(this.getTables().get(namesSelected[i])!=null)
            //    copy.addTable(this.getTables().get(namesSelected[i]));
        }
        
        return copy;
    }
    
    public void addTable(Table table){            
        this.getTables().put(table.getName(),table);
    }    
    
    public Enumeration<String> getTableNames(){
        return this.getTables().keys();
    }
	
	public Set<String> getTableNamesSet(){
        return this.getTables().keySet();
    }
    
    public Table getTable(String tableumnName){
        return this.getTables().get(tableumnName);
    } 

    public Enumeration<Table> getTablesElements(){
        return this.getTables().elements();
    }
    
    public List<Table> getTablesSortedForCreation(){        
        ArrayList<Table> result =  new ArrayList<Table>();
        Enumeration<Table> te = this.tables.elements();
        Table iterTable = null;
        while (te.hasMoreElements()){
            iterTable = te.nextElement();
            if(iterTable.countForeignKeys()==0)
                result.add(iterTable);
        }
        
        te = this.tables.elements();        
        while (te.hasMoreElements()){
            iterTable = te.nextElement();
            if(iterTable.countForeignKeys()>0)
                result.add(iterTable);
        }

        return result;        
    } 
    
    
    private Hashtable<String,Table> getTables(){
        if(this.tables == null){
            this.tables = new Hashtable<String,Table>();
        }
        return this.tables;
    }
    
    public Column getFinalJoinedColumn(Table rootTable,String fkColumn){
        ReferenceTable ref=null;
        Table parentTable = rootTable;
        ref=parentTable.getFKReferenceTable(fkColumn);
        while(ref!=null){            
            parentTable=getTable(ref.getTableName());
            if(parentTable.getColumn(fkColumn)!=null){
                if(parentTable.getColumn(fkColumn).isPrimaryKey() && 
                      ( parentTable.getFKReferenceTable(fkColumn)==null || 
                        parentTable.equals(rootTable) ) ){
                    return parentTable.getColumn(fkColumn);
                }
                if(parentTable.getFKReferenceTable(ref.getColumnName())!=null)
                    ref=parentTable.getFKReferenceTable(ref.getColumnName());
            }
        }
        return null;
    }
    
    public String getListForeignDescriptionColumnNames(Table targetTable){
        StringBuffer sb = new StringBuffer("");
        
        Collection<ReferenceTable> refFK = targetTable.getFKReferenceTables();
        int ftkn=0;
        for(ReferenceTable rt: refFK) {
            Table refTable = getTable(rt.getTableName());
            Collection<Column> forDesCols = refTable.getForeignDescriptionColumns();
            for(Column desCol: forDesCols){
                if(ftkn>0){
                    sb.append(", ");
                }
                sb.append(refTable.getName());
                sb.append(".");
                sb.append(desCol.getName());
                sb.append(" as ");
                sb.append(refTable.getName());
                sb.append("_");
                sb.append(desCol.getName());
                
                ftkn++;
            }
        }
        return sb.toString();
    }
    
    public String getListFKJoinExpressions(Table targetTable){
        StringBuffer sb = new StringBuffer();
        
        Enumeration<String> eFks = targetTable.getFKColumnNames();
        int ftkn=0;
        while(eFks.hasMoreElements()) {
            String fkColName = eFks.nextElement();
            ReferenceTable rt = targetTable.getFKReferenceTable(fkColName);
            if(ftkn>0) sb.append(" AND ");
            
            sb.append(targetTable.getName());
            sb.append(".");
            sb.append(fkColName);
            sb.append(" = ");
            sb.append(rt.getTableName());
            sb.append(".");
            sb.append(rt.getColumnName());
            ftkn++;
        }
        return sb.toString();
    }
    
    public String toString(){
        StringBuffer sb = new StringBuffer();
        
        Enumeration<String> et=getTableNames();
        sb.append("Tables {\n");
        while(et.hasMoreElements()){            
            Table t = getTable(et.nextElement());
            sb.append(t.toString());            
            sb.append("\n");            
        }
        sb.append("}\n");
        
        return sb.toString();
    }
    
    public void buildPosibleLabel() {
        Collection<Table> ct = tables.values();
        Iterator<Table> it=ct.iterator();
        
        while(it.hasNext()){            
            Table t=it.next();
            t.buildPosibleLabel();
        }
    }
    
    public Collection<Table> getManyToManyRelationTables(Table t) {
        ArrayList<Table> m2mTables = new ArrayList<Table>();
        
        //System.err.println("\t-->>getManyToManyRelationTables: "+t.getName());
        Enumeration<Table> relatedTablesEnumeration = tables.elements();

        while(relatedTablesEnumeration.hasMoreElements()) {
            Table relatedTable = relatedTablesEnumeration.nextElement();

            if(relatedTable.isManyToManyTable()) {
                //System.err.println("\t\t-->>M2M :  RelatedTable :"+relatedTable.getName());
                Collection<Column> ffks = relatedTable.getFKs();
                boolean m2mPontsHere = false;
                Table farTableM2M = null;
                for(Column ffkc: ffks) {
                    Table ftm2m = getTable(relatedTable.getFKReferenceTable(ffkc.getName()).getTableName());
                    if(ftm2m.getName().equals(t.getName())) {
                        m2mPontsHere = true;
                    }
                    if(! ftm2m.getName().equals(t.getName())) {

                        farTableM2M = ftm2m;
                        
                    }
                }
                if( m2mPontsHere ) {
                    //System.err.println("\t\t\t\t-->>M2M : "+t.getName()+" --<>-- "+farTableM2M.getName());
                    m2mTables.add(farTableM2M);
                }
            }

        }
        
        return m2mTables;
    }

    public Table getTableOwnerManyToManyRelation(Table t1,Table t2) {
        Table m2mTalbeOwner = null;
        Enumeration<Table> relatedTablesEnumeration = tables.elements();

        while(relatedTablesEnumeration.hasMoreElements()) {
            Table relatedTable = relatedTablesEnumeration.nextElement();

            if(relatedTable.isManyToManyTable()) {
                Collection<Column> ffks = relatedTable.getFKs();
                boolean m2mPointsT1 = false;
                boolean m2mPointsT2 = false;
                for(Column ffkc: ffks) {
                    Table ftm2m = getTable(relatedTable.getFKReferenceTable(ffkc.getName()).getTableName());
                    if(ftm2m.getName().equals(t1.getName())) {
                        m2mPointsT1 = true;
                    } else if(ftm2m.getName().equals(t2.getName())) {
                        m2mPointsT2 = true;
                    }
                }
                if( m2mPointsT1 && m2mPointsT2 ) {
                    m2mTalbeOwner = relatedTable;
                }
            }

        }

        return m2mTalbeOwner;
    }

    public void resolveFarFKDescription() {
        
        Enumeration<Table> et = this.getTablesElements();
        
        while(et.hasMoreElements()) {
            Table ti = et.nextElement();
            Table rt = null;
            Enumeration<String> fkne = ti.getFKColumnNames();
            while(fkne.hasMoreElements()){
                String fkn = fkne.nextElement();
                ReferenceTable rrt = ti.getFKReferenceTable(fkn);
                
                rt = getTable(rrt.getTableName());
                Collection<Column> rcc = rt.getForeignDescriptionColumns();
                StringBuffer sbfk = new StringBuffer(FormatString.firstLetterLowerCase(rt.getName()));
                int ic = 0;
                for(Column dc : rcc) {
                    sbfk.append("_");
                    
                    sbfk.append(FormatString.firstLetterLowerCase(dc.getName()));
                    ic++;
                }
                ti.getColumn(fkn).setFarFKDescription(sbfk.toString());
            }             
        }
        
    }
	
	public String getTableToStringConcatenable(Table table) {
        
        Iterator<Column> simpleColumnsIterator = table.getSortedColumnsForJPA();
        int numPrpertiesAdded = 0;
		StringBuilder sb= new StringBuilder();
		
        while (simpleColumnsIterator.hasNext()) {
            Column c = simpleColumnsIterator.next();
            if (c.isToStringConcatenable()) {
				if(numPrpertiesAdded > 0){
					sb.append(" + \", \" + ");
				}
				String jpaClass = c.getJavaClassType();
				
				if (!(table instanceof EmbeddeableColumn) && jpaClass.equals("double") || jpaClass.equals("int") || jpaClass.equals("float") || jpaClass.equals("char") || jpaClass.equals("byte")) {
					final ReferenceTable fkReferenceTable = table.getFKReferenceTable(c.getName());
					if(fkReferenceTable != null) {
						Table fTable = getTable(fkReferenceTable.getTableName());
						if(fTable != null){
							sb.append(fTable.getJavaDeclaredObjectName());                    
						}
					} else {
						sb.append(FormatString.renameForJavaMethod(c.getName()));
					}
                    
                } else {
                    sb.append(FormatString.renameForJavaMethod(c.getName()));
				}
                
				numPrpertiesAdded++;
            }
        }
        return sb.toString();
    }
	
	public String getTableToStringDTOConcatenable(Table table) {
        
        Iterator<Column> simpleColumnsIterator = table.getSortedColumnsForJPA();
        int numPrpertiesAdded = 0;
		StringBuilder sb= new StringBuilder();
		
        while (simpleColumnsIterator.hasNext()) {
            Column c = simpleColumnsIterator.next();
            if (c.isToStringConcatenable()) {
				if(numPrpertiesAdded > 0){
					sb.append(" + \", \" + ");
				}
				String jpaClass = c.getJavaClassType();
				
				if (!(table instanceof EmbeddeableColumn) && jpaClass.equals("double") || jpaClass.equals("int") || jpaClass.equals("float") || jpaClass.equals("char") || jpaClass.equals("byte")) {
					final ReferenceTable fkReferenceTable = table.getFKReferenceTable(c.getName());
					if(fkReferenceTable != null) {
						Table fTable = getTable(fkReferenceTable.getTableName());
						if(fTable != null){
							//sb.append(fTable.getJavaDeclaredObjectName());                    
							sb.append(c.getJavaDeclaredObjectName());
						}
					} else {
						sb.append(FormatString.renameForJavaMethod(c.getName()));
					}
                    
                } else {
                    sb.append(FormatString.renameForJavaMethod(c.getName()));
				}
                
				numPrpertiesAdded++;
            }
        }
        return sb.toString();
    }


}
