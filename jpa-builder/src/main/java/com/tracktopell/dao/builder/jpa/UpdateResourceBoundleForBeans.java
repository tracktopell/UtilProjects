package com.tracktopell.dao.builder.jpa;

import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.parser.VP6Parser;
import com.tracktopell.dao.builder.parser.VPModel;
import java.io.FileInputStream;
import java.util.Hashtable;

/**
 * UpdatePersistenceXML
 */
public class UpdateResourceBoundleForBeans {

    public static void main(String[] args) {
        String  pathToVPProject         = null;
        String[]tableNames2Gen          = null;
        try {
			//int argc=0;
			//System.err.print("\t->UpdateResourceBoundleForBeans args.length="+args.length);				
			//for(String argx:args){					
			//	System.err.print("\t->args["+(argc++)+"]="+argx);				
			//}

            if( args.length != 2) {
                System.err.print("use: <java ...> UpdateResourceBoundleForBeans pathToVPProject  tableNames2GenList,Separated,By,Comma" );
                System.exit(1);
            }

            pathToVPProject         = args[0];
            tableNames2Gen			= args[1].split(",");

            Hashtable<String, VPModel> vpModels;
            vpModels = VP6Parser.loadVPModels(new FileInputStream(pathToVPProject));
            
            DBTableSet dbSet;
            dbSet = VP6Parser.loadFromXMLWithVPModels(new FileInputStream(pathToVPProject), vpModels);

            if(!tableNames2Gen[0].equals("{all}")){
                dbSet = dbSet.copyJustSelectedNames(tableNames2Gen);
            }

            JPABeanBuilder.buildReourceBoundleBeans(dbSet, "");

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
