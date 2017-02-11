package com.tracktopell.dao.builder.jpa;

import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.parser.VP6Parser;
import com.tracktopell.dao.builder.parser.VPModel;
import java.io.FileInputStream;
import java.util.Hashtable;

/**
 * java com.tracktopell.dao.builder.jpa.VPModel2DTOAndBeans
 * VPModel2SQL
 */
public class VPModel2DTOAndBeans {

    public static void main(String[] args) {
        String   pathToVPProject  = null;
        String   dtoPackageBeanMember= null;
		String   jpaPackageBeanMember= null;
        String   basePath         = null;
        String[] tableNames2Gen   = null;
        boolean  flatDTOs         = false;
		try {
            if( args.length != 6 ) {
                System.err.println("use: <java ...> VPModel2DTOAndBeans  pathToVPProject  dtoPackageBeanMember jpaPackageBeanMember basePath   flatDTOs  [ tableNames2GenList,Separated,By,Comma | {all} ]" );
                System.exit(1);
            }

            pathToVPProject		= args[0];
            dtoPackageBeanMember= args[1];
			jpaPackageBeanMember= args[2];
            basePath			= args[3];
			flatDTOs			= args[4].trim().equalsIgnoreCase("true");
            tableNames2Gen		= args[5].split(",");

            Hashtable<String, VPModel> vpModels;
            vpModels = VP6Parser.loadVPModels(new FileInputStream(pathToVPProject));

            //System.err.println("DBBuilderFactory ->vpModels=" + vpModels);
            DBTableSet dbSet;
            dbSet = VP6Parser.loadFromXMLWithVPModels(new FileInputStream(pathToVPProject), vpModels);

            if(!tableNames2Gen[0].equals("{all}")){
                dbSet = dbSet.copyJustSelectedNames(tableNames2Gen);
            }

            //System.out.println("====================== END PARSE XML ========================");
            System.out.println("->" + dbSet);
			
			DTOBeanBuilder.buildMappingDTOBeansAndJPABeans(dbSet, dtoPackageBeanMember, jpaPackageBeanMember, basePath, flatDTOs );
			
			DTOBeanBuilder.buildAssembler(dbSet, dtoPackageBeanMember, jpaPackageBeanMember, basePath);
			
			JPABeanBuilder.buildMappingBeans(dbSet, jpaPackageBeanMember, basePath);
            
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
