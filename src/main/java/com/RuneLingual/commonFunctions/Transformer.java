package com.RuneLingual.commonFunctions;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.nonLatin.GeneralFunctions;
import com.RuneLingual.SQL.SqlVariables;
import com.RuneLingual.SQL.SqlQuery;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;

@Slf4j
public class Transformer {
    @Inject
    private RuneLingualPlugin plugin;

    private final Colors colorObj = Colors.blue;

    public enum TransformOption {
        AS_IS,
        TRANSLATE_LOCAL,
        TRANSLATE_API,
        TRANSLITERATE,
        TRANSFORM, // such as from alphabet to kanji, like neko -> 猫
    }

    @Inject
    public Transformer(RuneLingualPlugin plugin){
        this.plugin = plugin;
    }


    public String transformEngWithColor(TransformOption option, SqlQuery sqlQuery, boolean searchAlike){

        boolean needCharImage = plugin.getConfig().getSelectedLanguage().needsCharImages();
        GeneralFunctions generalFunctions = plugin.getGeneralFunctions();
        String text = sqlQuery.getEnglish();
        if(text == null || text.isEmpty()){
            plugin.getFailedTranslations().add(sqlQuery);
            return textAddColor(text, sqlQuery.getColor());
        }

        String translatedText = "";

        if(option == TransformOption.AS_IS){
            translatedText = text;
        } else if(option == TransformOption.TRANSLATE_LOCAL){
        /*
        if there are 2 or more color tags in the sqlQuery.english, english and translation in the database will have color tag placeholders.
        must replace the color tags in sqlQuery.english with the color tag placeholders,
        and do the reverse for the obtained translation

        e.g.
        sqlQuery.english: <col=ff>text1<col=0>text2
        english in database: <colNum1>text1<colNum2>text2
        translation in database: <colNum2>translatedText2<colNum1>translatedText1
        final translation: <col=0>translatedText2<col=ff>translatedText1
         */
            List<String> colorTagsAsIs = Colors.getColorTagsAsIs(sqlQuery.getEnglish());
//            int trueColorTagCount = Colors.countColorTagsAfterReformat(sqlQuery.getEnglish());
//            for(int i = 0; i < colorTagsAsIs.size(); i++){
//                sqlQuery.setEnglish(sqlQuery.getEnglish().replace(colorTagsAsIs.get(i), "<colNum" + i + ">")); // replace color tags with placeholders
//            }
            sqlQuery.setEnglish(Colors.getEnumeratedColorWord(sqlQuery.getEnglish())); // replace color tags with placeholders
            // if translating failed for this query before, return the original text with color
            if (plugin.getFailedTranslations().contains(sqlQuery)) {
                return sqlQuery.getEnglish();
            }
            String[] result = sqlQuery.getMatching(SqlVariables.columnTranslation, searchAlike);
            if(result.length == 0){
                log.info("(engWithColor) No translation found for " + text + " ");
                //log.info("query = " + sqlQuery.getSearchQuery());
                plugin.getFailedTranslations().add(sqlQuery);
                return textAddColor(text, sqlQuery.getColor());
                //translatedText = text;
            } else {
                if(result[0].isEmpty()) { // text exists in database but hasn't been translated yet
                    //translatedText = text;
                    log.info("{} has not been translated yet (engWithColor)", text);
                    plugin.getFailedTranslations().add(sqlQuery);
                    return textAddColor(text, sqlQuery.getColor());
                } else { // text has been translated
                    translatedText = convertFullWidthToHalfWidth(result[0]); // convert full width characters to half width
                    translatedText = Colors.getOriginalColorWord(translatedText, colorTagsAsIs); // replace placeholders with original color tags
//                    for(int i = 0; i < colorTagsAsIs.size(); i++){
//                        translatedText = translatedText.replace("<colNum" + i + ">", colorTagsAsIs.get(i)); // replace placeholders with original color tags
//                    }
                }
            }
            //translatedText = this.plugin.getTranscriptActions().getTranslation(text);
        } else if(option == TransformOption.TRANSLATE_API){ // wont have any colors
            translatedText = this.plugin.getDeepl().translate(Colors.removeAllTags(text),
                    LangCodeSelectableList.ENGLISH ,this.plugin.getConfig().getSelectedLanguage());
        } else if(option == TransformOption.TRANSLITERATE){
            //return
        }

        if(needCharImage) {
            // needs char image and has multiple colors
            String[] words = Colors.getWordArray(translatedText);
            Colors[] colorsArray = Colors.getColorArray(translatedText, sqlQuery.getColor());
            StringBuilder charImage = new StringBuilder();
            //log.info("words length = " + words.length + ", colorsArray length =" + colorsArray.length);
            for(int i = 0; i < words.length; i++){
                //log.info("words[" + i + "] = " + words[i] + ", colorsArray[" + i + "] = " + colorsArray[i]);
                charImage.append(generalFunctions.StringToTags(words[i], colorsArray[i]));
            }
            return charImage.toString();
        } else {// doesnt need char image and already has color tags
            return translatedText;
        }
    }

