package com.RuneLingual.Wigets;

import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.SQL.SqlQuery;
import com.RuneLingual.SQL.SqlVariables;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Ids;
import com.RuneLingual.commonFunctions.Transformer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.*;

import javax.inject.Inject;
import java.util.*;

import static com.RuneLingual.Wigets.WidgetsUtilRLingual.removeBrAndTags;
import static com.RuneLingual.debug.OutputToFile.appendIfNotExistToFile;

@Slf4j
public class WidgetCapture {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    Client client;
    @Inject
    private Transformer transformer;
    @Getter
    Set<String> pastTranslationResults = new HashSet<>(); //TODO: add translated text to this list
    Set<SqlQuery> failedTranslations = new HashSet<>();

    @Inject
    private WidgetsUtilRLingual widgetsUtilRLingual;
    @Inject
    private DialogTranslator dialogTranslator;
    @Inject
    private Ids ids;


    @Inject
    public WidgetCapture(RuneLingualPlugin plugin) {
        this.plugin = plugin;
        ids = this.plugin.getIds();
    }

    public void translateWidget() {
        Widget[] roots = client.getWidgetRoots();
        SqlQuery sqlQuery = new SqlQuery(this.plugin);
        for (Widget root : roots) {
            translateWidgetRecursive(root, sqlQuery);
        }
    }

    private void translateWidgetRecursive(Widget widget,SqlQuery sqlQuery) {
        int widgetId = widget.getId();

        // stop the recursion if the widget is hidden or should be ignored
        List<Integer> widgetIdsToIgnore = Arrays.asList(ComponentID.CHATBOX_INPUT);
        if (widget.isHidden() || widgetIdsToIgnore.contains(widgetId)) {
            return;
        }

        // skip all chatbox widgets for now TODO: chatbox buttons should be translated
        int widgetGroup = WidgetUtil.componentToInterface(widgetId);
        if (widgetGroup == InterfaceID.CHATBOX) {
            return;
        }

        modifySqlQuery4Widget(widget, sqlQuery);

        // recursive call
        for (Widget dynamicChild : widget.getDynamicChildren()) {
            translateWidgetRecursive(dynamicChild, sqlQuery);
        }
        for (Widget nestedChild : widget.getNestedChildren()) {
            translateWidgetRecursive(nestedChild, sqlQuery);
        }
        for (Widget staticChild : widget.getStaticChildren()) {
            translateWidgetRecursive(staticChild, sqlQuery);
        }

        // debug: if the widget is the target for dumping
        ifIsDumpTarget_thenDump(widget, sqlQuery);

        // translate the widget text////////////////
        // dialogues are handled separately
        if (widgetGroup == InterfaceID.DIALOG_NPC
                || widgetGroup == InterfaceID.DIALOG_PLAYER
                || widgetGroup == InterfaceID.DIALOG_OPTION) {
            dialogTranslator.handleDialogs(widget);
            return;
        }

//        if (widgetId == 14287050 && shouldTranslateWidget(widget)) {
//            log.info("original height: " + widget.getOriginalHeight());
//            log.info("height mode: " + widget.getHeightMode());
//            log.info("position mode: " + widget.getYPositionMode());
//            widget.getXTextAlignment();
//            log.info("text: " + widget.getText() + "\n\n");
//            widget.setHeightMode(WidgetSizeMode.ABSOLUTE)
//                    .setOriginalHeight(widget.getOriginalHeight() + 25)
//                    .revalidate();
//            return;
//        }

        if(shouldTranslateWidget(widget)) {
            SqlQuery queryToPass = sqlQuery.copy();
            // replace sqlQuery if they are defined as item, npc, object, quest names
            Colors textColor = Colors.getColorFromHex(Colors.IntToHex(widget.getTextColor()));
            if (ids.getWidgetIdItemName().contains(widgetId)) {
                String itemName = Colors.removeColorTag(widget.getText());
                queryToPass.setItemName(itemName, textColor);
            }
            if (ids.getWidgetIdNpcName().contains(widgetId)) {
                String npcName = Colors.removeColorTag(widget.getText());
                queryToPass.setNpcName(npcName, textColor);
            }
            if (ids.getWidgetIdObjectName().contains(widgetId)) {
                String objectName = Colors.removeColorTag(widget.getText());
                queryToPass.setObjectName(objectName, textColor);
            }
            if (ids.getWidgetIdQuestName().contains(widgetId)) {
                String questName = Colors.removeColorTag(widget.getText());
                queryToPass.setQuestName(questName, textColor);
            }

            // translate the widget text
            translateWidgetText(widget, queryToPass);

            return;
        }

    }

