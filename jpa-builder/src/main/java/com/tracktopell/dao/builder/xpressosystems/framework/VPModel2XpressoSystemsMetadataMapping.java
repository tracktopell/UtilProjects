package com.tracktopell.dao.builder.xpressosystems.framework;

import com.tracktopell.dao.builder.metadata.Column;
import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.metadata.Table;
import com.tracktopell.dao.builder.parser.VP6Parser;
import com.tracktopell.dao.builder.parser.VP6ParserForXpressoSystems;
import com.tracktopell.dao.builder.parser.VPModel;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * VPModel2XpressoSystemsMetadataMapping
 */
public class VPModel2XpressoSystemsMetadataMapping {

	public static void main(String[] args) {
		String pathToVPProject = null;
		String schemmaName = null;
		String outputPath = null;
		String[] tableNames2Gen = null;
		if(args.length != 3){
			System.err.println("usage: pathToVPProject  outputPath  tableNames2Gen");
			System.exit(1);
		}
		
		try {
			pathToVPProject = args[0];
			outputPath = args[1];
			tableNames2Gen = args[2].split(",");

			Hashtable<String, VPModel> vpModels;
			vpModels = VP6Parser.loadVPModels(new FileInputStream(pathToVPProject));

			//System.err.println("DBBuilderFactory ->vpModels=" + vpModels);
			DBTableSet dbSet;
			dbSet = VP6ParserForXpressoSystems.loadFromXMLWithVPModels(new FileInputStream(pathToVPProject), vpModels);

			if (!tableNames2Gen[0].equals("{all}")) {
				dbSet = dbSet.copyJustSelectedNames(tableNames2Gen);
			}

			System.err.println("====================== END PARSE XML ========================");
			//System.err.println("->" + dbSet);

			//System.out.println("->createDBSchema:");
			createMetaDataMapingSQL(dbSet, new PrintStream(new FileOutputStream(outputPath)));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void createMetaDataMapingSQL(DBTableSet dbSet, PrintStream out) {
		List<Table> lt = dbSet.getTablesSortedForCreation();

		Hashtable<String,Integer> propsDecorator = new Hashtable<String,Integer>();
		
		propsDecorator.put("cols",1);
		propsDecorator.put("disabled",2);
		propsDecorator.put("maxlength",3);
		propsDecorator.put("required",4);
		propsDecorator.put("rows",5);
		propsDecorator.put("label",6);
		propsDecorator.put("size",7);
		propsDecorator.put("rendered",8);
		propsDecorator.put("onblur",9);
		propsDecorator.put("onkeyup",10);
		propsDecorator.put("style",11);
		propsDecorator.put("styleClass",12);
		propsDecorator.put("selectItems",13);
		
		
		out.println("-- ============================= TABLES (" + lt.size() + ") =======================");
		int tableId = 1;
		int campoId = 1;
		for (Table t : lt) {
			out.println("");
			out.println("-- " + t.getName());
			Hashtable<String, String> metaProperties = t.getMetaProperties();
			out.println("INSERT INTO c_catalogo VALUES (" + tableId + ",'"
					+ t.getName() + "','"
					+ t.getName().toLowerCase() + "','"
					+ metaProperties.get("atributos_consulta") + "','"
					+ metaProperties.get("atributos_orderby") + "','"
					+ metaProperties.get("estilo_columnas") + "');");

			Iterator<Column> it = t.getSortedColumns();
			Column col = null;
			StringBuffer pkBuffer = new StringBuffer("PRIMARY KEY (");
			
			while (it.hasNext()) {
				col = it.next();
				Hashtable<String, String> metaPropertiesCol = col.getMetaProperties();
				//System.err.println("--->>column: "+col.getName()+", properties="+metaPropertiesCol);
				out.println("INSERT INTO campo VALUES ("+
						tableId+","+
						campoId+","+
						metaPropertiesCol.get("tipo_campo")+","+
						metaPropertiesCol.get("componente")+",'"+
						col.getName().toLowerCase()+"','"+
						metaPropertiesCol.get("descripcion")+"',"+
						metaPropertiesCol.get("tipo_dato")+","+
						col.getPosition()+");");
				
				Set<String> keySet = metaPropertiesCol.keySet();
				for(String k: keySet){
					//System.err.println("\t--->>"+propsDecorator+".contains("+k+")?");
					boolean containsKey=false;
					for(String kd:propsDecorator.keySet()){
						if(kd.equals(k)){
							containsKey=true;
							//System.err.println("\t\t-->> OK , contains , shuld be printed:"+k+"!");
							break;
						}
					}
					if(containsKey){
						out.println("INSERT INTO campo_atributo_valor VALUES ("+campoId+","+				
									propsDecorator.get(k)+",'"+
									metaPropertiesCol.get(k) +"'); -- "+k);				
					}
				}
				
				
				campoId++;
			}
			
			tableId++;
		}

	}
}

