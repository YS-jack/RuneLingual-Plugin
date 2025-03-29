package com.RuneLingual.Widgets;

import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.SQL.SqlQuery;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Transformer;
import com.RuneLingual.nonLatin.GeneralFunctions;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import java.util.*;


@Getter
@Setter
public class PartialTranslationManager {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private Transformer transformer;

    private List<PartialTranslation> partialTranslations = new ArrayList<>();

    @Inject
    public PartialTranslationManager(RuneLingualPlugin plugin) {
        this.plugin = plugin;
    }

    public enum PlaceholderType {
        PLAYER_NAME,
        ITEM_NAME,
        NPC_NAME,
        OBJECT_NAME,
        ANY_TRANSLATED // will first look for any matches in the local translation, but if it doesnt exist and the api config for interface is on, will try to translate with api
    } // after adding, add to getQuery4PlaceholderType

    @Getter
    public static class PartialTranslation {
        @Inject
        private RuneLingualPlugin plugin;
        @Inject
        private Transformer transformer;
        @Setter
        private int id;
        private final List<String> fixedTextParts; // fixed text parts of the text
        private final List<String> placeholders = new ArrayList<>(); // name and option for each placeholder

        public PartialTranslation(RuneLingualPlugin plugin, Transformer transformer, List<String> fixedTextParts,
                                  List<PlaceholderType> placeholders){
            /*
             fixedTextParts = ["slay ", " in "]
             placeholders = [(NPC_NAME, LOCAL_TRANSLATION), (LOCATION, AS_IS)]

             this.placeholders = [(NPC_NAME0, LOCAL_TRANSLATION), (LOCATION0, AS_IS)] (placeholder type with individual indexes)
            */
            this.plugin = plugin;
            this.transformer = transformer;
            this.fixedTextParts = fixedTextParts;

            int[] typeCounter = new int[PlaceholderType.values().length]; // count the number of each placeholder type, to be placed in the placeholder name (eg: <!PLAYER_NAME0>)
            Arrays.fill(typeCounter, 0);

            for (PlaceholderType placeholder : placeholders) {
                String type = placeholder.name();
                int counter = typeCounter[placeholder.ordinal()];
                String placeholderName = type + counter;

                this.placeholders.add(placeholderName);

                typeCounter[placeholder.ordinal()]++;
            }

        }

        public String getEnColVal(){
            /*
            fixedTextParts = ["slay ", " in "]
            placeholders = [(NPC_NAME, AS_IS), (LOCATION_NAME, TRANSLATE_LOCAL)]

            returns: "slay <!NPC_NAME0> in <!LOCATION_NAME1>"
             */
            StringBuilder enColVal = new StringBuilder();

            for (int i = 0; i < fixedTextParts.size(); i++) {
                enColVal.append(fixedTextParts.get(i));
                if (i < placeholders.size()) {
                    enColVal.append("<!");
                    enColVal.append(placeholders.get(i));
                    enColVal.append(">");
                }
            }
            return enColVal.toString();
        }

