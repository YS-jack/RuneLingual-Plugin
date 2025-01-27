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
                default:
                    return Transformer.TransformOption.AS_IS;
            }
        }

        private String getPlaceholderName(int i){
            return placeholders.get(i);
        }

        public List<String> translateAllPlaceholders(List<String> originalTexts, Colors defaultColor){
            List<String> translatedPlaceholders = new ArrayList<>();
            for (int i = 0; i < placeholders.size(); i++) {
                String originalText = originalTexts.get(i);
                Transformer.TransformOption option = getTransformOption(i);
                SqlQuery query = new SqlQuery(plugin);
                PlaceholderType placeholderType = PlaceholderType.valueOf(getPlaceholderName(i).replaceAll("[0-9]", ""));
                getQuery4PlaceholderType(originalText, placeholderType, defaultColor, query);
                String translatedText = transformer.transform(originalText, defaultColor, option, query, false);
                if (translatedText.equals(Colors.surroundWithColorTag(originalText, defaultColor))
                && !option.equals(Transformer.TransformOption.AS_IS)) {
                    return null;
                }
                translatedPlaceholders.add(translatedText);
            }
            return translatedPlaceholders;
        }
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

    public String translate(Widget widget, String translationWithPlaceHolder, String originalText, Colors defaultColor) {
        // for widgets like "slay <!NPC_NAME0> in <!LOCATION_NAME1>", where only the part of the text should be translated
        // originalText = "slay blue dragons in Taverley",
        // translationWithPlaceHolder = "<col=0><!LOCATION_NAME0></col>にいる<col=0><!NPC_NAME0></col>を討伐せよ" (the translated character can be char images like <img=23432>)

        // process:
        // 1. translate text in the tags, "blue dragons" and "Taverley" in this case
        // 2. replace the tags in the translation with the translated text ("ターベリーにいる青い竜を討伐せよ" (Taverley = ターベリー, blue dragons = 青い竜))
        // 3. return the translation with the replaced text

        String enColVal = getEnColVal(widget.getId()); // enColVal = "slay <!NPC_NAME0> in <!LOCATION_NAME0>"
        if (enColVal == null) {
            return Colors.surroundWithColorTag(originalText, defaultColor);
        }

        // from the originalText and enColVal, get the content of each placeholders
        // eg. <!NPC_NAME0> = blue dragons, <!LOCATION_NAME0> = Taverley
        Map<String, String> placeholder2Content = GeneralFunctions.getPlaceholder2Content(originalText, enColVal); // {"NPC_NAME0": "blue dragons", "LOCATION_NAME0": "Taverley"}

        PartialTranslation partialTranslation = partialTranslations.stream()
                .filter(partialTranslation1 -> partialTranslation1.getId() == widget.getId())
                .findFirst()
                .orElse(null);

        // translate the content of each placeholders
        assert partialTranslation != null;
        List<String> phContent = new ArrayList<>(placeholder2Content.values());
        List<String> translatedPlaceholders = partialTranslation.translateAllPlaceholders(phContent, defaultColor);

        if (translatedPlaceholders == null) {
            // return the original text if the translation failed
            return Colors.surroundWithColorTag(originalText, defaultColor);
        }
        for (int i = 0; i < translatedPlaceholders.size(); i++) {
            String translatedPlaceholder = translatedPlaceholders.get(i);
            String placeholderName = partialTranslation.getPlaceholderName(i);
            translationWithPlaceHolder = translationWithPlaceHolder.replaceAll("<!" + placeholderName + ">", translatedPlaceholder);
        }
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
        }
    }

}
