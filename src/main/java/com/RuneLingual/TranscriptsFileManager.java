package com.RuneLingual;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

public class TranscriptsFileManager
{
    @Getter
    private boolean changed;
    
    // main transcript modules
    @Nullable
    public TranscriptManager originalTranscript;
    @Nullable
    public TranscriptManager translatedTranscript;
    
    @Setter
    private String filePrefix;
    @Setter
    private String currentLang;
    
    // logging control
    @Nullable @Setter
    private LogHandler logger;
    private boolean logErrors;
    private boolean logLoad;
    private boolean logSave;
    
    public TranscriptsFileManager()
    {
        this.changed = false;
        
        // transcript loggers are copied from this context's
        this.logErrors = true;
        this.logLoad = true;
        this.logSave = true;
    }
    
    public void saveOriginalTranscript()
    {
        String filePath = getOriginalFilePath();
        
        try
        {
            URL resourceUrl = getClass().getResource(filePath);
            OutputStream outputStream = new FileOutputStream(new File(resourceUrl.toURI()));
            
            if(outputStream == null)
            {
                throw new Exception("Could not access or modify original transcripts file!");
            }
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            
            writer.setIndent("\t");
            gson.toJson(originalTranscript, TranscriptManager.class, writer);
            
            if(logSave)
            {
                logger.log("Original transcripts file was updated successfully!");
            }
            
            writer.close();
            //outputStream.close();
            
        }
        catch (Exception e)
        {
            if(logErrors)
            {
                logger.log("Could not update transcripts on master: " + e.getMessage());
            }
        }
    }
    
    public void loadTranscripts()
    {
        loadOriginalTranscript();
        loadTranslatedTranscript();
    }
    
    public void langChanged(String newLang)
    {
        currentLang = newLang;
        
        // reloads translated transcript
        translatedTranscript = null;
        loadTranslatedTranscript();
    }
    
    public boolean transcriptChanged() {return originalTranscript.isChanged();}
    
    private void loadTranslatedTranscript()
    {
        String filePath = getTranslationFilePath();
        
        if(logLoad)
        {
            logger.log("Now trying to load translated transcripts from " + filePath);
        }
        
        InputStream inputStream = getClass().getResourceAsStream(filePath);
        
        if(inputStream != null)
        {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
            {
                StringBuilder jsonBuilder = new StringBuilder();
                
                String line;
                
                while((line = reader.readLine()) != null)
                {
                    jsonBuilder.append(line);
                }
                
                String json = jsonBuilder.toString();
                if(json.trim().isEmpty())
                {
                    if(logErrors)
                    {
                        logger.log("Empty JSON file");
                    }
                    
                    translatedTranscript = null;
                }
                else
                {
                    // Manually check if JSON is a valid object
                    if(json.startsWith("{") && json.endsWith("}"))
                    {
                        Gson gson = new Gson();
                        originalTranscript = gson.fromJson(json, TranscriptManager.class);
                        originalTranscript.setLogger(this.logger);
                        
                        if(logLoad)
                        {
                            logger.log("Translated transcript was loaded successfully.");
                        }
                    }
                    else
                    {
                        if(logErrors)
                        {
                            logger.log("Invalid JSON format. Expected an object.");
                        }
                        
                        translatedTranscript = null;
                        translatedTranscript.setLogger(logger);
                    }
                }
            }
            catch(IOException e)
            {
                if(logErrors)
                {
                    logger.log("Could not load translated transcript: " + e.getMessage());
                }
                translatedTranscript = null;
            }
        }
        else
        {
            if(logErrors)
            {
                logger.log("JSON is empty!");
            }
            translatedTranscript = null;
        }
    }
    
    private void loadOriginalTranscript()
    {
        String filePath = getOriginalFilePath();
        
        if(logLoad)
        {
            logger.log("Now trying to load original transcripts from " + filePath);
        }
        
        InputStream inputStream = getClass().getResourceAsStream(filePath);
        
        if(inputStream != null)
        {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
            {
                StringBuilder jsonBuilder = new StringBuilder();
                
                String line;
                
                while((line = reader.readLine()) != null)
                {
                    jsonBuilder.append(line);
                }
                
                String json = jsonBuilder.toString();
                if(json.trim().isEmpty())
                {
                    if(logErrors)
                    {
                        logger.log("Empty JSON file");
                    }
                    
                    originalTranscript = null;
                }
                else
                {
                    // Manually check if JSON is a valid object
                    if(json.startsWith("{") && json.endsWith("}"))
                    {
                        Gson gson = new Gson();
                        originalTranscript = gson.fromJson(json, TranscriptManager.class);
                        originalTranscript.setLogger(this.logger);
                        
                        if(logLoad)
                        {
                            logger.log("Original transcript was loaded successfully.");
                        }
                    }
                    else
                    {
                        if(logErrors)
                        {
                            logger.log("Invalid JSON format. Expected an object.");
                        }
                        originalTranscript = null;
                    }
                }
            }
            catch(IOException e)
            {
                if(logErrors)
                {
                    logger.log("Could not load original transcript: " + e.getMessage());
                }
                originalTranscript = null;
            }
        }
        else
        {
            if(logErrors)
            {
                logger.log("JSON is empty!");
            }
            originalTranscript = null;
        }
    }
    
    private String getOriginalFilePath()
    {
        return "/" + this.filePrefix + "_en";
    }
    private String getTranslationFilePath()
    {
        return "/" + this.filePrefix + "_" + currentLang;
    }
}