        private Transformer.TransformOption getTransformOption(int i){
            PlaceholderType placeholderType = PlaceholderType.valueOf(getPlaceholderName(i).replaceAll("[0-9]", ""));
            RuneLingualConfig.ingameTranslationConfig config;
            switch (placeholderType) {
                case PLAYER_NAME:
                    return Transformer.TransformOption.AS_IS;
                case ITEM_NAME:
                    config = plugin.getConfig().getItemNamesConfig();
                    if (config.equals(RuneLingualConfig.ingameTranslationConfig.USE_LOCAL_DATA)) {
                        return Transformer.TransformOption.TRANSLATE_LOCAL;
                    } else if (config.equals(RuneLingualConfig.ingameTranslationConfig.USE_API)) {
                        return Transformer.TransformOption.TRANSLATE_API;
                    } else {
                        return Transformer.TransformOption.AS_IS;
                    }
                case NPC_NAME:
                    config = plugin.getConfig().getNPCNamesConfig();
                    if (config.equals(RuneLingualConfig.ingameTranslationConfig.USE_LOCAL_DATA)) {
                        return Transformer.TransformOption.TRANSLATE_LOCAL;
                    } else if (config.equals(RuneLingualConfig.ingameTranslationConfig.USE_API)) {
                        return Transformer.TransformOption.TRANSLATE_API;
                    } else {
                        return Transformer.TransformOption.AS_IS;
                    }
                case OBJECT_NAME:
                    config = plugin.getConfig().getObjectNamesConfig();
                    if (config.equals(RuneLingualConfig.ingameTranslationConfig.USE_LOCAL_DATA)) {
                        return Transformer.TransformOption.TRANSLATE_LOCAL;
                    } else if (config.equals(RuneLingualConfig.ingameTranslationConfig.USE_API)) {
                        return Transformer.TransformOption.TRANSLATE_API;
                    } else {
                        return Transformer.TransformOption.AS_IS;
                    }
                case ANY_TRANSLATED:
                    // after trying to translate but failed, if api config for interface is on, try to translate with api
                    return Transformer.TransformOption.TRANSLATE_LOCAL;
                default:
                    return Transformer.TransformOption.AS_IS;
            }
        }

        private String getPlaceholderName(int i){
            return placeholders.get(i);
        }

