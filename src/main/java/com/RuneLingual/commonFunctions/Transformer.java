package com.RuneLingual.commonFunctions;

import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.nonLatinChar.GeneralFunctions;
import com.RuneLingual.SQL.SqlVariables;
import com.RuneLingual.SQL.SqlQuery;

import javax.inject.Inject;
import java.util.List;


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
            return sqlQuery.getMatching(SqlVariables.columnTranslation)[0];
        } else if(option == TransformOption.TRANSLATE_API){
            //return
        } else if(option == TransformOption.TRANSLITERATE){
            //return
        }

        if(needCharImage) {
            return generalFunctions.StringToTags(text, colors);
        } else {
            return "<col=" + colors.getHex() + ">" + text + "</col>";
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

    public String transform(String stringWithColors, TransformOption option, SqlQuery sqlQuery){
        String[] targetWordArray = colorObj.getWordArray(stringWithColors); // eg. ["Sand Crab", " (level-15)"]
        Colors[] targetColorArray = colorObj.getColorArray(stringWithColors); // eg. [Colors.white, Colors.red]

        return transform(targetWordArray, targetColorArray, option, sqlQuery);
    }

    public String transform(String stringWithColors, TransformOption option, SqlQuery[] sqlQueries){
        String[] targetWordArray = colorObj.getWordArray(stringWithColors); // eg. ["Sand Crab", " (level-15)"]
        Colors[] targetColorArray = colorObj.getColorArray(stringWithColors); // eg. [Colors.white, Colors.red]

        return transform(targetWordArray, targetColorArray, option, sqlQueries);
    }


    //private String

}
