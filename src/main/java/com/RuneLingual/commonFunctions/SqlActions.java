package com.RuneLingual.commonFunctions;

import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.util.*;

import com.RuneLingual.commonFunctions.FileNameAndPath;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlActions {

    static final String tableName = "transcript";
    static final String databaseFileName = FileNameAndPath.getLocalSQLFileName();

    public static void createTable(String databaseFolder) {
        String databaseUrl = "jdbc:h2:" + databaseFolder + File.separator + databaseFileName;

        try (Connection conn = DriverManager.getConnection(databaseUrl)) {
            //remove table if exists
            String dropTable = "DROP TABLE IF EXISTS " + tableName;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(dropTable);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // then create new table
            String sql = "CREATE TABLE " + tableName + " (english TEXT)";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

//    public static void tsvToSqlQuery(String url, String tsvFile) {
//        String user = "sa"; // Change this to your H2 database username
//        String password = ""; // Change this to your H2 database password
//        //String tsvFile = "/path/to/your/file.tsv"; // Change this to your TSV file path
//        try (Connection conn = DriverManager.getConnection(url)) {
//            String sql = "INSERT INTO "+ tableName + " SELECT * FROM CSVREAD('" + tsvFile + "', null, 'fieldSeparator=\\t')";
//
//            try (Statement stmt = conn.createStatement()) {
//                stmt.execute(sql);
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }


    public static void TsvToSqlDatabase(String[] tsvFiles, String tsvFolderPath){
        // note: table must exist before calling this function

        String databaseUrl = "jdbc:h2:" + tsvFolderPath + File.separator + databaseFileName;
//        File folder = new File(tsvFolderPath);
//        File[] listOfFiles = folder.listFiles();
//
//        if (listOfFiles != null) {
//            for (File file : listOfFiles) {
//                if (file.isFile() && file.getName().endsWith(".tsv")) {
//                    processTsvFile(databaseUrl, file.getPath());
//                    //tsvToSqlQuery(databaseUrl, file.getPath());
//                }
//            }
//        }
        for (String tsvFile : tsvFiles) {
            processTsvFile(databaseUrl, tsvFolderPath + File.separator + tsvFile);
        }
    }

    private static void processTsvFile(String databaseUrl, String tsvFilePath) {
        log.info("Processing TSV file: " + tsvFilePath);
        try (Connection conn = DriverManager.getConnection(databaseUrl)) {
            List<String> lines = Files.readAllLines(Paths.get(tsvFilePath));
            String[] columnNames = lines.get(0).split("\t");

            // Ensure all columns exist
            ensureColumnsExist(conn, columnNames);

            // Insert data
            for (int i = 1; i < lines.size(); i++) {
                String[] fields = lines.get(i).split("\t", -1);

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

                try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                    for (int j = 0; j < fields.length; j++) {
                        pstmt.setString(j + 1, fields[j]);
                    }

                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            //log.info("Error processing TSV file " + tsvFilePath);
            e.printStackTrace();
        }
    }

    private static void ensureColumnsExist(Connection conn, String[] columnNames) {
        for (String columnName : columnNames) {
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " TEXT";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                log.info("Error adding column " + columnName + " to " + tableName);
                e.printStackTrace();
            }
        }
    }

}
