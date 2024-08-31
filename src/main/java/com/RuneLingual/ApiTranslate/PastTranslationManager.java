package com.RuneLingual.ApiTranslate;

import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.RuneLingual.commonFunctions.FileActions;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class PastTranslationManager {
    @Inject
    private Deepl deepl;
    private RuneLingualPlugin plugin;
    private HashMap<String, String> pastTranslations = new HashMap<>();
    private final String pastTranslationFile;


    @Inject
    public PastTranslationManager(Deepl deepl, RuneLingualPlugin plugin){
        this.plugin = plugin;
        this.deepl = deepl;
        pastTranslationFile = FileNameAndPath.getLocalBaseFolder().getPath() + File.separator +
                plugin.getConfig().getSelectedLanguage().getLangCode() + File.separator + "pastTranslations.txt";
        setPastTranslationsFromFile();
    }

    public void setPastTranslationsFromFile() {
        // if pastTranslationFile exists, read from it
        if (FileActions.fileExists(pastTranslationFile)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(pastTranslationFile), StandardCharsets.UTF_8))) {
                // Skip BOM if present
                reader.mark(1);
                if (reader.read() != 0xFEFF) {
                    reader.reset();
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        pastTranslations.put(key, value);
                    }
                }

            } catch (IOException e) {
                log.error("Error reading file: " + e.getMessage(), e);
            }
        } else {
            // Create an empty pastTranslationFile
            try {
                Files.createFile(Paths.get(pastTranslationFile));
                log.info("Created empty file: " + pastTranslationFile);
            } catch (IOException e) {
                log.error("Error creating file: " + e.getMessage(), e);
            }
        }
    }

    public String getPastTranslation(String text){
        return pastTranslations.getOrDefault(text, null);
    }

    public void addToPastTranslations(String text, String translation) {
        // add to the hashmap
        pastTranslations.put(text, translation);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(pastTranslationFile, true), StandardCharsets.UTF_8))) {
            String content = text + "|" + translation + "\n";
            writer.write(content);
            writer.flush(); // Ensure the content is written immediately
        } catch (IOException e) {
            log.error("Error writing to file: " + e.getMessage(), e);
        }
    }
}
