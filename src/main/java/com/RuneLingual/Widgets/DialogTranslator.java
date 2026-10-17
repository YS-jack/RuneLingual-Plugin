package com.RuneLingual.Widgets;

import com.RuneLingual.*;
import com.RuneLingual.SQL.SqlQuery;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Transformer;
import com.RuneLingual.commonFunctions.Transformer.TransformOption;
import com.RuneLingual.nonLatin.GeneralFunctions;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID.*;

import javax.inject.Inject;

import net.runelite.api.widgets.WidgetUtil;

import static com.RuneLingual.Widgets.WidgetsUtilRLingual.removeBrAndTags;


@Slf4j
public class DialogTranslator {
    @Inject
    private Client client;
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private RuneLingualConfig config;

    @Getter
    private final int playerNameWidgetId = ChatRight.NAME;
    @Getter
    private final int playerContinueWidgetId = ChatRight.CONTINUE;
    @Getter
    private final int playerContentWidgetId = ChatRight.TEXT;

    @Getter
    private final int npcNameWidgetId = ChatLeft.NAME;
    @Getter
    private final int npcContinueWidgetId = ChatLeft.CONTINUE;
    @Getter
    private final int npcContentWidgetId = ChatLeft.TEXT;

    @Getter
    private final int dialogOptionWidgetId = Chatmenu.OPTIONS;

    private static final String PLAYER_NAME_PLACEHOLDER = "[player name]";

    private final Colors defaultTextColor = Colors.black;
    private final Colors continueTextColor = Colors.blue;
    private final String continueText = "Click here to continue";
    private final Colors nameAndSelectOptionTextColor = Colors.red;
    private final String selectOptionText = "Select an option";
    private final Colors pleaseWaitTextColor = Colors.blue;
    private final String pleaseWaitText = "Please wait...";

    private TransformOption dialogOption;
    private TransformOption npcNameOption;
    @Inject
    Transformer transformer;

    @Inject
    private GeneralFunctions generalFunctions;
    @Inject
    private WidgetsUtilRLingual widgetsUtilRLingual;

    @Inject
    public DialogTranslator(RuneLingualConfig config, Client client, RuneLingualPlugin plugin) {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        this.transformer = new Transformer(plugin);
    }

    private String replacePlayerNameWithPlaceholder(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String playerName = client.getLocalPlayer().getName();
        if (playerName == null || playerName.isEmpty()) {
            return text;
        }
        return text.replace(playerName, PLAYER_NAME_PLACEHOLDER);
    }

    private String restorePlayerName(String translatedText) {
        // Player name restoration now happens in Transformer.stringToDisplayedString
        return translatedText;
    }

    public void handleDialogs(Widget widget) {
        if(widget.getText().contains("<img=")) {
            return;
        }
        dialogOption = MenuCapture.getTransformOption(plugin.getConfig().getNpcDialogueConfig(), plugin.getConfig().getSelectedLanguage());
        npcNameOption = MenuCapture.getTransformOption(plugin.getConfig().getNPCNamesConfig(), plugin.getConfig().getSelectedLanguage());
        if ((widget.getId() != npcNameWidgetId && dialogOption.equals(TransformOption.AS_IS))
                || (widget.getId() == npcNameWidgetId && npcNameOption.equals(TransformOption.AS_IS))) {
            return;
        }

        int interfaceID = WidgetUtil.componentToInterface(widget.getId());

        if (npcNameOption.equals(TransformOption.TRANSLATE_API) && widget.getId() == npcNameWidgetId) {
            String npcName = widget.getText();
            widgetsUtilRLingual.setWidgetText_ApiTranslation(widget, npcName, nameAndSelectOptionTextColor);
            return;
        }
        else if (dialogOption.equals(TransformOption.TRANSLATE_API) && widget.getId() != playerNameWidgetId && widget.getId() != npcNameWidgetId) {
            String dialogText = widget.getText();
            if(dialogText.isEmpty()) {
                return;
            }
            String textForTranslation = replacePlayerNameWithPlaceholder(dialogText);
            Colors[] textColor = {defaultTextColor};
            if(widget.getId() == npcContinueWidgetId || widget.getId() == playerContinueWidgetId)
                textColor[0] = continueTextColor;
            else if(widget.getId() == dialogOptionWidgetId && widget.getText().equals(selectOptionText))
                textColor[0] = nameAndSelectOptionTextColor;

            widgetsUtilRLingual.setWidgetText_ApiTranslation(widget, textForTranslation, textColor[0]);
            return;
        }

        switch (interfaceID) {
            case InterfaceID.DIALOG_NPC:
                handleNpcDialog(widget);
                return;
            case InterfaceID.DIALOG_PLAYER:
                handlePlayerDialog(widget);
                return;
            case InterfaceID.DIALOG_OPTION:
                handleOptionDialog(widget);
                return;
            default:
                break;
        }
    }

