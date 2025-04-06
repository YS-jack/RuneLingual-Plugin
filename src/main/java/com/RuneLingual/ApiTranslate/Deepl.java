package com.RuneLingual.ApiTranslate;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.TranslatingServiceSelectableList;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.nonLatin.GeneralFunctions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

@Slf4j
public class Deepl {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private RuneLingualConfig config;
    @Inject
    private OkHttpClient httpClient;
    private String deeplKey;
    @Getter @Setter
    private int deeplLimit = 500000;
    @Getter @Setter
    private int deeplCount = deeplLimit;
    @Getter @Setter
    private boolean keyValid = true;

    @Getter @Setter
    private PastTranslationManager deeplPastTranslationManager;
    private static final MediaType mediaType = MediaType.parse("Content-Type: application/json");

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final Object apiStateLock = new Object();
    private volatile int apiStateVersion = 0; // Tracks the current state version of the API

    // add texts that has already been attempted to be translated.
    // this avoids translating same texts multiple times when ran in a thread, which will waste limited or paid word count
    @Getter
    private List<String> translationAttempt = new ArrayList<>();

    @Inject
    public Deepl(RuneLingualPlugin plugin, OkHttpClient httpClient) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.httpClient = httpClient;
        //setUsageAndLimit();
        deeplKey = plugin.getConfig().getAPIKey();
        deeplPastTranslationManager = new PastTranslationManager(this, plugin);
    }

    /**
     * Translates the given text from the source language to the target language.
     * Sets deeplCount the number of characters translated using the API
     *
     * @param text the text to be translated
     * @param sourceLang the source language
     * @param targetLang the target language
     * @return the translated text if translated in the past, the original text if the translation fails or trying to translate
     */
    public String translate(String text, LangCodeSelectableList sourceLang, LangCodeSelectableList targetLang) {
        if(!plugin.getConfig().ApiConfig()){
            return text;
        }
        // if the text is already translated, return the past translation
        String pastTranslation = deeplPastTranslationManager.getPastTranslation(text);
        if (pastTranslation != null) {
            return pastTranslation;
        }

        // don't translate if text is empty, or has been attempted to translate
        if (text.isEmpty() || translationAttempt.contains(text)) {
            return text;
        }
        deeplKey = plugin.getConfig().getAPIKey();

        setUsageAndLimit();
        //if the character count is close to the limit, return the original text
        if(deeplCount > deeplLimit - text.length() - 1000){
            return text;
        }

        // from here, attempt to translate the text
        translationAttempt.add(text);


        String url = getTranslatorUrl();
        if(url.isEmpty()){// if selected service is not deepl, return as is
            return text;
        }

        JsonObject urlParameters = getUrlParameters(sourceLang, targetLang, text);

        getResponse(url, FormBody.create(mediaType, urlParameters.toString()), new ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                setUsageAndLimit();
                String translation = getTranslationInResponse(response);
                if (!translation.isEmpty()) {
                    // add the new translation to the past translations and its file
                    deeplPastTranslationManager.addToPastTranslations(text, translation);
                    setKeyValid(true);
                    translationAttempt.remove(text);
                }

            }

            @Override
            public void onFailure(Exception error) {
                setKeyValid(false);
                handleError(error);
            }

            @Override
            public void onApiOff() {
                translationAttempt.remove(text);
            }
        });

        return text; // return original text while the translation is being processed in the thread
    }

    private JsonObject getUrlParameters(LangCodeSelectableList sourceLang, LangCodeSelectableList targetLang, String text) {
        String targetLangCode = targetLang.getDeeplLangCodeTarget();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(text);

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("text", jsonArray);
        jsonObject.addProperty("target_lang", targetLangCode);
        jsonObject.addProperty("context", "runescape; dungeons and dragons; medieval fantasy;");
        jsonObject.addProperty("split_sentences", "nonewlines");
        jsonObject.addProperty("preserve_formatting", true);
        jsonObject.addProperty("formality", "prefer_less");
        jsonObject.addProperty("source_lang", "EN");

        return jsonObject;
    }

    private String getTranslatorUrl() {
        String baseUrl = getBaseUrl();
        if(baseUrl.isEmpty()){
            return "";
        }
        return baseUrl + "translate";
    }

    private String getUsageUrl() {
        String baseUrl = getBaseUrl();
        if(baseUrl.isEmpty()){
            return "";
        }
        return baseUrl + "usage";
    }

    private String getBaseUrl() {
        if (Objects.equals(config.getApiServiceConfig().getServiceName(), TranslatingServiceSelectableList.DeepL.getServiceName())) {
            return "https://api-free.deepl.com/v2/";
        } else if (Objects.equals(config.getApiServiceConfig().getServiceName(), TranslatingServiceSelectableList.DeepL_PRO.getServiceName())) {
            return "https://api.deepl.com/v2/";
        } else {
            return "";
        }
    }

    private void getResponse(String url, RequestBody requestBody, ResponseCallback callback) {
        getResponseWithRetry(url, requestBody, callback, 0);
    }

    private void getResponseWithRetry(String url, RequestBody requestBody, ResponseCallback callback, int retryCount) {
        final int currentVersion;
        synchronized (apiStateLock) {
            currentVersion = apiStateVersion; // Capture the current version
        }

        if (!plugin.getConfig().ApiConfig()) {
            callback.onApiOff();
            return;
        }

        if (deeplKey == null || deeplKey.isEmpty()) {
            callback.onFailure(new IOException("API key is missing"));
            return;
        }

        try {
            Request.Builder request = new Request.Builder()
                    .addHeader("User-Agent", RuneLite.USER_AGENT + " (runelingual)")
                    .addHeader("Authorization", "DeepL-Auth-Key " + deeplKey)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Content-Length", String.valueOf(requestBody.contentLength()))
                    .url(url)
                    .post(requestBody);

            httpClient.newCall(request.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException error) {
                    synchronized (apiStateLock) {
                        if (currentVersion != apiStateVersion || !plugin.getConfig().ApiConfig()) {
                            //log.info("Discarding outdated failure callback due to API state change.");
                            return;
                        }
                    }
                    if (retryCount < 5) {
                        int delay = new Random().nextInt(8) + 3;
                        synchronized (apiStateLock) {
                            if (currentVersion != apiStateVersion || !plugin.getConfig().ApiConfig()) {
                                //log.info("Discarding outdated retry scheduling due to API state change.");
                                return;
                            }
                        }
                        //log.info("on failure: Retrying API request in {} seconds (attempt {})", delay, retryCount + 1);
                        scheduler.schedule(() -> {
                            synchronized (apiStateLock) {
                                if (currentVersion != apiStateVersion || !plugin.getConfig().ApiConfig()) {
                                    //log.info("Discarding outdated retry execution due to API state change.");
                                    return;
                                }
                            }
                            getResponseWithRetry(url, requestBody, callback, retryCount + 1);
                        }, delay, TimeUnit.SECONDS);
                    } else {
                        callback.onFailure(error);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) {
                    synchronized (apiStateLock) {
                        if (currentVersion != apiStateVersion || !plugin.getConfig().ApiConfig()) {
                            //log.info("Discarding outdated response callback due to API state change.");
                            return;
                        }
                    }
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            String responseBodyString = responseBody.string();
                            if (response.code() == 429) { // Too Many Requests
                                if (retryCount < 5) {
                                    int delay = new Random().nextInt(8) + 3;
                                    synchronized (apiStateLock) {
                                        if (currentVersion != apiStateVersion || !plugin.getConfig().ApiConfig()) {
                                            //log.info("Discarding outdated retry scheduling due to API state change.");
                                            return;
                                        }
                                    }
                                    //log.info("on response: Retrying API request in {} seconds (attempt {})", delay, retryCount + 1);
                                    scheduler.schedule(() -> {
                                        synchronized (apiStateLock) {
                                            if (currentVersion != apiStateVersion || !plugin.getConfig().ApiConfig()) {
                                                //log.info("Discarding outdated retry execution due to API state change.");
                                                return;
                                            }
                                        }
                                        getResponseWithRetry(url, requestBody, callback, retryCount + 1);
                                    }, delay, TimeUnit.SECONDS);
                                } else {
                                    callback.onFailure(new IOException("Too many requests"));
                                }
                            } else {
                                callback.onSuccess(responseBodyString);
                            }
                        } else {
                            callback.onFailure(new IOException("Response body is null"));
                        }
                    } catch (Exception error) {
                        callback.onFailure(error);
                    }
                }
            });
        } catch (Exception error) {
            log.error("Failed to create the API request", error);
            callback.onFailure(error);
        }
    }

    private void handleError(Exception error) {
        log.error("Failed to get response from DeepL API.", error);
    }


    public interface ResponseCallback {
        void onSuccess(String response);
        void onFailure(Exception error);
        void onApiOff();
    }

    private String getTranslationInResponse(String response) {
        JSONObject jsonObject = new JSONObject(response);
        if (jsonObject.has("translations")) {
            JSONArray translationsArray = jsonObject.getJSONArray("translations");
            if (!translationsArray.isEmpty()) {
                JSONObject translationObject = translationsArray.getJSONObject(0);
                return translationObject.getString("text");
            }
        }
        return "";
    }

    // function to set usage of the API
    public void setUsageAndLimit() {
        getResponse(getUsageUrl(), FormBody.create(mediaType, ""), new ResponseCallback() {
            @Override
            public void onSuccess(String usage) {
                if (usage.isEmpty()) {
                    return;
                }
                JSONObject jsonObject = new JSONObject(usage);
                deeplCount = jsonObject.getInt("character_count");
                deeplLimit = jsonObject.getInt("character_limit");
                //log.info("updated deepl count:" + deeplCount + "\nupdated deepl limit" + deeplLimit);
            }

            @Override
            public void onFailure(Exception error) {
                handleError(error);
            }

            @Override
            public void onApiOff() {
                //log.info("API is disabled");
            }
        });
    }
}
