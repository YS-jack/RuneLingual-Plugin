package com.RuneLingual.prepareResources;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

@Slf4j
public class H2Manager {
    @Inject
    private RuneLingualPlugin plugin;

    public String getUrl(LangCodeSelectableList targetLanguage) {
        String databaseUrl = "jdbc:h2:" + FileNameAndPath.getLocalBaseFolder() + File.separator +
                targetLanguage.getLangCode() + File.separator + FileNameAndPath.getLocalSQLFileName();
        return databaseUrl;
    }

    public Connection getConn(LangCodeSelectableList targetLanguage) {
        Connection conn = null;
        String databaseUrl;

        databaseUrl = getUrl(targetLanguage);
        plugin.setDatabaseUrl(databaseUrl);
        try {
            conn = DriverManager.getConnection(databaseUrl);
        } catch (Exception e) {
            log.error("Error connecting to database: " + databaseUrl);
            plugin.setTargetLanguage(LangCodeSelectableList.ENGLISH);
            e.printStackTrace();
        }
        return conn;
    }

    public void closeConn() {
        try {
            if (plugin.getConn() != null) {
                plugin.getConn().close();
            }
        } catch (Exception e) {
            log.error("Error closing database connection.");
            e.printStackTrace();
        }
    }
}
