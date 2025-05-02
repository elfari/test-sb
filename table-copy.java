import java.sql.*;
import java.util.*;
import java.io.*;

public class DBDataCopier {

    private static final String DB_A_URL = "jdbc:oracle:thin:@A_HOST:1521:A_SID";
    private static final String DB_A_USER = "SOURCE_USER";
    private static final String DB_A_PASS = "SOURCE_PASS";

    private static final String DB_B_URL = "jdbc:oracle:thin:@B_HOST:1521:B_SID";
    private static final String DB_B_USER = "TARGET_USER";
    private static final String DB_B_PASS = "TARGET_PASS";

    public static void main(String[] args) throws Exception {
        List<String> tables = loadTableList("tables.txt");

        Class.forName("oracle.jdbc.driver.OracleDriver");

        Connection connA = DriverManager.getConnection(DB_A_URL, DB_A_USER, DB_A_PASS);
        Connection connB = DriverManager.getConnection(DB_B_URL, DB_B_USER, DB_B_PASS);

        connB.setAutoCommit(false);

        for (String table : tables) {
            try {
                copyTable(connA, connB, table);
                connB.commit();
                System.out.println(table + " : 복사완료");
            } catch (Exception e) {
                connB.rollback();
                System.err.println(table + " : 복사실패 - " + e.getMessage());
            }
        }

        connA.close();
        connB.close();
    }

    private static List<String> loadTableList(String filePath) throws IOException {
        List<String> tables = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                tables.add(line.trim());
            }
        }
        reader.close();
        return tables;
    }

    private static void copyTable(Connection connA, Connection connB, String tableName) throws Exception {
        List<String> sourceCols = getColumnList(connA, tableName);
        List<String> targetCols = getColumnList(connB, tableName);

        List<String> commonCols = new ArrayList<String>();
        for (String col : sourceCols) {
            if (targetCols.contains(col)) {
                commonCols.add(col);
            }
        }

        if (commonCols.isEmpty()) {
            throw new Exception("일치하는 컬럼 없음");
        }

        String colList = String.join(", ", commonCols);
        String selectSql = "SELECT " + colList + " FROM " + tableName;
        String insertSql = buildInsertSQL(tableName, commonCols);

        PreparedStatement pstmtInsert = connB.prepareStatement(insertSql);
        Statement stmtSelect = connA.createStatement();
        stmtSelect.setFetchSize(500);

        ResultSet rs = stmtSelect.executeQuery(selectSql);
        int colCount = commonCols.size();

        int count = 0;
        while (rs.next()) {
            for (int i = 1; i <= colCount; i++) {
                pstmtInsert.setObject(i, rs.getObject(i));
            }
            pstmtInsert.addBatch();
            count++;

            if (count % 1000 == 0) {
                pstmtInsert.executeBatch();
            }
        }
        pstmtInsert.executeBatch();

        rs.close();
        stmtSelect.close();
        pstmtInsert.close();
    }

    private static List<String> getColumnList(Connection conn, String tableName) throws SQLException {
        List<String> cols = new ArrayList<String>();
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getColumns(null, conn.getSchema(), tableName.toUpperCase(), null);
        while (rs.next()) {
            cols.add(rs.getString("COLUMN_NAME"));
        }
        rs.close();
        return cols;
    }

    private static String buildInsertSQL(String table, List<String> cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table).append(" (");
        sb.append(String.join(", ", cols));
        sb.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            sb.append("?");
            if (i < cols.size() - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

private static String joinStrings(List<String> list, String delimiter) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < list.size(); i++) {
        sb.append(list.get(i));
        if (i < list.size() - 1) {
            sb.append(delimiter);
        }
    }
    return sb.toString();
}

}