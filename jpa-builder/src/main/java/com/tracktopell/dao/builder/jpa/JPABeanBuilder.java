/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tracktopell.dao.builder.jpa;

import com.tracktopell.dao.builder.FormatString;
import com.tracktopell.dao.builder.metadata.Column;
import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.metadata.EmbeddeableColumn;
import com.tracktopell.dao.builder.metadata.ReferenceTable;
import com.tracktopell.dao.builder.metadata.Table;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author aegonzalez
 */
public class JPABeanBuilder {

    public static void buildMappingBeans(DBTableSet dbSet, String packageBeanMember, String schemmaName, String basePath)
            throws Exception {
        String fileName;
        File baseDir = null;
        File dirSourceFile = null;
        File sourceFile = null;

        FileOutputStream fos = null;
        PrintStream ps = null;
        BufferedReader br = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        String collectionClass = "Collection";

        Enumeration<String> tableNames = dbSet.getTableNames();
        ArrayList<Table> tablesForGeneration = new ArrayList<Table>();
        while (tableNames.hasMoreElements()) {
            Table simpleTable = dbSet.getTable(tableNames.nextElement());
            if (!simpleTable.isManyToManyTable()) {
                System.err.println("-->> + " + simpleTable.getName());
                tablesForGeneration.add(simpleTable);

                Iterator<Column> itFKC = simpleTable.getSortedColumnsForJPA();
                boolean addedAsFKEmbedded = false;
                while (itFKC.hasNext()) {
                    Column cctJpaC = itFKC.next();
                    if (cctJpaC instanceof EmbeddeableColumn) {
                        System.err.println("\t-->> + " + cctJpaC.getName());
                        tablesForGeneration.add((EmbeddeableColumn) cctJpaC);
                        addedAsFKEmbedded = true;
                    }
                }

                if (addedAsFKEmbedded) {
                }

            } else {
                //System.err.println("-->> [X] Many 2 Many : " + simpleTable.getName());
            }

        }
        //System.err.println("==============================>>> ");
        for (Table table : tablesForGeneration) {

            System.err.println("-->> generating: " + table.getJavaDeclaredName() + ".java :" +table);
            
            Iterator<Column> columnsSortedColumnsForJPA = table.getSortedColumnsForJPA();
            List<Column> definitiveColumns = new ArrayList();
            while (columnsSortedColumnsForJPA.hasNext()) {
                Column c = columnsSortedColumnsForJPA.next();
                definitiveColumns.add(c);
                System.err.println("\t-->> DefinitiveColumn: " + c);
            }

            //-------------------------------------------------------
            baseDir = new File(basePath);

            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            fileName = packageBeanMember.replace(".", File.separator) + File.separator;

            dirSourceFile = new File(baseDir.getPath() + File.separator + File.separator + fileName);
            if (!dirSourceFile.exists()) {
                dirSourceFile.mkdirs();
            }

            fileName = dirSourceFile.getPath() + File.separator + table.getJavaDeclaredName() + ".java";

            sourceFile = new File(fileName);
            fos = new FileOutputStream(sourceFile);
            ps = new PrintStream(fos);

            br = new BufferedReader(new InputStreamReader(
                    fos.getClass().getResourceAsStream("/templates/TableJPABean.java.template")));
            String line = null;
            ArrayList<String> linesToParse = null;
            int nl = 0;
            while ((line = br.readLine()) != null) {

                if (line.indexOf("%foreach") >= 0) {
                    linesToParse = new ArrayList<String>();
                } else if (line.indexOf("%endfor") >= 0) {                    
                    int numColumnGenerating = 0;
                    
                    for(Column column: definitiveColumns){
                        numColumnGenerating++;
                        
                        Table fTable = null;
                        String refObjFK = null;
                        
                        if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
                            fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
                        } else {
                            fTable = null;
                        }


                        for (String lineInLoop : linesToParse) {
                            if (lineInLoop.indexOf("${tablebean.member.javadocCommnet}") >= 0) {
                                if (!table.isManyToManyTableWinthMoreColumns()) {
                                    if (column.getComments() != null) {
                                        ps.println("    ");
                                        ps.println("    /**");
                                        ps.println("    * " + column.getComments().replace("\n", "\n     * "));
                                        ps.println("    */");
                                    } else {
                                        String commentForced = column.getName().toLowerCase().replace("_", " ");
                                        ps.println("    ");
                                        ps.println("    /**");
                                        ps.println("    * " + commentForced);
                                        ps.println("    */");
                                    }
                                }
                            } else if (lineInLoop.indexOf("${tablebean.member.declaration}") >= 0) {
                                
                                if (table.isManyToManyTableWinthMoreColumns()) {

                                    if (column instanceof EmbeddeableColumn) {
                                        ps.println("    @EmbeddedId");                                        
                                        ps.println("    private " + column.getJavaClassType().replace("java.lang.", "")
                                                + " " + column.getJavaDeclaredObjectName() + ";");
                                    } else if (fTable != null) {                                        
                                        //refObjFK = FormatString.getCadenaHungara(fTable.getName());
                                        ps.println("    @JoinColumn(name = \"" + column.getName().toUpperCase()
                                                + "\" , referencedColumnName = \"" + table.getFKReferenceTable(column.getName()).getColumnName().toUpperCase() + "\", "
                                                + " insertable = false, updatable = false)");                                        
                                        ps.println("    @ManyToOne(optional = " + column.isNullable() + ")");
                                        //ps.println("    private " + refObjFK + " " + FormatString.firstLetterLowerCase(refObjFK) + ";");
                                        ps.println("    private " + fTable.getJavaDeclaredName() + " " + fTable.getJavaDeclaredObjectName() + ";");
                                    } else {
                                        ps.println("    @Basic(optional = " + column.isNullable() + ")");
                                        ps.println("    @Column(name = \"" + column.getName().toUpperCase() + "\")");
                                        //if(column.getJavaClassType().equals("java.util.Date")){
                                        if(column.getSqlType().toLowerCase().equals("timestamp") || column.getSqlType().toLowerCase().equals("datetime")){
                                            ps.println("    @Temporal(TemporalType.TIMESTAMP)");
                                        } 
                                        ps.println("    private " + column.getJavaClassType().replace("java.lang.", "")
                                                + " " + column.getJavaDeclaredObjectName() + ";");
                                    }
                                } else {
                                    if (column.isPrimaryKey() && !column.isForeignKey()) {
                                        if (column instanceof EmbeddeableColumn) {
                                            ps.println("    @EmbeddedId");
                                        } else {
                                            if(! (table instanceof EmbeddeableColumn)){
                                                ps.println("    @Id");
                                            }
                                            ps.println("    @Basic(optional = false)");
                                            ps.println("    @Column(name = \"" + column.getName().toUpperCase() + "\")");
                                            if (column.isAutoIncremment()) {
                                                //ps.println("    @GeneratedValue(strategy=GenerationType.IDENTITY)");
                                                ps.println("    @GeneratedValue(strategy=GenerationType.AUTO)");
                                            }
                                        }
                                        if(column.getJavaClassType().equals("java.util.Date")){
                                            ps.println("    @Temporal(TemporalType.TIMESTAMP)");
                                        } else if(column.getJavaClassType().equals("java.util.Calendar")){
                                            ps.println("    @Temporal(TemporalType.DATE)");
                                        }
                                        ps.println("    private " + column.getJavaClassType().replace("java.lang.", "")
                                                + " " + column.getJavaDeclaredObjectName() + ";");
                                    } else{ 
                                        if (fTable != null) {
                                            
                                            //ps.println("    // (insertable = false, updatable = false) FIX ?"+table.hasEmbeddedPK()+" && "+column.isPrimaryKey());
                                            ps.print("    @JoinColumn(name = \"" + column.getName().toUpperCase() + "\" , referencedColumnName = \"" + table.getFKReferenceTable(column.getName()).getColumnName().toUpperCase() + "\"");
                                            if (table.hasEmbeddedPK() && column.isPrimaryKey()) {
                                                ps.println(", insertable = false, updatable = false)");
                                            } else {
                                                ps.println(")");
                                            }
                                            ps.println("    @ManyToOne(optional = " + column.isNullable() + ")");
                                            //ps.println("    private " + refObjFK + " " + FormatString.firstLetterLowerCase(refObjFK) + ";");
                                            ps.println("    private " + fTable.getJavaDeclaredName() + " " + fTable.getJavaDeclaredObjectName() + ";");
                                        } else {
                                            ps.println("    @Basic(optional = " + column.isNullable() + ")");
                                            ps.println("    @Column(name = \"" + column.getName().toUpperCase() + "\")");
                                            if(column.getJavaClassType().equals("java.util.Date")){
                                                ps.println("    @Temporal(TemporalType.TIMESTAMP)");
                                            } else if(column.getJavaClassType().equals("java.util.Calendar")){
                                                ps.println("    @Temporal(TemporalType.DATE)");
                                            }
                                            ps.println("    private " + column.getJavaClassType().replace("java.lang.", "")
                                                    + " " + column.getJavaDeclaredObjectName() + ";");
                                        }
                                    }
                                }

                            } else if (lineInLoop.indexOf("${tablebean.member.getter}") >= 0) {
                                if (fTable!=null && !table.getName().toUpperCase().endsWith("_P_K")) {
                                    
                                    refObjFK = fTable.getJavaDeclaredName();
                                    
                                    if (table instanceof EmbeddeableColumn) {
                                        refObjFK = column.getJavaClassType().replace("java.lang.", "");
                                    } 

                                    ps.println("    public " + refObjFK + " get" + fTable.getJavaDeclaredName() + "() {");
                                    if (table instanceof EmbeddeableColumn && column.isForeignKey()) {
                                        ps.println("        return this." + column.getJavaDeclaredObjectName() + ";");
                                    } else {
                                        ps.println("        return this." + fTable.getJavaDeclaredObjectName() + ";");
                                    }
                                    ps.println("    }");
                                } else {
                                    ps.println("    public " + column.getJavaClassType().replace("java.lang.", "")
                                            + " get" + column.getJavaDeclaredName() + "() {");
                                    ps.println("        return this." + column.getJavaDeclaredObjectName() + ";");
                                    ps.println("    }");
                                }
                            } else if (lineInLoop.indexOf("${tablebean.member.setter}") >= 0) {
                                if (fTable!=null && !table.getName().toUpperCase().endsWith("_P_K")) {
                                    
                                    refObjFK = fTable.getJavaDeclaredName();
                                    
                                    if (table instanceof EmbeddeableColumn) {
                                        refObjFK = column.getJavaClassType().replace("java.lang.", "");
                                    }

                                    ps.println("    public void set" + fTable.getJavaDeclaredName() + "(" + refObjFK + " v) {");
                                    
                                    if (table instanceof EmbeddeableColumn && column.isForeignKey()) {
                                        ps.println("        this." + column.getJavaDeclaredObjectName() + " = v;");
                                    } else {
                                        ps.println("        this." + fTable.getJavaDeclaredObjectName()+ " = v;");
                                    }
                                    ps.println("    }");
                                } else {
                                    ps.println("    public void set" + FormatString.getCadenaHungara(column.getName())
                                            + "(" + column.getJavaClassType().replace("java.lang.", "") + " v) {");
                                    ps.println("        this." + column.getJavaDeclaredObjectName() + " = v;");
                                    ps.println("    }");
                                }
                            } else {
                                ps.println(lineInLoop);
                            }
                        }
                    }

                    linesToParse = null;
                } else if (linesToParse != null) {
                    linesToParse.add(line);
                } else if (line.indexOf("${tablebean.oneToManyRelations.declarations}") >= 0) {
                    for (Table posibleTableOneToMany : tablesForGeneration) {
                        Collection<ReferenceTable> fKReferenceTables4OneToMany = posibleTableOneToMany.getFKReferenceTables();
                        for (ReferenceTable rt4OneToMany : fKReferenceTables4OneToMany) {
                            if (rt4OneToMany.getTableName().equals(table.getName())
                                    && !FormatString.getCadenaHungara(posibleTableOneToMany.getName()).endsWith("PK")) {

                                String tableReferenceOneToMany = "null";
                                Collection<Column> fks = posibleTableOneToMany.getFKs();

                                for (Column cfk : fks) {
                                    if (posibleTableOneToMany.getFKReferenceTable(cfk.getName()).getTableName().equals(table.getName())) {
                                        tableReferenceOneToMany = FormatString.renameForJavaMethod(posibleTableOneToMany.getFKReferenceTable(cfk.getName()).getTableName());
                                    }
                                }

                                ps.println("    ");
                                ps.println("    @OneToMany(cascade = CascadeType.ALL, mappedBy = \"" + tableReferenceOneToMany + "\")");
                                ps.println("    private " + collectionClass + "<" + FormatString.getCadenaHungara(posibleTableOneToMany.getName()) + "> " + FormatString.renameForJavaMethod(posibleTableOneToMany.getName()) + collectionClass + ";");
                                ps.println("    ");
                            }
                        }
                    }
                } else if (line.indexOf("${tablebean.oneToManyRelations.gettersAndSetters}") >= 0) {
                    for (Table posibleTableOneToMany : tablesForGeneration) {
                        Collection<ReferenceTable> fKReferenceTables4OneToMany = posibleTableOneToMany.getFKReferenceTables();
                        for (ReferenceTable rt4OneToMany : fKReferenceTables4OneToMany) {
                            if (rt4OneToMany.getTableName().equals(table.getName())
                                    && !FormatString.getCadenaHungara(posibleTableOneToMany.getName()).endsWith("PK")) {
                                Table fTable = dbSet.getTable(posibleTableOneToMany.getName());
                                //String refObjFK = FormatString.getCadenaHungara(fTable.getName());
                                ps.println("    ");
                                ps.println("    public " + collectionClass + "<" + FormatString.getCadenaHungara(posibleTableOneToMany.getName()) + "> get" + fTable.getJavaDeclaredName() + collectionClass + "() {");
                                //ps.println("        return this." + FormatString.firstLetterLowerCase(refObjFK) + collectionClass + ";");
                                ps.println("        return this." + fTable.getJavaDeclaredObjectName() + collectionClass + ";");
                                ps.println("    }");
                                ps.println("    ");
                                ps.println("    ");
                                ps.println("    public void set" + fTable.getJavaDeclaredName() + collectionClass + "(" + collectionClass + "<" + FormatString.getCadenaHungara(posibleTableOneToMany.getName()) + ">  v) {");
                                //ps.println("        this." + FormatString.firstLetterLowerCase(refObjFK) + collectionClass + " = v;");
                                ps.println("        this." + fTable.getJavaDeclaredObjectName() + collectionClass + " = v;");
                                ps.println("    }");
                            }
                        }
                    }
                } else if (line.indexOf("${tablebean.ManyToManyRelations.declarations}") >= 0) {

                    Collection<Table> m2mTables = dbSet.getManyToManyRelationTables(table);

                    for (Table fm2mTable : m2mTables) {

                        //System.err.println("\t\t-->>@ManyToMany:" + fm2mTable.getName());

                        Table tableOwnerManyToManyRelation = dbSet.getTableOwnerManyToManyRelation(table, fm2mTable);
                        Iterator<Column> fKsM2M = tableOwnerManyToManyRelation.getFKs().iterator();

                        Column rtCol1 = fKsM2M.next();
                        Column rtCol2 = fKsM2M.next();

                        ps.println("    ");
                        if (tableOwnerManyToManyRelation.getFKReferenceTable(rtCol1.getName()).getTableName().equals(table.getName())) {
                            ps.println("    @JoinTable(name               = \"" + tableOwnerManyToManyRelation.getName().toUpperCase() + "\",");
                            ps.println("               joinColumns        = {@JoinColumn(name = \"" + rtCol1.getName().toUpperCase() + "\", referencedColumnName =\"" + tableOwnerManyToManyRelation.getFKReferenceTable(rtCol1.getName()).getColumnName().toUpperCase() + "\")},");
                            ps.println("               inverseJoinColumns = {@JoinColumn(name = \"" + rtCol2.getName().toUpperCase() + "\", referencedColumnName =\"" + tableOwnerManyToManyRelation.getFKReferenceTable(rtCol2.getName()).getColumnName().toUpperCase() + "\")}");
                            ps.println("               )");

                            ps.println("    @ManyToMany");
                        } else {
                            //ps.println("    @ManyToMany(cascade= CascadeType.ALL,mappedBy = \"" + FormatString.renameForJavaMethod(table.getName()) + collectionClass + "\")");
                            ps.println("    @ManyToMany(mappedBy = \"" + FormatString.renameForJavaMethod(table.getName()) + collectionClass + "\")");

                        }

                        ps.println("    private " + collectionClass + "<" + FormatString.getCadenaHungara(fm2mTable.getName()) + "> " + FormatString.renameForJavaMethod(fm2mTable.getName()) + collectionClass + ";");
                        ps.println("    ");
                    }

                } else if (line.indexOf("${tablebean.ManyToManyRelations.gettersAndSetters}") >= 0) {
                    Collection<Table> m2mTables = dbSet.getManyToManyRelationTables(table);

                    for (Table fm2mTable : m2mTables) {
                        ps.println("    // Getter and Setters @ManyToMany Collection<" + FormatString.getCadenaHungara(fm2mTable.getName()) + ">");
                        ps.println("    ");
                        ps.println("    public " + collectionClass + "<" + FormatString.getCadenaHungara(fm2mTable.getName()) + "> get" + FormatString.getCadenaHungara(fm2mTable.getName()) + collectionClass + "() {");
                        ps.println("        return this." + FormatString.renameForJavaMethod(fm2mTable.getName()) + collectionClass + ";");
                        ps.println("    }");
                        ps.println("    ");
                        ps.println("    ");
                        ps.println("    public void set" + FormatString.getCadenaHungara(fm2mTable.getName()) + collectionClass + "(" + collectionClass + "<" + FormatString.getCadenaHungara(fm2mTable.getName()) + ">  v) {");
                        ps.println("        this." + FormatString.renameForJavaMethod(fm2mTable.getName()) + collectionClass + " = v;");
                        ps.println("    }");
                    }

                } else {
                    line = line.replace("${date}", sdf.format(new Date()));
                    line = line.replace("${tablebean.serialId}", String.valueOf(table.hashCode()));
                    line = line.replace("${tablebean.name}", table.getName());
                    line = line.replace("${tablebean.declaredName}", table.getJavaDeclaredName());
                    line = line.replace("${tablebean.PKMembersParameters}", membersParameters(table,dbSet));

                    if (table instanceof EmbeddeableColumn) {
                        line = line.replace("${tablebean.jpa_entity_or_embeddeable}", "@Embeddable");
                        line = line.replace("${tablebean.jpa_talbe}", "");
                    } else {
                        line = line.replace("${tablebean.jpa_entity_or_embeddeable}", "@Entity");
                        line = line.replace("${tablebean.jpa_talbe}", "@Table(name = \"" + table.getName().toUpperCase() + "\")");
                        line = line.replace("${tablebean.id}", table.getJPAPK());
                        line = line.replace("${tablebean.id.javaClass}", table.getJPAPKClass().replace("java.lang.", ""));
                    }

                    line = line.replace("${tablebean.hashCodeSumCode}", table.getHashCodeSumCode());
                    line = line.replace("${tablebean.PKMembersParametersInitCode}", membersParametersInitCode(table,dbSet));
                    line = line.replace("${tablebean.equalsCode}", table.getEqualsCode());
                    line = line.replace("${tablebean.toStringCode}", table.getToStringCode(packageBeanMember));
                    line = line.replace("${tablebean.name.uc}", table.getName().toUpperCase());                    
                    line = line.replace("${tablebean.package}", packageBeanMember);
                    ps.println(line);
                }
            }
            //-------------------------------------------------------
            ps.close();
            fos.close();

            sourceFile = null;
            ps = null;
            fos = null;
        }
    }

    private static String membersParameters(Table table, DBTableSet dbSet) {
        StringBuffer sb = new StringBuffer();

        String varName = null;
        String varClassName = null;
        String refObjFK = null;
        
        Iterator<Column> simpleColumnsIterator = table.getSortedColumnsForJPA();
        
        for(int numColumnGenerating = 0;simpleColumnsIterator.hasNext();numColumnGenerating++) {
            
            Column column = simpleColumnsIterator.next();
            if(! column.isPrimaryKey()) {
                continue;
            }
            
            if(numColumnGenerating>0){
                sb.append(", ");
            }
            
            if (column.isForeignKey() && !table.isManyToManyTable() && !table.getName().toUpperCase().endsWith("_P_K")) {
                Table fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
                if (table instanceof EmbeddeableColumn) {
                    refObjFK = column.getJavaClassType().replace("java.lang.", "");
                } else {
                    refObjFK = FormatString.getCadenaHungara(fTable.getName());
                }
                varClassName = refObjFK;
                
                if (table instanceof EmbeddeableColumn && column.isForeignKey()) {
                    varName = column.getJavaDeclaredObjectName();
                    
                } else {
                    varName = fTable.getJavaDeclaredObjectName();
                }
            } else {
                varClassName = column.getJavaClassType().replace("java.lang.", "");
                varName = column.getJavaDeclaredObjectName();
            }
            
            sb.append(varClassName);
            sb.append(" " );
            sb.append(varName);
        }

        return sb.toString();
    }

    private static String membersParametersInitCode(Table table, DBTableSet dbSet) {
        StringBuffer sb = new StringBuffer();

        String varName = null;
        String varClassName = null;
        String refObjFK = null;
        
        Iterator<Column> simpleColumnsIterator = table.getSortedColumnsForJPA();
        
        for(int numColumnGenerating = 0;simpleColumnsIterator.hasNext();numColumnGenerating++) {
            
            Column column = simpleColumnsIterator.next();
            if(! column.isPrimaryKey()) {
                continue;
            }
            
            if(numColumnGenerating>0){
                sb.append("        ");
            }
            
            if (column.isForeignKey() && !table.isManyToManyTable() && !table.getName().toUpperCase().endsWith("_P_K")) {
                Table fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
                if (table instanceof EmbeddeableColumn) {
                    refObjFK = column.getJavaClassType().replace("java.lang.", "");
                } else {
                    refObjFK = FormatString.getCadenaHungara(fTable.getName());
                }
                varClassName = refObjFK;
                
                if (table instanceof EmbeddeableColumn && column.isForeignKey()) {
                    varName = column.getJavaDeclaredObjectName();
                    
                } else {
                    varName = fTable.getJavaDeclaredObjectName();
                }
            } else {
                varClassName = column.getJavaClassType().replace("java.lang.", "");
                varName = column.getJavaDeclaredObjectName();
            }
            
            sb.append("this.");
            sb.append(varName);
            sb.append(" \t= \t");            
            sb.append(varName);
            sb.append(";\n");
        }

        return sb.toString();
    }

    static void updatePersistenceXML(DBTableSet dbSet, String packageBeanMember, String path_2_Parsistence_xml) {

        Enumeration<String> tableNames = dbSet.getTableNames();
        ArrayList<Table> tablesForGeneration = new ArrayList<Table>();
        while (tableNames.hasMoreElements()) {
            Table simpleTable = dbSet.getTable(tableNames.nextElement());
            if (!simpleTable.isManyToManyTable()) {
                //System.err.println("-->> + " + simpleTable.getName());
                tablesForGeneration.add(simpleTable);

                Iterator<Column> itFKC = simpleTable.getSortedColumnsForJPA();
                boolean addedAsFKEmbedded = false;
                while (itFKC.hasNext()) {
                    Column cctJpaC = itFKC.next();
                    if (cctJpaC instanceof EmbeddeableColumn) {
                        //System.err.println("\t-->> + " + cctJpaC.getName());
                        tablesForGeneration.add((EmbeddeableColumn) cctJpaC);
                        addedAsFKEmbedded = true;
                    }
                }

                if (addedAsFKEmbedded) {
                }
            } else {
                //System.err.println("-->> [X] Many 2 Many : " + simpleTable.getName());
            }

        }
        //System.err.println("==============================>>> ");

        InputStream is = null;
        BufferedReader br = null;
        try {
            is = new FileInputStream(path_2_Parsistence_xml);
            br = new BufferedReader(new InputStreamReader(is));

            String line = null;

            while ((line = br.readLine()) != null) {
                if (line.indexOf("${InsertDeclaration4JPABeans}") >= 0) {
                    for (Table table : tablesForGeneration) {
                        System.out.println("\t\t<class>" + packageBeanMember + "." + table.getJavaDeclaredName() + "</class>");
                    }
                } else {
                    System.out.println(line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
    }
}