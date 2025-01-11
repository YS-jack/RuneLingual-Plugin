package com.RuneLingual.Wigets;

import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.SQL.SqlQuery;
import com.RuneLingual.SQL.SqlVariables;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Ids;
import com.RuneLingual.commonFunctions.Transformer;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.RuneLingual.debug.OutputToFile.appendIfNotExistToFile;

public class WidgetCapture {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    Client client;
    @Inject
    private Transformer transformer;
    @Getter
    List<String> translationResults = new ArrayList<>(); //TODO: add translated text to this list

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

        sqlQuery = modifySqlQuery4Widget(widget, sqlQuery);

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

        // translate the widget text////////////////
        int widgetGroup = WidgetUtil.componentToInterface(widget.getId());
        if (widgetGroup == InterfaceID.CHATBOX) {// skip all chatbox widgets for now TODO: chatbox buttons should be translated
            return;
        }

//        if (isDumpTarget_dump(widget)) {
//        }

        if (widgetGroup == InterfaceID.DIALOG_NPC
                || widgetGroup == InterfaceID.DIALOG_PLAYER
                || widgetGroup == InterfaceID.DIALOG_OPTION) {
            dialogTranslator.handleDialogs(widget);
            return;
        }

        if(shouldTranslateWidget(widget)) {
            translateWidgetText(widget, sqlQuery);
            return;
        }

//        if (!widget.getText().isEmpty()
//                && !widget.getText().contains("<img=") // check if already translated to japanese. TODO: need something else after adding other languages
//                && plugin.getConfig().getInterfaceTextConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)) {
//
//            // if its only numbers and symbols dont do anything
//            String re = "^[^\\p{Alpha}]+$";
//            if (widget.getText().matches(re))
//                return;
//
//            Colors[] textColor = Colors.getColorArray(widget.getText(), Colors.black);
//            // for now only translate interfaces and buttons with API
//            widgetsUtilRLingual.setWidgetText_ApiTranslation(widget, widget.getText(), textColor[0]);
//
//        }
    }

    private SqlQuery modifySqlQuery4Widget(Widget widget, SqlQuery sqlQuery) {
        sqlQuery.setColor(Colors.getColorFromHex(Colors.IntToHex(widget.getTextColor())));
        int widgetId = widget.getId();
        if (widgetId == ids.getWidgetIdSkillGuide()) { //Id for parent of skill guide, or parent of element in list
            sqlQuery.setCategory(SqlVariables.categoryValue4Interface.getValue());
            sqlQuery.setSubCategory(SqlVariables.subcategoryValue4GeneralUI.getValue());
            sqlQuery.setSource(SqlVariables.sourceValue4SkillGuideInterface.getValue());

        }
        if (widgetId == ids.getWidgetIdMainTabs()) {
            sqlQuery.setCategory(SqlVariables.categoryValue4Interface.getValue());
            sqlQuery.setSubCategory(SqlVariables.subcategoryValue4MainTabs.getValue());
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
        return sqlQuery;
    }

    private void translateWidgetText(Widget widget, SqlQuery sqlQuery) {
        if (shouldTranslateWidget(widget)) {
            String textToTranslate = getEnglishColValFromWidget(widget);
            sqlQuery.setEnglish(textToTranslate);
            Transformer.TransformOption option = Transformer.TransformOption.TRANSLATE_LOCAL;
            String translatedText = transformer.transformWithPlaceholders(widget.getText(), textToTranslate, option, sqlQuery);



            //below is for debugging
//            int widgetId = widget.getId();
//            if(widgetId == 25034758){
//                int widgetId2;
//                for(int j = 0; j < 100; j++){
//                    widget = widget.getParent();
//                    if (widget == null) {
//                        break;
//                    }
//                    widgetId2 = widget.getId();
//                }
//                return;
//            }
//            // debug end

            if(Objects.equals(translatedText, textToTranslate)){
                return;
            }
            widgetsUtilRLingual.setWidgetText_NiceBr_CharImages(widget, translatedText);
        }
    }


    private boolean shouldTranslateWidget(Widget widget) {
        return shouldTranslateText(widget.getText())
                && widget.getFontId() != -1; // if font id is -1 it's probably not shown
    }

    /* check if the text should be translated
     * returns true if the text contains letters excluding tags, has at least 1 alphabet, and has not been translated
     */
    private boolean shouldTranslateText(String text) {
        String modifiedText = text.trim();
        modifiedText = Colors.removeAllTags(modifiedText);
        return !modifiedText.isEmpty()
                && !translationResults.contains(text)
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
        text = SqlQuery.replaceSpecialSpaces(text);
        text = Colors.getEnumeratedColorWord(text);
        text = SqlQuery.replaceNumbersWithPlaceholders(text);

        return text.replace("<br>", " ");
    }

    // used for creating the English transcript used for manual translation
    private boolean isDumpTarget_dump(Widget widget) {
        if (widget.getParent() != null && (widget.getParent().getId() == 14024705 || widget.getParent().getId() == 14024714)) { //parent of skill guide, or parent of element in list
            if (widget.getText() == null || !shouldTranslateWidget(widget)) {
                return true;
            }
            String textToDump = getEnglishColValFromWidget(widget);
            translationResults.add(widget.getText());
            appendIfNotExistToFile(textToDump + "\t\t" + SqlVariables.categoryValue4Interface.getValue() +
                    "\t" + SqlVariables.subcategoryValue4GeneralUI.getValue() +
                    "\t" + SqlVariables.sourceValue4SkillGuideInterface.getValue(), "skillGuideDump.txt");

            return true;
        }
        return false;
    }
}

