package com.RuneLingual.SQL;

import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.util.*;

import com.RuneLingual.commonFunctions.FileActions;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.RuneLingual.prepareResources.Downloader;
import com.RuneLingual.RuneLingualPlugin;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class SqlActions {

    public static final String tableName = "transcript";
    static final String databaseFileName = FileNameAndPath.getLocalSQLFileName();
    ;
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private Downloader downloader;
    @Inject
    private FileNameAndPath fileNameAndPath;

    @Inject
    public SqlActions(RuneLingualPlugin plugin) {
        this.plugin = plugin;
    }

    // private String databaseUrl = "jdbc:h2:" + downloader.getLocalLangFolder() + File.separator + databaseFileName;

    public void createTable() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Connection conn = DriverManager.getConnection(this.plugin.getDatabaseUrl());
        this.plugin.setConn(conn);

        // Check if the table already exists
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            if (tables.next()) {
                //log.info("Table '{}' already exists. Skipping creation.", tableName);
                return;
            }
        }

        // Create the table if it does not exist
        String sql = "CREATE TABLE " + tableName + " ()";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            log.error("Error creating table '{}'.", tableName, e);
            throw new RuntimeException(e);
        }
    }


    public void tsvToSqlDatabase(String[] tsvFiles, String tsvFolderPath){
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        // note: table must exist before calling this function

        for (String tsvFile : tsvFiles) {
            processTsvFile(tsvFolderPath + File.separator + tsvFile);
        }

        // Check if the index already exists
        DatabaseMetaData metaData = null;
        try {
            metaData = this.plugin.getConn().getMetaData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName.toUpperCase(), false, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if ("ENGLISH_INDEX".equalsIgnoreCase(indexName)) {
                    //"Index 'english_index' already exists. Skipping creation.
                    return;
                }
            }
        } catch (SQLException e) {
            log.error("Error checking for existing index on " + tableName, e);
        }

        // Index the English column
        String sql = "CREATE INDEX english_index ON " + tableName + " (" + SqlVariables.columnEnglish.getColumnName()
                + ", " + SqlVariables.columnTranslation.getColumnName()
                + ", " + SqlVariables.columnCategory.getColumnName()
                + ", " + SqlVariables.columnSubCategory.getColumnName()
                + ", " + SqlVariables.columnSource.getColumnName() + ")";
        try (Statement stmt = this.plugin.getConn().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            log.error("Error creating index on " + tableName, e);
        }
    }

    private void processTsvFile(String tsvFilePath) {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        //log.info("Processing TSV file: " + tsvFilePath);
        try {
            List<String> lines = Files.readAllLines(Paths.get(tsvFilePath));
            String[] columnNames = lines.get(0).split("\t");

            // Ensure all columns exist
            ensureColumnsExist(columnNames);

            // Insert data
            for (int i = 1; i < lines.size(); i++) {
                if(lines.get(i).split("\t").length > columnNames.length){
                    //log.info("Warning processing TSV file " + tsvFilePath + " at line " + i + " : " + lines.get(i));
                    //log.info("found more values than number of columns.");
                    //log.info("Column names: " + Arrays.toString(columnNames));
                    //log.info("Column values: " + Arrays.toString(lines.get(i).split("\t")));
                }
                String[] fields = lines.get(i).split("\t", columnNames.length);

                StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");

                for (String columnName : columnNames) {
                    sql.append(columnName).append(",");
                }

                sql.deleteCharAt(sql.length() - 1); // remove last comma
                sql.append(") VALUES (");

                for (int j = 0; j < fields.length; j++) {
                    sql.append("?,");
                }

                sql.deleteCharAt(sql.length() - 1); // remove last comma
                sql.append(")");

                try (PreparedStatement pstmt = this.plugin.getConn().prepareStatement(sql.toString())) {
                    for (int j = 0; j < fields.length; j++) {
                        pstmt.setString(j + 1, fields[j]);
                    }

                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    log.error("Error processing TSV file {} at line {} : '{}'", tsvFilePath, i, lines.get(i));
                    log.error("sql: '{}'", sql, e);
                }
            }
        } catch (Exception e) {
            log.error("Error processing TSV file {}", tsvFilePath, e);
        }
    }

    private void ensureColumnsExist(String[] columnNames) {
        for (String columnName : columnNames) {
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " VARCHAR(2000)";
            try (Statement stmt = this.plugin.getConn().createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                log.error("Error adding column {} to " + tableName, columnName, e);
            }
        }
    }

    public String[][] executeSearchQuery(String query) {
        /*
        * Execute a search query and return the results as a 2D array
        * eg. SELECT * FROM transcript WHERE english = 'hello'
        * returns [["hello", "hola"], ["hello", "こんにちは"]]
         */

        List<List<String>> results = new ArrayList<>();
        try (Statement stmt = this.plugin.getConn().createStatement();){
            ResultSet rs = stmt.executeQuery(query);

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();

            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnsNumber; i++) {
                    row.add(rs.getString(i));
                }
                results.add(row);
            }
            String[][] array = new String[results.size()][];
            for (int i = 0; i < results.size(); i++) {
                List<String> row = results.get(i);
                array[i] = row.toArray(new String[0]);
            }
            return array;
        }
        catch (SQLException e) {
            log.error("Error executing search query: {}.\nDeleting hash file, restart plugin", query, e);
            // delete the hash file. When the plugin restarts, it will download the latest files, replacing possible corrupted files.
            String hashFilePath = FileActions.getHashFile(this.plugin.getConfig().getSelectedLanguage());
            FileActions.deleteFile(hashFilePath);
        }
        return new String[0][0];
    }

    public String[] executeQuery(String query) {
        /*
            * Execute a query and return the results as a 1D array
            * eg. SELECT translation FROM transcript WHERE english = 'hello'
            * returns ["hola", "こんにちは"]
         */
        List<String> results = new ArrayList<>();
        try (Statement stmt = this.plugin.getConn().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String columnValue = rs.getString(1);
                results.add(columnValue);
            }
        } catch (SQLException e) {
            log.error("Error executing query: {}", query, e);
        }
        return results.toArray(new String[0]);
    }

    public static String[][] executePreparedStatement(PreparedStatement preparedStatement) throws SQLException {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            ArrayList<String[]> results = new ArrayList<>();
            int columnCount = resultSet.getMetaData().getColumnCount();

            while (resultSet.next()) {
                String[] row = new String[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = resultSet.getString(i + 1);
                }
                results.add(row);
            }

            return results.toArray(new String[0][0]);
        }
    }

    public static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?")) {
            stmt.setString(1, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static boolean tableIsEmpty(Connection conn, String tableName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM " + tableName)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) == 0;
            }
        }
    }

    public static boolean noTableExistsOrIsEmpty(Connection conn){
        String tableName = SqlActions.tableName;
        try {
            if (!tableExists(conn, tableName) || tableIsEmpty(conn, tableName)) {
                if(conn!=null) {
                    conn.close();
                }
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }




}
