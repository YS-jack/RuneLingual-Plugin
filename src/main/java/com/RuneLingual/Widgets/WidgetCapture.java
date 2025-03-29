package com.RuneLingual.Widgets;

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

import static com.RuneLingual.Widgets.WidgetsUtilRLingual.removeBrAndTags;
import static com.RuneLingual.debug.OutputToFile.appendIfNotExistToFile;

@Slf4j
public class WidgetCapture {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    Client client;
    @Inject
    private Transformer transformer;


    @Inject
    private WidgetsUtilRLingual widgetsUtilRLingual;
    @Inject
    private DialogTranslator dialogTranslator;
    @Inject
    private Ids ids;
    @Getter
    Set<String> pastTranslationResults = new HashSet<>();


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
        if (widget.isHidden() || ids.getWidgetIdNot2Translate().contains(widgetId)) {
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
            if (queryToPass.getCategory() == null) {
                queryToPass.setCategory(SqlVariables.categoryValue4Interface.getValue());
            }
            if (queryToPass.getSubCategory() == null) {
                queryToPass.setSubCategory(SqlVariables.subcategoryValue4GeneralUI.getValue());
            }

            if (ids.getWidgetIdItemName().contains(widgetId)
                && !(widgetId == ComponentID.COMBAT_WEAPON_NAME && Objects.equals(widget.getText(), "Unarmed")) // "Unarmed" in combat tab is not an item
                ) {
                    String itemName = Colors.removeNonImgTags(widget.getText());
                    queryToPass.setItemName(itemName, textColor);
            } else if (ids.getWidgetIdNpcName().contains(widgetId)) {
                String npcName = Colors.removeNonImgTags(widget.getText());
                queryToPass.setNpcName(npcName, textColor);
            } else if (ids.getWidgetIdObjectName().contains(widgetId)) {
                String objectName = Colors.removeNonImgTags(widget.getText());
                queryToPass.setObjectName(objectName, textColor);
            } else if (ids.getWidgetIdQuestName().contains(widgetId)) {
                String questName = Colors.removeNonImgTags(widget.getText());
                queryToPass.setQuestName(questName, textColor);
            } else {
                queryToPass.setColor(textColor);
            }

            // translate the widget text
            translateWidgetText(widget, queryToPass);
        }

    }

    private void modifySqlQuery4Widget(Widget widget, SqlQuery sqlQuery) {
        sqlQuery.setColor(Colors.getColorFromHex(Colors.IntToHex(widget.getTextColor())));
        int widgetId = widget.getId();
        if (widgetId == ids.getWidgetIdSkillGuide()) { //Id for parent of skill guide, or parent of element in list
            sqlQuery.setGeneralUI(SqlVariables.sourceValue4SkillGuideInterface.getValue());
        }
        /* example of using Sets:
        if (ids.getWidgetIdPlayerName().contains(widgetId)) {
            sqlQuery.setPlayerName(widget.getText(), sqlQuery.getColor());
        }
         */
        // add more general UIs here

        // if one of the main tabs, set the category and subcategory. main tabs = combat options, skills tab, etc.
        if (widgetId == ids.getWidgetIdMainTabs()) {
            sqlQuery.setCategory(SqlVariables.categoryValue4Interface.getValue());
            sqlQuery.setSubCategory(SqlVariables.subcategoryValue4MainTabs.getValue());
        }
        // if one of the main tabs, set the source as the tab name
        if (sqlQuery.getCategory() != null && sqlQuery.getCategory().equals(SqlVariables.categoryValue4Interface.getValue())
                && sqlQuery.getSubCategory() != null && sqlQuery.getSubCategory().equals(SqlVariables.subcategoryValue4MainTabs.getValue())) {
            // set the source as the tab name
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
            } else if (widgetId == ids.getWidgetIdPrayerTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4PrayerTab.getValue());
            } else if (widgetId == ids.getWidgetIdSpellBookTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4SpellBookTab.getValue());
            } else if (widgetId == ids.getWidgetIdGroupsTab() || widgetId == ids.getWidgetIdGroupTabNonGIM() || widgetId == ids.getWidgetIdPvPArena()) {
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
        String translatedText;
        if (widgetsUtilRLingual.shouldPartiallyTranslateWidget(widget)) {
            // for widgets like "Name: <playerName>" (found in accounts management tab), where only the part of the text should be translated
            // order:
            // textToTranslate = "Name: <playerName>" -> translatedText = "名前: <playerName>" -> translatedText = "名前: Durial321"
            String translationWIthPlaceHolder = getTranslationFromQuery(sqlQuery, originalText, textToTranslate);
            translatedText = ids.getPartialTranslationManager().translateWidget(widget, translationWIthPlaceHolder, originalText, sqlQuery.getColor());
        } else if (!ids.getWidgetId2SplitTextAtBr().contains(widgetId)// for most cases
            && !ids.getWidgetId2KeepBr().contains(widgetId)) {
            translatedText = getTranslationFromQuery(sqlQuery, originalText, textToTranslate);

        } else if (ids.getWidgetId2SplitTextAtBr().contains(widgetId)){// for widgets that have <br> in the text and should be kept where they are, translate each line separately
            String[] textList = textToTranslate.split("<br>");
            String[] originalTextList = originalText.split("<br>");
            StringBuilder translatedTextBuilder = new StringBuilder();

            for (int i = 0; i < textList.length; i++) {
                String text = textList[i];
                String originalTextLine = originalTextList[i];
                String translatedTextPart = getTranslationFromQuery(sqlQuery, originalTextLine, text);
                if (translatedTextPart == null) { // if translation failed
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
            translatedText = getTranslationFromQuery(sqlQuery, originalText, textToTranslate);
        }

        // translation was not available

        if(translatedText == null){ // if the translation is the same as the original with <br>
            return;
        }
        String originalWithoutBr = removeBrAndTags(originalText);
        String translationWithoutBr = removeBrAndTags(translatedText);
        if(Objects.equals(translatedText, originalText) // if the translation is the same as the original
                || originalWithoutBr.equals(translationWithoutBr)){ // if the translation is the same as the original without <br>
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
        widgetsUtilRLingual.changeLineHeight(widget);
        widgetsUtilRLingual.changeWidgetSize_ifNeeded(widget);
    }

    private String getTranslationFromQuery(SqlQuery sqlQuery, String originalText, String textToTranslate) {
        sqlQuery.setEnglish(textToTranslate);
        Transformer.TransformOption option = Transformer.TransformOption.TRANSLATE_LOCAL;
        return transformer.transformWithPlaceholders(originalText, textToTranslate, option, sqlQuery);
    }


    private boolean shouldTranslateWidget(Widget widget) {
        int widgetId = widget.getId();

        return shouldTranslateText(widget.getText())
                && widget.getType() == WidgetType.TEXT
                && widget.getFontId() != -1 // if font id is -1 it's probably not shown
                && !ids.getWidgetIdNot2Translate().contains(widgetId)
                && !isWidgetIdNot2Translate(widget);
    }

    public boolean isWidgetIdNot2Translate(Widget widget) {
        int widgetId = widget.getId();
        boolean isFriendsListNames = ComponentID.FRIEND_LIST_NAMES_CONTAINER == widgetId
                    && widget.getXTextAlignment() == WidgetTextAlignment.LEFT;
        boolean isGimMemberNames = ids.getGimMemberNameId() == widgetId
                    && widget.getXTextAlignment() == WidgetTextAlignment.LEFT
                    && widget.getTextColor() == 0xffffff; // if not white text its "Vacancy". use color because "Vacancy" could be player name
        boolean isFriendsChatList = ComponentID.FRIENDS_CHAT_LIST == widgetId
                    && widget.getType() == WidgetType.TEXT && widget.getTextColor() == 0xffffff; // check colors so its not world #
        boolean isFcTitleOrOwner = (ComponentID.FRIENDS_CHAT_TITLE == widgetId || ComponentID.FRIENDS_CHAT_OWNER == widgetId)
                && client.getFriendsChatManager() != null;
        // if its orange text, its clan name, world, member count etc,
        // but if its grey its "Your Clan" and "No current clan" which is displayed when not in a clan
        boolean isClanName = ComponentID.CLAN_HEADER == widgetId
                    && widget.getTextColor() == 0xe6981f;
        boolean isClanMemberName = ComponentID.CLAN_MEMBERS == widgetId
                    && widget.getTextColor() == 0xffffff;
        boolean isGuesClanName = ComponentID.CLAN_GUEST_HEADER == widgetId
                    && widget.getTextColor() == 0xe6981f;
        boolean isGuestClanMemberName = ComponentID.CLAN_GUEST_MEMBERS == widgetId
                    && widget.getTextColor() == 0xffffff;
        boolean isPvPMemberName = ids.getGroupTabPvPGroupMemberId() == widgetId
                    && (widget.getTextColor() == 0xffffff || widget.getTextColor() == 0x9f9f9f);
        boolean isGroupingGroupMemberName = ids.getGroupingGroupMemberNameId() == widgetId
                && widget.getTextColor() == 0xffffff;

        return isFriendsListNames || isGimMemberNames || isFcTitleOrOwner || isFriendsChatList || isClanMemberName || isClanName
                || isGuesClanName || isGuestClanMemberName || isPvPMemberName || isGroupingGroupMemberName;
    }

    /* check if the text should be translated
     * returns true if the text contains letters excluding tags, has at least 1 alphabet, and has not been translated
     */
    private boolean shouldTranslateText(String text) {
        String modifiedText = text.trim();
        modifiedText = Colors.removeAllTags(modifiedText);
        return !modifiedText.isEmpty()
                && !widgetsUtilRLingual.isTranslatedWidget(text)
                //&& !pastTranslationResults.contains(text)
                && modifiedText.matches(".*[a-zA-Z].*")
                && !plugin.getConfig().getInterfaceTextConfig().equals(RuneLingualConfig.ingameTranslationConfig.DONT_TRANSLATE);
    }

    /*
      * get the English text from the widget that should be identical to the corresponding sql column value
      * used when creating the dump file for manual translation
      * and when searching for English added manually originating from the dump file
     */
    public String getEnglishColValFromWidget(Widget widget) {
        String text = widget.getText();
        if (text == null) {
            return "";
        }
        // special case: if the text should only be partially translated
        if (widgetsUtilRLingual.shouldPartiallyTranslateWidget(widget)) {
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
//        if (sqlQuery.getSource() != null &&
//                (sqlQuery.getSource().equals(SqlVariables.sourceValue4FriendsTab.getValue())
//        || sqlQuery.getSource().equals(SqlVariables.sourceValue4IgnoreTab.getValue())
//        || sqlQuery.getSource().equals(SqlVariables.sourceValue4AccountManagementTab.getValue()))) {
        if (sqlQuery.getSource() != null && sqlQuery.getSource().equals(SqlVariables.sourceValue4MusicTab.getValue())){
            if (widget.getText() == null || !shouldTranslateWidget(widget)) {
                return;
            }
            String fileName = "MusicTab.txt";
            String textToDump = getEnglishColValFromWidget(widget);

            // for partial translation
            if (widgetsUtilRLingual.shouldPartiallyTranslateWidget(widget)) {
                textToDump = widgetsUtilRLingual.getEnColVal4PartialTranslation(widget);
            }
            //pastTranslationResults.add(widget.getText());
            if (ids.getWidgetId2SplitTextAtBr().contains(widget.getId())) {
                String[] textList = textToDump.split("<br>");
                for (String text : textList) {
                    appendIfNotExistToFile(text + "\t" + sqlQuery.getCategory() +
                            "\t" + sqlQuery.getSubCategory() +
                            "\t" + sqlQuery.getSource(), fileName);
                }
            } else {
                appendIfNotExistToFile(textToDump + "\t" + sqlQuery.getCategory() +
                        "\t" + sqlQuery.getSubCategory() +
                        "\t" + sqlQuery.getSource(), fileName);
            }
        }

    }
}

