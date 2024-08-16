package com.RuneLingual.commonFunctions;

import java.io.File;
import java.io.IOException;

import com.RuneLingual.Downloader;
import com.RuneLingual.LangCodeSelectableList;


public class FileActions {
    /*
    mainly for managing the file whose name includes the current selected language

    getLangCodeFromFile() - get the language code from the file name
    createLangCodeNamedFile() - create a file with the language code in the name
    deleteAllLangCodeNamedFile() - delete all files with the language code in the name

    these 3 functions should be used to manage the language named files.
     */
    static String fileNameStart = "setLang_";
    static String langNameFolder = Downloader.localBaseFolder.toString();

    public static LangCodeSelectableList getLangCodeFromFile() {
        String existingFileName = getFileNameInFolderStartsWith(langNameFolder, fileNameStart);
        if (existingFileName != null){
            String langCode = existingFileName.substring(existingFileName.indexOf("_") + 1, existingFileName.indexOf("."));
            for (LangCodeSelectableList lang : LangCodeSelectableList.values()) {
                if (lang.getCode().equals(langCode)) {
                    return lang;
                }
            }
        }
        return LangCodeSelectableList.ENGLISH;
    }

    public static void createLangCodeNamedFile(LangCodeSelectableList lang) {
        String fileName = langNameFolder + File.separator + fileNameStart + lang.getCode() + ".txt";
        createFile(fileName);
    }

    public static void deleteAllLangCodeNamedFile() {
        for (LangCodeSelectableList lang : LangCodeSelectableList.values()) {
            String fileName = langNameFolder + File.separator + fileNameStart + lang.getCode() + ".txt";
            deleteFile(fileName);
        }
    }

    public static void createFile(String fileName) {
        try {
            File myObj = new File(fileName);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
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

    public static void deleteFile(String fileName) {
        File myObj = new File(fileName);
        if (myObj.delete()) {
            System.out.println("Deleted the file: " + myObj.getName());
        } else {
            System.out.println("Failed to delete the file.");
        }
    }
}