    /*
        * transform text with placeholders to translated text
        * example: transformWithPlaceholders("Sand <col=ff>Crab</col> (level-15)", "Sand <colNum0>Crab</col> (level-<Num0>)", TransformOption.TRANSLATE_LOCAL, sqlQuery)
        * matching translation will be "サンド <colNum0>クラブ</col> (レベル<Num0>)"
        * return value will be "サンド <col=ff>クラブ</col> (レベル15)"
     */
    public String transformWithPlaceholders(String originalText, String textWithPlaceholders,TransformOption option , SqlQuery sqlQuery){
        if(textWithPlaceholders == null || textWithPlaceholders.isEmpty()){
            plugin.getFailedTranslations().add(sqlQuery);
            return textWithPlaceholders;
        }

        String translatedText = "";

        if(option == TransformOption.AS_IS){
            return textWithPlaceholders;
        } else if(option == TransformOption.TRANSLATE_LOCAL){
            sqlQuery.setEnglish(textWithPlaceholders);
            // if translating failed for this query before, return the original text with color
            if (plugin.getFailedTranslations().contains(sqlQuery)) {
                return textWithPlaceholders;
            }

            String[] result = sqlQuery.getMatching(SqlVariables.columnTranslation, false);
            if(result.length == 0){
                log.info("(withPlaceholders func) the following placeholder text doesn't exist in the English column :{}", textWithPlaceholders);
                log.info("   query = {}", sqlQuery.getSearchQuery());
                // translatedText = text;
                plugin.getFailedTranslations().add(sqlQuery);
                return null;
            } else {
                if(result[0].isEmpty()) { // text exists in database but hasn't been translated yet
                    //translatedText = text;
                    log.info("{} has not been translated yet (withPlaceholders func)", textWithPlaceholders);
                    plugin.getFailedTranslations().add(sqlQuery);
                    return null;

                } else { // text has been translated
                    translatedText = convertFullWidthToHalfWidth(result[0]); // convert full width characters to half width
                }
            }
            //translatedText = this.plugin.getTranscriptActions().getTranslation(text);
        } else if(option == TransformOption.TRANSLATE_API){
            translatedText = this.plugin.getDeepl().translate(textWithPlaceholders,
                    LangCodeSelectableList.ENGLISH ,this.plugin.getConfig().getSelectedLanguage());
        } else if(option == TransformOption.TRANSLITERATE){
            //return
        }
        List<String> colorTags = Colors.getColorTagsAsIs(originalText);
        translatedText = Colors.getOriginalColorWord(translatedText, colorTags); // replace placeholders of color tags with original color tags
        translatedText = SqlQuery.replacePlaceholdersWithNumbers(originalText, translatedText); // replace placeholders of numbers with original numbers
        translatedText = translatedText.replace("<br>", "<asis><br></asis>"); // keep <br> tags
        // convert to displayed string
        boolean needCharImage = plugin.getConfig().getSelectedLanguage().needsCharImages();
        GeneralFunctions generalFunctions = plugin.getGeneralFunctions();
        if(needCharImage) {
            // needs char image and could have multiple colors
            String[] words = Colors.getWordArray(translatedText);
            Colors[] colorsArray = Colors.getColorArray(translatedText, sqlQuery.getColor());
            StringBuilder charImage = new StringBuilder();
            //log.info("words length = " + words.length + ", colorsArray length =" + colorsArray.length);
            for(int i = 0; i < words.length; i++){
                //log.info("words[" + i + "] = " + words[i] + ", colorsArray[" + i + "] = " + colorsArray[i]);
                charImage.append(generalFunctions.StringToTags(words[i], colorsArray[i]));
            }
            return charImage.toString();
        } else {// doesnt need char image and already has color tags
            return translatedText;
        }
    }

