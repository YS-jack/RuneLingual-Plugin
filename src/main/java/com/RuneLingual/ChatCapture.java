package com.RuneLingual;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;

import javax.inject.Inject;

import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Transformer.TransformOption;

@Slf4j
public class ChatCapture
{
    /* Captures chat messages from any source
    ignores npc dialog, as they are handled in DialogCapture*/
    
    @Inject
    private Client client;
    @Inject
    private RuneLingualConfig config;
    @Inject @Getter
    private RuneLingualPlugin plugin;
    
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
    ChatCapture(RuneLingualConfig config, Client client, RuneLingualPlugin plugin)
    {
        this.config = config;
        this.client = client;
        this.plugin = plugin;
    }

    public enum openChatbox{
        ALL,
        GAME,
        PUBLIC,
        PRIVATE,
        CHANNEL,
        CLAN,
        TRADE_GIM,
        CLOSED
    }

    public enum setChatMode{
        PUBLIC,
        CHANNEL,
        CLAN,
        GUEST_CLAN,
        GROUP
    }


    public void handleChatMessage(ChatMessage chatMessage) throws Exception {
        ChatMessageType type = chatMessage.getType();
        MessageNode messageNode = chatMessage.getMessageNode();
        String message = chatMessage.getMessage();// e.g.<col=6800bf>Some cracks around the cave begin to ooze water.
        String name = chatMessage.getName(); // getName always returns player name

        // debug
        log.info("Chat message received: " + message + " | type: " + type.toString() + " | name: " + name);
        openChatbox chatbox = getOpenChatbox();
        setChatMode chatMode = getChatMode();
        log.info("Chatbox: " + chatbox.toString() + " | Chat mode: " + chatMode.toString());


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

    private TransformOption getTranslationOption(ChatMessage chatMessage) {
        String playerName = Colors.removeAllTags(chatMessage.getName());
        if (isInConfigList(playerName, config.getSpecificDontTranslate()))
            return TransformOption.AS_IS;
        else if (isInConfigList(playerName, config.getSpecificApiTranslate()))
            return TransformOption.TRANSLATE_API;
        else if (isInConfigList(playerName, config.getSpecificTransform()))
            return TransformOption.TRANSFORM;

        // if possible, check what chat im typing into, and decide with that
        //if its by the player themselves
        if (Objects.equals(playerName, client.getLocalPlayer().getName()))
            switch (config.getMyChatConfig()) {
                case TRANSFORM:
                    return TransformOption.TRANSFORM;
                case LEAVE_AS_IS:
                    return TransformOption.AS_IS;
            }

        // if its from a friend
        boolean isFriend = client.isFriended(playerName,true);
        if (isFriend)
            switch (config.getAllFriendsConfig()) {
                case TRANSFORM:
                    return TransformOption.TRANSFORM;
                case LEAVE_AS_IS:
                    return TransformOption.AS_IS;
//                case 簡易翻訳:
//                    return transformOptions.wordToWord;
                case USE_API:
                    return TransformOption.TRANSLATE_API;
            }
        switch (chatMessage.getType()){
            case PUBLICCHAT:
                return getChatsChatConfig(config.getPublicChatConfig());
            case CLAN_CHAT:
                return getChatsChatConfig(config.getClanChatConfig());
            case CLAN_GUEST_CHAT:
                return getChatsChatConfig(config.getGuestClanChatConfig());
            case FRIENDSCHAT:
                return getChatsChatConfig(config.getFriendsChatConfig());
            case CLAN_GIM_CHAT:
                if (!Objects.equals(playerName, "null") && !playerName.isEmpty())
                    return getChatsChatConfig(config.getGIMChatConfig());

            default://if its examine, engine, etc
                switch (config.getGameMessagesConfig()) {
                    case DONT_TRANSLATE:
                        return TransformOption.AS_IS;
                    case USE_LOCAL_DATA:
                        return TransformOption.TRANSLATE_LOCAL;
                    case USE_API:
                        return TransformOption.TRANSLATE_API;
                }
        }
        return TransformOption.AS_IS;
    }

    private TransformOption getChatsChatConfig(RuneLingualConfig.chatConfig chatConfig) {
        switch (chatConfig) {
            case TRANSFORM:
                return TransformOption.TRANSFORM;
            case LEAVE_AS_IS:
                return TransformOption.AS_IS;
//            case 簡易翻訳:
//                return transformOptions.wordToWord;
            case USE_API:
                return TransformOption.TRANSLATE_API;
            default:
                switch (config.getGameMessagesConfig()) {
                    case USE_API:
                        return TransformOption.TRANSLATE_API;
                    case USE_LOCAL_DATA:
                        return TransformOption.TRANSLATE_LOCAL;
                    default:
                        return TransformOption.AS_IS;
                }
        }
    }

    private boolean isInConfigList(String item, String arrayInString) {
        String[] array = arrayInString.split(",");
        for (String s:array)
            if (item.equals(s.trim()))
                return true;
        return false;
    }

    private openChatbox getOpenChatbox() {
        int chatboxVarbitValue = client.getVarcIntValue(41);
        switch (chatboxVarbitValue) {
            case 0:
                return openChatbox.ALL;
            case 1:
                return openChatbox.GAME;
            case 2:
                return openChatbox.PUBLIC;
            case 3:
                return openChatbox.PRIVATE;
            case 4:
                return openChatbox.CHANNEL;
            case 5:
                return openChatbox.CLAN;
            case 6:
                return openChatbox.TRADE_GIM;
            case 1337:
                return openChatbox.CLOSED;
            default:
                log.info("Chatbox not found, defaulting to all");
                return openChatbox.ALL;
        }
    }

    private setChatMode getChatMode() {
        int forceSendVarbitValue = client.getVarcIntValue(945);
        switch(forceSendVarbitValue) {
            case 0:
                return setChatMode.PUBLIC;
            case 1:
                return setChatMode.CHANNEL;
            case 2:
                return setChatMode.CLAN;
            case 3:
                return setChatMode.GUEST_CLAN;
            case 4:
                return setChatMode.GROUP;
            default:
                log.info("Chat mode not found, defaulting to public");
                return setChatMode.PUBLIC;
        }
    }
//
//    private boolean sendTextData(ChatMessageType type){
//
//    }
}
