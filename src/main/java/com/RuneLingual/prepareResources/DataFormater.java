package com.RuneLingual.prepareResources;

import com.RuneLingual.commonFunctions.FileActions;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.RuneLingual.SQL.SqlActions;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.File;

@Slf4j
public class DataFormater {
    @Inject
    private SqlActions sqlActions;
    public void updateSqlFromTsv(String localLangFolder, String[] tsvFiles){
        log.info("Updating SQL database from TSV files.");
        String SQLFilePath = localLangFolder + File.separator + FileNameAndPath.getLocalSQLFileName() + ".mv.db";
        String SQLFilePath2 = localLangFolder + File.separator + FileNameAndPath.getLocalSQLFileName() + ".trace.db";

        if (FileActions.fileExists(SQLFilePath)){
            FileActions.deleteFile(SQLFilePath);
        }
        if (FileActions.fileExists(SQLFilePath2)){
            FileActions.deleteFile(SQLFilePath2);
        }
        sqlActions.createTable(localLangFolder);
        SqlActions.TsvToSqlDatabase(tsvFiles, localLangFolder);
    }
}
