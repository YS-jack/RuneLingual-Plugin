package com.RuneLingual.commonFunctions;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.RuneLingual.LangCodeSelectableList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileActions {
    /*
    mainly for managing the file whose name includes the current selected language

    getLangCodeFromFile() - get the language code from the file name
    createLangCodeNamedFile() - create a file with the language code in the name
    deleteAllLangCodeNamedFile() - delete all files with the language code in the name

    these 3 functions should be used to manage the language named files.
     */
    static String fileNameStart = "setLang_";
    static String langNameFolder = FileNameAndPath.getLocalBaseFolder().toString();

    public static LangCodeSelectableList getLangCodeFromFile() {
        String existingFileName = getFileNameInFolderStartsWith(langNameFolder, fileNameStart);
        if (existingFileName != null){
            String langCode = existingFileName.substring(existingFileName.indexOf("_") + 1, existingFileName.indexOf("."));
            for (LangCodeSelectableList lang : LangCodeSelectableList.values()) {
                if (lang.getLangCode().equals(langCode)) {
                    return lang;
                }
            }
        }
        return LangCodeSelectableList.ENGLISH;
    }

    public static void createLangCodeNamedFile(LangCodeSelectableList lang) {
        String fileName = langNameFolder + File.separator + fileNameStart + lang.getLangCode() + ".txt";
        createFile(fileName);
    }

    public static void deleteAllLangCodeNamedFile() {
        for (LangCodeSelectableList lang : LangCodeSelectableList.values()) {
            String fileName = langNameFolder + File.separator + fileNameStart + lang.getLangCode() + ".txt";
            deleteFile(fileName);
        }
    }

    public static void createFile(String fileName) {
        try {
            File myObj = new File(fileName);
            if (myObj.createNewFile()) {
                log.info("File created: " + myObj.getName());
            } else {
                log.info("File already exists.");
            }
        } catch (IOException e) {
            log.info("An error occurred.");
            e.printStackTrace();
        }
    }

    public static Boolean checkFileExists(String fileName) {
        File myObj = new File(fileName);
        return myObj.exists();
    }

    public static String getFileNameInFolderStartsWith(String path, String fileName) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        for (File file : listOfFiles) {
            if (file.isFile()) {
                if (file.getName().startsWith(fileName)) {
                    return file.getName();
                }
            }
        }
        return null;
    }

    public static List<String> getFileNamesWithExtension(String path, String extension){
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        List<String> matchedFileNames = null;
        for (File file : listOfFiles) {
            if (file.isFile()) {
                if (file.getName().endsWith(extension)) {
                    matchedFileNames.add(file.getName());
                }
            }
        }
        return matchedFileNames;
    }

    public static void deleteFile(String fileName) {
        File myObj = new File(fileName);
        if (myObj.delete()) {
            log.info("Deleted the file: " + myObj.getName());
        } else {
            log.info("Failed to delete the file.");
        }
    }

    public static boolean fileExists(String filename) {
        File myObj = new File(filename);
        return myObj.exists();
    }

    public static void deleteFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
        folder.delete();
    }
}
