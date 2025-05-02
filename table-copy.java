import java.io.*;
import java.sql.*;
import java.util.*;

public class OracleDataCopier {

    // Oracle DB 접속 정보
    private static final String SOURCE_URL = "jdbc:oracle:thin:@//source_host:1521/SOURCE_SID";
    private static final String SOURCE_USER = "source_user";
    private static final String SOURCE_PASS = "source_password";

    private static final String TARGET_URL = "jdbc:oracle:thin:@//target_host:1521/TARGET_SID";
    private static final String TARGET_USER = "target_user";
    private static final String TARGET_PASS = "target_password";

    public static void main(String[] args) throws Exception {
        List<String> tables = readTablesFromFile("tables.txt");

        try (
            Connection sourceConn = DriverManager.getConnection(SOURCE_URL, SOURCE_USER, SOURCE_PASS);
            Connection targetConn = DriverManager.getConnection(TARGET_URL, TARGET_USER, TARGET_PASS)
        ) {
            sourceConn.setAutoCommit(false);
            targetConn.setAutoCommit(false);

            for (String table : tables) {
                copyTableData(sourceConn, targetConn, table.trim().toUpperCase());
                System.out.println(table + " : 복사완료");
            }
        }
    }

    private static List<String> readTablesFromFile(String filename) throws IOException {
        List<String> tables = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    tables.add(line.trim());
                }
            }
        }
        return tables;
    }

    private static void copyTableData(Connection sourceConn, Connection targetConn, String tableName) throws SQLException {
        List<String> commonColumns = getCommonColumns(sourceConn, targetConn, tableName);
        if (commonColumns.isEmpty()) return;

        String columns = String.join(",", commonColumns);
        String selectSQL = "SELECT " + columns + " FROM " + tableName;
        String insertSQL = buildInsertSQL(tableName, commonColumns);

        try (
            PreparedStatement selectStmt = sourceConn.prepareStatement(selectSQL);
            ResultSet rs = selectStmt.executeQuery();
            PreparedStatement insertStmt = targetConn.prepareStatement(insertSQL)
        ) {
            int batchSize = 0;

            while (rs.next()) {
                for (int i = 0; i < commonColumns.size(); i++) {
                    insertStmt.setObject(i + 1, rs.getObject(commonColumns.get(i)));
                }
                insertStmt.addBatch();
                batchSize++;

                if (batchSize % 1000 == 0) {
                    insertStmt.executeBatch();
                    targetConn.commit();
                }
            }

            insertStmt.executeBatch();
            targetConn.commit();
        }
    }

    private static List<String> getCommonColumns(Connection sourceConn, Connection targetConn, String tableName) throws SQLException {
        Set<String> sourceCols = getColumnNames(sourceConn, tableName);
        Set<String> targetCols = getColumnNames(targetConn, tableName);
        sourceCols.retainAll(targetCols); // intersection
        return new ArrayList<>(sourceCols);
    }

    private static Set<String> getColumnNames(Connection conn, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, conn.getSchema(), tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }

    private static String buildInsertSQL(String tableName, List<String> columns) {
        String cols = String.join(",", columns);
        String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
        return "INSERT INTO " + tableName + " (" + cols + ") VALUES (" + placeholders + ")";
    }
}