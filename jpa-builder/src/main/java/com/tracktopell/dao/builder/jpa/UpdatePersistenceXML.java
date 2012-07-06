package com.tracktopell.dao.builder.jpa;

import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.parser.VP6Parser;
import com.tracktopell.dao.builder.parser.VPModel;
import java.io.FileInputStream;
import java.util.Hashtable;

/**
 * UpdatePersistenceXML
 */
public class UpdatePersistenceXML {

    public static void main(String[] args) {
        String  pathToVPProject         = null;
        String  path_2_Parsistence_xml  = null;
        String  packageBeanMember       = null;
        String[]tableNames2Gen          = null;
        try {

            if( args.length != 4) {
                System.err.print("use: <java ...> UpdatePersistenceXML pathToVPProject path_2_Parsistence_xml  packageBeanMember tableNames2GenList,Separated,By,Comma" );
                System.exit(1);
            }

            pathToVPProject         = args[0];
            path_2_Parsistence_xml  = args[1];
            packageBeanMember       = args[2];
            tableNames2Gen   = args[3].split(",");

            Hashtable<String, VPModel> vpModels;
            vpModels = VP6Parser.loadVPModels(new FileInputStream(pathToVPProject));
            
            DBTableSet dbSet;
            dbSet = VP6Parser.loadFromXMLWithVPModels(new FileInputStream(pathToVPProject), vpModels);

            if(!tableNames2Gen[0].equals("{all}")){
                dbSet = dbSet.copyJustSelectedNames(tableNames2Gen);
            }

            JPABeanBuilder.updatePersistenceXML(dbSet, packageBeanMember, path_2_Parsistence_xml);

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