    public String transform(String text, Colors colors, TransformOption option, SqlQuery sqlQuery, boolean searchAlike){
        if(text == null || text.isEmpty()){
            plugin.getFailedTranslations().add(sqlQuery);
            return textAddColor(text, colors);
        }

        String translatedText = "";

        if(option == TransformOption.AS_IS){
            return textAddColor(text, colors);
        } else if(option == TransformOption.TRANSLATE_LOCAL){
            sqlQuery.setEnglish(text);
            // if translating failed for this query before, return the original text with color
            if (plugin.getFailedTranslations().contains(sqlQuery)) {
                return textAddColor(text, colors);
            }

            String[] result = sqlQuery.getMatching(SqlVariables.columnTranslation, searchAlike);
            if(result.length == 0){
                log.info("(transform func) the following text doesn't exist in the English column :{}", text);
                log.info("   query = {}", sqlQuery.getSearchQuery());
                // translatedText = text;
                plugin.getFailedTranslations().add(sqlQuery);
                return textAddColor(text, colors);
            } else {
                if(result[0].isEmpty()) { // text exists in database but hasn't been translated yet
                    //translatedText = text;
                    log.info("{} has not been translated yet (transform func)", text);
                    plugin.getFailedTranslations().add(sqlQuery);
                    return textAddColor(text, colors);

                } else { // text has been translated
                    translatedText = convertFullWidthToHalfWidth(result[0]); // convert full width characters to half width
                }
            }
            //translatedText = this.plugin.getTranscriptActions().getTranslation(text);
        } else if(option == TransformOption.TRANSLATE_API){
            translatedText = this.plugin.getDeepl().translate(text,
                    LangCodeSelectableList.ENGLISH ,this.plugin.getConfig().getSelectedLanguage());
        } else if(option == TransformOption.TRANSLITERATE){
            //return
        }
        return stringToDisplayedString(translatedText, colors);
    }

    /*
     * general idea:
     * 1. split into string array [name, level] and color array [color of name, color of level]
     * 2. translate name and level
     *    - if target language needs char image, after translating the string, use color and string to get char image
     *    - else translate the string, then combine each string with its color
     * 3. recombine into string
     */
    public String transform(String[] texts, Colors[] colors, TransformOption option, SqlQuery sqlQuery, boolean searchAlike){
        if(Colors.countColorTagsAfterReformat(sqlQuery.getEnglish()) > 1 && option != TransformOption.TRANSLATE_API){
            return transformEngWithColor(option, sqlQuery, searchAlike);
        }
        StringBuilder transformedTexts = new StringBuilder();
        for(int i = 0; i < texts.length; i++){
            transformedTexts.append(transform(texts[i], colors[i], option, sqlQuery, searchAlike));
        }
        return transformedTexts.toString();
    }

    public String transform(String[] texts, Colors[] colors, TransformOption option, SqlQuery[] sqlQueries, boolean searchAlike){
        StringBuilder transformedTexts = new StringBuilder();
        for(int i = 0; i < texts.length; i++){
            if(Colors.countColorTagsAfterReformat(sqlQueries[i].getEnglish()) > 1 && option != TransformOption.TRANSLATE_API){
                transformedTexts.append(transformEngWithColor(option, sqlQueries[i], searchAlike));
            } else {
                transformedTexts.append(transform(texts[i], colors[i], option, sqlQueries[i], searchAlike));
            }
        }
        return transformedTexts.toString();
    }

//    public String transform(String[] texts, Colors[] colors, TransformOption[] options, SqlQuery sqlQuery){
//        if(Colors.countColorTagsAfterReformat(sqlQuery.getEnglish()) > 1){
//            log.info("may not work as expected");
//        }
//        StringBuilder transformedTexts = new StringBuilder();
//        for(int i = 0; i < texts.length; i++){
//            transformedTexts.append(transform(texts[i], colors[i], options[i], sqlQuery));
//        }
//        return transformedTexts.toString();
//    }

    public String transform(String[] texts, Colors[] colors, TransformOption[] options, SqlQuery[] sqlQueries, boolean searchAlike){
        StringBuilder transformedTexts = new StringBuilder();
        for(int i = 0; i < texts.length; i++){
            if(Colors.countColorTagsAfterReformat(sqlQueries[i].getEnglish()) > 1 && options[i] != TransformOption.TRANSLATE_API){
                transformedTexts.append(transformEngWithColor(options[i], sqlQueries[i], searchAlike));
            } else {
                transformedTexts.append(transform(texts[i], colors[i], options[i], sqlQueries[i], searchAlike));
            }
        }
        return transformedTexts.toString();
    }

