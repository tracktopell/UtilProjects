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
public class DTOBeanBuilder {

	public static void buildMappingBeans(DBTableSet dbSet, String packageBeanMember, String schemmaName, String basePath)
			throws Exception {
		String fileName;
		File baseDir = null;
		File dirSourceFile = null;
		File sourceFile = null;

		FileOutputStream fos = null;
		PrintStream ps = null;
		BufferedReader br = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
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

			System.err.println("-->> generating: " + table.getJavaDeclaredName() + ".java :" + table);

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
					fos.getClass().getResourceAsStream("/templates/TableDTOBean.java.template")));
			String line = null;
			ArrayList<String> linesToParse = null;
			int nl = 0;
			while ((line = br.readLine()) != null) {

				if (line.indexOf("%foreach") >= 0) {
					linesToParse = new ArrayList<String>();
				} else if (line.indexOf("%endfor") >= 0) {
					int numColumnGenerating = 0;

					for (Column column : definitiveColumns) {
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
		
								ps.println("    private " + column.getJavaClassType().replace("java.lang.", "")
										+ " " + column.getJavaDeclaredObjectName() + ";");
							} else if (lineInLoop.indexOf("${tablebean.member.javaIdentifier}") >= 0) {
								
								lineInLoop = lineInLoop.replace("${tablebean.member.javaIdentifier}", column.getJavaDeclaredObjectName());
								ps.println(lineInLoop+" // BUG ?");

							} else if (lineInLoop.indexOf("${tablebean.member.getter}") >= 0) {
								if (fTable != null && !table.getName().toUpperCase().endsWith("_P_K")) {

									refObjFK = fTable.getJavaDeclaredName();

									if (table instanceof EmbeddeableColumn) {
										refObjFK = column.getJavaClassType().replace("java.lang.", "");
									}

									int ccrfk = 0;
									ccrfk = table.countReferencesToTable(fTable.getName());

									String finalyVarName = null;
									String finalyXetterName = null;

									if (table instanceof EmbeddeableColumn && column.isForeignKey()) {
										finalyVarName = column.getJavaDeclaredObjectName();
									} else {
										finalyVarName = ((ccrfk > 1) ? FormatString.renameForJavaMethod(column.getName()) : fTable.getJavaDeclaredObjectName());
									}
									finalyXetterName = "et" + FormatString.firstLetterUpperCase(finalyVarName);

									ps.println("    public " + refObjFK + " g" + finalyXetterName + " () {");
									ps.println("        return this." + finalyVarName + ";");
									ps.println("    }");
								} else {
									ps.println("    public " + column.getJavaClassType().replace("java.lang.", "")
											+ " get" + column.getJavaDeclaredName() + "() {");
									ps.println("        return this." + column.getJavaDeclaredObjectName() + ";");
									ps.println("    }");
								}
							} else if (lineInLoop.indexOf("${tablebean.member.setter}") >= 0) {
								if (fTable != null && !table.getName().toUpperCase().endsWith("_P_K")) {

									refObjFK = fTable.getJavaDeclaredName();

									if (table instanceof EmbeddeableColumn) {
										refObjFK = column.getJavaClassType().replace("java.lang.", "");
									}

									int ccrfk = 0;
									ccrfk = table.countReferencesToTable(fTable.getName());

									String finalyVarName = null;
									String finalyXetterName = null;

									if (table instanceof EmbeddeableColumn && column.isForeignKey()) {
										finalyVarName = column.getJavaDeclaredObjectName();
									} else {
										finalyVarName = ((ccrfk > 1) ? FormatString.renameForJavaMethod(column.getName()) : fTable.getJavaDeclaredObjectName());
									}
									finalyXetterName = "et" + FormatString.firstLetterUpperCase(finalyVarName);

									ps.println("    public void s" + finalyXetterName + "(" + refObjFK + " v) {");
									ps.println("        this." + finalyVarName + " = v;");
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

				} else if (line.indexOf("${tablebean.oneToManyRelations.gettersAndSetters}") >= 0) {

				} else if (line.indexOf("${tablebean.ManyToManyRelations.declarations}") >= 0) {

				} else if (line.indexOf("${tablebean.ManyToManyRelations.gettersAndSetters}") >= 0) {

				} else {
					line = line.replace("${date}", sdf.format(new Date()));
					line = line.replace("${tablebean.serialId}", String.valueOf(table.hashCode()));
					line = line.replace("${tablebean.name}", table.getName());
					line = line.replace("${tablebean.declaredName}", table.getJavaDeclaredName());
					line = line.replace("${tablebean.PKMembersParameters}", membersParameters(table, dbSet));					

					if (table instanceof EmbeddeableColumn) {
//                        line = line.replace("${tablebean.jpa_entity_or_embeddeable}", "@Embeddable");
						line = line.replace("${tablebean.jpa_talbe}", "");
					} else {
//                        line = line.replace("${tablebean.jpa_entity_or_embeddeable}", "@Entity");
//                        line = line.replace("${tablebean.jpa_talbe}", "@Table(name = \"" + table.getName().toUpperCase() + "\")");
						line = line.replace("${tablebean.id}", table.getJPAPK());
						line = line.replace("${tablebean.id.javaClass}", table.getJPAPKClass().replace("java.lang.", ""));
					}

					line = line.replace("${tablebean.hashCodeSumCode}", table.getHashCodeSumCode());
					line = line.replace("${tablebean.PKMembersParametersInitCode}", membersParametersInitCode(table, dbSet));
					line = line.replace("${tablebean.equalsCode}", table.getEqualsCode());
					line = line.replace("${tablebean.toStringCode}", table.getToDTOStringCode(dbSet, packageBeanMember));
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

	public static void buildMappingDTOBeansAndJPABeans(DBTableSet dbSet, String dtoPackageBeanMember, String jpaPackageBeanMember, String basePath,boolean flatDTOs)
			throws Exception {
		String fileName;
		File baseDir = null;
		File dirSourceFile = null;
		File sourceFile = null;

		FileOutputStream fos = null;
		PrintStream ps = null;
		BufferedReader br = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");		
		
		System.err.println("=============================>>> DTOs ");
		
		
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
						System.err.println("\t-->>SKIPING DTO + " + cctJpaC.getName());
						//tablesForGeneration.add((EmbeddeableColumn) cctJpaC);
						//addedAsFKEmbedded = true;
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

			System.err.println("-->> generating DTO: " + table.getJavaDeclaredName() + "DTO.java :" + table);

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

			fileName = dtoPackageBeanMember.replace(".", File.separator) + File.separator;

			dirSourceFile = new File(baseDir.getPath() + File.separator + File.separator + fileName);
			if (!dirSourceFile.exists()) {
				dirSourceFile.mkdirs();
			}

			fileName = dirSourceFile.getPath() + File.separator + table.getJavaDeclaredName() + "DTO.java";

			sourceFile = new File(fileName);
			fos = new FileOutputStream(sourceFile);
			ps = new PrintStream(fos);

			br = new BufferedReader(new InputStreamReader(
					fos.getClass().getResourceAsStream("/templates/TableDTOBeanMapingJPABean.java.template")));
			String line = null;
			ArrayList<String> linesToParse = null;
			int nl = 0;
			while ((line = br.readLine()) != null) {

				if (line.indexOf("%foreach") >= 0) {
					linesToParse = new ArrayList<String>();
				} else if (line.indexOf("%endfor") >= 0) {
					int numColumnGenerating = 0;

					for (Column column : definitiveColumns) {
						numColumnGenerating++;

						Table fTable = null;
						String refObjFK = null;

						if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
							fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
						} else {
							fTable = null;
						}

						for (String lineInLoop : linesToParse) {
							if (lineInLoop.indexOf("${tablebean.member.javaIdentifier}") >= 0) {
								if (! (column instanceof EmbeddeableColumn) ){
									lineInLoop = lineInLoop.replace("${tablebean.member.javaIdentifier}", column.getJavaDeclaredObjectName());
									ps.println(lineInLoop);
								}
							} else if (lineInLoop.indexOf("${tablebean.member.javadocCommnet}") >= 0) {
								
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

							} else if (lineInLoop.indexOf("${tablebean.member.declaration}") >= 0) {
								if (column instanceof EmbeddeableColumn){
									EmbeddeableColumn eColumn = (EmbeddeableColumn)column;
									ps.println("    // " + column.getJavaDeclaredObjectName() + " EmbedableColumn ID References: FKs {"+eColumn.getFKs()+"}");
								} else {
									ps.println("    // Simple: PK?"+column.isPrimaryKey()+", FK?"+column.isForeignKey()+", class="+column.getClass());
									ps.println("    private " + column.getJavaClassType().replace("java.lang.", "") + " " + column.getJavaDeclaredObjectName() + ";");
								}

							} else if (lineInLoop.indexOf("${tablebean.member.getter}") >= 0) {
								
								if (column instanceof EmbeddeableColumn) {
									EmbeddeableColumn eColumn = (EmbeddeableColumn)column;
									ps.println("    // " + column.getJavaDeclaredObjectName() + " EmbedableColumn ID References: FKs {"+eColumn.getFKs()+"}");
								} else {
									
									ps.println("    public "+column.getJavaClassType().replace("java.lang.", "")+" get"+FormatString.getCadenaHungara(column.getName())+"() {");
									ps.println("        return this." + column.getJavaDeclaredObjectName() + ";");								
									ps.println("    }");
								}
							} else if (lineInLoop.indexOf("${tablebean.member.setter}") >= 0) {
								if (column instanceof EmbeddeableColumn) {
									EmbeddeableColumn eColumn = (EmbeddeableColumn)column;
									ps.println("    // " + column.getJavaDeclaredObjectName() + " EmbedableColumn ID References: FKs {"+eColumn.getFKs()+"}");
								} else {
									
									ps.println("    public void set"+FormatString.getCadenaHungara(column.getName())+"("+column.getJavaClassType().replace("java.lang.", "")+" v) {");
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
				} else if (line.indexOf("${jpaCopyedToDtoMembers.code.code}") >= 0) {
					for (Column column : definitiveColumns) {

						Table fTable = null;

						if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
							fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
														
							if( hasNomalizaedFKReferences(fTable, column) ){
								//ps.println("        this." + column.getJavaDeclaredObjectName() + " = jpaEntity.get" + FormatString.getCadenaHungara(fTable.getName()) + "().get" + FormatString.getCadenaHungara(fTable.getJPAPK()) + "(); // normalized ");
								ps.println("        this." + column.getJavaDeclaredObjectName() + " = jpaEntity.get" + FormatString.getCadenaHungara(fTable.getName()) + "()!=null?jpaEntity.get" + FormatString.getCadenaHungara(fTable.getName()) + "().get" + FormatString.getCadenaHungara(fTable.getJPAPK()) + "():null; // normalized ");
							}else{
								ps.println("        this." + column.getJavaDeclaredObjectName() + " = jpaEntity.get" + FormatString.getCadenaHungara(column.getName()) + "()!=null?jpaEntity.get" + FormatString.getCadenaHungara(column.getName()) + "().get" + FormatString.getCadenaHungara(fTable.getJPAPK()) + "():null; // custom");
							}
							
						} else if (column instanceof EmbeddeableColumn) {
							EmbeddeableColumn eColumn = (EmbeddeableColumn)column;							
							
							ps.println("        // "+FormatString.getCadenaHungara(column.getName())+" is Embeddable. Begin nested settings");
							final Collection<Column> fKs = eColumn.getFKs();
							for(Column fC: fKs){
								ps.print  ("        this." + column.getJavaDeclaredObjectName() + ".set"+fC.getJavaDeclaredName());
								ps.println("( jpaEntity.get" + FormatString.getCadenaHungara(column.getName()) + "().get"+fC.getJavaDeclaredName()+"() );");							
							}
							ps.println("        // End nested settings");
							
						} else {
							ps.println("        this." + column.getJavaDeclaredObjectName() + " = jpaEntity.get" + FormatString.getCadenaHungara(column.getName()) + "(); // primitive");							
						}
					}
				} else if (line.indexOf("${dtoCopyedToJpaMembers.code.code}") >= 0) {
					for (Column column : definitiveColumns) {
						Table fTable = null;

						if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
							fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
							
							if( hasNomalizaedFKReferences(fTable, column) ){
								//ps.println("        jpaEntity.set" + FormatString.getCadenaHungara(fTable.getName()) + "( new " + jpaPackageBeanMember + "." + fTable.getJavaDeclaredName() + "(this.get" + FormatString.getCadenaHungara(column.getName()) + "())); // normalized");
								ps.println("        jpaEntity.set" + FormatString.getCadenaHungara(fTable.getName()) + "( this.get" + FormatString.getCadenaHungara(column.getName()) + "()!=null? new " + jpaPackageBeanMember + "." + fTable.getJavaDeclaredName() + "(this.get" + FormatString.getCadenaHungara(column.getName()) + "()):null); // normalized");
							}else{
								ps.println("        jpaEntity.set" + FormatString.getCadenaHungara(column.getName()) + "( new " + jpaPackageBeanMember + "." + fTable.getJavaDeclaredName() + "(this.get" + FormatString.getCadenaHungara(column.getName()) + "())); // custom");
							}
						} else if (column instanceof EmbeddeableColumn) {
							EmbeddeableColumn eColumn = (EmbeddeableColumn)column;							
							
							ps.println("        // "+FormatString.getCadenaHungara(column.getName())+" is Embeddable. Begin nested settings");
							final Collection<Column> fKs = eColumn.getFKs();
							for(Column fC: fKs){
								ps.print  ("        jpaEntity.get" + FormatString.getCadenaHungara(column.getName()) + "().set"+fC.getJavaDeclaredName());
								ps.println("( this." + column.getJavaDeclaredObjectName() + ".get"+fC.getJavaDeclaredName()+"() );");
							}
							ps.println("        // End nested settings");
							
						} else {

							ps.println("        jpaEntity.set" + FormatString.getCadenaHungara(column.getName()) + "( this.get" + FormatString.getCadenaHungara(column.getName()) + "());");
						}

					}
				} else {
					line = line.replace("${date}", sdf.format(new Date()));
					line = line.replace("${tablebean.serialId}", String.valueOf(table.hashCode()));
					line = line.replace("${tablebean.name}", table.getName());
					line = line.replace("${tablebean.declaredName}", table.getJavaDeclaredName());
					line = line.replace("${tablebean.PKMembersParameters}", membersParameters(table, dbSet));

					if (table instanceof EmbeddeableColumn) {
//                        line = line.replace("${tablebean.jpa_entity_or_embeddeable}", "@Embeddable");
						line = line.replace("${tablebean.jpa_talbe}", "");
					} else {
//                        line = line.replace("${tablebean.jpa_entity_or_embeddeable}", "@Entity");
//                        line = line.replace("${tablebean.jpa_talbe}", "@Table(name = \"" + table.getName().toUpperCase() + "\")");
						line = line.replace("${tablebean.id}", table.getJPAPK());
						line = line.replace("${tablebean.id.javaClass}", table.getJPAPKClass().replace("java.lang.", ""));
					}

					line = line.replace("${tablebean.hashCodeSumCode}", table.getHashCodeSumCode());
					line = line.replace("${tablebean.PKMembersParametersInitCode}", membersParametersInitCode(table, dbSet));
					line = line.replace("${tablebean.equalsCode}", table.getEqualsCode());
					line = line.replace("${tablebean.toStringCode}", table.getToDTOStringCode(dbSet, dtoPackageBeanMember));
					line = line.replace("${tablebean.name.uc}", table.getName().toUpperCase());
					line = line.replace("${tablebean.package}", dtoPackageBeanMember);
					line = line.replace("${tableJPAbean.package}", jpaPackageBeanMember);

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

	public static void buildAssembler(DBTableSet dbSet, String dtoPackageBeanMember, String jpaPackageBeanMember, String basePath)
			throws Exception {
		String fileName;
		File baseDir = null;
		File dirSourceFile = null;
		File sourceFile = null;

		FileOutputStream fos = null;
		PrintStream ps = null;
		BufferedReader br = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
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

			if(table.getName().endsWith("_P_K")){
				continue;
			}
			System.err.println("-->> generating: " + table.getJavaDeclaredName() + ".java :" + table);

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

			fileName = dtoPackageBeanMember.replace(".", File.separator) + File.separator;

			dirSourceFile = new File(baseDir.getPath() + File.separator + File.separator + fileName);
			if (!dirSourceFile.exists()) {
				dirSourceFile.mkdirs();
			}

			fileName = dirSourceFile.getPath() + File.separator + table.getJavaDeclaredName() + "Assembler.java";

			sourceFile = new File(fileName);
			fos = new FileOutputStream(sourceFile);
			ps = new PrintStream(fos);

			br = new BufferedReader(new InputStreamReader(
					fos.getClass().getResourceAsStream("/templates/AssemblerDTOJPABean.java.template")));
			String line = null;
			ArrayList<String> linesToParse = null;
			int nl = 0;
			while ((line = br.readLine()) != null) {

				if (line.indexOf("%foreach") >= 0) {
					linesToParse = new ArrayList<String>();
				} else if (line.indexOf("%endfor") >= 0) {
					int numColumnGenerating = 0;

					for (Column column : definitiveColumns) {
						numColumnGenerating++;

						Table fTable = null;
						String refObjFK = null;

						if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
							fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
						} else {
							fTable = null;
						}

						for (String lineInLoop : linesToParse) {
							if (lineInLoop.indexOf("${tablebean.member.declaration}") >= 0) {
								
								if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
									fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());									
									ps.println("    // Direct ID References");
									ps.println("    private "+fTable.getJPAPKClass().replace("java.lang.", "")+" "+FormatString.renameForJavaMethod(column.getName())+";");
								} else if (column instanceof EmbeddeableColumn) {
									EmbeddeableColumn eColumn = (EmbeddeableColumn)column;
									ps.println("    // EmbedableColumn ID References: FKs {"+eColumn.getFKs()+"}");
									//ps.println("    private "+fTable.getJPAPKClass().replace("java.lang.", "")+" "+FormatString.renameForJavaMethod(column.getName())+";");
									ps.println("    private " + column.getJavaClassType().replace("java.lang.", "") + " " + column.getJavaDeclaredObjectName() + ";");
								} else {
									ps.println("    // primitive");
									ps.println("    private " + column.getJavaClassType().replace("java.lang.", "") + " " + column.getJavaDeclaredObjectName() + ";");
								}
							} else {
								ps.println(lineInLoop);
							}
						}
					}

					linesToParse = null;
				} else if (linesToParse != null) {
					linesToParse.add(line);
				} else if (line.indexOf("${jpaCopyedToDtoMembers.code.code}") >= 0) {
					for (Column column : definitiveColumns) {

						Table fTable = null;

						if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
							fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
							ps.println("        //Embedable: "+FormatString.getCadenaHungara(column.getName())+" -> "+FormatString.getCadenaHungara(fTable.getName())+", STD?"+hasNomalizaedFKReferences(fTable, column));
							if(hasNomalizaedFKReferences(fTable, column)) {
								for(Column cfk: fTable.getColums()){
									if(cfk.isPrimaryKey()){									
										ps.println("        dtoEntity.set" + FormatString.getCadenaHungara(column.getName()) + "( jpaEntity.get"+FormatString.getCadenaHungara(fTable.getName())+"()!=null?jpaEntity.get"+FormatString.getCadenaHungara(fTable.getName())+"().get"+FormatString.getCadenaHungara(cfk.getName())+"():null); ");
									}
								}
							} else {
								for(Column cfk: fTable.getColums()){
									if(cfk.isPrimaryKey()){									
										ps.println("        dtoEntity.set" + FormatString.getCadenaHungara(column.getName()) + "( jpaEntity.get"+FormatString.getCadenaHungara(column.getName())+"()!=null?jpaEntity.get"+FormatString.getCadenaHungara(column.getName())+"().get"+FormatString.getCadenaHungara(cfk.getName())+"():null);");
									}
								}
							}
						} else if (column instanceof EmbeddeableColumn) {
							EmbeddeableColumn eColumn = (EmbeddeableColumn)column;							
							
							ps.println("        // "+FormatString.getCadenaHungara(column.getName())+" is Embeddable. Begin nested settings");
							final Collection<Column> fKs = eColumn.getFKs();
							for(Column fC: fKs){
								ps.print  ("        dtoEntity.set" + FormatString.getCadenaHungara(fC.getName()));
								ps.println("( jpaEntity.get" + FormatString.getCadenaHungara(column.getName()) + "().get"+fC.getJavaDeclaredName()+"() ); // bug 3 ?");
							}
							ps.println("        // End nested settings");
							
						} else {
							ps.println("        dtoEntity.set" + FormatString.getCadenaHungara(column.getName()) + "( jpaEntity.get" + FormatString.getCadenaHungara(column.getName()) + "() ); // primitive");
						}
					}
				} else if (line.indexOf("${dtoCopyedToJpaMembers.code.code}") >= 0) {
					for (Column column : definitiveColumns) {
						Table fTable = null;

						if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
							fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
							
							if( hasNomalizaedFKReferences(fTable, column) ){
								ps.println("        " + FormatString.getCadenaHungara(fTable.getName()) + "DTO pk"+FormatString.getCadenaHungara(fTable.getName())+"DTO = new "+FormatString.getCadenaHungara(fTable.getName())+"DTO();");
								for(Column cfk: fTable.getColums()){
									if(cfk.isPrimaryKey()){
										ps.println("        pk"+FormatString.getCadenaHungara(fTable.getName())+"DTO.set"+FormatString.getCadenaHungara(cfk.getName())+"(dtoEntity.get"+FormatString.getCadenaHungara(column.getName())+"()); // FK");
									}
								}							
								ps.println("        jpaEntity.set" + FormatString.getCadenaHungara(fTable.getName()) + "( "+FormatString.getCadenaHungara(fTable.getName())+"Assembler.buildJpaEntity( pk"+FormatString.getCadenaHungara(fTable.getName())+"DTO )); // Assembler delegation by PKs");
							}else{
								ps.println("        "+FormatString.getCadenaHungara(fTable.getName())+"DTO dto"+FormatString.getCadenaHungara(fTable.getName())+"DTO = new "+FormatString.getCadenaHungara(fTable.getName())+"DTO();");
								ps.println("        dto"+FormatString.getCadenaHungara(fTable.getName())+"DTO.set"+FormatString.getCadenaHungara(column.getName())+"( dtoEntity.get" + FormatString.getCadenaHungara(column.getName())+ "());");
								ps.println("        jpaEntity.set" + FormatString.getCadenaHungara(column.getName()) + "( "+FormatString.getCadenaHungara(fTable.getName())+"Assembler.buildJpaEntity( dto"+FormatString.getCadenaHungara(fTable.getName())+"DTO )); // Assembler delegation not normalizaed");
							}
						} else if (column instanceof EmbeddeableColumn) {
							EmbeddeableColumn eColumn = (EmbeddeableColumn)column;							
							
							ps.println("        // "+FormatString.getCadenaHungara(column.getName())+" is Embeddable. Begin nested settings");
							final Collection<Column> fKs = eColumn.getFKs();
							for(Column fC: fKs){
								ps.print  ("        jpaEntity.get" + FormatString.getCadenaHungara(column.getName()) + "().set"+fC.getJavaDeclaredName());
								ps.println("( dtoEntity.get"+fC.getJavaDeclaredName()+"() );  // nested FKs > BUG");
							}
							ps.println("        // End nested settings");
							
						} else {
							ps.println("        jpaEntity.set" + FormatString.getCadenaHungara(column.getName()) + "( dtoEntity.get" + FormatString.getCadenaHungara(column.getName()) + "()); // normal");
						}

					}
				} else {
					line = line.replace("${date}", sdf.format(new Date()));
					line = line.replace("${tablebean.serialId}", String.valueOf(table.hashCode()));
					line = line.replace("${tablebean.name}", table.getName());
					line = line.replace("${tablebean.declaredName}", table.getJavaDeclaredName());
					line = line.replace("${tablebean.PKMembersParameters}", membersParameters(table, dbSet));

					if (table instanceof EmbeddeableColumn) {
//                        line = line.replace("${tablebean.jpa_entity_or_embeddeable}", "@Embeddable");
						line = line.replace("${tablebean.jpa_talbe}", "");
					} else {
//                        line = line.replace("${tablebean.jpa_entity_or_embeddeable}", "@Entity");
//                        line = line.replace("${tablebean.jpa_talbe}", "@Table(name = \"" + table.getName().toUpperCase() + "\")");
						line = line.replace("${tablebean.id}", table.getJPAPK());
						line = line.replace("${tablebean.id.javaClass}", table.getJPAPKClass().replace("java.lang.", ""));
					}

					line = line.replace("${tablebean.hashCodeSumCode}", table.getHashCodeSumCode());
					line = line.replace("${tablebean.PKMembersParametersInitCode}", membersParametersInitCode(table, dbSet));
					line = line.replace("${tablebean.equalsCode}", table.getEqualsCode());
					line = line.replace("${tablebean.toStringCode}", table.getToDTOStringCode(dbSet, dtoPackageBeanMember));
					line = line.replace("${tablebean.name.uc}", table.getName().toUpperCase());
					line = line.replace("${tablebean.package}", dtoPackageBeanMember);
					line = line.replace("${tableJPAbean.package}", jpaPackageBeanMember);

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
	
	public static void buildMappingDTOBeans(DBTableSet dbSet, String dtoPackageBeanMember, String basePath)
			throws Exception {
		String fileName;
		File baseDir = null;
		File dirSourceFile = null;
		File sourceFile = null;

		FileOutputStream fos = null;
		PrintStream ps = null;
		BufferedReader br = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
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

			System.err.println("-->> generating: " + table.getJavaDeclaredName() + ".java :" + table);

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

			fileName = dtoPackageBeanMember.replace(".", File.separator) + File.separator;

			dirSourceFile = new File(baseDir.getPath() + File.separator + File.separator + fileName);
			if (!dirSourceFile.exists()) {
				dirSourceFile.mkdirs();
			}

			fileName = dirSourceFile.getPath() + File.separator + table.getJavaDeclaredName() + ".java";

			sourceFile = new File(fileName);
			fos = new FileOutputStream(sourceFile);
			ps = new PrintStream(fos);

			br = new BufferedReader(new InputStreamReader(
					fos.getClass().getResourceAsStream("/templates/TableDTOBean.java.template")));
			String line = null;
			ArrayList<String> linesToParse = null;
			int nl = 0;
			while ((line = br.readLine()) != null) {

				if (line.indexOf("%foreach") >= 0) {
					linesToParse = new ArrayList<String>();
				} else if (line.indexOf("%endfor") >= 0) {
					int numColumnGenerating = 0;

					for (Column column : definitiveColumns) {
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
								
								if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
									fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());									
									ps.println("    private "+fTable.getJPAPKClass().replace("java.lang.", "")+" "+FormatString.renameForJavaMethod(column.getName())+";");
								} else {
									ps.println("    private " + column.getJavaClassType().replace("java.lang.", "") + " " + column.getJavaDeclaredObjectName() + ";");
								}								
							} else if (lineInLoop.indexOf("${tablebean.member.getter}") >= 0) {
								
								if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
									ps.println("    public "+fTable.getJPAPKClass().replace("java.lang.", "")+"  get"+FormatString.getCadenaHungara(column.getName())+"() {");									
									ps.println("        return this." + FormatString.renameForJavaMethod(column.getName()) + ";");
								} else {
									ps.println("    public "+column.getJavaClassType().replace("java.lang.", "")+" get"+FormatString.getCadenaHungara(column.getName())+"() {");
									ps.println("        return this." + column.getJavaDeclaredObjectName() + ";");								
								}
								ps.println("    }");

							} else if (lineInLoop.indexOf("${tablebean.member.setter}") >= 0) {
								if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
									ps.println("    public void set"+FormatString.getCadenaHungara(column.getName())+"("+fTable.getJPAPKClass().replace("java.lang.", "")+" id) {");									
									ps.println("        this." + column.getJavaDeclaredObjectName() + " = id;");
								} else {
									ps.println("    public void set"+FormatString.getCadenaHungara(column.getName())+"("+column.getJavaClassType().replace("java.lang.", "")+" v) {");
									ps.println("        this." + column.getJavaDeclaredObjectName() + " = v;");								
								}
								ps.println("    }");

							} else {
								ps.println(lineInLoop);
							}
						}
					}

					linesToParse = null;
				} else if (linesToParse != null) {
					linesToParse.add(line);
				} else if (line.indexOf("${jpaCopyedToDtoMembers.code.code}") >= 0) {
					for (Column column : definitiveColumns) {

						Table fTable = null;

						if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
							fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
														
							if( hasNomalizaedFKReferences(fTable, column) ){
								//ps.println("        this." + column.getJavaDeclaredObjectName() + " = jpaEntity.get" + FormatString.getCadenaHungara(fTable.getName()) + "().get" + FormatString.getCadenaHungara(fTable.getJPAPK()) + "(); // normalized ");
								ps.println("        this." + column.getJavaDeclaredObjectName() + " = jpaEntity.get" + FormatString.getCadenaHungara(fTable.getName()) + "()!=null?jpaEntity.get" + FormatString.getCadenaHungara(fTable.getName()) + "().get" + FormatString.getCadenaHungara(fTable.getJPAPK()) + "():null; // normalized ");
							}else{
								ps.println("        this." + column.getJavaDeclaredObjectName() + " = jpaEntity.get" + FormatString.getCadenaHungara(column.getName()) + "()!=null?jpaEntity.get" + FormatString.getCadenaHungara(column.getName()) + "().get" + FormatString.getCadenaHungara(fTable.getJPAPK()) + "():null; // custom");
							}
							
						} else {
							ps.println("        this." + column.getJavaDeclaredObjectName() + " = jpaEntity.get" + FormatString.getCadenaHungara(column.getName()) + "(); // primitive");
						}
					}
				} else if (line.indexOf("${dtoCopyedToJpaMembers.code.code}") >= 0) {
					for (Column column : definitiveColumns) {
						Table fTable = null;

						if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
							fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
							
							if( hasNomalizaedFKReferences(fTable, column) ){
								//ps.println("        jpaEntity.set" + FormatString.getCadenaHungara(fTable.getName()) + "( new " + jpaPackageBeanMember + "." + fTable.getJavaDeclaredName() + "(this.get" + FormatString.getCadenaHungara(column.getName()) + "())); // normalized");
								//ps.println("        jpaEntity.set" + FormatString.getCadenaHungara(fTable.getName()) + "( this.get" + FormatString.getCadenaHungara(column.getName()) + "()!=null? new " + jpaPackageBeanMember + "." + fTable.getJavaDeclaredName() + "(this.get" + FormatString.getCadenaHungara(column.getName()) + "()):null); // normalized");
							}else{
								//ps.println("        jpaEntity.set" + FormatString.getCadenaHungara(column.getName()) + "( new " + jpaPackageBeanMember + "." + fTable.getJavaDeclaredName() + "(this.get" + FormatString.getCadenaHungara(column.getName()) + "())); // custom");
							}
						} else {

							ps.println("        jpaEntity.set" + FormatString.getCadenaHungara(column.getName()) + "( this.get" + FormatString.getCadenaHungara(column.getName()) + "());");
						}

					}
				} else {
					line = line.replace("${date}", sdf.format(new Date()));
					line = line.replace("${tablebean.serialId}", String.valueOf(table.hashCode()));
					line = line.replace("${tablebean.name}", table.getName());
					line = line.replace("${tablebean.declaredName}", table.getJavaDeclaredName());
					line = line.replace("${tablebean.PKMembersParameters}", membersParameters(table, dbSet));

					if (table instanceof EmbeddeableColumn) {
//                        line = line.replace("${tablebean.jpa_entity_or_embeddeable}", "@Embeddable");
						line = line.replace("${tablebean.jpa_talbe}", "");
					} else {
//                        line = line.replace("${tablebean.jpa_entity_or_embeddeable}", "@Entity");
//                        line = line.replace("${tablebean.jpa_talbe}", "@Table(name = \"" + table.getName().toUpperCase() + "\")");
						line = line.replace("${tablebean.id}", table.getJPAPK());
						line = line.replace("${tablebean.id.javaClass}", table.getJPAPKClass().replace("java.lang.", ""));
					}

					line = line.replace("${tablebean.hashCodeSumCode}", table.getHashCodeSumCode());
					line = line.replace("${tablebean.PKMembersParametersInitCode}", membersParametersInitCode(table, dbSet));
					line = line.replace("${tablebean.equalsCode}", table.getEqualsCode());
					line = line.replace("${tablebean.toStringCode}", table.getToDTOStringCode(dbSet, dtoPackageBeanMember));
					line = line.replace("${tablebean.name.uc}", table.getName().toUpperCase());
					line = line.replace("${tablebean.package}", dtoPackageBeanMember);
					//line = line.replace("${tableJPAbean.package}", jpaPackageBeanMember);

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
	
	public static void buildDAOs(DBTableSet dbSet, String dtoPackageBeanMember,String daoPackage, String basePath)
			throws Exception {
		String fileName;
		File baseDir = null;
		File dirSourceFile = null;
		File sourceFile = null;
		System.err.println("-->> buildDAOs");
		FileOutputStream fos = null;
		PrintStream ps = null;
		BufferedReader br = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
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

			System.err.println("-->> generating: " + table.getJavaDeclaredName() + "DAO.java :" + table);

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

			fileName = daoPackage.replace(".", File.separator) + File.separator;

			dirSourceFile = new File(baseDir.getPath() + File.separator + File.separator + fileName);
			if (!dirSourceFile.exists()) {
				dirSourceFile.mkdirs();
			}

			fileName = dirSourceFile.getPath() + File.separator + table.getJavaDeclaredName() + "DAO.java";

			sourceFile = new File(fileName);
			fos = new FileOutputStream(sourceFile);
			ps = new PrintStream(fos);
			
			StringBuffer sbCols      = new StringBuffer();
			StringBuffer sbPKCols    = new StringBuffer();
			StringBuffer sbColsUpdate= new StringBuffer();
			StringBuffer sbColsParams= new StringBuffer();
			Iterator<Column> itCols = table.getSortedColumns();
			int pkc=0;
			int npkc=0;
			Column tablePKColumn =null;
			for(int icr=0;itCols.hasNext();icr++){
				Column col=itCols.next();
				if(icr>0){
					sbCols.append(",");
					sbColsParams.append(",");
				}
				
				sbCols.append(col.getName().toUpperCase());
				sbColsParams.append("?");
				
				if(col.isPrimaryKey()){
					tablePKColumn = col;
					if(icr>0){
						sbPKCols.append(",");						
					}
					pkc++;
					sbPKCols.append(col.getName().toUpperCase());
				} else{
					if(npkc>0){
						sbPKCols.append(",");
						sbColsUpdate.append(",");
					}
					npkc++;
					sbColsUpdate.append(col.getName().toUpperCase());
					sbColsUpdate.append("=?");					
				}
			}
			
			String allCols=sbCols.toString();
			String pksCols=sbPKCols.toString();
			String collToUpdate=sbColsUpdate.toString();
			String colsParams=sbColsParams.toString();
			
			br = new BufferedReader(new InputStreamReader(
					fos.getClass().getResourceAsStream("/templates/TableDAO.java.template")));
			String line = null;
			ArrayList<String> linesToParse = null;
			int nl = 0;
			while ((line = br.readLine()) != null) {

				if (line.indexOf("%foreach") >= 0) {
					linesToParse = new ArrayList<String>();
				} else if (line.indexOf("%endfor") >= 0) {
					int numColumnGenerating = 0;

					for (Column column : definitiveColumns) {
						numColumnGenerating++;

						Table fTable = null;
						String refObjFK = null;

						if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
							fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());
						} else {
							fTable = null;
						}
						boolean skipPKMemberLine=false;
						for (String lineInLoop : linesToParse) {
							String lineIL = lineInLoop;
							skipPKMemberLine=false;
							String jc=column.getJavaClassType();
							boolean blob=false;
							if(jc.equals("byte[]")){
								//ps.println("\t\t\t\t// name="+column.getName()+", javaClsassType="+column.getJavaClassType()+", javaDeclaredName="+column.getJavaDeclaredName()+", PK?"+column.isPrimaryKey());
								blob=true;
								if(lineIL.contains("${tablebean.member.setter}")){
									ps.println("\t\t\t\tBlob bc=rs.getBlob(\""+column.getName().toUpperCase()+"\");");
									lineIL = lineIL.replace("${tablebean.member.setter}", "set"+column.getJavaDeclaredName());
									lineIL = lineIL.replace("rs.get${tablebean.member.javaClass}(\"${tablebean.member.name}\")", "bc.getBytes(0,(int)bc.length())");
								} else if(lineIL.contains("${tablebean.member.getter}")||lineIL.contains("${tablebean.memberNotPK.getter}")){
									lineIL = lineIL.replace("${tablebean.member.javaClass}", "Blob");									
									lineIL = lineIL.replace("${tablebean.memberNotPK.javaClass}", "Blob");
									lineIL = lineIL.replace("x.${tablebean.member.getter}(", "new ByteArrayInputStream(x.getContenidoOriginalXml()");
									lineIL = lineIL.replace("x.${tablebean.memberNotPK.getter}(", "new ByteArrayInputStream(x.getContenidoOriginalXml()");
								}
							} else {
								lineIL = lineIL.replace("${tablebean.member.setter}", "set"+column.getJavaDeclaredName());
								lineIL = lineIL.replace("${tablebean.member.javaClass}", FormatString.firstLetterUpperCase(jc.replace("java.lang.", "").replace("java.util.", "").replace("Integer","Int")));
							}
							
							lineIL = lineIL.replace("${tablebean.member.name}", column.getName().toUpperCase());
							lineIL = lineIL.replace("${tablebean.member.getter}", "get"+column.getJavaDeclaredName());
							
							if(column.isPrimaryKey()){
								if(lineIL.contains("${tablebean.memberNotPK.javaClass}")){
									skipPKMemberLine=true;
								} else {
									lineIL = lineIL.replace("${tablebean.memberPK.setter}", "set"+column.getJavaDeclaredName());
									lineIL = lineIL.replace("${tablebean.memberPK.getter}", "get"+column.getJavaDeclaredName());									
									if(jc.equals("java.util.Date")){
										lineIL = lineIL.replace("${tablebean.memberNotPK.javaClass}", FormatString.firstLetterUpperCase(jc.replace("java.util.", "")));
										if(lineIL.contains("x.get")){
											lineIL = lineIL.replace("x.get", "new java.sql.Date(x.get").replace(");", ".getTime()));");										
										}
									}else {
										lineIL = lineIL.replace("${tablebean.memberPK.javaClass}", FormatString.firstLetterUpperCase(jc.replace("java.lang.", "").replace("java.util.", "").replace("Integer","Int")));
									}
									
								}
							}else{								
								
								lineIL = lineIL.replace("${tablebean.memberNotPK.setter}", "set"+column.getJavaDeclaredName());
								lineIL = lineIL.replace("${tablebean.memberNotPK.getter}", "get"+column.getJavaDeclaredName());
																	
								if(jc.equals("java.util.Date")){
									lineIL = lineIL.replace("${tablebean.memberNotPK.javaClass}", FormatString.firstLetterUpperCase(jc.replace("java.util.", "")));
									
									if(lineIL.contains("x.get")){
										lineIL = lineIL.replace("x.get", "new java.sql.Date(x.get").replace(");", ".getTime()));");										
									}
								}else{
									lineIL = lineIL.replace("${tablebean.memberNotPK.javaClass}", FormatString.firstLetterUpperCase(jc.replace("java.lang.", "").replace("java.util.", "").replace("Integer","Int")));
								};
							}
							
							if(! skipPKMemberLine){
								ps.println(lineIL);
							}
							
						}
					}

					linesToParse = null;
				} else if (linesToParse != null) {
					
					linesToParse.add(line);
				} else {
					line = line.replace("${date}", sdf.format(new Date()));
					line = line.replace("${tablebean.serialId}", String.valueOf(table.hashCode()));
					line = line.replace("${tablebean.name}", table.getName().toUpperCase());
					line = line.replace("${tablebean.declaredName}", table.getJavaDeclaredName());
					//line = line.replace("${tablebean.PKMembersParameters}", membersParameters(table, dbSet));
										
					line = line.replace("${tablebean.pk}", tablePKColumn.getName().toUpperCase());
					line = line.replace("${tablebean.pk.javaClass}", tablePKColumn.getJavaClassType().replace("java.lang.", "").replace("java.util.", "").replace("Integer","Int"));
					line = line.replace("${tablebean.getPK}", "get"+tablePKColumn.getJavaDeclaredName());
					line = line.replace("${tablebean.setPK}", "set"+tablePKColumn.getJavaDeclaredName());

					line = line.replace("${tablebean.hashCodeSumCode}", table.getHashCodeSumCode());
					//line = line.replace("${tablebean.PKMembersParametersInitCode}", membersParametersInitCode(table, dbSet));
					line = line.replace("${tablebean.equalsCode}", table.getEqualsCode());
					line = line.replace("${tablebean.toStringCode}", table.getToDTOStringCode(dbSet, dtoPackageBeanMember));
					line = line.replace("${tablebean.package}", dtoPackageBeanMember);
					line = line.replace("${tablebean.listColumns}", allCols);
					line = line.replace("${tablebean.listParamColumns}", colsParams);
					line = line.replace("${tablebean.listColumns4Update}", collToUpdate);
					
					line = line.replace("${dao.package}", daoPackage);
					
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

		for (int numColumnGenerating = 0; simpleColumnsIterator.hasNext(); numColumnGenerating++) {

			Column column = simpleColumnsIterator.next();
			if (!column.isPrimaryKey()) {
				continue;
			}

			if (numColumnGenerating > 0) {
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
			sb.append(" ");
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

		for (int numColumnGenerating = 0; simpleColumnsIterator.hasNext(); numColumnGenerating++) {

			Column column = simpleColumnsIterator.next();
			if (!column.isPrimaryKey()) {
				continue;
			}

			if (numColumnGenerating > 0) {
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

	private static boolean hasNomalizaedFKReferences(Table fTable, Column column) {
		String x= (fTable.getName()+"_"+fTable.getJPAPK()).toLowerCase();
		String y= column.getName().toLowerCase();
		return x.equals(y);
	}

}
