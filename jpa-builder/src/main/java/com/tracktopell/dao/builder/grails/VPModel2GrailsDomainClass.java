package com.tracktopell.dao.builder.grails;

import com.tracktopell.dao.builder.jpa.*;
import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.parser.VP6Parser;
import com.tracktopell.dao.builder.parser.VPModel;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * VPModel2SQL
 */
public class VPModel2GrailsDomainClass {

    public static void main(String[] args) {
        String  pathToVPProject  = null;
        String  schemmaName      = null;
        String  packageBeanMember= null;
        String  basePath         = null;
        String[]tableNames2Gen   = null;
        try {

            if( args.length != 5) {
                System.err.print("use: <java ...> VPModel2GrailsDomainClass  pathToVPProject  schemmaName  packageBeanMember  basePath   tableNames2GenList,Separated,By,Comma" );
                System.exit(1);
            }


            pathToVPProject  = args[0];
            schemmaName      = args[1];
            packageBeanMember= args[2];
            basePath         = args[3];
            tableNames2Gen   = args[4].split(",");

            Hashtable<String, VPModel> vpModels;
            vpModels = VP6Parser.loadVPModels(new FileInputStream(pathToVPProject));

            //System.err.println("DBBuilderFactory ->vpModels=" + vpModels);
            DBTableSet dbSet;
            dbSet = VP6Parser.loadFromXMLWithVPModels(new FileInputStream(pathToVPProject), vpModels);
			Enumeration<String> tableNamesEnum = dbSet.getTableNames();
			int numTN = 0;
			System.err.print("OriginalTableNames: {");
			while(tableNamesEnum.hasMoreElements()){
				String originalTableName = tableNamesEnum.nextElement();
				if(numTN++>0){
					System.err.print(",");
				}
				System.err.print(originalTableName);
			}
			System.err.println("}");

            if(!tableNames2Gen[0].equals("{all}")){
                dbSet = dbSet.copyJustSelectedNames(tableNames2Gen);
            }

            //System.out.println("====================== END PARSE XML ========================");
            //System.out.println("->" + dbSet);

            DomainClassBuilder.buildMappingBeans(dbSet, packageBeanMember, schemmaName, basePath);

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
