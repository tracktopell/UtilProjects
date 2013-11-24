/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tracktopell.dao.builder.grails;

import com.tracktopell.dao.builder.jpa.*;
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
public class DomainClassBuilder {

	static String collectionClass = "Collection";
	static boolean generateCustomMappingM2M = false;

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
		List<String> mappingDeclarations;
		List<String> constraintsDeclarations;

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
				////System.err.println("-->> [X] Many 2 Many : " + simpleTable.getName());
			}

		}
		////System.err.println("==============================>>> ");
		System.err.println("-->tables generate:[");
		int ncg = 0;
		for (Table table : tablesForGeneration) {
			if (ncg > 0) {
				System.err.print(",");
			}
			System.err.print(table.getName().toLowerCase());
			ncg++;
		}
		System.err.println("]");

		for (Table table : tablesForGeneration) {
			if (table instanceof EmbeddeableColumn) {
				System.err.println("-->> TEMPORAL skiping generation for " + table.getJavaDeclaredName() + ".groovy, because instanceof EmbeddeableColumn?" + (table instanceof EmbeddeableColumn));
				continue;
			}
			System.err.println("-->> generating: " + table.getJavaDeclaredName() + ".groovy");
			if (table.hasEmbeddedPK()) {
				System.err.println("\t-->> with Embedded: " + table);
			}

//			Iterator<Column> columnsSortedColumnsForJPA = table.getSortedColumnsForJPA();
			Iterator<Column> columnsSortedColumnsForJPA = table.getSortedColumns();
			List<Column> definitiveColumns = new ArrayList();

			mappingDeclarations		= new ArrayList<String>();
			constraintsDeclarations = new ArrayList<String>();
			
			while (columnsSortedColumnsForJPA.hasNext()) {
				Column c = columnsSortedColumnsForJPA.next();
				definitiveColumns.add(c);
				if (table.hasEmbeddedPK()) {
					System.err.println("\t\t-->> DefinitiveColumn: " + c);
				}
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

			fileName = dirSourceFile.getPath() + File.separator + table.getJavaDeclaredName() + ".groovy";

			sourceFile = new File(fileName);
			fos = new FileOutputStream(sourceFile);
			ps = new PrintStream(fos);

			br = new BufferedReader(new InputStreamReader(
					fos.getClass().getResourceAsStream("/templates/TableGrailsDomainClass.groovy.template")));
			String line = null;
			ArrayList<String> linesToParse = null;
			int nl = 0;
			while ((line = br.readLine()) != null) {

				if (line.indexOf("${tablebean.simpleMember.declaration}") >= 0) {
					boolean pkIdShuldBeRemapped = false;
					Column pkToRemapped = null;
					for (Column column : definitiveColumns) {
						if (column.isPrimaryKey()
								&& table.getPrimaryKeys().size() == 1
								&& (!column.getName().equalsIgnoreCase("id") || (!column.isAutoIncremment() && !table.hasEmbeddedPK()))) {

							pkIdShuldBeRemapped = true;
							pkToRemapped = column;
							ps.println("\t// pk Id="+pkToRemapped.getName()+", type="+pkToRemapped.getJavaClassType()+", SQLType="+pkToRemapped.getSqlType()+", Precision="+pkToRemapped.getPrecision()+", Scale="+pkToRemapped.getScale()+", Shuld be remapped !!");
						}
						if (table.hasEmbeddedPK()) {
							if (column.isForeignKey()) {
								ReferenceTable fkReferenceTable = table.getFKReferenceTable(column.getName());
								if (fkReferenceTable != null) {
									ps.println("\t" + FormatString.getCadenaHungara(fkReferenceTable.getTableName()) + " " + FormatString.renameForJavaMethod(fkReferenceTable.getTableName()));
								}
							} else if (column.getJavaClassType().equals("byte[]")) {
								ps.println("\t" + column.getJavaClassType().replace("java.lang.", "") + " " + column.getJavaDeclaredObjectName());
								//mappingDeclarations.add(column.getJavaDeclaredObjectName() + " maxSize: 16777216// 16MB => Longblob's mysql ?");
							} else {
								ps.println("\t" + column.getJavaClassType().replace("java.lang.", "") + " " + column.getJavaDeclaredObjectName());
							}

						} else {
							if (column.getJavaClassType().equals("byte[]")) {
								ps.println("\t" + column.getJavaClassType().replace("java.lang.", "") + " " + column.getJavaDeclaredObjectName());
								//mappingDeclarations.add(column.getJavaDeclaredObjectName() + " maxSize: 16777216// 16MB => Longblob's mysql ?");
							} else if (!column.isForeignKey() && !(column.isPrimaryKey() && column.isAutoIncremment())) {
								ps.println("\t" + column.getJavaClassType().replace("java.lang.", "") + " " + column.getJavaDeclaredObjectName());
							}
						}
						int cd = 0;
						StringBuffer columnCosntrain = new StringBuffer();
						ReferenceTable fkReferenceTable = table.getFKReferenceTable(column.getName());
						
						if(!column.isForeignKey() ) {
							
							columnCosntrain.append(column.getJavaDeclaredObjectName());

							if (!column.isForeignKey() && column.getJavaClassType().equals("java.lang.String") && column.getName().equalsIgnoreCase("email")) {							
								columnCosntrain.append((cd++>0?", ":"") + " email:true");
							}
							if ( column.isNullable() && !column.isPrimaryKey() ) {
								columnCosntrain.append((cd++>0?", ":"") +" nullable:true");
							} 						
							if ( !column.isForeignKey() && column.getJavaClassType().equals("java.lang.String") && column.getScale()>2) {							
								//columnCosntrain.append((cd++>0?", ":"") + " size: "+(column.getScale()/2)+".."+column.getScale());
								columnCosntrain.append((cd++>0?", ":"") + " maxSize:"+column.getScale());
							}
							if ( !column.isForeignKey() && column.getJavaClassType().equals("byte[]") ) {							
								columnCosntrain.append((cd++>0?", ":"") + " maxSize: 16777216   // 16MB => Longblob's mysql ?");
							}
						
						} else if(fkReferenceTable != null){
							columnCosntrain.append(FormatString.renameForJavaMethod(fkReferenceTable.getTableName()));

							if ( column.isNullable() ){
								columnCosntrain.append((cd++>0?", ":"") +" nullable:true");							
							}
						}
						
						if(cd>0) {
							constraintsDeclarations.add(columnCosntrain.toString());
						}
					}
					if (pkIdShuldBeRemapped) {
						String pkIdShuldBeRemappedName = "id ";
						int customizedProps = 0;
						if (!pkToRemapped.getName().equalsIgnoreCase("id")) {
							if (customizedProps++ > 0) {								
								pkIdShuldBeRemappedName += ", ";
							}
							pkIdShuldBeRemappedName += " name:'" + pkToRemapped.getJavaDeclaredObjectName() + "'";
							if (customizedProps++ > 0) {							
								pkIdShuldBeRemappedName += ", ";
							}							
							pkIdShuldBeRemappedName += " column:'" + pkToRemapped.getName() + "'";
						}
						if (!pkToRemapped.isAutoIncremment()) {
							if (pkToRemapped.getJavaClassType().equals("java.lang.String")) {
								if (customizedProps++ > 0) {								
									pkIdShuldBeRemappedName += ", ";
								}
								pkIdShuldBeRemappedName +=	"sqlType:'"+pkToRemapped.getSqlType();
								if(pkToRemapped.getScale() > 0) {
									pkIdShuldBeRemappedName +=	"("+pkToRemapped.getScale()+")";
								}
								pkIdShuldBeRemappedName +=	"'";
							}
							if (customizedProps++ > 0) {								
								pkIdShuldBeRemappedName += ", ";
							}
							pkIdShuldBeRemappedName += " generator:'assigned'";
						}
						mappingDeclarations.add(pkIdShuldBeRemappedName);
					}

				} else if (line.indexOf("${tablebean.hasMany.declarations}") >= 0) {
					List<String> hasManyList = getHasManyList(table, dbSet, tablesForGeneration);
					if (hasManyList.size() > 0) {
						//ps.println("\t//hasMany with M2M ?");
						ps.println("\tstatic hasMany = " + hasManyList);
					} else {
					}
				} else if (line.indexOf("${tablebean.ManyToOneRelations.declarations}") >= 0) {
					if (table.hasEmbeddedPK()) {
						ps.println("\tstatic mapping = {");
						ps.println("\t\tversion false");
						ps.print("\t\tid composite:[");
						int numFKembeddes = 0;
						Collection<Column> primaryKeys = table.getPrimaryKeys();
						for (Column column : primaryKeys) {
							if (numFKembeddes > 0) {
								ps.print(", ");
							}
							if (column.isForeignKey()) {
								ReferenceTable fkReferenceTable = table.getFKReferenceTable(column.getName());
								if (fkReferenceTable != null) {
									ps.print("'" + FormatString.renameForJavaMethod(fkReferenceTable.getTableName()) + "'");
								}
							} else {
								ps.print("'" + column.getJavaDeclaredObjectName() + "'");
							}

							numFKembeddes++;
						}
						ps.println("] , generator:'assigned'");
						ps.println("\t}");
					} else {
						int manyToOneRelations = 0;
						for (Column column : definitiveColumns) {
							Table fTable = null;
							String refObjFK = null;

							if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
								fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
							} else {
								fTable = null;
							}

							if (fTable != null) {
								if (manyToOneRelations == 0) {
									//ps.println("\t//belongsTo  Many2One");
									//ps.print("\tstatic belongsTo = [ ");
								} else {
									//ps.print(", ");
								}

								//ps.print(fTable.getJavaDeclaredObjectName() + ":" + fTable.getJavaDeclaredName());
								ps.println("\t//belongsTo  Many2One");
								ps.println("\t" + fTable.getJavaDeclaredName() + " " + fTable.getJavaDeclaredObjectName());
								manyToOneRelations++;
							}
						}
						if (manyToOneRelations > 0) {
							//ps.println("] ");
						}
					}
				} else if (line.indexOf("${tablebean.ManyToManyRelations.declarations}") >= 0) {

					Collection<Table> m2mTables = dbSet.getManyToManyRelationTables(table);
					int manyToManyRelations = 0;
					Table tableOwner = null;

					List<String> belongsToNames = new ArrayList<String>();
					//List<String> m2mMappingDefinitions = new ArrayList<String>();
					for (Table fm2mTable : m2mTables) {

						Table tableOwnerManyToManyRelation = dbSet.getTableOwnerManyToManyRelation(table, fm2mTable);

						//ps.println("\t// M2M <->:"+tableOwnerManyToManyRelation.getName());						
						Column rtCol1 = null;
						Column rtCol2 = null;

						Collection<Column> fKs = tableOwnerManyToManyRelation.getFKs();
						for (Column fkM2Mi : fKs) {
							if (fkM2Mi.getPosition() == 1) {
								rtCol1 = fkM2Mi;
							} else if (fkM2Mi.getPosition() == 2) {
								rtCol2 = fkM2Mi;
							}
						}

						String trn1 = tableOwnerManyToManyRelation.getFKReferenceTable(rtCol1.getName()).getTableName();
						String trn2 = tableOwnerManyToManyRelation.getFKReferenceTable(rtCol2.getName()).getTableName();

						if (trn2.equals(table.getName())) {
							belongsToNames.add(FormatString.getCadenaHungara(trn1));

							String joinTableName = tableOwnerManyToManyRelation.getName();
							String columnReferenceName = rtCol1.getName();

							mappingDeclarations.add(FormatString.renameForJavaMethod(trn1) + collectionClass + " joinTable: [ name: '" + joinTableName.toLowerCase() + "', key: '" + rtCol2.getName().toLowerCase() + "', column: '" + rtCol1.getName().toLowerCase() + "']");

						} else {
							String joinTableName = tableOwnerManyToManyRelation.getName();
							String columnReferenceName = rtCol2.getName();

							mappingDeclarations.add(FormatString.renameForJavaMethod(trn2) + collectionClass + " joinTable: [ name: '" + joinTableName.toLowerCase() + "', key: '" + rtCol1.getName().toLowerCase() + "', column: '" + rtCol2.getName().toLowerCase() + "']");
						}
						manyToManyRelations++;
					}
					Collection<Column> fKs = table.getFKs();
					for (Column fk : fKs) {
						if (!belongsToNames.contains(fk.getName())) {

							Table fTable = null;
							String refObjFK = null;

							fTable = dbSet.getTable(table.getFKReferenceTable(fk.getName()).getTableName());
							ps.println("\t//add Entity " + fTable.getJavaDeclaredName() + " from Many2One member.");
							belongsToNames.add(fTable.getJavaDeclaredName());
						}
					}

					if (belongsToNames.size() > 0) {
						ps.println("\t//belongsTo  ManyToMany, Many2One member");
						ps.println("\tstatic belongsTo = " + belongsToNames);
					}
				} else if (line.indexOf("${tablebean.mappings}") >= 0) {
					if (mappingDeclarations.size() > 0) {
						ps.println("\tstatic mapping = {");
					}
					for (String lineMappings : mappingDeclarations) {
						ps.println("\t\t" + lineMappings);
					}
					if (mappingDeclarations.size() > 0) {
						ps.println("\t}");
					}
				} else if (line.indexOf("${tablebean.constraints}") >= 0) {
					if (constraintsDeclarations.size() > 0) {
						ps.println("\tstatic constraints = {");
					}
					for (String lineConstraints : constraintsDeclarations) {
						ps.println("\t\t" + lineConstraints);
					}
					if (constraintsDeclarations.size() > 0) {
						ps.println("\t}");
					}
				} else {
					line = line.replace("${date}", sdf.format(new Date()));
					line = line.replace("${tablebean.serialId}", String.valueOf(table.hashCode()));
					line = line.replace("${tablebean.name}", table.getName());
					line = line.replace("${tablebean.declaredName}", table.getJavaDeclaredName());

					line = line.replace("${tablebean.toStringCode}", table.getToStringCode(dbSet,packageBeanMember));
					line = line.replace("${tablebean.constraints}", generateTableConstraints(table));
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

	private static List<String> getHasManyList(Table table, DBTableSet dbSet, ArrayList<Table> tablesForGeneration) {

		List<String> hasManyTables = new ArrayList<String>();
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

					hasManyTables.add(FormatString.renameForJavaMethod(posibleTableOneToMany.getName()) + collectionClass + ":" + FormatString.getCadenaHungara(posibleTableOneToMany.getName()));
				}
			}
		}

		Collection<Table> m2mTables = dbSet.getManyToManyRelationTables(table);

		List<String> belongsToNames = new ArrayList<String>();
		for (Table fm2mTable : m2mTables) {

			hasManyTables.add(FormatString.renameForJavaMethod(fm2mTable.getName()) + collectionClass + ":" + FormatString.getCadenaHungara(fm2mTable.getName()));
		}

		return hasManyTables;
	}

	private static String generateTableConstraints(Table table) {
		return "// Not-implemented in generator";
	}
}
