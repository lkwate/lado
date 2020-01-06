package dataBaseManager;

import dataDefinition.BidirectionalLinkedHashMap;
import dataDefinition.Row;
import dataDefinition.SpreadSheet;
import exceptions.CodeOutOfBoundsException;
import exceptions.ImpossibleToInsertException;
import exceptions.NotUniqueTableInQueryException;
import manageMomento.CareTaker;
import manageMomento.Originator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class KeyGenerator {

    /**
     * attributes
     */
    private static LinkedHashMap<String, Row> columnInformation = new LinkedHashMap<>();
    private static BidirectionalLinkedHashMap<String, Integer> indexColumnName;
    private static String table = "tableCode"; // name of table "tableCode"
    private static String tableName = "tableName"; // name of table in a given record of resultSet come from Tablecode
    private static String length_code = "length_code";
    private static String prefix_code = "prefix_code";
    private static String suffix_code = "suffix_code";
    private static String index_code = "index_code";
    private static String code = "code";
    private static String code_unique = "code_unique";
    private static List<String> keySet = new ArrayList<>();
    public static Set<String> generateKey = new HashSet<>();

    static  {
        KeyGenerator.keySet.add(KeyGenerator.code);
        KeyGenerator.generateKey.add(KeyGenerator.code_unique);
        KeyGenerator.generateKey.add(KeyGenerator.code);
    }

    /**
     *
     * @param catalog
     * @param schema
     * @param tableName
     * @return an LinkedHashMap which contains the name of column belongs to the keyset of table and its corresponding autogenerated value
     */
    public static  LinkedHashMap<String, String> generateNextKey(Connection con, String catalog, String schema, String tableName) throws CodeOutOfBoundsException, CloneNotSupportedException, SQLException, ImpossibleToInsertException {
        /**
         * check integrity of arguments
         */
        if (tableName == null || !KeyGenerator.columnInformation.containsKey(tableName)) throw new IllegalArgumentException();
        /**
         * key Generation
         */
        /**
         * loop over key column and unique code
         */
        String[] keys = {KeyGenerator.code_unique, tableName};
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (String key : keys) {
            Row row = KeyGenerator.columnInformation.get(key);
            long index = Long.parseLong(row.getStringValue(KeyGenerator.indexColumnName.getValueByKey(KeyGenerator.index_code)));
            int length = Integer.parseInt(row.getStringValue(KeyGenerator.indexColumnName.getValueByKey(KeyGenerator.length_code)));
            String suffix = row.getStringValue(KeyGenerator.indexColumnName.getValueByKey(KeyGenerator.suffix_code));
            String prefix = row.getStringValue(KeyGenerator.indexColumnName.getValueByKey(KeyGenerator.prefix_code));

            /**
             * check availability in the range of code
             */
            if (length < prefix.length() + suffix.length() + String.valueOf(index).length()) {
                throw new CodeOutOfBoundsException("range of code of " + tableName + " if full. Please apply the operation to redefine it");
            }

            /**
             * transaction for key generation
             */
            Originator<Row> rowOriginator = new Originator<>();
            rowOriginator.setState((Row)row.clone());
            CareTaker<Row> careTaker = new CareTaker<>();
            careTaker.save(rowOriginator.originatorMomento());

            /**
             * update table code in database
             */

            row.setValue(KeyGenerator.indexColumnName.getValueByKey(KeyGenerator.index_code), Long.toString(++index));
            try {
                DataBaseAdministrator.getQueryManager().updateOnTable(con, catalog, schema, KeyGenerator.table, row, KeyGenerator.keySet, KeyGenerator.indexColumnName);
                String code = prefix + KeyGenerator.formatLong(length - prefix.length() - suffix.length(), index) + suffix;
                if (!key.equals(KeyGenerator.code_unique)) {
                    result.put(KeyGenerator.code, code);
                }
                else {
                    result.put(KeyGenerator.code_unique, code);
                }
            }
            catch (SQLException e) {
                KeyGenerator.columnInformation.put(key, rowOriginator.revert(careTaker.retrieve()));
                throw e;
            }
        }
        return result;
    }

    public static void loadData(Connection connection, String catalog, String schema) throws SQLException, NotUniqueTableInQueryException {
        /**
         * check the availability of connection
         */
        if (connection == null) throw new IllegalArgumentException();
        /**
         * load necessary data for key generation
         */
        ResultSet resultSet = DataBaseAdministrator.getQueryManager().readOnTable(connection, catalog, schema, KeyGenerator.table,0,null,null,null, -1, -1, null);
        SpreadSheet spreadSheet = new SpreadSheet(connection, resultSet);
        int t = spreadSheet.indexOfColumnName(KeyGenerator.tableName);
        for (Row row : spreadSheet.getRows()) {
            KeyGenerator.columnInformation.put(row.getStringValue(t), row);
        }
        KeyGenerator.indexColumnName = spreadSheet.getIndexColumnName();
    }

    private static String formatLong(int length, long index) {
        String result = String.valueOf(index);
        int n = length - result.length();

        for(int i = 0; i < n; ++i) {
            result = "0" + result;
        }

        return result;
    }
}