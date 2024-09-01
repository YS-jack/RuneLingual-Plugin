package com.RuneLingual;

import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;

import javax.inject.Inject;

import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

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
        ChatMessageType type = event.getType();
        MessageNode messageNode = event.getMessageNode();
        String message = event.getMessage();

        Map<ChatMessageType, Runnable> actions = new HashMap<>();
        actions.put(ChatMessageType.AUTOTYPER, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.BROADCAST, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.CLAN_CHAT, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.CLAN_GIM_CHAT, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.CLAN_GUEST_CHAT, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.FRIENDSCHAT, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.MODAUTOTYPER, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.MODCHAT, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.MODPRIVATECHAT, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.PRIVATECHAT, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.PRIVATECHATOUT, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.PUBLICCHAT, () -> {
            onlineTranslator(message, messageNode);
            if (dynamicTranslations) {
                String newMessage = messageNode.getValue();
                overheadReplacer(message, newMessage);
            }
        });
        actions.put(ChatMessageType.TRADE, () -> onlineTranslator(message, messageNode));
        actions.put(ChatMessageType.UNKNOWN, () -> onlineTranslator(message, messageNode));

        if (messageTypeRequiresApi(type) && dynamicTranslations) {
            Runnable action = actions.get(type);
            if (action != null) {
                action.run();
            } else {
                if (logErrors) {
                    logger.log("Unknown message '" + message + "' type: " + type.toString());
                }
            }
        } else {
            localTranslator(message, messageNode);
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
    
    private boolean messageTypeRequiresApi(ChatMessageType type)
    {
        // checks if the message requires an API key for translating
        if(type.equals(ChatMessageType.AUTOTYPER)
                || type.equals(ChatMessageType.BROADCAST)
                || type.equals(ChatMessageType.CLAN_CHAT)
                || type.equals(ChatMessageType.CLAN_GIM_CHAT)
                || type.equals(ChatMessageType.CLAN_GUEST_CHAT)
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
