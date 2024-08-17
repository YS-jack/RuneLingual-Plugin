package com.RuneLingual.prepareResources;

import com.RuneLingual.commonFunctions.FileActions;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.RuneLingual.commonFunctions.SqlActions;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
public class DataFormater {
    public static void updateSqlFromTsv(String localLangFolder, String[] tsvFiles){
        log.info("Updating SQL database from TSV files.");
        String SQLFilePath = localLangFolder + File.separator + FileNameAndPath.getLocalSQLFileName() + ".mv.db";
        String SQLFilePath2 = localLangFolder + File.separator + FileNameAndPath.getLocalSQLFileName() + ".trace.db";

        if (FileActions.fileExists(SQLFilePath)){
            FileActions.deleteFile(SQLFilePath);
        }
        if (FileActions.fileExists(SQLFilePath2)){
            FileActions.deleteFile(SQLFilePath2);
        }
        SqlActions.createTable(localLangFolder);
        SqlActions.TsvToSqlDatabase(tsvFiles, localLangFolder);
    }
}
