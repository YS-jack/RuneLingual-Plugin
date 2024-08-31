package com.RuneLingual.ApiTranslate;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.TranslatingServiceSelectableList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import javax.inject.Inject;

@Slf4j
public class Deepl {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private RuneLingualConfig config;
    private String deeplKey;
    @Getter @Setter
    private int deeplLimit = 500000;
    @Getter @Setter
    private int deeplCount = deeplLimit;

    @Inject
    public Deepl(RuneLingualPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        setUsageAndLimit();
    }

    // function to translate from and to specified language
    public String translate(String text, LangCodeSelectableList sourceLang, LangCodeSelectableList targetLang) {
        /*
        returns: String =
            the translation of the text from sourceLang to targetLang
            if failed, returns the original text

        sets: deeplCount = the number of characters translated using the API
         */
        deeplKey = plugin.getConfig().getAPIKey();

        String url = getTranslatorUrl();
        if(url.isEmpty()){// if selected service is not deepl, return as is
            return text;
        }

        String urlParameters = getUrlParameters(sourceLang, targetLang, text);
        String response = getResponce(url, urlParameters);
        if (response.isEmpty()) { // if response is empty, return as is
            return text;
        }

        setUsageAndLimit();

        return getTranslationInResponse(response);

    }

    private String getUrlParameters(LangCodeSelectableList sourceLang, LangCodeSelectableList targetLang, String text) {

        String key = config.getAPIKey();
        String sourceLangCode = sourceLang.getDeeplLangCodeSource();
        String targetLangCode = targetLang.getDeeplLangCodeTarget();
        return "auth_key=" + key + "&text=" + text + "&source_lang=" + sourceLangCode + "&target_lang=" + targetLangCode;
    }

    private String getTranslatorUrl() {
        String baseUrl = getBaseUrl();
        if(baseUrl.isEmpty()){
            return "";
        }
        return baseUrl + "translate";
    }

    private String getBaseUrl() {
        if (Objects.equals(config.getService().getServiceName(), TranslatingServiceSelectableList.DeepL.getServiceName())) {
            return "https://api-free.deepl.com/v2/";
        } else if (Objects.equals(config.getService().getServiceName(), TranslatingServiceSelectableList.DeepL_PRO.getServiceName())) {
            return "https://api.deepl.com/v2/";
        } else {
            return "";
        }
    }

    private String getResponce(String url, String urlParameters){
        try {
            URL deeplUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) deeplUrl.openConnection();
            // Set the request method to POST
            connection.setRequestMethod("POST");

            // Enable input and output streams
            connection.setDoOutput(true);

            // Write the parameters to the request
            OutputStream os = connection.getOutputStream();
            os.write(urlParameters.getBytes());
            os.flush();
            os.close();

            // Get the response code
            int responseCode = connection.getResponseCode();

            // If the response code is 200 (HTTP_OK), read the response
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Print the response
                return response.toString();
            } else {
                System.out.println("POST request not worked");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getTranslationInResponse(String response){
        JSONObject jsonObject = new JSONObject(response);
        JSONArray translationsArray = jsonObject.getJSONArray("translations");
        JSONObject firstTranslation = translationsArray.getJSONObject(0);
        return firstTranslation.getString("text");
    }

    // function to set usage of the API
    private void setUsageAndLimit() {
        String usage = getUsage();
        //log.info("usage: " + usage);
        if(usage.isEmpty()){
            return;
        }
        JSONObject jsonObject = new JSONObject(usage);
        deeplCount = jsonObject.getInt("character_count");
        deeplLimit = jsonObject.getInt("character_limit");
        //log.info("updated deepl count:" + deeplCount+"\nupdated deepl limit" + deeplLimit);
    }
    private String getUsage() {
        // URL of the DeepL API
        String url = getUsageUrl();
        if(url.isEmpty()){
            return "";
        }
        String paramUrl = "auth_key=" + config.getAPIKey();
        return getResponce(url, paramUrl);
    }

    private String getUsageUrl() {
        String baseUrl = getBaseUrl();
        if (baseUrl.isEmpty()) {
            return "";
        }
        return getBaseUrl() + "usage";
    }

    // function to add to hashmap of past translations

    // function to add the translation to a file of past translations
}
