package com.tracktopell.dao.builder.jdbc;

import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.parser.VP6Parser;
import com.tracktopell.dao.builder.parser.VPModel;
import java.io.FileInputStream;
import java.util.Hashtable;

/**
 * VPModel2DAO
 */
public class VPModel2DAO {

    public static void main(String[] args) {
        String  pathToVPProject  = null;
        String  schemmaName      = null;
        String  packageBeanMember= null;
		String  packageDAOMember= null;
        String  basePath         = null;
        String[]tableNames2Gen   = null;
        try {

            if( args.length != 6) {
                System.err.println("use: <java ...> DAOBuilder pathToVPProject  schemmaName  packageBeanMember  packageDAOMember  basePath   tableNames2GenList,Separated,By,Comma" );
                System.exit(1);
            }


            pathToVPProject  = args[0];
            schemmaName      = args[1];
            packageBeanMember= args[2];
			packageDAOMember = args[3];
            basePath         = args[4];
            tableNames2Gen   = args[5].split(",");

            Hashtable<String, VPModel> vpModels;
            vpModels = VP6Parser.loadVPModels(new FileInputStream(pathToVPProject));

            //System.err.println("DBBuilderFactory ->vpModels=" + vpModels);
            DBTableSet dbSet;
            dbSet = VP6Parser.loadFromXMLWithVPModels(new FileInputStream(pathToVPProject), vpModels);

            if(!tableNames2Gen[0].equals("{all}")){
                dbSet = dbSet.copyJustSelectedNames(tableNames2Gen);
            }

            //System.out.println("====================== END PARSE XML ========================");
            //System.out.println("->" + dbSet);

            CodeBuilder.buildDAOs(dbSet, packageBeanMember,packageDAOMember,basePath);
			

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