    private void modifySqlQuery4Widget(Widget widget, SqlQuery sqlQuery) {
        sqlQuery.setColor(Colors.getColorFromHex(Colors.IntToHex(widget.getTextColor())));
        int widgetId = widget.getId();
        if (widgetId == ids.getWidgetIdSkillGuide()) { //Id for parent of skill guide, or parent of element in list
            sqlQuery.setCategory(SqlVariables.categoryValue4Interface.getValue());
            sqlQuery.setSubCategory(SqlVariables.subcategoryValue4GeneralUI.getValue());
            sqlQuery.setSource(SqlVariables.sourceValue4SkillGuideInterface.getValue());
        }
        // if one of the main tabs, set the category and subcategory. main tabs = combat options, skills tab, etc.
        if (widgetId == ids.getWidgetIdMainTabs()) {
            sqlQuery.setCategory(SqlVariables.categoryValue4Interface.getValue());
            sqlQuery.setSubCategory(SqlVariables.subcategoryValue4MainTabs.getValue());
        }
        // if one of the main tabs, set the source as the tab name
        if (sqlQuery.getCategory() != null && sqlQuery.getCategory().equals(SqlVariables.categoryValue4Interface.getValue())
                && sqlQuery.getSubCategory() != null && sqlQuery.getSubCategory().equals(SqlVariables.subcategoryValue4MainTabs.getValue())) {
            if (widgetId == ids.getWidgetIdAttackStyleTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4CombatOptionsTab.getValue());
            } else if (widgetId == ids.getWidgetIdSkillsTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4SkillsTab.getValue());
            } else if (widgetId == ids.getWidgetIdCharacterSummaryTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4CharacterSummaryTab.getValue());
            } else if (widgetId == ids.getWidgetIdQuestTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4QuestListTab.getValue());
            } else if (widgetId == ids.getWidgetIdAchievementDiaryTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4AchievementDiaryTab.getValue());
