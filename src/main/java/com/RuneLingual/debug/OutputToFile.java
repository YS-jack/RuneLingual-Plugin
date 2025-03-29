package com.RuneLingual.debug;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.SQL.SqlVariables;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OutputToFile {
    public void menuTarget(String target, String subCategory, String source){
        if (Colors.countColorTagsAfterReformat(target) <= 1){
            target = Colors.removeNonImgTags(target);
        }
        target = Colors.getColorPlaceholdedColWord(target);
        appendToFile(target + "\t" + SqlVariables.categoryValue4Name.getValue() + "\t" + subCategory + "\t" + source, "menuTarget_debug.txt");
    }

    public void menuOption(String option, String subCategory, String source){
        if (Colors.countColorTagsAfterReformat(option) <= 1){
            option = Colors.removeNonImgTags(option);
        }
        option = Colors.getColorPlaceholdedColWord(option);
        appendToFile(option + "\t" + SqlVariables.categoryValue4Actions.getValue() + "\t" + subCategory + "\t" + source, "menuOption_debug.txt");
    }

    public void dumpGeneral(String english, String category, String subCategory, String source){
        if (english == null || english.isEmpty()){
            return;
        }
        if (category == null){
            category = "";
        }
        if (subCategory == null){
            subCategory = "";
        }
        if (source == null){
            source = "";
        }
        appendToFile(english + "\t" + category + "\t" + subCategory + "\t" + source, "dumpGeneral_debug.txt");
    }

    public static void appendToFile(String str, String fileName){
        try {
            createDirectoryIfNotExists("output");
            Path filePath = Paths.get("output" + File.separator + fileName);
            createFileIfNotExists(filePath.toString());
            Files.write(filePath, (str + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Error writing to file.", e);
        }
    }

    public static void appendIfNotExistToFile(String str, String fileName) {
        try {
            createDirectoryIfNotExists("output");
            Path filePath = Paths.get("output" + File.separator + fileName);
            createFileIfNotExists(filePath.toString());

            // Read all lines from the file
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

            // Check if the string is already in the file
            if (!lines.contains(str)) {
                Files.write(filePath, (str + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createFileIfNotExists(String fileName) {
        Path path = Paths.get(fileName);
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
            }
        }
    }

    public static void createDirectoryIfNotExists(String dirName) {
        Path path = Paths.get(dirName);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
            }
        }
    }
}
