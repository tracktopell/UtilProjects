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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author aegonzalez
 */
public class JPABeanBuilder {

	public static void buildMappingBeans(DBTableSet dbSet, String packageBeanMember, String basePath)
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
					fos.getClass().getResourceAsStream("/templates/TableJPABean.java.template")));
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
							} else if (lineInLoop.indexOf("${tablebean.member.javaIdentifier}") >= 0) {								
								if(column.isForeignKey()){
									if(!(table instanceof EmbeddeableColumn)){
										if(hasNomalizaedFKReferences(fTable, column)){
											lineInLoop = lineInLoop.replace("${tablebean.member.javaIdentifier}", fTable.getJavaDeclaredObjectName());
										} else {
											lineInLoop = lineInLoop.replace("${tablebean.member.javaIdentifier}", column.getJavaDeclaredObjectName());
										}
									} else {
										lineInLoop = lineInLoop.replace("${tablebean.member.javaIdentifier}", column.getJavaDeclaredObjectName());
									} 
								} else {
									lineInLoop = lineInLoop.replace("${tablebean.member.javaIdentifier}", column.getJavaDeclaredObjectName());
								}
								ps.println(lineInLoop);

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
										ps.println("    @ManyToOne");
										ps.println("    @Column(name = \"" + column.getName().toUpperCase() + "\")");
										//if(column.getJavaClassType().equals("java.util.Date")){
										if (column.getSqlType().toLowerCase().equals("timestamp") || column.getSqlType().toLowerCase().equals("datetime")) {
											ps.println("    @Temporal(TemporalType.TIMESTAMP)");
										}
										ps.println("    private " + column.getJavaClassType().replace("java.lang.", "") + " " + column.getJavaDeclaredObjectName() + ";");
									}
								} else {
									if (column.isPrimaryKey() && !column.isForeignKey()) {
										if (column instanceof EmbeddeableColumn) {
											ps.println("    @EmbeddedId");
										} else {
											if (!(table instanceof EmbeddeableColumn)) {
												ps.println("    @Id");
											}
											String extraCoulmnDeclarationInfo = "";

											ps.println("    @Basic(optional = false)");
											if (column.getJavaClassType().equals("java.lang.String")) {
												extraCoulmnDeclarationInfo = ", length=" + column.getScale();
											}
											ps.println("    @Column(name = \"" + column.getName().toUpperCase() + "\" " + extraCoulmnDeclarationInfo + "  )");

											if (column.isAutoIncremment()) {
												//ps.println("    @GeneratedValue(strategy=GenerationType.IDENTITY)");
												ps.println("    @GeneratedValue(strategy=GenerationType.AUTO)");
											}
										}
										if (column.getJavaClassType().equals("java.util.Date")) {
											ps.println("    @Temporal(TemporalType.TIMESTAMP)");
										} else if (column.getJavaClassType().equals("java.util.Calendar")) {
											ps.println("    @Temporal(TemporalType.DATE)");
										}
										ps.println("    private " + column.getJavaClassType().replace("java.lang.", "")
												+ " " + column.getJavaDeclaredObjectName() + ";");
									} else {
										if (fTable != null) {

											//ps.println("    // (insertable = false, updatable = false) FIX ?"+table.hasEmbeddedPK()+" && "+column.isPrimaryKey());
											ps.print("    @JoinColumn(name = \"" + column.getName().toUpperCase() + "\" , referencedColumnName = \"" + table.getFKReferenceTable(column.getName()).getColumnName().toUpperCase() + "\"");
											if (table.hasEmbeddedPK() && column.isPrimaryKey()) {
												ps.println(", insertable = false, updatable = false)");
											} else {
												ps.println(")");
											}
											ps.println("    @ManyToOne(optional = " + column.isNullable() + ")");

											if (hasNomalizaedFKReferences(fTable, column)) {
												ps.println("    private " + fTable.getJavaDeclaredName() + " " + fTable.getJavaDeclaredObjectName() + ";");
											} else {
												ps.println("    private " + fTable.getJavaDeclaredName() + " " + FormatString.renameForJavaMethod(column.getName()) + ";");
											}
										} else {
											String extraCoulmnDeclarationInfo = "";
											ps.println("    @Basic(optional = " + column.isNullable() + ")");
											if (column.getJavaClassType().equals("java.util.Date")) {
												ps.println("    @Temporal(TemporalType.TIMESTAMP)");
											} else if (column.getJavaClassType().equals("java.util.Calendar")) {
												ps.println("    @Temporal(TemporalType.DATE)");
											} else if (column.getJavaClassType().equals("java.lang.String")) {
												extraCoulmnDeclarationInfo = ", length=" + column.getScale();
											}
											ps.println("    @Column(name = \"" + column.getName().toUpperCase() + "\" " + extraCoulmnDeclarationInfo + "  )");
											ps.println("    private " + column.getJavaClassType().replace("java.lang.", "")
													+ " " + column.getJavaDeclaredObjectName() + ";");
										}
									}
								}

							} else if (lineInLoop.indexOf("${tablebean.member.getter}") >= 0) {
								if (fTable != null && !table.getName().toUpperCase().endsWith("_P_K")) {

									refObjFK = fTable.getJavaDeclaredName();

									if (table instanceof EmbeddeableColumn) {
										refObjFK = column.getJavaClassType().replace("java.lang.", "");
									}

									String finalyVarName = null;
									String finalyXetterName = null;

									if (table instanceof EmbeddeableColumn && column.isForeignKey()) {
										finalyVarName = column.getJavaDeclaredObjectName();
									} else {
										if (hasNomalizaedFKReferences(fTable, column)) {
											finalyVarName = fTable.getJavaDeclaredObjectName();
										} else {
											finalyVarName = FormatString.renameForJavaMethod(column.getName());
										}
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

									String finalyVarName = null;
									String finalyXetterName = null;

									if (table instanceof EmbeddeableColumn && column.isForeignKey()) {
										finalyVarName = column.getJavaDeclaredObjectName();
									} else {
										if (hasNomalizaedFKReferences(fTable, column)) {
											finalyVarName = fTable.getJavaDeclaredObjectName();
										} else {
											finalyVarName = FormatString.renameForJavaMethod(column.getName());
										}
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
					for (Table posibleTableOneToMany : tablesForGeneration) {
						Enumeration<String> fkColumnNames = posibleTableOneToMany.getFKColumnNames();
						int sameTableTargetFK = 0;
						while (fkColumnNames.hasMoreElements()) {
							String columnNameFK = fkColumnNames.nextElement();
							ReferenceTable rt4OneToMany = posibleTableOneToMany.getFKReferenceTable(columnNameFK);

							if (rt4OneToMany.getTableName().equals(table.getName())
									&& !FormatString.getCadenaHungara(posibleTableOneToMany.getName()).endsWith("PK")) {
								sameTableTargetFK++;
							}
						}
						fkColumnNames = posibleTableOneToMany.getFKColumnNames();
						while (fkColumnNames.hasMoreElements()) {
							String columnNameFK = fkColumnNames.nextElement();
							ReferenceTable rt4OneToMany = posibleTableOneToMany.getFKReferenceTable(columnNameFK);

							if (rt4OneToMany.getTableName().equals(table.getName())
									&& !FormatString.getCadenaHungara(posibleTableOneToMany.getName()).endsWith("PK")) {

								String tableReferenceOneToMany = "null";
								Collection<Column> fks = posibleTableOneToMany.getFKs();
								Column colmnMappedBy = null;
								for (Column cfk : fks) {
									if (posibleTableOneToMany.getFKReferenceTable(cfk.getName()).getTableName().equals(table.getName())) {
										colmnMappedBy = cfk;
										tableReferenceOneToMany = FormatString.renameForJavaMethod(posibleTableOneToMany.getFKReferenceTable(cfk.getName()).getTableName());
									}
								}
								String posibleOneToManyMamber = FormatString.renameForJavaMethod(posibleTableOneToMany.getName()) + collectionClass;
								String fkReferencedMemberName = rt4OneToMany.getTableName() + "_" + rt4OneToMany.getColumnName();
								String realSugestedCollectionName = (!columnNameFK.equals(fkReferencedMemberName) && sameTableTargetFK > 1)
										? FormatString.renameForJavaMethod(posibleTableOneToMany.getName() + "_To_" + columnNameFK + "_" + collectionClass)
										: posibleOneToManyMamber;

								tableReferenceOneToMany = sameTableTargetFK > 1 ? FormatString.renameForJavaMethod(columnNameFK) : tableReferenceOneToMany;

								ps.println("    // bug , must refering " + posibleTableOneToMany.getName() + "." + colmnMappedBy.getName() + " => " + colmnMappedBy.getJavaDeclaredObjectName() + ", normalized ? " + hasNomalizaedFKReferences(table, colmnMappedBy));
								if (hasNomalizaedFKReferences(table, colmnMappedBy)) {
									ps.println("    @OneToMany(cascade = CascadeType.ALL, mappedBy = \"" + table.getJavaDeclaredObjectName() + "\")");
								} else {
									ps.println("    @OneToMany(cascade = CascadeType.ALL, mappedBy = \"" + colmnMappedBy.getJavaDeclaredObjectName() + "\")");
								}
								ps.println("    private " + collectionClass + "<" + FormatString.getCadenaHungara(posibleTableOneToMany.getName()) + "> " + realSugestedCollectionName + ";");
								ps.println("    ");
							}
						}
					}
				} else if (line.indexOf("${tablebean.oneToManyRelations.gettersAndSetters}") >= 0) {
					for (Table posibleTableOneToMany : tablesForGeneration) {
						Enumeration<String> fkColumnNames = posibleTableOneToMany.getFKColumnNames();
						int sameTableTargetFK = 0;
						while (fkColumnNames.hasMoreElements()) {
							String columnNameFK = fkColumnNames.nextElement();
							ReferenceTable rt4OneToMany = posibleTableOneToMany.getFKReferenceTable(columnNameFK);

							if (rt4OneToMany.getTableName().equals(table.getName())
									&& !FormatString.getCadenaHungara(posibleTableOneToMany.getName()).endsWith("PK")) {
								sameTableTargetFK++;
							}
						}
						fkColumnNames = posibleTableOneToMany.getFKColumnNames();
						while (fkColumnNames.hasMoreElements()) {
							String columnNameFK = fkColumnNames.nextElement();
							ReferenceTable rt4OneToMany = posibleTableOneToMany.getFKReferenceTable(columnNameFK);
							if (rt4OneToMany.getTableName().equals(table.getName())
									&& !FormatString.getCadenaHungara(posibleTableOneToMany.getName()).endsWith("PK")) {
								Table fTable = dbSet.getTable(posibleTableOneToMany.getName());
								String posibleOneToManyMamber = posibleTableOneToMany.getName() + "_" + collectionClass;
								String fkReferencedMemberName = rt4OneToMany.getTableName() + "_" + rt4OneToMany.getColumnName();
								String realSugestedCollectionName = (!columnNameFK.equals(fkReferencedMemberName) && sameTableTargetFK > 1)
										? posibleTableOneToMany.getName() + "_To_" + columnNameFK + "_" + collectionClass
										: posibleOneToManyMamber;

								ps.println("    ");
								ps.println("    public " + collectionClass + "<" + FormatString.getCadenaHungara(posibleTableOneToMany.getName()) + "> " + FormatString.renameForJavaMethod("Get_" + realSugestedCollectionName) + "() {");
								ps.println("        return this." + FormatString.renameForJavaMethod(realSugestedCollectionName) + ";");
								ps.println("    }");
								ps.println("    ");
								ps.println("    ");
								ps.println("    public void " + FormatString.renameForJavaMethod("Set_" + realSugestedCollectionName) + "(" + collectionClass + "<" + FormatString.getCadenaHungara(posibleTableOneToMany.getName()) + ">  v) {");
								ps.println("        this." + FormatString.renameForJavaMethod(realSugestedCollectionName) + " = v;");
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
							ps.println("    @ManyToMany(mappedBy = \"" + FormatString.renameForJavaMethod(table.getName()) + collectionClass + "\")");
						} else {
							ps.println("    @JoinTable(name               = \"" + tableOwnerManyToManyRelation.getName().toUpperCase() + "\",");
							ps.println("               joinColumns        = {@JoinColumn(name = \"" + rtCol2.getName().toUpperCase() + "\", referencedColumnName =\"" + tableOwnerManyToManyRelation.getFKReferenceTable(rtCol2.getName()).getColumnName().toUpperCase() + "\")},");
							ps.println("               inverseJoinColumns = {@JoinColumn(name = \"" + rtCol1.getName().toUpperCase() + "\", referencedColumnName =\"" + tableOwnerManyToManyRelation.getFKReferenceTable(rtCol1.getName()).getColumnName().toUpperCase() + "\")}");
							ps.println("               )");
							ps.println("    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)");
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
					line = line.replace("${tablebean.PKMembersParameters}", membersParameters(table, dbSet));

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
					line = line.replace("${tablebean.PKMembersParametersInitCode}", membersParametersInitCode(table, dbSet));
					line = line.replace("${tablebean.equalsCode}", table.getEqualsCode());
					line = line.replace("${tablebean.toStringCode}", table.getToStringCode(dbSet, packageBeanMember));
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

	public static void buildReourceBoundleBeans(DBTableSet dbSet, String basePath)
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
		for (Table table : tablesForGeneration) {

			//System.err.println("-->> generating: " + table.getJavaDeclaredName() + ".java :" + table);
			System.out.println("LABEL_" + table.getJavaDeclaredName().toUpperCase() + " = " + table.getLabel().toUpperCase());
			String tableLabel = table.getLabel().toUpperCase();
			char lastLetter = tableLabel.toCharArray()[tableLabel.length() - 1];
			if (lastLetter == 'A' || lastLetter == 'E' || lastLetter == 'I' || lastLetter == 'O' || lastLetter == 'U') {
				System.out.println("MENU_CRUD_" + table.getJavaDeclaredName().toUpperCase() + " = CATALOGO DE " + tableLabel + "S");
			} else {
				System.out.println("MENU_CRUD_" + table.getJavaDeclaredName().toUpperCase() + " = CATALOGO DE " + tableLabel.toUpperCase() + "ES");
			}

			System.out.println("LABEL_NEW_" + table.getJavaDeclaredName().toUpperCase() + " = AGREGAR " + table.getLabel().toUpperCase());
			System.out.println("LABEL_EDIT_" + table.getJavaDeclaredName().toUpperCase() + " = EDITAR " + table.getLabel().toUpperCase());
			System.out.println();
			Iterator<Column> columnsSortedColumnsForJPA = table.getSortedColumnsForJPA();
			List<Column> definitiveColumns = new ArrayList();
			while (columnsSortedColumnsForJPA.hasNext()) {
				Column c = columnsSortedColumnsForJPA.next();
				definitiveColumns.add(c);
				//System.err.println("\t-->> DefinitiveColumn: " + c);

				System.out.println("LABEL_" + table.getJavaDeclaredName().toUpperCase() + "_" + c.getJavaDeclaredName().toUpperCase() + " = " + c.getLabel().toUpperCase());
				System.out.println("TEXT_MAXCHARS_" + table.getJavaDeclaredName().toUpperCase() + "_" + c.getJavaDeclaredName().toUpperCase() + " = " + c.getScale());
				System.out.println("INPUT_REQUIRED_" + table.getJavaDeclaredName().toUpperCase() + "_" + c.getJavaDeclaredName().toUpperCase() + " = " + (!c.isNullable()));

			}
			System.out.println();
		}
	}

	private static boolean hasNomalizaedFKReferences(Table fTable, Column column) {
		String x = (fTable.getName() + "_" + fTable.getJPAPK()).toLowerCase();
		String y = column.getName().toLowerCase();
		return x.equals(y);
	}

}
