package com.RuneLingual.commonFunctions;

import com.RuneLingual.LangCodeSelectableList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.File;

@Slf4j
public class FileNameAndPath {
    @Getter
    private static final File localBaseFolder = new File(RuneLite.RUNELITE_DIR.getPath() + File.separator + "RuneLingual_resources");
    @Getter
    private static final String localSQLFileName = "transcript";


    public static String getLocalLangFolder(LangCodeSelectableList lang) {
        String langCode = lang.getLangCode();
        File localLangFolder = new File(localBaseFolder.getPath() + File.separator + langCode);
        return localLangFolder.getPath();
    }
    @Getter @Setter
    private String localLangFolder;

    public static void createDirectoryIfNotExists(String folderPath) {
        //create the directory if it doesn't exist
        File directory = new File(folderPath);
        directory.mkdir();
    }

    public static void createFileIfNotExists(String filePath) {
        //create the file if it doesn't exist
        File file = new File(filePath);
        try {
            if (!file.createNewFile()) {
                log.error("File already exists: {}", filePath);
            }
        } catch (Exception e) {
            log.error("Error creating file: {}", e.getMessage());
        }
    }
}