        public List<String> translateAllPlaceholders(List<String> originalTexts, Colors defaultColor, Colors[] placeholdedColors){
            List<String> translatedPlaceholders = new ArrayList<>();
            for (int i = 0; i < placeholders.size(); i++) {
                String originalText = originalTexts.get(i);
                Transformer.TransformOption option = getTransformOption(i);
                SqlQuery query = new SqlQuery(plugin);
                PlaceholderType placeholderType = PlaceholderType.valueOf(getPlaceholderName(i).replaceAll("[0-9]", ""));
                Colors placeholderColor = getPlaceholderColor(fixedTextParts.get(i), defaultColor, placeholdedColors);
                getQuery4PlaceholderType(originalText, placeholderType, defaultColor, query);
                String translatedText = transformer.transform(originalText, placeholderColor, option, query, false);
                if (translatedText.equals(Colors.surroundWithColorTag(originalText, placeholderColor))) { // if the translation failed
                    if(!option.equals(Transformer.TransformOption.AS_IS)) {
                        return null;
                    }
                    if(placeholderType.equals(PlaceholderType.ANY_TRANSLATED) // if the placeholder type is ANY_TRANSLATED and api config for interface is on, try to translate with api
                            && plugin.getConfig().getInterfaceTextConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)){
                        translatedText = transformer.transform(originalText, placeholderColor, Transformer.TransformOption.TRANSLATE_API, query, false);
                    }
                }
                translatedPlaceholders.add(translatedText);
            }
            return translatedPlaceholders;
        }
    }

    public static Colors getPlaceholderColor(String text, Colors defaultColor, Colors[] colors){
        /* if the text ends with a color tag, return that color
        * eg. in "slay <col=ff0000>blue dragons<col=ffffff> in Taverley" -> text = "slay <col=ff0000>", which ends with color tag, so return Colors.RED
        */
        if(text.matches(".*<col=[a-zA-Z0-9]*?>$")){
            return Colors.getColorFromHex(text.substring(text.lastIndexOf("<col=") + 5, text.lastIndexOf(">")));
        }

        /*
         * the text can end in color placeholder tags, eg. "<colNum0>blue dragons<colNum1> in Taverley"
         * in this case
         * eg. text = "slay <colNum0>", colors = ["ff0000", "ffffff"] -> colNum0 is the first color so return Colors.RED
         */
        for (int i = 0; i < colors.length; i++) {
            if(text.matches(".*<colNum" + i + ">$")){
                return colors[i];
            }
        }
        return defaultColor;
    }

    public void addPartialTranslation(List<String> fixedTextParts,
                                        List<PlaceholderType> placeholders){
            partialTranslations.add(new PartialTranslation(plugin, transformer, fixedTextParts, placeholders));
    }

    public void addPartialTranslation(int id, List<String> fixedTextParts,
                                      List<PlaceholderType> placeholders){
        PartialTranslation partialTranslation = new PartialTranslation(plugin, transformer, fixedTextParts, placeholders);
        partialTranslation.setId(id);
        partialTranslations.add(partialTranslation);
    }

    public static String protectPlaceholderTags(String text){
        // surround <!.*> tags with <asis> and </asis> to prevent them from being turned into char images
        return text.replaceAll("<!(.+?)>", "<asis><!$1></asis>");
    }

    public boolean hasId(int id){
        return partialTranslations.stream().anyMatch(partialTranslation -> partialTranslation.getId() == id);
    }
    public String getEnColVal(int id){
        return partialTranslations.stream()
                .filter(partialTranslation -> partialTranslation.getId() == id)
                .findFirst()
                .map(PartialTranslation::getEnColVal)
                .orElse(null);
    }
    public String translateWidget(Widget widget, String translationWithPlaceHolder, String originalText, Colors defaultColor) {
        String enColVal = getEnColVal(widget.getId());
        return translate(enColVal, translationWithPlaceHolder, originalText, defaultColor);
    }
    public String translateString(String textToTranslate, String translationWithPlaceHolder, String originalText, Colors defaultColor) {
        String enColVal = getMatchingEnColVal(textToTranslate);
        return translate(enColVal, translationWithPlaceHolder, originalText, defaultColor);
    }
    public String translate(String enColVal, String translationWithPlaceHolder, String originalText, Colors defaultColor) {
        // for widgets like "slay <!NPC_NAME0> in <!LOCATION_NAME1>", where only the part of the text should be translated
        // originalText = "slay blue dragons in Taverley",
        // translationWithPlaceHolder = "<col=0><!LOCATION_NAME0></col>にいる<col=0><!NPC_NAME0></col>を討伐せよ" (the translated character can be char images like <img=23432>)

        // process:
        // 1. translate text in the tags, "blue dragons" and "Taverley" in this case
        // 2. replace the tags in the translation with the translated text ("ターベリーにいる青い竜を討伐せよ" (Taverley = ターベリー, blue dragons = 青い竜))
        // 3. return the translation with the replaced text

         // enColVal = "slay <!NPC_NAME0> in <!LOCATION_NAME0>"
        if (enColVal == null || translationWithPlaceHolder == null) {
            return Colors.surroundWithColorTag(originalText, defaultColor);
        }

        // from the originalText and enColVal, get the content of each placeholders
        // eg. <!NPC_NAME0> = blue dragons, <!LOCATION_NAME0> = Taverley
        String originalWithoutColNum = originalText.replaceAll("<[^!]+?>", ""); // remove all tags that doesn't start with <! (eg. <col=ffffff>), which means they are not placeholders for partial translation
        String enColValWithoutColNum = enColVal.replaceAll("<[^!]+?>", "");
        Map<String, String> placeholder2Content = GeneralFunctions.getPlaceholder2Content(originalWithoutColNum, enColValWithoutColNum); // {"NPC_NAME0": "blue dragons", "LOCATION_NAME0": "Taverley"}

        // translationWithPlaceholder's color tag is specific (eg. <col=ffffff>), but the text given at initialization is not (eg. <colNum0>)
        // so, replace the color tag with color placeholder tag, then turn it back later
        List<String> colorList = Colors.getColorTagsAsIs(translationWithPlaceHolder); // get the color tags in the translation, eg. ["ffffff"]
        Colors[] colorArray = Colors.getColorArray(translationWithPlaceHolder, defaultColor);
        translationWithPlaceHolder = Colors.getColorPlaceholdedColWord(translationWithPlaceHolder); // eg. "<colNum0><!LOCATION_NAME0></colNum0>にいる<colNum1><!NPC_NAME0></colNum1>を討伐せよ"


        PartialTranslation partialTranslation = partialTranslations.stream() //partialTranslation = "slay <!NPC_NAME0> in <!LOCATION_NAME0>"
                .filter(partialTranslation1 -> partialTranslation1.getEnColVal().equals(enColVal))
                .findFirst()
                .orElse(null);

        // translate the content of each placeholders
        assert partialTranslation != null;
        List<String> phContent = new ArrayList<>(placeholder2Content.values()); // phContent = ["blue dragons", "Taverley"]

        List<String> translatedPlaceholders = partialTranslation.translateAllPlaceholders(phContent, defaultColor, colorArray); // translatedPlaceholders = ["青い竜", "ターベリー"]

        if (translatedPlaceholders == null) {
            // return the original text if the translation failed
            return Colors.surroundWithColorTag(originalText, defaultColor);
        }
        for (int i = 0; i < translatedPlaceholders.size(); i++) {
            String translatedPlaceholder = translatedPlaceholders.get(i); // translatedPlaceholder = "青い竜"
            String placeholderName = partialTranslation.getPlaceholderName(i); // placeholderName = "NPC_NAME0"
            translationWithPlaceHolder = translationWithPlaceHolder.replaceAll("<!" + placeholderName + ">", translatedPlaceholder);
        }
        translationWithPlaceHolder = Colors.getOriginalColorWord(translationWithPlaceHolder, colorList); // turn the color placeholder tags back to color tags
        return translationWithPlaceHolder;

    }

    protected static void getQuery4PlaceholderType(String text, PlaceholderType type, Colors defaultColor, SqlQuery query){
        if (type == PlaceholderType.PLAYER_NAME) {
            query.setGenMenuAcitons(text, defaultColor);
        } else if (type == PlaceholderType.ITEM_NAME) {
            query.setItemName(text, defaultColor);
        } else if (type == PlaceholderType.NPC_NAME) {
            query.setNpcName(text, defaultColor);
        } else if (type == PlaceholderType.OBJECT_NAME) {
            query.setObjectName(text, defaultColor);
        } else if (type == PlaceholderType.ANY_TRANSLATED) {
            query.setEnglish(text);
            query.setColor(defaultColor);
        }
    }

    /*
    * Check if the text matches the enColVal of the partial translation.
    * Searches by id given at initialization.
    * eg. "slay blue dragons in Taverley" matches "slay <!NPC_NAME0> in <!LOCATION_NAME0>"
     */
    public boolean stringMatchesEnColVal(String text, int id) {
        if(!hasId(id)){
            return false;
        }
        String template = getEnColVal(id);
        // Escape special regex characters in the template except for the placeholder
        String regex = template.replaceAll("([\\\\.*+\\[\\](){}|^$])", "\\\\$1")
                .replaceAll("<!.+?>", ".*");
        return text.matches(regex);
    }

    /*
     * Check if the text matches the enColVal of the partial translation
     * Checks only text pattern
     * eg. "slay blue dragons in Taverley" matches "slay <!NPC_NAME0> in <!LOCATION_NAME0>"
     */
    public boolean stringMatchesEnColVal(String text) {
        for(PartialTranslation partialTranslation : partialTranslations){
            // Escape special regex characters in the template except for the placeholder
            String regex = partialTranslation.getEnColVal().replaceAll("([\\\\.*+\\[\\](){}|^$])", "\\\\$1")
                    .replaceAll("<!.+?>", ".*");
            if(text.matches(regex)){
                return true;
            }
        }
        return false;
    }

    public String getMatchingEnColVal(String text) {
        for(PartialTranslation partialTranslation : partialTranslations){
            // Escape special regex characters in the template except for the placeholder
            String regex = partialTranslation.getEnColVal().replaceAll("([\\\\.*+\\[\\](){}|^$])", "\\\\$1")
                    .replaceAll("<!.+?>", ".*");
            if(text.matches(regex)){
                return partialTranslation.getEnColVal();
            }
        }
        return null;
    }
}
