/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tracktopell.dao.builder.jdbc;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author aegonzalez
 */
public class CodeBuilder {

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

		Enumeration<String> tableNames = dbSet.getTableNames();
		ArrayList<Table> tablesForGeneration = new ArrayList<Table>();
		while (tableNames.hasMoreElements()) {
			Table simpleTable = dbSet.getTable(tableNames.nextElement());
			if (!simpleTable.isManyToManyTable()) {
				System.err.println("-->> + " + simpleTable.getName());
				tablesForGeneration.add(simpleTable);

				Iterator<Column> itFKC = simpleTable.getSortedColumns();
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

			Iterator<Column> columnsSortedColumns = table.getSortedColumns();
			List<Column> definitiveColumns = new ArrayList();
			while (columnsSortedColumns.hasNext()) {
				Column c = columnsSortedColumns.next();
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
								
								//if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
								//	fTable = dbSet.getTable(table.getFKReferenceTable(column.getName()).getTableName());									
								//	ps.println("    private "+fTable.getJPAPKClass().replace("java.lang.", "")+" "+FormatString.renameForJavaMethod(column.getName())+";");
								//} else {									
									ps.println("    private " + column.getJavaClassType().replace("java.lang.", "") + " " + column.getJavaDeclaredObjectName() + ";");
								//}								
							} else if (lineInLoop.indexOf("${tablebean.member.getter}") >= 0) {
								
								//if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
								//	ps.println("    public "+fTable.getJPAPKClass().replace("java.lang.", "")+"  get"+FormatString.getCadenaHungara(column.getName())+"() {");									
								//	ps.println("        return this." + FormatString.renameForJavaMethod(column.getName()) + ";");
								//} else {
									ps.println("    public "+column.getJavaClassType().replace("java.lang.", "")+" get"+FormatString.getCadenaHungara(column.getName())+"() {");
									ps.println("        return this." + column.getJavaDeclaredObjectName() + ";");								
								//}
								ps.println("    }");

							} else if (lineInLoop.indexOf("${tablebean.member.setter}") >= 0) {
								//if (column.isForeignKey() && !(table instanceof EmbeddeableColumn)) {
								//	ps.println("    public void set"+FormatString.getCadenaHungara(column.getName())+"("+fTable.getJPAPKClass().replace("java.lang.", "")+" id) {");									
								//	ps.println("        this." + column.getJavaDeclaredObjectName() + " = id;");
								//} else {
									ps.println("    public void set"+FormatString.getCadenaHungara(column.getName())+"("+column.getJavaClassType().replace("java.lang.", "")+" v) {");
									ps.println("        this." + column.getJavaDeclaredObjectName() + " = v;");								
								//}
								ps.println("    }");

							} else {
								ps.println(lineInLoop);
							}
						}
					}