    public String transform(String stringWithColors, TransformOption option, SqlQuery sqlQuery, Colors defaultColor, boolean searchAlike){
        String[] targetWordArray = Colors.getWordArray(stringWithColors); // eg. ["Sand Crab", " (level-15)"]
        Colors[] targetColorArray = Colors.getColorArray(stringWithColors, defaultColor); // eg. [Colors.white, Colors.red]

        return transform(targetWordArray, targetColorArray, option, sqlQuery, searchAlike);
    }

    public String transform(String stringWithColors, TransformOption option, SqlQuery[] sqlQueries, Colors defaultColor, boolean searchAlike){
        String[] targetWordArray = Colors.getWordArray(stringWithColors); // eg. ["Sand Crab", " (level-15)"]
        Colors[] targetColorArray = Colors.getColorArray(stringWithColors, defaultColor); // eg. [Colors.white, Colors.red]

        return transform(targetWordArray, targetColorArray, option, sqlQueries, searchAlike);
    }

    public String convertFullWidthToHalfWidth(String fullWidthStr) {
        String[][] fullWidthToHalfWidth = {
                {"０", "0"}, {"１", "1"}, {"２", "2"}, {"３", "3"}, {"４", "4"}, {"５", "5"}, {"６", "6"}, {"７", "7"}, {"８", "8"}, {"９", "9"},
                {"Ａ", "A"}, {"Ｂ", "B"}, {"Ｃ", "C"}, {"Ｄ", "D"}, {"Ｅ", "E"}, {"Ｆ", "F"}, {"Ｇ", "G"}, {"Ｈ", "H"}, {"Ｉ", "I"}, {"Ｊ", "J"},
                {"Ｋ", "K"}, {"Ｌ", "L"}, {"Ｍ", "M"}, {"Ｎ", "N"}, {"Ｏ", "O"}, {"Ｐ", "P"}, {"Ｑ", "Q"}, {"Ｒ", "R"}, {"Ｓ", "S"}, {"Ｔ", "T"},
                {"Ｕ", "U"}, {"Ｖ", "V"}, {"Ｗ", "W"}, {"Ｘ", "X"}, {"Ｙ", "Y"}, {"Ｚ", "Z"},
                {"ａ", "a"}, {"ｂ", "b"}, {"ｃ", "c"}, {"ｄ", "d"}, {"ｅ", "e"}, {"ｆ", "f"}, {"ｇ", "g"}, {"ｈ", "h"}, {"ｉ", "i"}, {"ｊ", "j"},
                {"ｋ", "k"}, {"ｌ", "l"}, {"ｍ", "m"}, {"ｎ", "n"}, {"ｏ", "o"}, {"ｐ", "p"}, {"ｑ", "q"}, {"ｒ", "r"}, {"ｓ", "s"}, {"ｔ", "t"},
                {"ｕ", "u"}, {"ｖ", "v"}, {"ｗ", "w"}, {"ｘ", "x"}, {"ｙ", "y"}, {"ｚ", "z"},
                {"！", "!"}, {"”", "\""}, {"＃", "#"}, {"＄", "$"}, {"％", "%"}, {"＆", "&"}, {"＇", "'"}, {"（", "("}, {"）", ")"}, {"＊", "*"},
                {"＋", "+"}, {"，", ","}, {"－", "-"}, {"．", "."}, {"／", "/"}, {"：", ":"}, {"；", ";"}, {"＜", "<"}, {"＝", "="}, {"＞", ">"},
                {"？", "?"}, {"＠", "@"}, {"［", "["}, {"＼", "\\"}, {"］", "]"}, {"＾", "^"}, {"＿", "_"}, {"｀", "`"}, {"｛", "{"}, {"｜", "|"},
                {"｝", "}"}, {"　", " "}//, {"～", "~"}
        };
        for (String[] pair : fullWidthToHalfWidth) {
            fullWidthStr = fullWidthStr.replace(pair[0], pair[1]);
        }
        return fullWidthStr;
    }

    public String stringToDisplayedString(String translatedText, Colors colors){
        boolean needCharImage = plugin.getConfig().getSelectedLanguage().needsCharImages();
        GeneralFunctions generalFunctions = plugin.getGeneralFunctions();

        if(needCharImage) {// needs char image but just 1 color
            return generalFunctions.StringToTags(Colors.removeColorTag(translatedText), colors);
        } else { // doesnt need char image and just 1 color
            return "<col=" + colors.getHex() + ">" + translatedText + "</col>";
        }
    }

    private String textAddColor(String text, Colors color){
        if (Colors.getColorTagsAsIs(text).isEmpty()) {
            return Colors.surroundWithColorTag(text, color);
        }
        return text;
    }

}
