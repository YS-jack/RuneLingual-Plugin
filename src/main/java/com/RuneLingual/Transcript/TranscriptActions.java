package com.RuneLingual.Transcript;

import com.RuneLingual.commonFunctions.FileActions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;


@Slf4j @Getter
public class TranscriptActions {
    private final String englishKeyName = "english";
    private final String translationKeyName = "translation";
    private final String categoryKeyName = "category";
    private final String subCategoryKeyName = "sub_category";
    private final String sourceKeyName = "source";
    @Getter
    HashMap<String,JSONObject> transcript = new HashMap<>();

    public HashMap<String,JSONObject> getTranscript(String localLangFolder, String[] tsvFiles){
        HashMap<String,JSONObject> transcriptTmp = new HashMap<>();
        for (int i = 0; i < tsvFiles.length; i++){
            String fullPath = localLangFolder + File.separator + tsvFiles[i];
            if (!FileActions.fileExists(fullPath)){
                log.error("File does not exist: " + fullPath);
                return null;
            }

            // read from file
            try (BufferedReader br = new BufferedReader(new FileReader(fullPath))){
                String line;
                JSONObject json = new JSONObject();

                line = br.readLine(); // skip header
                String[] header = line.split("\t");
                while ((line = br.readLine()) != null){
                    String[] values = line.split("\t");
                    for(int j = 0; j < values.length; j++){
                        json.put(header[j], values[j]);
                    }
                }
                transcriptTmp.put(json.getString(englishKeyName),json);
            } catch (Exception e){
                log.error("Error reading file: " + fullPath);
                e.printStackTrace();
            }
        }
        transcript = transcriptTmp;
        return transcriptTmp;
    }

    public String getTranslation(String english){
        JSONObject translation = transcript.get(english);
        if (translation == null){
            log.error("No translation found for: " + english);
            return english;
        }
        return translation.getString(translationKeyName);
    }
}
