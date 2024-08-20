package com.RuneLingual.Transcript;

import lombok.Getter;
import lombok.Setter;

import org.json.JSONObject;

@Setter @Getter
public class TranscriptRecord {
    private String english = null;
    private String translation = null;
    private String category = null;
    private String subCategory = null;
    private String source = null;

    public TranscriptRecord(String english) {
        this.english = english;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        if (english != null){
            json.put("english", english);
        }
        if (translation != null){
            json.put("translation", translation);
        }
        if (category != null){
            json.put("category", category);
        }
        if (subCategory != null){
            json.put("subCategory", subCategory);
        }
        if (source != null){
            json.put("source", source);
        }
        return json;
    }
}