//            } else if (widgetId == ids.getWidgetIdInventoryTab()) {
//                sqlQuery.setSource(SqlVariables.sourceValue4InventTab.getValue());
//            } else if (widgetId == ids.getWidgetIdEquipmentTab()) {
//                sqlQuery.setSource(SqlVariables.sourceValue4WornEquipmentTab.getValue());
            } else if (widgetId == ids.getWidgetIdPrayerTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4PrayerTab.getValue());
            } else if (widgetId == ids.getWidgetIdSpellBookTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4SpellBookTab.getValue());
            } else if (widgetId == ids.getWidgetIdGroupsTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4GroupTab.getValue());
            } else if (widgetId == ids.getWidgetIdFriendsTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4FriendsTab.getValue());
            } else if (widgetId == ids.getWidgetIdIgnoreTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4IgnoreTab.getValue());
            } else if (widgetId == ids.getWidgetIdAccountManagementTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4AccountManagementTab.getValue());
            } else if (widgetId == ids.getWidgetIdSettingsTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4SettingsTab.getValue());
            } else if (widgetId == ids.getWidgetIdEmotesTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4EmotesTab.getValue());
            } else if (widgetId == ids.getWidgetIdMusicTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4MusicTab.getValue());
            } else if (widgetId == ids.getWidgetIdLogoutTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4LogoutTab.getValue());
            } else if (widgetId == ids.getWidgetIdWorldSwitcherTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4WorldSwitcherTab.getValue());
            }
        }
    }

    private void translateWidgetText(Widget widget, SqlQuery sqlQuery) {
        int widgetId = widget.getId();
        String originalText = widget.getText();
        String textToTranslate = getEnglishColValFromWidget(widget);
        String translatedText = null;
        // for most cases
        if (!ids.getWidgetId2SplitTextAtBr().contains(widgetId)
            && !ids.getWidgetId2KeepBr().contains(widgetId)) {
            translatedText = getTranslationFromQuery(sqlQuery, widget.getText(), textToTranslate);

        } else if (widgetsUtilRLingual.shouldPartiallyTranslate(widget)) {
            // for widgets like "Name: <playerName>" (found in accounts management tab), where only the part of the text should be translated
            // order:
            // textToTranslate = "Name: <playerName>" -> translatedText = "名前: <playerName>" -> translatedText = "名前: Durial321"
            //todo: complete this
            translatedText = getTranslationFromQuery(sqlQuery, widget.getText(), textToTranslate);
        }

        else if (ids.getWidgetId2SplitTextAtBr().contains(widgetId)){// for widgets that have <br> in the text and should be kept where they are, translate each line separately
            String[] textList = textToTranslate.split("<br>");
            String[] originalTextList = widget.getText().split("<br>");
            StringBuilder translatedTextBuilder = new StringBuilder();

            for (int i = 0; i < textList.length; i++) {
                String text = textList[i];
                String originalTextLine = originalTextList[i];
                String translatedTextPart = getTranslationFromQuery(sqlQuery, originalTextLine, text);
                if (translatedTextPart == null) { // if translation failed
                    failedTranslations.add(sqlQuery);
                    return;
                }
                translatedTextBuilder.append(translatedTextPart);
                if (i != textList.length - 1) { // if it's not the last line, add <br>
                    translatedTextBuilder.append("<br>");
                }
            }

            translatedText = translatedTextBuilder.toString();
        } else { // if(ids.getWidgetId2KeepBr().contains(widgetId))
            // for widgets that have <br> in the text and should be kept where they are, translate the whole text
            translatedText = getTranslationFromQuery(sqlQuery, widget.getText(), textToTranslate);
        }

        // translation was not available

        if(translatedText == null){ // if the translation is the same as the original without <br>
            failedTranslations.add(sqlQuery);
            return;
        }
        String originalWithoutBr = removeBrAndTags(widget.getText());
        String translationWithoutBr = removeBrAndTags(translatedText);
        if(Objects.equals(translatedText, textToTranslate) // if the translation is the same as the original
                || originalWithoutBr.equals(translationWithoutBr)){ // if the translation is the same as the original without <br>
            failedTranslations.add(sqlQuery);
            return;
        }

        pastTranslationResults.add(translatedText);

        if (ids.getWidgetId2SplitTextAtBr().contains(widgetId)
                || ids.getWidgetId2KeepBr().contains(widgetId)
                || ids.getWidgetId2FixedSize().containsKey(widgetId)) {
            widgetsUtilRLingual.setWidgetText_BrAsIs(widget, translatedText);
        } else {
            widgetsUtilRLingual.setWidgetText_NiceBr(widget, translatedText);
        }
        widgetsUtilRLingual.changeLineSize_ifNeeded(widget);
        widgetsUtilRLingual.changeWidgetSize_ifNeeded(widget);


        //below is for debugging
//            int widgetId = widgetId;
//            if(widgetId == 25034758){
//                int widgetId2;
//                for(int j = 0; j < 100; j++){
//                    widget = widget.getParent();
//                    if (widget == null) {
//                        break;
//                    }
//                    widgetId2 = widgetId;
//                }
//                return;
//            }
//            // debug end

    }

    private String getTranslationFromQuery(SqlQuery sqlQuery, String originalText, String textToTranslate) {
        sqlQuery.setEnglish(textToTranslate);
        if (failedTranslations.contains(sqlQuery)) {
            return null;
        }
        Transformer.TransformOption option = Transformer.TransformOption.TRANSLATE_LOCAL;
        return transformer.transformWithPlaceholders(originalText, textToTranslate, option, sqlQuery);
    }


    private boolean shouldTranslateWidget(Widget widget) {
        int widgetId = widget.getId();
        boolean isFriendsListNames = ids.getFriendsTabPlayerNameTextId() == widgetId
                && widget.getXTextAlignment() == WidgetTextAlignment.LEFT;
        return shouldTranslateText(widget.getText())
                && widget.getFontId() != -1 // if font id is -1 it's probably not shown
                && !ids.getWidgetIdPlayerName().contains(widgetId) // player name should not be translated
                && !isFriendsListNames;
    }

    /* check if the text should be translated
     * returns true if the text contains letters excluding tags, has at least 1 alphabet, and has not been translated
     */
    private boolean shouldTranslateText(String text) {
        String modifiedText = text.trim();
        modifiedText = Colors.removeAllTags(modifiedText);
        return !modifiedText.isEmpty()
                && !widgetsUtilRLingual.isTranslatedWidget(text)
                && !pastTranslationResults.contains(text)
                && modifiedText.matches(".*[a-zA-Z].*")
                && !plugin.getConfig().getInterfaceTextConfig().equals(RuneLingualConfig.ingameTranslationConfig.DONT_TRANSLATE);
    }

    /*
      * get the English text from the widget that should be identical to the corresponding sql column value
      * used when creating the dump file for manual translation
      * and when searching for English added manually originating from the dump file
     */
    private String getEnglishColValFromWidget(Widget widget) {
        String text = widget.getText();
        if (text == null) {
            return "";
        }
        // special case: if the text should only be partially translated
        if (widgetsUtilRLingual.shouldPartiallyTranslate(widget)) {
            return widgetsUtilRLingual.getEnColVal4PartialTranslation(widget);
        }


        text = SqlQuery.replaceSpecialSpaces(text);
        text = Colors.getEnumeratedColorWord(text);
        text = SqlQuery.replaceNumbersWithPlaceholders(text);
        if (!ids.getWidgetId2SplitTextAtBr().contains(widget.getId())
                && !ids.getWidgetId2KeepBr().contains(widget.getId())) {
            text = text.replace(" <br>", " ");
            text = text.replace("<br> ", " ");
            text = text.replace("<br>", " ");
        }
        return text;
    }



    // used for creating the English transcript used for manual translation
    private void ifIsDumpTarget_thenDump(Widget widget, SqlQuery sqlQuery) {
//        if (sqlQuery.getSource() != null && sqlQuery.getSource().equals(SqlVariables.sourceValue4FriendsTab.getValue())
//        || sqlQuery.getSource() != null && sqlQuery.getSource().equals(SqlVariables.sourceValue4IgnoreTab.getValue())
//        || sqlQuery.getSource() != null && sqlQuery.getSource().equals(SqlVariables.sourceValue4AccountManagementTab.getValue())) {
        if (sqlQuery.getSource() != null && sqlQuery.getSource().equals(SqlVariables.sourceValue4SkillsTab.getValue())){
            if (widget.getText() == null || !shouldTranslateWidget(widget)) {
                return;
            }
            String fileName = "skillsTab.txt";
            String textToDump = getEnglishColValFromWidget(widget);

            //pastTranslationResults.add(widget.getText());
            if (ids.getWidgetId2SplitTextAtBr().contains(widget.getId())) {
                String[] textList = textToDump.split("<br>");
                for (String text : textList) {
                    appendIfNotExistToFile(text + "\t\t" + sqlQuery.getCategory() +
                            "\t" + sqlQuery.getSubCategory() +
                            "\t" + sqlQuery.getSource(), fileName);
                }
            } else {
                appendIfNotExistToFile(textToDump + "\t\t" + sqlQuery.getCategory() +
                        "\t" + sqlQuery.getSubCategory() +
                        "\t" + sqlQuery.getSource(), fileName);
            }
            return;
        }

    }
}