    private void handleNpcDialog(Widget widget) {
        if (widget.getId() == npcNameWidgetId) {
            String npcName = widget.getText();
            npcName = removeBrAndTags(npcName);

            SqlQuery query = new SqlQuery(this.plugin);
            query.setNpcName(npcName, nameAndSelectOptionTextColor);
            String translatedText = transformer.transform(npcName, nameAndSelectOptionTextColor,
                    npcNameOption, query, false);
            widget.setText(translatedText);
        } else if (widget.getId() == npcContinueWidgetId) {
            translateContinueWidget(widget);
        } else if (widget.getId() == npcContentWidgetId) {
            String npcContent = widget.getText();
            npcContent = removeBrAndTags(npcContent);

            String npcContentForQuery = replacePlayerNameWithPlaceholder(npcContent);

            String npcName = getInteractingNpcName();
            SqlQuery query = new SqlQuery(this.plugin);
            query.setDialogue(npcContentForQuery, npcName, false, defaultTextColor);
            String translatedText = transformer.transform(npcContentForQuery, defaultTextColor, dialogOption, query, false);

            translatedText = restorePlayerName(translatedText);

            setDialogText(widget, npcContent, translatedText);
        }
    }

    private void handlePlayerDialog(Widget widget) {
        if (widget.getId() == playerContinueWidgetId) {
            translateContinueWidget(widget);
            return;
        }
        if (widget.getId() == playerContentWidgetId) {
            String playerContent = widget.getText();
            playerContent = removeBrAndTags(playerContent);

            String playerContentForQuery = replacePlayerNameWithPlaceholder(playerContent);

            String npcName = getInteractingNpcName();

            SqlQuery query = new SqlQuery(this.plugin);
            query.setDialogue(playerContentForQuery, npcName, true, defaultTextColor);
            String translatedText = transformer.transform(playerContentForQuery, defaultTextColor, dialogOption, query, false);

            translatedText = restorePlayerName(translatedText);

            setDialogText(widget, playerContent, translatedText);
        }
    }

    private void handleOptionDialog(Widget widget) {
        String dialogOption = widget.getText();
        if (dialogOption.equals(selectOptionText)) {
            widget.setText(getSelectOptionTranslation());
            return;
        }
        if (dialogOption.equals(pleaseWaitText)) {
            widget.setText(getPleaseWaitTranslation());
            return;
        }
        dialogOption = removeBrAndTags(dialogOption);

        String dialogOptionForQuery = replacePlayerNameWithPlaceholder(dialogOption);

        SqlQuery query = new SqlQuery(this.plugin);
        query.setDialogue(dialogOptionForQuery, getInteractingNpcName(), false, defaultTextColor);
        String translatedText = transformer.transform(dialogOptionForQuery, defaultTextColor, this.dialogOption, query, false);

        translatedText = restorePlayerName(translatedText);

        setDialogText(widget, dialogOption, translatedText);
    }

    // transform() returns the original text when no translation is found;
    // keep the game's own line breaks and line height for it instead of laying it out like translated text
    private void setDialogText(Widget widget, String originalText, String translatedText) {
        if (Colors.removeNonImgTags(translatedText).equals(originalText)) {
            return;
        }
        widgetsUtilRLingual.setWidgetText_NiceBr(widget, translatedText);
        widgetsUtilRLingual.changeLineHeight(widget);
    }

    private String getInteractingNpcName() {
        NPC npc = plugin.getInteractedNpc();
        if (npc == null) {
            return "";
        }
        return npc.getName();
    }

    private String getContinueTranslation() {
        SqlQuery query = new SqlQuery(this.plugin);
        query.setDialogue(continueText, "", true, continueTextColor);
        return transformer.transform(continueText, continueTextColor, dialogOption, query, false);
    }

    private String getSelectOptionTranslation() {
        SqlQuery query = new SqlQuery(this.plugin);
        query.setDialogue(selectOptionText, "", true, nameAndSelectOptionTextColor);
        return transformer.transform(selectOptionText, nameAndSelectOptionTextColor, dialogOption, query, false);
    }

    private String getPleaseWaitTranslation() {
        SqlQuery query = new SqlQuery(this.plugin);
        query.setDialogue(pleaseWaitText, "", true, pleaseWaitTextColor);
        return transformer.transform(pleaseWaitText, pleaseWaitTextColor, dialogOption, query, false);
    }

    private void translateContinueWidget(Widget widget) {
        if (widget.getText().equals(continueText)) {
            widget.setText(getContinueTranslation());
        } else if (widget.getText().equals(pleaseWaitText)) {
            widget.setText(getPleaseWaitTranslation());
        }
    }
}
