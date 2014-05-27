package com.tracktopell.dao.builder.jpa;

import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.parser.VP6Parser;
import com.tracktopell.dao.builder.parser.VPModel;
import java.io.FileInputStream;
import java.util.Hashtable;

/**
 * VPModel2SQL
 */
public class VPModel2JPA {

    public static void main(String[] args) {
        String  pathToVPProject  = null;
        String  schemmaName      = null;
        String  packageBeanMember= null;
        String  basePath         = null;
        String[]tableNames2Gen   = null;
        try {

            if( args.length != 4) {
                System.err.println("use: <java ...> VPModel2JPA  pathToVPProject  packageBeanMember  basePath   tableNames2GenList,Separated,By,Comma" );
                System.exit(1);
            }


            pathToVPProject  = args[0];
            packageBeanMember= args[1];
            basePath         = args[2];
            tableNames2Gen   = args[3].split(",");

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

            JPABeanBuilder.buildMappingBeans(dbSet, packageBeanMember, basePath);

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
