package com.RuneLingual.ApiTranslate;

import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.FileActions;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.RuneLingual.commonFunctions.Transformer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuEntry;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PastTranslationManager {
    private final String pastTranslationFile;
    @Inject
    private Deepl deepl;
    private RuneLingualPlugin plugin;
    private Map<String, String> pastTranslations = new ConcurrentHashMap<>();
    @Getter
    private Set<String> translationResults = new HashSet<>();


    @Inject
    public PastTranslationManager(Deepl deepl, RuneLingualPlugin plugin) {
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
                        translationResults.add(value);
                    }
                }

            } catch (IOException e) {
                log.error("Error reading file: " + e.getMessage(), e);
            }
        } else {
            // Ensure the directory exists
            try {
                Files.createDirectories(Paths.get(pastTranslationFile).getParent());
            } catch (IOException e) {
                log.error("Error creating directories: " + e.getMessage(), e);
                return; // Exit the method if we can't create the directory
            }
            try {
                Path filePath = Paths.get(pastTranslationFile);
                Files.createDirectories(filePath.getParent());
                Files.createFile(Paths.get(pastTranslationFile));
            } catch (FileAlreadyExistsException e) {
                log.info("File already exists: " + pastTranslationFile);
            } catch (NoSuchFileException e) {
                log.error("Unable to create file, directory doesn't exist: " + e.getMessage(), e);
            } catch (AccessDeniedException e) {
                log.error("Permission denied when creating file: " + e.getMessage(), e);
            } catch (IOException e) {
                log.error("Error creating file: " + e.getMessage(), e);
            }

        }
    }

    /**
     * Get past translation from hashmap
     *
     * @param text the text to get past translation for
     * @return the past translation if it exists, null otherwise
     */
    public String getPastTranslation(String text) {
        // first check that the text is not a result of translation
        if (translationResults.contains(text) || text.isEmpty() || text.isBlank()) {
            return text;
        }
        // if its not a result of translation, check the past translation map
        String translation = pastTranslations.getOrDefault(text, null);
        if (translation != null) {
            return translation;
        }

        /////////////////////////////////////////////////
        // text may have been transformed into generic text (enColVal, where color tags are tunred to <colNum#> and numbers are turned to <valNum#>)
        String genericText = Transformer.getEnglishColValFromText(text);
        // check that the text is not a result of translation
        if (translationResults.contains(genericText)) {
            return text;
        }
        // if its not a result of translation, check the past translation map
        translation = pastTranslations.getOrDefault(text, null);
        if (translation != null) {
            return translation;
        }

        ////////////////////////////////////////////////
        // texts with just 1 color tag may have had their color tags removed
        String textWithColorTagRemoved = Colors.removeNonImgTags(text);
        Colors colTag = Colors.getColorArray(text, Colors.white)[0];
        if (colTag == null) {
            return null;
        }
        // check that the text is not a result of translation
        if (translationResults.contains(textWithColorTagRemoved)) {
            return text;
        }
        // if its not a result of translation, check the past translation map
        translation = pastTranslations.getOrDefault(textWithColorTagRemoved, null);
        if (translation != null) {
            return colTag.getColorTag() + translation;
        }
        return null;
    }

    public void addToPastTranslations(String text, String translation) {
        // add to the map
        pastTranslations.put(text, translation);
        // add to the set of translation results
        translationResults.add(translation);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(pastTranslationFile, true), StandardCharsets.UTF_8))) {
            String content = text + "|" + translation + "\n";
            writer.write(content);
            writer.flush(); // Ensure the content is written immediately
        } catch (IOException e) {
            log.error("Error writing to file: " + e.getMessage(), e);
        }
    }

    public boolean haveTranslatedMenuBefore(String option, String target, MenuEntry menuEntry) {
        String[] optionWordArray = Colors.getWordArray(option);
        String[] targetWordArray = Colors.getWordArray(target);

        // if option is set to be translated with API, check if all elements have been translated before
        if (plugin.getConfig().getMenuOptionConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)) {
            if (check4PendingElementInPastTranslation(optionWordArray)) {
                return false;
            }
        }

        // if target is item name and that is set to be translated with API,
        // check if all elements have been translated before
        if (plugin.getConfig().getItemNamesConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)
                && (plugin.getMenuCapture().isItemInWidget(menuEntry) || plugin.getMenuCapture().isItemOnGround(menuEntry.getType()))) {
            if (check4PendingElementInPastTranslation(targetWordArray)) {
                return false;
            }
        }

        // if target is object name, check if all elements have been translated before
        if (plugin.getConfig().getObjectNamesConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)
                && plugin.getMenuCapture().isObjectMenu(menuEntry.getType())) {
            if (check4PendingElementInPastTranslation(targetWordArray)) {
                return false;
            }
        }

        // if target is npc name, check if all elements have been translated before
        if (plugin.getConfig().getNPCNamesConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)
                && plugin.getMenuCapture().isNpcMenu(menuEntry.getType())) {
            if (check4PendingElementInPastTranslation(targetWordArray)) {
                return false;
            }
        }


        // if other target (general menu, walk here, player, etc) is set to be translated with API,
        // check if all elements have been translated before
        if (!target.isEmpty() &&
                plugin.getConfig().getMenuOptionConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)
                && !plugin.getMenuCapture().isItemInWidget(menuEntry)
                && !plugin.getMenuCapture().isItemOnGround(menuEntry.getType())
                && !plugin.getMenuCapture().isObjectMenu(menuEntry.getType())
                && !plugin.getMenuCapture().isNpcMenu(menuEntry.getType())) {
            if (check4PendingElementInPastTranslation(targetWordArray)) {
                return false;
            }
        }

        return true;
    }

    private boolean check4PendingElementInPastTranslation(String[] wordArray) {
        for (String word : wordArray) {
            if (plugin.getDeepl().getDeeplPastTranslationManager().getPastTranslation(word) == null) {
                return true;
            }
        }
        return false;
    }

    public boolean haveTranslatedBefore(String text) {
        return pastTranslations.containsKey(text) || translationResults.contains(text)
                || pastTranslations.containsKey(Transformer.getEnglishColValFromText(text))
                || translationResults.contains(Transformer.getEnglishColValFromText(text))
                || pastTranslations.containsKey(Colors.removeNonImgTags(text))
                || translationResults.contains(Colors.removeNonImgTags(text));
    }
}
