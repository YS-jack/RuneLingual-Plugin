package com.RuneLingual;

import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;

import javax.inject.Inject;

import lombok.Setter;

public class ChatCapture
{
    /* Captures chat messages from any source
    ignores npc dialog, as they are handled in DialogCapture*/
    
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
    private TranscriptManager onlineTranslator;
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
    private boolean translateGame;
    private boolean translatePublic;
    private boolean translateClan;
    private boolean translateFriends;
    private boolean translateOverHeads;
    private boolean dynamicTranslations;
    
    @Inject
    ChatCapture(RuneLingualConfig config, Client client)
    {
        this.config = config;
        this.client = client;
        
        this.logErrors = true;
        this.logTranslations = false;
        this.logCaptures = false;
        
        this.translateNames = true;
        this.translateGame = true;
        this.translateOverHeads = true;
    }
    
    
    
    public void handleChatMessage(ChatMessage event) throws Exception {
        // tries to translate and replace chat messages by their given message node
        ChatMessageType type = event.getType();
        MessageNode messageNode = event.getMessageNode();
        String message = event.getMessage();
        
        if(messageTypeRequiresKey(type) && dynamicTranslations)
        {
            if(type.equals(ChatMessageType.AUTOTYPER) && translatePublic)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.BROADCAST) && translateGame)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_CHAT) && translateClan)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_CHAT) && translateClan)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GUEST_CHAT) && translateClan)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.FRIENDSCHAT) && translateFriends)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.MODAUTOTYPER) && translatePublic)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.MODCHAT) && translatePublic)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.MODPRIVATECHAT) && translateFriends)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.PRIVATECHAT) && translateFriends)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.PRIVATECHATOUT) && translateFriends)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.PUBLICCHAT) && translatePublic)
            {
                onlineTranslator(message, messageNode);
                if(dynamicTranslations)
                {
                    // avoids duplicate translation requests
                    // looks for the player that sent the message and translates it
                    String newMessage = messageNode.getValue();
                    overheadReplacer(message, newMessage);
                }
            }
            else if(type.equals(ChatMessageType.TRADE) && translatePublic)
            {
                onlineTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.UNKNOWN) && translatePublic)
            {
                onlineTranslator(message, messageNode);
            }
            else
            {
                if(logErrors)
                {
                    logger.log("Unknown message '" + message + "' type: " + type.toString());
                }
            }
        }
        else
        {
            if(type.equals(ChatMessageType.CHALREQ_CLANCHAT) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CHALREQ_FRIENDSCHAT) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CHALREQ_TRADE) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_CREATION_INVITATION) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_FORM_GROUP) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_GROUP_WITH) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_MESSAGE) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GUEST_MESSAGE) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_MESSAGE) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CONSOLE) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.ENGINE) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.FRIENDNOTIFICATION) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.FRIENDSCHATNOTIFICATION) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.GAMEMESSAGE) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.IGNORENOTIFICATION) && translateFriends)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.ITEM_EXAMINE) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.LOGINLOGOUTNOTIFICATION) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.NPC_EXAMINE) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.OBJECT_EXAMINE) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.SNAPSHOTFEEDBACK) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.TENSECTIMEOUT) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.TRADE_SENT) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.TRADEREQ) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else if(type.equals(ChatMessageType.WELCOME) && translateGame)
            {
                localTranslator(message, messageNode);
            }
            else
            {
                if(logErrors)
                {
                    logger.log("Unknown message '" + message + "' type: " + type.toString());
                }
            }
        }
    }
    
    public void updateConfigs()
    {
        this.translateNames = config.getAllowName();
        this.translateGame = config.getAllowGame();
        this.translateOverHeads = config.getAllowOverHead();
    }
    
    private void localTranslator(String message, MessageNode node)
    {
        try
        {
            String translatedMessage = translatedDialog.getText("game", message, true);
            node.setValue(translatedMessage);
            
            if(logTranslations)
            {
                logger.log("Replaced game message '" + message + "'.");
            }
        }
        catch (Exception e)
        {
            if(e.getMessage() == "LineNotFound")
            {
                try
                {
                    originalDialog.addTranscript("game", message);
                    return;
                }
                catch(Exception unknownException)
                {
                    if(logErrors)
                    {
                        logger.log("Could not add '"
                            + message
                            + "'line to transcript: "
                            + unknownException.getMessage());
                    }
                }
            }
            
            if(logErrors)
            {
                String originalContents = node.getValue();
                logger.log("Could not replace contents for '"
                   + originalContents + "', exception occurred: "
                   + e.getMessage());
            }
        }
    }
    
    private void onlineTranslator(String message, MessageNode node)
    {
        // TODO: this
    }
    
    private void overheadReplacer(String currentMessage, String newMessage)
    {
        try
        {
            String translatedMessage = overheadReplacer.replace(currentMessage, newMessage);
            
            if(logTranslations)
            {
                logger.log("Replaced overhead message '" + currentMessage + "'.");
            }
        }
        catch (Exception e)
        {
            if(logErrors)
            {
                logger.log("Could not replace contents for '"
                    + currentMessage
                    + "', exception occurred: "
                    + e.getMessage());
            }
        }
    }
    
    private boolean messageTypeRequiresKey(ChatMessageType type)
    {
        // checks if the message requires an API key for translating
        if(type.equals(ChatMessageType.AUTOTYPER)
                || type.equals(ChatMessageType.BROADCAST)
                || type.equals(ChatMessageType.CLAN_CHAT)
                || type.equals(ChatMessageType.CLAN_GIM_CHAT)
                || type.equals(ChatMessageType.CLAN_GUEST_CHAT)
                || type.equals(ChatMessageType.BROADCAST)
                || type.equals(ChatMessageType.MODCHAT)
                || type.equals(ChatMessageType.MODPRIVATECHAT)
                || type.equals(ChatMessageType.PRIVATECHAT)
                || type.equals(ChatMessageType.PUBLICCHAT)
                || type.equals(ChatMessageType.SPAM)
                || type.equals(ChatMessageType.UNKNOWN))
        {
            return true;
        }
        return false;
    }
}
