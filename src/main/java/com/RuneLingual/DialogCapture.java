package com.RuneLingual;

import static com.RuneLingual.WidgetsUtil.getAllChildren;

import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import lombok.Setter;

public class DialogCapture
{
    // Dialog happens in a separate widget than the ChatBox itself
    // not limited to npc conversations themselves, but also chat actions
    @Inject
    private Client client;
    @Inject
    private RuneLingualConfig config;
    
    // transcript managers
    @Setter
    private TranscriptManager translatedDialog;
    @Setter
    private TranscriptManager originalDialog;
    @Setter
    private TranscriptManager translatedNames;
    @Setter
    private TranscriptManager originalNames;
    @Setter
    private MessageReplacer overheadReplacer;
    
    // logging control
    @Setter
    private LogHandler logger;
    private boolean logErrors;
    private boolean logTranslations;
    private boolean logCaptures;
    
    // configs - translation control
    private boolean translateNames;
    private boolean translateDialog;
    private boolean translateMenus;
    private boolean translateOverheads;
    
    // widget control
    private String lastNpc;
    private List<Widget> widgetsLoaded;
    
    @Inject // init
    public DialogCapture(RuneLingualConfig config, Client client)
    {
        this.client = client;
        this.config = config;
        
        this.logErrors = true;
        this.logTranslations = false;
        this.logCaptures = false;
        
        this.translateNames = true;
        this.translateDialog = true;
        this.translateMenus = true;
        this.translateOverheads = true;
    }
    
    private void updateConfigs()
    {
        this.translateNames = config.getAllowName();
        this.translateDialog = config.getAllowGame();
        this.translateMenus = config.getAllowGame();
        this.translateOverheads = config.getAllowOverHead();
    }
    
    public void handleDialogs()
    {
        // loads the chatBox widget itself
        Widget chatBox = client.getWidget(ComponentID.CHATBOX_MESSAGES);
        
        // gets all children widgets from chatBox (other than chat messages)
        List<Widget> tempWidgetList = getAllChildren(chatBox);
        
        if(tempWidgetList.size() != 0 && !tempWidgetList.equals(widgetsLoaded))
        {
            // replaces current loaded widget list
            widgetsLoaded = tempWidgetList;
        }
        else
        {
            return;
        }
        
        String currentNpc = "";
        List<Integer> handledWidgetsIds = new ArrayList<Integer>();
        for(Widget widget : tempWidgetList)
        {
            // filters by all known chat widgets types
            // if some translation appears to be missing for some type of dialog
            // probably something should be added or handled here
            int widgetId = widget.getId();
            
            // checks if the widget was already dealt with
            if(handledWidgetsIds.contains(widgetId))
            {
                // skips to the next interaction
                continue;
            }
            
            if(widgetId == ComponentID.DIALOG_NPC_NAME)
            {
                currentNpc = widget.getText();
                // updates the npc name of the character that was last spoken to
                // but first checks if the name is valid
                if(currentNpc.length() > 0)
                {
                    lastNpc = currentNpc;
                }
                
                if(translateNames || true)
                {
                    String newName = localTextTranslatorCaller(currentNpc, "name", widget);
                }
                
                handledWidgetsIds.add(widgetId);
            }
            else if(widgetId == ComponentID.DIALOG_NPC_TEXT)
            {
                // if no npc was read this interaction
                // assumes the player is still speaking to the last npc
                currentNpc = lastNpc;
                
                String currentText = widget.getText();
                String newText = localTextTranslatorCaller(currentNpc, currentText, widget);
                
                // this should work nicely with that one plugin that
                // propagates npc dialog as their overheads
                if(translateOverheads)
                {
                    overheadTextReplacerCaller(currentText, newText);
                }
                
                handledWidgetsIds.add(widgetId);
            }
            else if(widgetId == ComponentID.DIALOG_PLAYER_TEXT)
            {
                String currentText = widget.getText();
                String newText = localTextTranslatorCaller("player", currentText, widget);
                
                // this should work nicely with that one plugin that
                // propagates npc dialog as their overheads
                if(translateOverheads)
                {
                    overheadTextReplacerCaller(currentText, newText);
                }
                
                handledWidgetsIds.add(widgetId);
            }
            else if(widgetId == ComponentID.DIALOG_OPTION_OPTIONS)
            {
                String currentText = widget.getText();
                localTextTranslatorCaller("playeroption", currentText, widget);
            }
            else
            {
                String currentText = widget.getText();
                // unknown-source widgets
                String newText = localTextTranslatorCaller("dialogflow", currentText, widget);
                logger.log("UNKNOWN WIDGET " + widgetId + ": " + currentText);
                handledWidgetsIds.add(widgetId);
            }
        }
    }
    
    private String localTextTranslatorCaller(String senderName, String currentMessage, Widget messageWidget)
    {
        try
        {
            String newMessage = translatedDialog.getText(senderName, currentMessage, true);
            messageWidget.setText(newMessage);
            
            if(logTranslations)
            {
                logger.log("Dialog message '"
                    + currentMessage
                    + "' was translated and replaced for '"
                    + newMessage
                    + "'.");
            }
            return newMessage;
        }
        catch(Exception e)
        {
            if(e.getMessage().equals("EntryNotFound") || e.getMessage().equals("LineNotFound"))
            {
                try
                {
                    //dialogTranslationService.addTranscript(senderName, currentMessage);
                    return "";
                }
                catch(Exception unknownException)
                {
                    if(logErrors)
                    {
                        logger.log("Could not add '"
                            + currentMessage
                            + "'line to transcript: "
                            + unknownException.getMessage());
                    }
                }
            }
            
            if(logErrors)
            {
                logger.log("Could not translate dialog message '"
                    + currentMessage
                    + "'! Exception captured: "
                    + e.getMessage());
            }
            return currentMessage;
        }
    }
    private void overheadTextReplacerCaller(String currentMessage, String newMessage)
    {
        // TODO: remove this
        if(true)
            return;
        
        try
        {
            overheadReplacer.replace(currentMessage, newMessage);
            
            if(logTranslations)
            {
                logger.log("Found and replaced overhead message for '" + currentMessage + "'.");
            }
        }
        catch(Exception e)
        {
            if(logErrors)
            {
                logger.log("Could not replace overhead message '"
                    + currentMessage
                    + "'! Exception captured: "
                    + e);
            }
        }
    }
}
