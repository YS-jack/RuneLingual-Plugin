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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

@Slf4j
public class Deepl {
    private static final int DEEPL_MAX_HEADER_BYTES = 16 * 1024;
    private static final int DEEPL_MAX_TOTAL_REQUEST_BYTES = 128 * 1024;
    private static final int DEEPL_FREE_MONTHLY_CHAR_LIMIT = 500_000;
    private static final int DEEPL_MONTHLY_LIMIT_SAFETY_BUFFER = 1000;
    private static final long USAGE_REFRESH_INTERVAL_MILLIS = 60_000L;
    private static final long RATE_LIMIT_BACKOFF_MILLIS = 10_000L;
    private static final long GENERIC_BACKOFF_MILLIS = 5_000L;

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
    private volatile long lastUsageRefreshMillis = 0L;
    private volatile long globalRetryNotBeforeMillis = 0L;
    private final Map<String, Long> textRetryNotBeforeMillis = new ConcurrentHashMap<>();

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

    private void apiDebugLog(String message, Object... args) {
        if (!config.apiDebugLogs()) {
            return;
        }
        log.info("[RuneLingual API] " + message, args);
    }

    private String previewText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= 100) {
            return normalized;
        }
        return normalized.substring(0, 100) + "...";
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
        if (isRetryCoolingDown(text)) {
            apiDebugLog("Skip translate: retry cooldown active (service={}, text='{}')",
                    config.getApiServiceConfig(), previewText(text));
            return text;
        }
        // if the text is already translated, return the past translation
        String pastTranslation = deeplPastTranslationManager.getPastTranslation(text);
        if (pastTranslation != null) {
            return pastTranslation;
        }

        // don't translate if text is empty, or has been attempted to translate, or is a result of translation
        if (text.isEmpty() || translationAttempt.contains(text) || deeplPastTranslationManager.getTranslationResults().contains(text)) {
            return text;
        }
        deeplKey = plugin.getConfig().getAPIKey();

        if (isDeepLServiceSelected()) {
            setUsageAndLimit();
            int billedCharacters = countTextCharacters(text);
            int effectiveLimit = getEffectiveMonthlyCharacterLimit();
            // if the character count is close to the limit, return the original text
            if(deeplCount > effectiveLimit - billedCharacters - DEEPL_MONTHLY_LIMIT_SAFETY_BUFFER){
                return text;
            }
        }


        String url = getTranslatorUrl();
        if(url.isEmpty()){// if selected service is not deepl, return as is
            apiDebugLog("Skip translate: no API URL configured (service={})", config.getApiServiceConfig());
            return text;
        }

        JsonObject urlParameters = getUrlParameters(sourceLang, targetLang, text);
        RequestBody requestBody = FormBody.create(mediaType, urlParameters.toString());
        long requestBodyBytes = getRequestBodyBytes(requestBody);
        apiDebugLog("Queue request service={} url={} source={} target={} chars={} bodyBytes={} text='{}'",
                config.getApiServiceConfig(),
                url,
                getLanguageCodeForService(sourceLang, true),
                getLanguageCodeForService(targetLang, false),
                countTextCharacters(text),
                requestBodyBytes,
                previewText(text));
        if (requestBodyBytes > DEEPL_MAX_TOTAL_REQUEST_BYTES) {
            log.warn("Skipping DeepL request: request body too large ({} bytes, max {} bytes).",
                    requestBodyBytes, DEEPL_MAX_TOTAL_REQUEST_BYTES);
            return text;
        }

        // from here, attempt to translate the text
        translationAttempt.add(text);

        getResponse(url, requestBody, new ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                translationAttempt.remove(text);
                setUsageAndLimit();
                String translation = getTranslationInResponse(response);
                if (!translation.isEmpty()) {
                    // add the new translation to the past translations and its file
                    deeplPastTranslationManager.addToPastTranslations(text, translation);
                    setKeyValid(true);
                    apiDebugLog("Success service={} source='{}' translated='{}'",
                            config.getApiServiceConfig(),
                            previewText(text),
                            previewText(translation));
                } else {
                    apiDebugLog("Success but empty translation service={} source='{}'",
                            config.getApiServiceConfig(),
                            previewText(text));
                }

            }

            @Override
            public void onFailure(Exception error) {
                if (isDeepLServiceSelected()) {
                    setKeyValid(false);
                } else if (isLikelyAuthenticationError(error)) {
                    setKeyValid(false);
                }
                translationAttempt.remove(text);
                scheduleRetryBackoff(text, error);
                apiDebugLog("Failure service={} source='{}' error={}",
                        config.getApiServiceConfig(),
                        previewText(text),
                        error == null ? "unknown" : previewText(error.getMessage()));
                handleError(error);
            }

            @Override
            public void onApiOff() {
                translationAttempt.remove(text);
            }
        });

        return text; // return original text while the translation is being processed in the thread
    }

    /**
     * Attempts to translate and waits for async API completion up to timeoutMillis.
     * Returns original text when timeout or error happens.
     */
    public String translateBlocking(String text, LangCodeSelectableList sourceLang, LangCodeSelectableList targetLang, long timeoutMillis) {
        String immediate = translate(text, sourceLang, targetLang);
        if (!Objects.equals(immediate, text) && !immediate.isBlank()) {
            return immediate;
        }

        long deadline = System.currentTimeMillis() + Math.max(timeoutMillis, 0);
        while (System.currentTimeMillis() < deadline) {
            String pastTranslation = deeplPastTranslationManager.getPastTranslation(text);
            if (pastTranslation != null && !Objects.equals(pastTranslation, text) && !pastTranslation.isBlank()) {
                return pastTranslation;
            }

            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return text;
    }

    private JsonObject getUrlParameters(LangCodeSelectableList sourceLang, LangCodeSelectableList targetLang, String text) {
        JsonObject jsonObject = new JsonObject();
        String targetLangCode = getLanguageCodeForService(targetLang, false);
        String sourceLangCode = getLanguageCodeForService(sourceLang, true);

        if (isDeepLServiceSelected()) {
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(text);
            jsonObject.add("text", jsonArray);
            jsonObject.addProperty("target_lang", targetLangCode);
            jsonObject.addProperty("context", "runescape; dungeons and dragons; medieval fantasy;");
            jsonObject.addProperty("split_sentences", "nonewlines");
            jsonObject.addProperty("preserve_formatting", true);
            jsonObject.addProperty("formality", "prefer_less");
            jsonObject.addProperty("source_lang", sourceLangCode);
            return jsonObject;
        }

        if (isLibreTranslateServiceSelected()) {
            jsonObject.addProperty("q", text);
            jsonObject.addProperty("source", sourceLangCode);
            jsonObject.addProperty("target", targetLangCode);
            jsonObject.addProperty("format", "text");
            if (deeplKey != null && !deeplKey.isEmpty()) {
                jsonObject.addProperty("api_key", deeplKey);
            }
        }

        return jsonObject;
    }

    private String getTranslatorUrl() {
        String baseUrl = getBaseUrl();
        if(baseUrl.isEmpty()){
            return "";
        }
        return baseUrl + "/translate";
    }

    private String getUsageUrl() {
        if (!isDeepLServiceSelected()) {
            return "";
        }
        String baseUrl = getBaseUrl();
        if(baseUrl.isEmpty()){
            return "";
        }
        return baseUrl + "/usage";
    }

    private String getBaseUrl() {
        if (isDeepLFreeServiceSelected()) {
            return "https://api-free.deepl.com/v2";
        } else if (isDeepLProServiceSelected()) {
            return "https://api.deepl.com/v2";
        } else if (isLibreTranslateServiceSelected()) {
            String configuredUrl = config.getLibreTranslateUrl();
            if (configuredUrl == null) {
                return "";
            }
            String trimmed = configuredUrl.trim();
            if (trimmed.isEmpty()) {
                return "";
            }
            while (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            return trimmed;
        } else {
            return "";
        }
    }

    private String getLanguageCodeForService(LangCodeSelectableList lang, boolean isSource) {
        if (isDeepLServiceSelected()) {
            return isSource ? lang.getDeeplLangCodeSource() : lang.getDeeplLangCodeTarget();
        }
        if (isLibreTranslateServiceSelected()) {
            String langCode = lang.getLangCode();
            if ("pt_br".equalsIgnoreCase(langCode)) {
                return "pt-BR";
            }
            if ("pt".equalsIgnoreCase(langCode)) {
                return "pt";
            }
            if ("no".equalsIgnoreCase(langCode)) {
                return "nb";
            }
            return langCode.replace('_', '-').toLowerCase(Locale.ROOT);
        }
        return lang.getLangCode();
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

        if (isDeepLServiceSelected() && (deeplKey == null || deeplKey.isEmpty())) {
            apiDebugLog("Request blocked: missing API key for DeepL service");
            callback.onFailure(new IOException("API key is missing"));
            return;
        }

        try {
            Request.Builder request = new Request.Builder()
                    .addHeader("User-Agent", RuneLite.USER_AGENT + " (runelingual)")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .url(url)
                    .post(requestBody);
            if (isDeepLServiceSelected()) {
                request.addHeader("Authorization", "DeepL-Auth-Key " + deeplKey);
            }
            long contentLength = requestBody.contentLength();
            if (contentLength >= 0L) {
                request.addHeader("Content-Length", String.valueOf(contentLength));
            }

            Request builtRequest = request.build();
            long headerBytes = getHeaderBytes(builtRequest);
            long requestBodyBytes = getRequestBodyBytes(requestBody);
            long totalRequestBytes = headerBytes + requestBodyBytes;

            if (headerBytes > DEEPL_MAX_HEADER_BYTES) {
                callback.onFailure(new IOException(
                        String.format("DeepL request header too large: %d bytes (max %d)", headerBytes, DEEPL_MAX_HEADER_BYTES)));
                return;
            }
            if (totalRequestBytes > DEEPL_MAX_TOTAL_REQUEST_BYTES) {
                callback.onFailure(new IOException(
                        String.format("DeepL request too large: %d bytes (max %d)", totalRequestBytes, DEEPL_MAX_TOTAL_REQUEST_BYTES)));
                return;
            }

            httpClient.newCall(builtRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException error) {
                    synchronized (apiStateLock) {
                        if (currentVersion != apiStateVersion || !plugin.getConfig().ApiConfig()) {
                            //log.info("Discarding outdated failure callback due to API state change.");
                            return;
                        }
                    }
                    if (retryCount < 5) {
                        long delaySeconds = getRetryDelaySeconds();
                        apiDebugLog("Network failure (service={}) retry={} in {}s error={}",
                                config.getApiServiceConfig(),
                                retryCount + 1,
                                delaySeconds,
                                previewText(error.getMessage()));
                        scheduler.schedule(() -> {
                            synchronized (apiStateLock) {
                                if (currentVersion != apiStateVersion || !plugin.getConfig().ApiConfig()) {
                                    return;
                                }
                            }
                            getResponseWithRetry(url, requestBody, callback, retryCount + 1);
                        }, delaySeconds, TimeUnit.SECONDS);
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
                        if (responseBody == null) {
                            callback.onFailure(new IOException("Response body is null"));
                            return;
                        }

                        String responseBodyString = responseBody.string();
                        int responseCode = response.code();
                        boolean isCongestionCode = responseCode == 429 || responseCode == 503 || responseCode == 529;

                        if (isCongestionCode) {
                            setGlobalRetryBackoff(System.currentTimeMillis() + RATE_LIMIT_BACKOFF_MILLIS);
                            if (retryCount < 5) {
                                long delaySeconds = getRetryDelaySeconds();
                                apiDebugLog("HTTP congestion service={} status={} retry={} in {}s",
                                        config.getApiServiceConfig(),
                                        responseCode,
                                        retryCount + 1,
                                        delaySeconds);
                                scheduler.schedule(() -> {
                                    synchronized (apiStateLock) {
                                        if (currentVersion != apiStateVersion || !plugin.getConfig().ApiConfig()) {
                                            return;
                                        }
                                    }
                                    getResponseWithRetry(url, requestBody, callback, retryCount + 1);
                                }, delaySeconds, TimeUnit.SECONDS);
                                return;
                            }
                            callback.onFailure(new IOException("Translation API congestion/rate limit: HTTP " + responseCode));
                            return;
                        }

                        if (!response.isSuccessful()) {
                            String message = responseBodyString;
                            if (message.length() > 300) {
                                message = message.substring(0, 300) + "...";
                            }
                            apiDebugLog("HTTP error service={} status={} body='{}'",
                                    config.getApiServiceConfig(),
                                    responseCode,
                                    previewText(message));
                            callback.onFailure(new IOException("Translation API HTTP " + responseCode + ": " + message));
                            return;
                        }

                        callback.onSuccess(responseBodyString);
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
        log.error("Failed to get response from translation API {}.", config.getApiServiceConfig(), error);
    }


    public interface ResponseCallback {
        void onSuccess(String response);
        void onFailure(Exception error);
        void onApiOff();
    }

    private String getTranslationInResponse(String response) {
        JSONObject jsonObject = new JSONObject(response);
        if (jsonObject.has("translatedText")) {
            Object translatedText = jsonObject.get("translatedText");
            if (translatedText instanceof JSONArray) {
                JSONArray arr = (JSONArray) translatedText;
                if (arr.length() > 0) {
                    return arr.getString(0);
                }
            } else if (translatedText != null) {
                return translatedText.toString();
            }
        }
        if (jsonObject.has("translations")) {
            JSONArray translationsArray = jsonObject.getJSONArray("translations");
            if (translationsArray != null && translationsArray.length() > 0) { // .length() > 0 is used instead of ! .isEmpty() because .isEmpty on JsonObject doesnt work on the runelite client for some reason
                JSONObject translationObject = translationsArray.getJSONObject(0);
                return translationObject.getString("text");
            }
        }
        return "";
    }

    // function to set usage of the API
    public void setUsageAndLimit() {
        if (!isDeepLServiceSelected()) {
            keyValid = true;
            deeplCount = 0;
            deeplLimit = Integer.MAX_VALUE;
            return;
        }
        String usageUrl = getUsageUrl();
        if (usageUrl.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < globalRetryNotBeforeMillis) {
            return;
        }
        if (now - lastUsageRefreshMillis < USAGE_REFRESH_INTERVAL_MILLIS) {
            return;
        }
        lastUsageRefreshMillis = now;

        getResponse(usageUrl, FormBody.create(mediaType, ""), new ResponseCallback() {
            @Override
            public void onSuccess(String usage) {
                if (usage.isEmpty()) {
                    log.error("API response is empty.");
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(usage);
                    if (jsonObject.has("character_count") && jsonObject.has("character_limit")) {
                        keyValid = true;
                        deeplCount = jsonObject.getInt("character_count");
                        deeplLimit = jsonObject.getInt("character_limit");
                        apiDebugLog("Usage update service={} count={} limit={}",
                                config.getApiServiceConfig(),
                                deeplCount,
                                deeplLimit);
                        //log.info("Updated deepl count: " + deeplCount + "\nUpdated deepl limit: " + deeplLimit);
                    } else {
                        keyValid = false;
                        log.error("Required keys not found in API response: " + usage);
                    }
                } catch (JSONException e) {
                    keyValid = false;
                    log.error("Failed to parse API response: " + usage, e);
                }
            }

            @Override
            public void onFailure(Exception error) {
                lastUsageRefreshMillis = System.currentTimeMillis() - USAGE_REFRESH_INTERVAL_MILLIS + 5000L;
                handleError(error);
            }

            @Override
            public void onApiOff() {
                //log.info("API is disabled");
            }
        });
    }

    private boolean isRetryCoolingDown(String text) {
        long now = System.currentTimeMillis();
        if (now < globalRetryNotBeforeMillis) {
            return true;
        }
        Long textRetryNotBefore = textRetryNotBeforeMillis.get(text);
        if (textRetryNotBefore == null) {
            return false;
        }
        if (textRetryNotBefore > now) {
            return true;
        }
        textRetryNotBeforeMillis.remove(text);
        return false;
    }

    private void scheduleRetryBackoff(String text, Exception error) {
        long now = System.currentTimeMillis();
        String message = error == null || error.getMessage() == null
                ? ""
                : error.getMessage().toLowerCase(Locale.ROOT);

        boolean isCongestion = message.contains("429")
                || message.contains("too many requests")
                || message.contains("congestion")
                || message.contains("rate limit")
                || message.contains("503")
                || message.contains("529");

        long backoffMillis = isCongestion ? RATE_LIMIT_BACKOFF_MILLIS : GENERIC_BACKOFF_MILLIS;
        textRetryNotBeforeMillis.put(text, now + backoffMillis);
        if (isCongestion) {
            setGlobalRetryBackoff(now + RATE_LIMIT_BACKOFF_MILLIS);
        }
    }

    private void setGlobalRetryBackoff(long notBeforeMillis) {
        if (notBeforeMillis > globalRetryNotBeforeMillis) {
            globalRetryNotBeforeMillis = notBeforeMillis;
        }
    }

    private long getRetryDelaySeconds() {
        return new Random().nextInt(8) + 3;
    }

    public boolean canTranslateNow() {
        if (!plugin.getConfig().ApiConfig()) {
            return false;
        }
        if (System.currentTimeMillis() < globalRetryNotBeforeMillis) {
            return false;
        }
        if (isDeepLServiceSelected()) {
            deeplKey = plugin.getConfig().getAPIKey();
            if (deeplKey == null || deeplKey.isEmpty()) {
                return false;
            }
            if (!keyValid) {
                return false;
            }
            int effectiveLimit = getEffectiveMonthlyCharacterLimit();
            return deeplCount <= effectiveLimit - DEEPL_MONTHLY_LIMIT_SAFETY_BUFFER;
        }
        if (isLibreTranslateServiceSelected()) {
            return keyValid && !getTranslatorUrl().isEmpty();
        }
        if (!keyValid) {
            return false;
        }
        return !getTranslatorUrl().isEmpty();
    }

    private int getEffectiveMonthlyCharacterLimit() {
        if (!isDeepLServiceSelected()) {
            return Integer.MAX_VALUE;
        }
        int configuredLimit = deeplLimit > 0 ? deeplLimit : DEEPL_FREE_MONTHLY_CHAR_LIMIT;
        if (isDeepLFreeServiceSelected()) {
            return Math.min(configuredLimit, DEEPL_FREE_MONTHLY_CHAR_LIMIT);
        }
        return configuredLimit;
    }

    public boolean usesDeepLUsageLimits() {
        return isDeepLServiceSelected();
    }

    private boolean isDeepLServiceSelected() {
        return config.getApiServiceConfig().isDeepLFamily();
    }

    private boolean isDeepLFreeServiceSelected() {
        return config.getApiServiceConfig() == TranslatingServiceSelectableList.DeepL;
    }

    private boolean isDeepLProServiceSelected() {
        return config.getApiServiceConfig() == TranslatingServiceSelectableList.DeepL_PRO;
    }

    private boolean isLibreTranslateServiceSelected() {
        return config.getApiServiceConfig() == TranslatingServiceSelectableList.LibreTranslate;
    }

    private boolean isLikelyAuthenticationError(Exception error) {
        if (error == null || error.getMessage() == null) {
            return false;
        }
        String message = error.getMessage().toLowerCase(Locale.ROOT);
        return message.contains("api key")
                || message.contains("invalid key")
                || message.contains("unauthorized")
                || message.contains("forbidden")
                || message.contains("http 401")
                || message.contains("http 403");
    }

    private int countTextCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.codePointCount(0, text.length());
    }

    private long getRequestBodyBytes(RequestBody requestBody) {
        try {
            long contentLength = requestBody.contentLength();
            return contentLength < 0 ? DEEPL_MAX_TOTAL_REQUEST_BYTES + 1L : contentLength;
        } catch (IOException e) {
            return DEEPL_MAX_TOTAL_REQUEST_BYTES + 1L;
        }
    }

    private long getHeaderBytes(Request request) {
        long bytes = 0L;
        Headers headers = request.headers();
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            String value = headers.value(i);
            bytes += name.getBytes(StandardCharsets.UTF_8).length;
            bytes += 2; // ": "
            bytes += value.getBytes(StandardCharsets.UTF_8).length;
            bytes += 2; // CRLF
        }
        bytes += 2; // final CRLF
        return bytes;
    }
}
