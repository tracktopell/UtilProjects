/**
 * Archivo: JDBCToJPABeans.java
 *
 * Fecha de Creaci&oacute;n: 1/06/2011
 *
 * 2H Software - Bursatec 2011
 */
package com.tracktopell.dao.builder.dbextractor;

import com.tracktopell.dao.builder.FormatString;
import com.tracktopell.dao.builder.jpa.JPABeanBuilder;
import com.tracktopell.dao.builder.metadata.*;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 *
 * @author Alfredo Estrada Gonz&aacute;lez.
 * @version 1.0
 *
 */
public class JDBCToJPABeans {

    public static void main(String args[]) {

        if (args.length != 8) {
            System.err.print("use: <java ...> JDBCToJPABeans  jdbcDriver urlConnection user password schemma tableNames2GenList,Separated,By,Comma  packageBeanMember  basePath");
            System.exit(1);
        }

        String driver = args[0]; //"oracle.jdbc.driver.OracleDriver";
        String connection = args[1]; //"jdbc:oracle:thin:@10.100.230.18:1521:ODSDESA";
        String user = args[2]; //"APPS_EMI";
        String password = args[3]; //"EMI_D3V1RT";

        String schemma = args[4]; //"ADMIN_EMI";
        String[] tables = args[5].split(","); //"EMI_CCONCEPTOS_MULTIP_PREFIJOS";
        String packageBeanMember = args[6];
        String basePath = args[7];

        try {
            System.out.println("===>>>Connecting for retrieve Data");
            Class.forName(driver);
            Connection con = DriverManager.getConnection(connection, user, password);

            Statement st = con.createStatement();

            List<String> tableList = new ArrayList<String>();

            for (String table : tables) {
                tableList.add(table);
            }
            DBTableSet dbSet = extractDBSet(con, schemma, tableList);
            System.out.println("====>>Ok after extractDBSet:");
            System.out.println(dbSet);

            System.out.println("==============================-------================================");
            List<String> foreignTableNames = new ArrayList<String>();

            Enumeration<Table> tablesElements = dbSet.getTablesElements();
            while (tablesElements.hasMoreElements()) {
                Table tableE = tablesElements.nextElement();
                Collection<ReferenceTable> fKReferenceTables = tableE.getFKReferenceTables();
                for (ReferenceTable rt : fKReferenceTables) {
                    foreignTableNames.add(rt.getTableName());
                }
            }


            for (String foreignTableName : foreignTableNames) {
                if (dbSet.getTable(foreignTableName) == null) {
                    System.out.println("=> " + foreignTableName + " IS MISSING FOR FOREIGN REFERENCES!");
                }
            }

            JPABeanBuilder.buildMappingBeans(dbSet, packageBeanMember, basePath);

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public static DBTableSet extractDBSet(Connection conn, String schemma, List<String> tableList) {
        DBTableSet dbSet = null;

        Statement st = null;
        dbSet = new DBTableSet();

        try {
            DatabaseMetaData dbMetaData = conn.getMetaData();
            ResultSet schemasRS = dbMetaData.getSchemas();

            //System.out.println("TABLE_SCHEM");
            String realSchemma = null;
            while (schemasRS.next()) {
                if (schemasRS.getString(1).equalsIgnoreCase(schemma)) {
                    realSchemma = schemasRS.getString(1);
                    //System.out.println("->" + realSchemma);
                }
            }
            schemasRS.close();

            if (realSchemma == null) {
                throw new Exception("Schemma not found !");
            }

            //System.out.println("==============================-------================================");

            for (String tableName : tableList) {

                Table table = new Table();

                table.setName(tableName);
                table.setSchemma(realSchemma);

                ResultSet rsColumns = dbMetaData.getColumns(null, realSchemma, tableName, null);
                //System.out.println("========>> List of Columns for table: "+tableName);
                while (rsColumns.next()) {

                    Column column = new SimpleColumn();

                    column.setName(rsColumns.getString("COLUMN_NAME"));
                    //column.setAutoIncremment();
                    column.setSqlType(getSQLTypeFor(rsColumns.getInt("DATA_TYPE")).toLowerCase());
                    column.setPosition(rsColumns.getInt("ORDINAL_POSITION"));
                    column.setPrecision(rsColumns.getInt("DECIMAL_DIGITS"));
                    column.setScale(rsColumns.getInt("COLUMN_SIZE"));
                    column.setNullable(rsColumns.getInt("NULLABLE") == 1);
                    column.setJavaClassType(SQLTypesToJavaTypes.getTypeFor(column.getSqlType()));

                    if (!column.isNullable()) {
                        if (!column.isPrimaryKey()
                                && (SQLTypesToJavaTypes.getTypeFor(column.getSqlType()).endsWith("Double") || SQLTypesToJavaTypes.getTypeFor(column.getSqlType()).endsWith("Float"))
                                && column.getPrecision() == 0) {

                            column.setJavaClassType("int");
                        } else {
                            column.fixBestJavaClassForSQLType();
                        }
                    }

                    if (column.isNullable()
                            && (SQLTypesToJavaTypes.getTypeFor(column.getSqlType()).endsWith("Double")
                            || SQLTypesToJavaTypes.getTypeFor(column.getSqlType()).endsWith("Float"))
                            && column.getPrecision() == 0) {
                        column.setJavaClassType(Integer.class.toString().replace("class ", ""));
                    }

                    /*
                     * System.out.println( rsColumns.getInt("ORDINAL_POSITION")+
                     * " : " + rsColumns.getString("TABLE_NAME")+ "." +
                     * rsColumns.getString("COLUMN_NAME") + " " +
                     * getSQLTypeFor(rsColumns.getInt("DATA_TYPE")) + "(" +
                     * rsColumns.getString("COLUMN_SIZE") + "," +
                     * rsColumns.getString("DECIMAL_DIGITS")+ ") " + "
                     * nuLL?"+rsColumns.getInt("NULLABLE") );
                     */
                    table.addColumn(column);
                }
                rsColumns.close();

                ResultSet rsPKs = dbMetaData.getPrimaryKeys(null, realSchemma, tableName);
                System.out.println("==============================");
                System.out.println("List of PKs for table: ");
                ArrayList<Column> pkColumns = new ArrayList<Column>();
                
                while (rsPKs.next()) {
                    System.out.println(rsPKs.getString("TABLE_NAME") + "." + rsPKs.getString("COLUMN_NAME"));
                    Column pkColumn = table.getColumn(rsPKs.getString("COLUMN_NAME"));
                    pkColumn.setPrimaryKey(true);
                    pkColumns.add(pkColumn);
                }
                rsPKs.close();
                                
                ResultSet rsFKs = dbMetaData.getImportedKeys(null, realSchemma, tableName);
                //System.out.println("==============================");
                //System.out.println("List of FKs for table: ");
                ResultSetMetaData rsFKsMetaData = rsFKs.getMetaData();
                while (rsFKs.next()) {
                    /*
                     * System.out.print(rsFKs.getString("FKTABLE_NAME")+ "." +
                     * rsFKs.getString("FKCOLUMN_NAME")); System.out.print(" ->
                     * "); System.out.println(rsFKs.getString("PKTABLE_NAME")+
                     * "." + rsFKs.getString("PKCOLUMN_NAME"));
                     */
                    table.getColumn(rsFKs.getString("FKCOLUMN_NAME")).setForeignKey(true);
                    ReferenceTable rt = new ReferenceTable();
                    rt.setColumnName(rsFKs.getString("PKCOLUMN_NAME"));
                    rt.setTableName(rsFKs.getString("PKTABLE_NAME"));

                    table.addForeignKey(rsFKs.getString("FKCOLUMN_NAME"), rt);
                }
                rsFKs.close();
                //==============================================================
                if(pkColumns.size()>1){
                    System.out.println("===>>Lather shuld has embedeable columns");
                    /*
                    System.out.println("===>>Building composite Primary keys["+pkColumns.size()+"]");
                    EmbeddeableColumn embeddeablePK = null;
                    
                    embeddeablePK.setName(table.getName()+"_P_K");            
                    embeddeablePK.setJavaClassType(FormatString.getCadenaHungara(table.getName()+"_P_K"));
                    embeddeablePK.buildPosibleLabel();
                    embeddeablePK.setPrimaryKey(true);

                    for(Column cpk:pkColumns){
                        embeddeablePK.addColumn(cpk);
                        System.out.print("\t===>>removeing the composite PK from parent table:"+cpk.getName());
                        table.removeColumn(cpk);
                        System.out.println(" ? "+(table.getColumn(cpk.getName())==null));                        
                    }
                    System.out.print("===>>add EmebdableColumn:"+embeddeablePK);
                    table.addColumn(embeddeablePK);
                    */
                }

                dbSet.addTable(table);
            }
            //System.out.println("==============================-------================================");            
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        return dbSet;
    }
    private static Hashtable<Integer, String> sqlTypes;

    public static String getSQLTypeFor(int type) {

        if (sqlTypes == null) {
            sqlTypes = new Hashtable<Integer, String>();

            Class classTypes = java.sql.Types.class;
            Field[] declaredFields = classTypes.getDeclaredFields();

            for (Field f : declaredFields) {

                f.setAccessible(true);
                try {
                    //System.out.println(f.getName() + ": " + f.get(null));
                    sqlTypes.put(new Integer(f.get(null).toString()), f.getName());
                } catch (Exception ex) {
                    //System.out.println("X");
                }

            }
        }

        return sqlTypes.get(new Integer(type));
    }
}