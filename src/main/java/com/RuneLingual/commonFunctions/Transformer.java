package com.RuneLingual.commonFunctions;

import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.nonLatinChar.GeneralFunctions;
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
    }

    @Inject
    public Transformer(RuneLingualPlugin plugin){
        this.plugin = plugin;
    }

    /*
     * general idea:
     * 1. split into string array [name, level] and color array [color of name, color of level]
     * 2. translate name and level
     *    - if target language needs char image, after translating the string, use color and string to get char image
     *    - else translate the string, then combine each string with its color
     * 3. recombine into string
     */

    public String transform(String text, Colors colors, TransformOption option, SqlQuery sqlQuery){
        boolean needCharImage = plugin.getConfig().getSelectedLanguage().needCharImages();
        GeneralFunctions generalFunctions = plugin.getGeneralFunctions();

        if(text == null || text.isEmpty()){
            return text;
        }

        String translatedText = "";

        if(option == TransformOption.AS_IS){
            translatedText = text;
        } else if(option == TransformOption.TRANSLATE_LOCAL){
            sqlQuery.setEnglish(text);
            String[] result = sqlQuery.getMatching(SqlVariables.columnTranslation);
            if(result.length == 0){
//                log.info("No translation found for " + text);
//                log.info("query = " + sqlQuery.getSearchQuery());
                translatedText = text;
            } else {
                if(result[0].isEmpty()) { // text exists in database but hasn't been translated yet
                    translatedText = text;
                    //log.info("{} has not been translated yet", text);
                } else {
                    translatedText = convertFullWidthToHalfWidth(result[0]);
                }
            }
            //translatedText = this.plugin.getTranscriptActions().getTranslation(text);
        } else if(option == TransformOption.TRANSLATE_API){
            //return
        } else if(option == TransformOption.TRANSLITERATE){
            //return
        }

        if(needCharImage) {
            return generalFunctions.StringToTags(translatedText, colors);
        } else {
            return "<col=" + colors.getHex() + ">" + translatedText + "</col>";
        }
    }

    public String transform(String[] texts, Colors[] colors, TransformOption option, SqlQuery sqlQuery){
        StringBuilder transformedTexts = new StringBuilder();
        for(int i = 0; i < texts.length; i++){
            transformedTexts.append(transform(texts[i], colors[i], option, sqlQuery));
        }
        return transformedTexts.toString();
    }

    public String transform(String[] texts, Colors[] colors, TransformOption option, SqlQuery[] sqlQueries){
        StringBuilder transformedTexts = new StringBuilder();
        for(int i = 0; i < texts.length; i++){
            transformedTexts.append(transform(texts[i], colors[i], option, sqlQueries[i]));
        }
        return transformedTexts.toString();
    }

    public String transform(String[] texts, Colors[] colors, TransformOption[] options, SqlQuery sqlQuery){
        StringBuilder transformedTexts = new StringBuilder();
        for(int i = 0; i < texts.length; i++){
            transformedTexts.append(transform(texts[i], colors[i], options[i], sqlQuery));
        }
        return transformedTexts.toString();
    }

    public String transform(String[] texts, Colors[] colors, TransformOption[] options, SqlQuery[] sqlQueries){
        StringBuilder transformedTexts = new StringBuilder();
        for(int i = 0; i < texts.length; i++){
            transformedTexts.append(transform(texts[i], colors[i], options[i], sqlQueries[i]));
        }
        return transformedTexts.toString();
    }

    public String transform(String stringWithColors, TransformOption option, SqlQuery sqlQuery, Colors defaultColor){
        String[] targetWordArray = colorObj.getWordArray(stringWithColors); // eg. ["Sand Crab", " (level-15)"]
        Colors[] targetColorArray = colorObj.getColorArray(stringWithColors, defaultColor); // eg. [Colors.white, Colors.red]

        return transform(targetWordArray, targetColorArray, option, sqlQuery);
    }

    public String transform(String stringWithColors, TransformOption option, SqlQuery[] sqlQueries, Colors defaultColor){
        String[] targetWordArray = colorObj.getWordArray(stringWithColors); // eg. ["Sand Crab", " (level-15)"]
        Colors[] targetColorArray = colorObj.getColorArray(stringWithColors, defaultColor); // eg. [Colors.white, Colors.red]

        return transform(targetWordArray, targetColorArray, option, sqlQueries);
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
                {"｝", "}"}, {"～", "~"}, {"　", " "}
        };
        for (String[] pair : fullWidthToHalfWidth) {
            fullWidthStr = fullWidthStr.replace(pair[0], pair[1]);
        }
        return fullWidthStr;
    }

}
