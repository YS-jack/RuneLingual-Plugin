# Developer Guide for RuneLingual Plugin

Welcome to the developer guide for the RuneLingual plugin. This document is intended for developers and RuneLite reviewers to provide a comprehensive overview of the plugin.

## 1. Features

This plugin aims to translate all on-screen text. It also supports chat input for languages that use non-Latin characters. Refer to the main README for some images.

- **Translation Methods**:
  - **Manual Transcripts**: Uses transcripts created manually on the [RuneLingual transcript](https://github.com/YS-jack/Runelingual-Transcripts) repository.
  - **DeepL API**: Translates text using DeepL's API, selectable in the configuration.

## 2. Brief Explanation of What Happens

The plugin processes every text in the menu entries and visible widgets. Depending on the user's configuration:

- **DeepL Translation**: Checks if the text has been translated before. If yes, it displays the cached translation; otherwise, it translates the text via the API.
- **Manual Transcript Translation**: Searches the SQL database for the text and displays the returned translation.

## 3. Resources
This plugin downloads as well as creates some files for it to work. Here is a brief explanation of those resources:
1. On start up, the plugin checks if the transcripts has been updated by downloading the hash file of the selected language from the [RuneLingual Transcript repository](https://github.com/YS-jack/RuneLingual-Plugin/tree/master?tab=readme-ov-file), such as [the Japanese transcript](https://github.com/YS-jack/Runelingual-Transcripts/blob/original-main/public/ja/hashList_ja.txt).
2. After downloading the hash file, the plugin compares the hash with the local hash file. If the hashes are different, the plugin downloads the updated transcripts and creates/updates the local SQL database.
3. After confirming that the transcripts are up-to-date, the plugin loads the SQL database into memory for quick access.
4. Other than the transcripts, the plugin also downloads other files that are necessary for languages that use non-Latin characters, such as Japanese and Chinese. 
These files include:
   1. Character images. This will be loaded into the plugin's image cache for quick access at startup.
   2. Input mapping, such as "a" to "あ", "i" to "い", etc.
   3. The transform candidate ranking file. In case of Japanese, this will be the mapping of Hiragana to Kanji ordered in terms of frequency.
5. The plugin produces some files for the user's convenience:
   1. A list of texts and their translation by the DeepL API, which will be used instead of API calls to save translation credits.
   2. A text file whose file name includes the user's selected language to load the side panel in that language. e.g 'setLang_ja.txt' for Japanese.

## 4. Limitations

- The plugin cannot translate texts that are not widgets or menu entries, such as texts on the world map.
- Currently only translates to Japanese. Will add other languages if there are support from native speakers.
- Only translates texts in interfaces and buttons via DeepL API. This may cause texts to overlap or not fit inside the parent widget. Will add support for manual transcripts soon (TM).

## 5. Features to be Added

- **Setting Interface**: Create a settings interface that displays texts in the selected language.
- **Bank Tags**: Add bank tags to every item for searching in the selected language (may not be possible).
- **Plugin Compatibility**: Ensure compatibility with other useful plugins like the menu entry swapper and ground marker. This may involve creating similar versions within this plugin or making pull requests to those plugins.

## 6. Collecting Transcripts
1. Get the Widget ID of the parent that contains the text you want to translate.
2. Add the ID to [Ids class](src/main/java/com/RuneLingual/commonFunctions/Ids.java) below line 64. Make sure to add a comment to describe what widget it includes, such as `// parent id for the texts in the shop UI`.
3. Add corresponding sql values to [SqlVariables class](src/main/java/com/RuneLingual/Sql/SqlVariables.java), below line 62. Name should be `"sourceValue4" + the name of the source`.
   The second value "source" should not be changed, as it is the name of the column in the database. Also make sure to add a comment to describe what the text is, such as `// shop UI text`.
4. Go to [WidgetCapture class'](src/main/java/com/RuneLingual/Widgets/WidgetCapture.java) `modifySqlQuery4Widget` function and an if statement to in the form of
   ```java
   if(widgetId == ids.getNameOfIdInStep2){
      sqlQuery.setGeneralUI(SqlVariables.NameInStep3.getValue());
   }
   ```
5. Also in the same WidgetCapture class, edit the `ifIsDumpTarget_thenDump` function, change the first if statement to detect the widget you want to capture, like:
   ```java
   if (sqlQuery.getSource() != null && sqlQuery.getSource().equals(SqlVariables.NameInStep3.getValue())){
    ...
   ```
   Then change the file name variable `String fileName` to something appropriate, and doesnt exist yet in the `dump` folder.
   You don't have to change anything else.
6. Run the plugin and open the interface you want to capture. You should see the text file in the `dump` folder.
7. If this is your first time, be sure to make a PR to the latest branch so someone can check you are doing thing right.