					linesToParse = null;
				} else if (linesToParse != null) {
					linesToParse.add(line);
				} else {
					line = line.replace("${date}", sdf.format(new Date()));
					line = line.replace("${tablebean.serialId}", String.valueOf(table.hashCode()));
					line = line.replace("${tablebean.name}", table.getName());
					line = line.replace("${tablebean.declaredName}", table.getJavaDeclaredName());
					line = line.replace("${tablebean.PKMembersParameters}", membersParameters(table, dbSet));
					line = line.replace("${tablebean.hashCodeSumCode}", table.getSimpleHashCodeSumCode());
					line = line.replace("${tablebean.PKMembersParametersInitCode}", membersParametersInitCode(table, dbSet));
					line = line.replace("${tablebean.equalsCode}", table.getSimpleEqualsCode());
					line = line.replace("${tablebean.serializeCode}", serializeCode(table));
					line = line.replace("${tablebean.scanCode}", scanCode(table));
					line = line.replace("${tablebean.equalsCode}", table.getSimpleEqualsCode());
					line = line.replace("${tablebean.toStringCode}", table.getToDTOStringCode(dbSet, dtoPackageBeanMember));
					line = line.replace("${tablebean.name.uc}", table.getName().toUpperCase());
					line = line.replace("${tablebean.package}", dtoPackageBeanMember);

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
			} else {
				//System.err.println("-->> [X] Many 2 Many : " + simpleTable.getName());
			}

		}
		//System.err.println("==============================>>> ");
		for (Table table : tablesForGeneration) {

			System.err.println("-->> generating: " + table.getJavaDeclaredName() + "DAO.java :" + table);

			Iterator<Column> columnsSortedColumns = table.getSortedColumns();
			List<Column> definitiveColumns = new ArrayList();
			while (columnsSortedColumns.hasNext()) {
				Column c = columnsSortedColumns.next();
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
			StringBuffer sbColsInsert= new StringBuffer();
			StringBuffer sbColsParams= new StringBuffer();
			StringBuffer sbColsParamsInsert= new StringBuffer();
			Iterator<Column> itCols = table.getSortedColumns();
			int pkc=0;
			int npkc=0;
			int nci=0;
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
					if(!col.isAutoIncremment() ){
						if( nci>0){
							sbColsInsert.append(",");
							sbColsParamsInsert.append(",");
						}
						sbColsInsert.append(col.getName().toUpperCase());
						sbColsParamsInsert.append("?");
						nci++;
					}
				} else{
					if(npkc>0){
						sbPKCols.append(",");
						sbColsUpdate.append(",");												
					}
					if(nci>0){
						sbColsInsert.append(",");
						sbColsParamsInsert.append(",");
					}
					
					sbColsUpdate.append(col.getName().toUpperCase());
					sbColsUpdate.append("=?");
					
					sbColsInsert.append(col.getName().toUpperCase());
					sbColsParamsInsert.append("?");
					npkc++;
					nci++;
				}
			}
			
			String allCols=sbCols.toString();
			String pksCols=sbPKCols.toString();
			String collToUpdate=sbColsUpdate.toString();
			String collToInsert=sbColsInsert.toString();
			String colsParams=sbColsParams.toString();
			String colsParamsInsert=sbColsParamsInsert.toString();
			HashSet<String> nullablesJavaTypes=new HashSet<String>();
			
			nullablesJavaTypes.add("java.lang.Integer");
			nullablesJavaTypes.add("java.lang.Double");
			nullablesJavaTypes.add("java.lang.String");
			nullablesJavaTypes.add("java.util.Date");
			nullablesJavaTypes.add("java.sql.Timestamp");
			nullablesJavaTypes.add("java.sql.Date");
			nullablesJavaTypes.add("byte[]");
			nullablesJavaTypes.add("java.lang.Boolean");
			nullablesJavaTypes.add("java.lang.Long");
			
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
							if(lineIL.contains("${tablebean.pk.autoGeneratedFilter}") && column.isPrimaryKey() && column.isAutoIncremment()){
								lineIL = lineIL.replace("${tablebean.pk.autoGeneratedFilter}", "//");
							} else {
								lineIL = lineIL.replace("${tablebean.pk.autoGeneratedFilter}", "");
							}
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
								lineIL = lineIL.replace("${tablebean.member.javaObjectClass}", FormatString.firstLetterUpperCase(jc.replace("java.lang.", "").replace("java.sql.", "").replace("java.util.", "").replace("int","integer")));
								lineIL = lineIL.replace("${tablebean.member.javaClass}", FormatString.firstLetterUpperCase(jc.replace("java.lang.", "").replace("java.sql.", "").replace("java.util.", "").replace("Integer","Int")));
							}
							
							String pkSetNullInRSExpression="";
							//ps.println("\t\t\t//"+column.getJavaDeclaredName()+"("+column.isNullable()+"):"+column.getJavaClassType()+" in {"+nullablesJavaTypes+"}?"+nullablesJavaTypes.contains(column.getJavaClassType()));
							if(lineIL.contains("${tablebean.member.pkSetNullInRSExpression}")){
								if (column.isNullable() && nullablesJavaTypes.contains(column.getJavaClassType())){									
									pkSetNullInRSExpression = "if(x.get"+column.getJavaDeclaredName()+"()==null) ps.setObject(ci++,null); else ";
								}
								lineIL = lineIL.replace("${tablebean.member.pkSetNullInRSExpression}", pkSetNullInRSExpression);
							}
							
							if(column.isPrimaryKey()){
								if(lineIL.contains("${tablebean.memberNotPK.javaClass}")){
									skipPKMemberLine=true;
								} else {									
									lineIL = lineIL.replace("${tablebean.memberPK.setter}", "set"+column.getJavaDeclaredName());
									lineIL = lineIL.replace("${tablebean.memberPK.getter}", "get"+column.getJavaDeclaredName());									
									if(jc.equals("java.util.Date")&& !lineIL.contains("if(")){
										lineIL = lineIL.replace("${tablebean.memberNotPK.javaClass}", FormatString.firstLetterUpperCase(jc.replace("java.util.", "").replace("java.sql.", "")));
										if(lineIL.contains("x.get")){
											lineIL = lineIL.replace("x.get", "new java.sql.Date(x.get").replace(");", ".getTime()));");										
										}
									}else {
										lineIL = lineIL.replace("${tablebean.memberPK.javaClass}", FormatString.firstLetterUpperCase(jc.replace("java.lang.", "").replace("java.sql.", "").replace("java.util.", "").replace("Integer","Int")));
									}									
								}
							}else{								
								lineIL = lineIL.replace("${tablebean.memberNotPK.name}", column.getName().toUpperCase());
								lineIL = lineIL.replace("${tablebean.memberNotPK.setter}", "set"+column.getJavaDeclaredName());
								lineIL = lineIL.replace("${tablebean.memberNotPK.getter}", "get"+column.getJavaDeclaredName());
																	
								if(jc.equals("java.util.Date") && !lineIL.contains("if(")){
									lineIL = lineIL.replace("${tablebean.memberNotPK.javaClass}", FormatString.firstLetterUpperCase(jc.replace("java.util.", "").replace("java.sql.", "")));
									
									if(lineIL.contains("x.get")){
										lineIL = lineIL.replace("x.get", "new java.sql.Date(x.get").replace(");", ".getTime()));");										
									}
								}else{
									lineIL = lineIL.replace("${tablebean.memberNotPK.javaClass}", FormatString.firstLetterUpperCase(jc.replace("java.lang.", "").replace("java.util.", "").replace("java.sql.", "").replace("Integer","Int")));
								};
							}
							lineIL = lineIL.replace("${tablebean.member.name}", column.getName().toUpperCase());
							lineIL = lineIL.replace("${tablebean.member.getter}", "get"+column.getJavaDeclaredName());
							
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
										
					line = line.replace("${tablebean.pk.name}", tablePKColumn.getName().toUpperCase());
					line = line.replace("${tablebean.pk.javaObjectClass}", tablePKColumn.getJavaClassType().replace("java.lang.", "").replace("java.sql.", "").replace("java.util.", ""));
					line = line.replace("${tablebean.pk.javaClass}", tablePKColumn.getJavaClassType().replace("java.lang.", "").replace("java.sql.", "").replace("java.util.", "").replace("Integer","Int"));
					line = line.replace("${tablebean.getPK}", "get"+tablePKColumn.getJavaDeclaredName());
					line = line.replace("${tablebean.setPK}", "set"+tablePKColumn.getJavaDeclaredName());
					
					line = line.replace("${tablebean.hashCodeSumCode}", table.getSimpleHashCodeSumCode());
					line = line.replace("${tablebean.equalsCode}", table.getSimpleEqualsCode());
					line = line.replace("${tablebean.toStringCode}", table.getToDTOStringCode(dbSet, dtoPackageBeanMember));
					line = line.replace("${tablebean.package}", dtoPackageBeanMember);
					line = line.replace("${tablebean.listColumns}", allCols);
					line = line.replace("${tablebean.listParamColumns}", colsParams);
					line = line.replace("${tablebean.listParamColumns4Insert}", colsParamsInsert);
					line = line.replace("${tablebean.listColumns4Update}", collToUpdate);
					line = line.replace("${tablebean.listColumns4Insert}", collToInsert);
					
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

		Iterator<Column> simpleColumnsIterator = table.getSortedColumns();

		for (int numColumnGenerating = 0; simpleColumnsIterator.hasNext(); numColumnGenerating++) {

			Column column = simpleColumnsIterator.next();
			if (!column.isPrimaryKey()) {
				continue;
			}

			if (numColumnGenerating > 0) {
				sb.append(", ");
			}

			varClassName = column.getJavaClassType().replace("java.lang.", "");
			varName = column.getJavaDeclaredObjectName();

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

		Iterator<Column> simpleColumnsIterator = table.getSortedColumns();

		for (int numColumnGenerating = 0; simpleColumnsIterator.hasNext(); numColumnGenerating++) {

			Column column = simpleColumnsIterator.next();
			if (!column.isPrimaryKey()) {
				continue;
			}

			if (numColumnGenerating > 0) {
				sb.append("        ");
			}

			varClassName = column.getJavaClassType().replace("java.lang.", "");
			varName = column.getJavaDeclaredObjectName();

			sb.append("this.");
			sb.append(varName);
			sb.append(" \t= \t");
			sb.append(varName);
			sb.append(";\n");
		}

		return sb.toString();
	}

	private static String serializeCode(Table table) {
		StringBuffer sb = new StringBuffer();

		String varName = null;
		String varClassName = null;


		Iterator<Column> simpleColumnsIterator = table.getSortedColumns();
		sb.append("\n");
		for (int numColumnGenerating = 0; simpleColumnsIterator.hasNext(); numColumnGenerating++) {

			Column column = simpleColumnsIterator.next();
			varClassName = column.getJavaClassType().replace("java.lang.", "");
			varName = column.getJavaDeclaredObjectName();
			if(numColumnGenerating>0){
				sb.append("\t\tsb.append(s);\n");
			}
			sb.append("\t\t// "+varClassName+"\n");
			
			if(varClassName.equals("String")||varClassName.equals("Integer")||varClassName.equals("int")){
				sb.append("\t\tsb.append(this.");
				sb.append(varName);
			} else if(varClassName.equals("double")||varClassName.equals("Double")){
				sb.append("\t\tsb.append( df.format(this.");
				sb.append(varName);
				sb.append(")");
			} else if(varClassName.equals("java.util.Date")){
				sb.append("\t\tsb.append(this."+varName+"==null?\"null\":sdf.format(this.");
				sb.append(varName);
				sb.append(")");
			} else if(varClassName.equals("java.sql.Timestamp")){
				sb.append("\t\tsb.append(this."+varName+"==null?\"null\":sdf.format(this.");
				sb.append(varName);
				sb.append(")");
			} else if(varClassName.equals("byte[]")){
				sb.append("\t\tsb.append(this."+varName+"==null?\"null\":javax.xml.bind.DatatypeConverter.printBase64Binary((this.");
				sb.append(varName);
				sb.append("))");
			}
			sb.append(");\n");
		}

		return sb.toString();
	}	
	
	private static String scanCode(Table table) {
		StringBuffer sb = new StringBuffer();

		String varName = null;
		String varClassName = null;


		Iterator<Column> simpleColumnsIterator = table.getSortedColumns();
		sb.append("\n");
		for (int numColumnGenerating = 0; simpleColumnsIterator.hasNext(); numColumnGenerating++) {
			Column column = simpleColumnsIterator.next();
			varClassName = column.getJavaClassType().replace("java.lang.", "");
			varName = column.getJavaDeclaredObjectName();
			if(numColumnGenerating>0){
				
			}
			sb.append("\t\t\t// "+varClassName+"\n");
			if(varClassName.equals("String")){
				sb.append("\t\t\tthis.");
				sb.append(varName);
				sb.append(" = ");						
				sb.append("srcSpplited[nf].equals(\"null\")?null:srcSpplited[nf];\n");
			} else if(varClassName.equals("Integer")||varClassName.equals("int")){
				sb.append("\t\t\tthis.");
				sb.append(varName);
				sb.append(" = ");						
				sb.append(" Integer.parseInt(srcSpplited[nf]);\n");			
			} else if(varClassName.equals("double")||varClassName.equals("Double")){
				sb.append("\t\t\tthis.");
				sb.append(varName);
				sb.append(" = ");						
				sb.append(" df.parse(srcSpplited[nf]).doubleValue();\n");			
			} else if(varClassName.equals("java.util.Date")){
				sb.append("\t\t\tthis.");
				sb.append(varName);
				sb.append(" = ");						
				sb.append(" srcSpplited[nf].equals(\"null\")?null:sdf.parse(srcSpplited[nf]);\n");			
			} else if(varClassName.equals("java.sql.Timestamp")){
				sb.append("\t\t\tthis.");
				sb.append(varName);
				sb.append(" = ");						
				sb.append(" srcSpplited[nf].equals(\"null\")?null:new java.sql.Timestamp(sdf.parse(srcSpplited[nf]).getTime());\n");			
			} else if(varClassName.equals("byte[]")){
				sb.append("\t\t\tthis.");
				sb.append(varName);
				sb.append(" = ");						
				sb.append(" srcSpplited[nf].equals(\"null\")?null:javax.xml.bind.DatatypeConverter.parseBase64Binary(srcSpplited[nf]);\n");			
			}
			
			sb.append("\t\t\tnf++;\n");
		}

		return sb.toString();
	}	
}
