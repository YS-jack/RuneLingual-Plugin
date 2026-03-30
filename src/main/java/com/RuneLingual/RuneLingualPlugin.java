package com.RuneLingual;

import com.RuneLingual.ApiTranslate.*;
import com.RuneLingual.ChatMessages.*;
import com.RuneLingual.MouseOverlays.MenuEntryHighlightOverlay;
import com.RuneLingual.MouseOverlays.MouseTooltipOverlay;
import com.RuneLingual.SQL.SqlActions;
import com.RuneLingual.SQL.SqlQuery;
import com.RuneLingual.Widgets.PartialTranslationManager;
import com.RuneLingual.Widgets.Widget2ModDict;
import com.RuneLingual.Widgets.WidgetCapture;
import com.RuneLingual.Widgets.WidgetsUtilRLingual;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.RuneLingual.debug.OutputToFile;
import com.RuneLingual.nonLatin.*;
import com.RuneLingual.prepareResources.H2Manager;
import com.RuneLingual.prepareResources.SpriteReplacer;
import com.google.inject.Provides;

import javax.annotation.Nullable;
import javax.inject.Inject;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.callback.ClientThread;


import lombok.Getter;

import com.RuneLingual.SidePanelComponents.SidePanel;
import com.RuneLingual.commonFunctions.FileActions;
import com.RuneLingual.prepareResources.Downloader;
import com.RuneLingual.commonFunctions.Ids;
import okhttp3.OkHttpClient;


import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@PluginDescriptor(
        // Plugin name shown at plugin hub
        name = "RuneLingual Dev",
        description = "All-in-one translation plugin for OSRS.",
        loadInSafeMode = true
)

public class RuneLingualPlugin extends Plugin {
    private static final String CHATBOX_INPUT_EVENT = "chatboxInput";
    private static final int CHATBOX_TYPE_PUBLIC = 0;
    private static final int CHATBOX_TYPE_CHEAT = 1;
    private static final int CHATBOX_TYPE_FRIENDS = 2;
    private static final int CHATBOX_TYPE_CLAN = 3;
    private static final int CHATBOX_TYPE_GUEST_CLAN = 4;
    private static final int CHATBOX_TYPE_GIM = 5;
    private static final int OUTGOING_TRANSFORM_MIN_LENGTH = 2;
    private static final long OUTGOING_TRANSLATION_TIMEOUT_MILLIS = 5000L;
    private static final long OUTGOING_RESTORE_TIMEOUT_MILLIS = 15000L;

    private static final class OutgoingRestoreMessage {
        private final ChatMessageType expectedType;
        private final String sentMessage;
        private final String originalMessage;
        private final long expiresAt;

        private OutgoingRestoreMessage(ChatMessageType expectedType, String sentMessage, String originalMessage, long expiresAt) {
            this.expectedType = expectedType;
            this.sentMessage = sentMessage;
            this.originalMessage = originalMessage;
            this.expiresAt = expiresAt;
        }
    }

    @Inject
    @Getter
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private EventBus eventBus;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientToolbar clientToolBar;
    @Inject
    @Getter
    private ChatIconManager chatIconManager;
    @Getter
    private HashMap<String, Integer> charIds = new HashMap<>();    // colour-char(key) <-> CharIds(val)

    @Inject
    @Getter
    private RuneLingualConfig config;
    @Inject
    private CharImageInit charImageInit;

    @Getter @Setter
    private LangCodeSelectableList targetLanguage;
    @Getter
    private String selectedLanguageName;


    // main modules
    @Inject
    @Getter
    private ChatCapture chatCapture;
    @Inject
    @Getter
    private MenuCapture menuCapture;


    @Inject
    @Getter
    private Downloader downloader;
    @Inject
    @Getter
    private H2Manager h2Manager;
    @Inject
    @Getter
    private SidePanel panel;
    private NavigationButton navButton;
    @Inject
    @Getter
    private GeneralFunctions generalFunctions;
    @Inject
    @Getter
    private FileNameAndPath fileNameAndPath = new FileNameAndPath();
    @Inject
    @Getter
    private SqlActions sqlActions;
    @Inject
    private SqlQuery sqlQuery;
    @Getter
    @Setter
    private String[] tsvFileNames;
    @Getter @Setter
    private String databaseUrl;
    @Getter
    @Setter
    private Connection conn;
    @Inject
    @Getter
    private Ids ids;
    @Inject
    @Getter
    private Widget2ModDict widget2ModDict;
    @Inject @Getter
    private PartialTranslationManager partialTranslationManager;
    @Inject
    @Getter
    private WidgetsUtilRLingual widgetsUtilRLingual;
    @Inject
    private MouseTooltipOverlay mouseTooltipOverlay;
    @Inject
    @Getter
    private Deepl deepl;
    @Inject
    private DeeplUsageOverlay deeplUsageOverlay;
    @Inject
    @Getter
    private ChatInputRLingual chatInputRLingual;
    @Inject
    @Getter
    private ChatInputOverlay chatInputOverlay;
    @Inject
    private MenuEntryHighlightOverlay menuEntryHighlightOverlay;
    @Inject
    private ChatInputCandidateOverlay chatInputCandidateOverlay;
    @Inject
    private OverheadCapture overheadCapture;
    @Inject @Getter
    private WidgetCapture widgetCapture;

    @Getter
    private TileObject interactedObject;
    @Getter
    private NPC interactedNpc;
    @Getter
    boolean attacked;
    private int clickTick;
    @Getter
    private int gameCycle;
    @Inject
    private OkHttpClient httpClient;
    @Inject
    SpriteReplacer spriteReplacer;
    @Inject
    @Getter
    OutputToFile outputToFile;

    @Getter
    Set<SqlQuery> failedTranslations = new HashSet<>();


    // stores selected languages during this session, to prevent re-initializing char images
    private final Set<LangCodeSelectableList> pastLanguages = new HashSet<>();
    private final Deque<OutgoingRestoreMessage> outgoingMessagesToRestore = new ConcurrentLinkedDeque<>();
    private final ArrayList<EventBus.Subscriber> manualSubscriptions = new ArrayList<>();
    private ExecutorService outgoingTranslationExecutor;
    private boolean suppressOutgoingScriptCallback;

    @Override
    protected void startUp() throws Exception {
        if (outgoingTranslationExecutor == null || outgoingTranslationExecutor.isShutdown()) {
            outgoingTranslationExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "runelingual-outgoing-translate");
                thread.setDaemon(true);
                return thread;
            });
        }

        //get selected language
        targetLanguage = config.getSelectedLanguage();
        pastLanguages.add(targetLanguage);
        databaseUrl = h2Manager.getUrl(targetLanguage);
        // check if online files have changed, if so download and update local files
        initLangFiles();

        //connect to database
        conn = h2Manager.getConn(targetLanguage);

        // initiate overlays
        overlayManager.add(mouseTooltipOverlay);
        overlayManager.add(deeplUsageOverlay);
        overlayManager.add(chatInputOverlay);
        overlayManager.add(chatInputCandidateOverlay);
        overlayManager.add(menuEntryHighlightOverlay);

        // load image files
        charImageInit.loadCharImages();
        queueUpdateAllOverrides();

        // side panel
        startPanel();
        registerEventSubscriptions();
        //log.info("RuneLingual started!");
    }

    public void onOverheadTextChanged(OverheadTextChanged event) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }
        try {
            overheadCapture.translateOverhead(event);
        } catch (Exception e) {
            log.debug("Failed to translate overhead text.", e);
        }
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }
        //log.info("Widget loaded:" + event.getGroupId());
//		clientThread.invokeLater(() -> {
//			widgetCapture.translateWidget();
//		});
    }

    private void onBeforeRender(BeforeRender event) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }

        chatInputRLingual.updateChatInput();
        widgetCapture.translateWidget();
    }

    public void onMenuOpened(MenuOpened event) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }

        menuCapture.handleOpenedMenu(event);
    }

    public void onScriptCallbackEvent(ScriptCallbackEvent event) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH
                || client.getGameState() != GameState.LOGGED_IN
                || !CHATBOX_INPUT_EVENT.equals(event.getEventName())) {
            return;
        }
        if (suppressOutgoingScriptCallback) {
            return;
        }

        Object[] objectStack = client.getObjectStack();
        int[] intStack = client.getIntStack();
        int objectStackSize = client.getObjectStackSize();
        int intStackSize = client.getIntStackSize();

        if (objectStackSize < 1 || intStackSize < 2 || !(objectStack[objectStackSize - 1] instanceof String)) {
            return;
        }

        String originalText = (String) objectStack[objectStackSize - 1];
        int chatType = intStack[intStackSize - 2];
        int clanTarget = intStack[intStackSize - 1];

        if (!shouldTransformOutgoingMessage(originalText, chatType)) {
            return;
        }

        // Prevent the default sender from sending the original text.
        objectStack[objectStackSize - 1] = "";
        translateAndSendOutgoingMessage(originalText, chatType, clanTarget);
    }
    public void onChatMessage(ChatMessage event) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }
        if (client.getGameState() != GameState.LOGGED_IN && client.getGameState() != GameState.HOPPING) {
            return;
        }
        try {
            chatCapture.handleChatMessage(event);
        } catch (Exception e) {
            log.debug("Failed handling chat message event.", e);
        }
    }


    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }
        if (gameStateChanged.getGameState() == GameState.LOADING) {
            deepl.setUsageAndLimit();
            interactedObject = null;
        }
    }

    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(RuneLingualConfig.GROUP)) {
            return;
        }
        outgoingMessagesToRestore.clear();
        // if language is changed
        if (targetLanguage != config.getSelectedLanguage()) {
            targetLanguage = config.getSelectedLanguage();
            spriteReplacer.resetWidgetSprite();
            if (targetLanguage.hasLocalTranscript()) {
                //close current connection
                h2Manager.closeConn();
            }
            if (targetLanguage == LangCodeSelectableList.ENGLISH || !targetLanguage.hasLocalTranscript()) {
                clientToolBar.removeNavigation(navButton);
                if(targetLanguage != LangCodeSelectableList.ENGLISH){
                    deepl = new Deepl(this, httpClient);
                }
                return;
            }

            databaseUrl = h2Manager.getUrl(targetLanguage);
            initLangFiles();
            conn = h2Manager.getConn(targetLanguage);

            clientToolBar.removeNavigation(navButton);
            queueUpdateAllOverrides();
            if (targetLanguage.needsCharImages() && !pastLanguages.contains(targetLanguage)) {
                charImageInit.loadCharImages();
            }

            overlayManager.remove(mouseTooltipOverlay);
            MouseTooltipOverlay.setAttemptedTranslation(new ArrayList<>());
            overlayManager.add(mouseTooltipOverlay);

            //reset deepl's past translations
            deepl = new Deepl(this, httpClient);

            restartPanel();
            pastLanguages.add(targetLanguage);
        }
        if(config.ApiConfig()){
            deepl.setUsageAndLimit();
            deepl.getTranslationAttempt().clear();
        }

    }

    public void onNpcDespawned(NpcDespawned npcDespawned) {
        if (npcDespawned.getNpc() == interactedNpc) {
            interactedNpc = null;
        }
    }

    public void onGameTick(GameTick gameTick) {
        if (client.getTickCount() > clickTick && client.getLocalDestinationLocation() == null) {
            // when the destination is reached, clear the interacting object
            interactedObject = null;
            interactedNpc = null;
        }

        if (client.isMenuOpen()) {
            menuCapture.handlePendingApiTranslation();
        }

        chatCapture.handlePendingChatMessages();
        overheadCapture.handlePendingOverheadTranslations();
    }

    public void onInteractingChanged(InteractingChanged interactingChanged) {
        if (interactingChanged.getSource() == client.getLocalPlayer()
                && client.getTickCount() > clickTick && interactingChanged.getTarget() != interactedNpc) {
            interactedNpc = null;
            attacked = interactingChanged.getTarget() != null && interactingChanged.getTarget().getCombatLevel() > 0;
        }
    }

    public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked) {
        switch (menuOptionClicked.getMenuAction()) {
            case WIDGET_TARGET_ON_GAME_OBJECT:
            case GAME_OBJECT_FIRST_OPTION:
            case GAME_OBJECT_SECOND_OPTION:
            case GAME_OBJECT_THIRD_OPTION:
            case GAME_OBJECT_FOURTH_OPTION:
            case GAME_OBJECT_FIFTH_OPTION: {
                int x = menuOptionClicked.getParam0();
                int y = menuOptionClicked.getParam1();
                int id = menuOptionClicked.getId();
                interactedObject = findTileObject(x, y, id);
                interactedNpc = null;
                clickTick = client.getTickCount();
                gameCycle = client.getGameCycle();
                break;
            }
            case WIDGET_TARGET_ON_NPC:
            case NPC_FIRST_OPTION:
            case NPC_SECOND_OPTION:
            case NPC_THIRD_OPTION:
            case NPC_FOURTH_OPTION:
            case NPC_FIFTH_OPTION: {
                interactedObject = null;
                interactedNpc = menuOptionClicked.getMenuEntry().getNpc();
                attacked = menuOptionClicked.getMenuAction() == MenuAction.NPC_SECOND_OPTION ||
                        menuOptionClicked.getMenuAction() == MenuAction.WIDGET_TARGET_ON_NPC
                                && client.getSelectedWidget() != null
                                && WidgetUtil.componentToInterface(client.getSelectedWidget().getId()) == InterfaceID.SPELLBOOK;
                clickTick = client.getTickCount();
                gameCycle = client.getGameCycle();
                break;
            }
            // Any menu click which clears an interaction
            case WALK:
            case WIDGET_TARGET_ON_WIDGET:
            case WIDGET_TARGET_ON_GROUND_ITEM:
            case WIDGET_TARGET_ON_PLAYER:
            case GROUND_ITEM_FIRST_OPTION:
            case GROUND_ITEM_SECOND_OPTION:
            case GROUND_ITEM_THIRD_OPTION:
            case GROUND_ITEM_FOURTH_OPTION:
            case GROUND_ITEM_FIFTH_OPTION:
                interactedObject = null;
                interactedNpc = null;
                break;
            default:
                if (menuOptionClicked.isItemOp()) {
                    interactedObject = null;
                    interactedNpc = null;
                }
        }
    }

    private void registerEventSubscriptions() {
        unregisterEventSubscriptions();
        manualSubscriptions.add(eventBus.register(OverheadTextChanged.class, this::onOverheadTextChanged, 0f));
        manualSubscriptions.add(eventBus.register(WidgetLoaded.class, this::onWidgetLoaded, 0f));
        manualSubscriptions.add(eventBus.register(BeforeRender.class, this::onBeforeRender, 0f));
        manualSubscriptions.add(eventBus.register(MenuOpened.class, this::onMenuOpened, 0f));
        manualSubscriptions.add(eventBus.register(ScriptCallbackEvent.class, this::onScriptCallbackEvent, 100f));
        manualSubscriptions.add(eventBus.register(ChatMessage.class, this::onChatMessage, 0f));
        manualSubscriptions.add(eventBus.register(GameStateChanged.class, this::onGameStateChanged, 0f));
        manualSubscriptions.add(eventBus.register(ConfigChanged.class, this::onConfigChanged, 0f));
        manualSubscriptions.add(eventBus.register(NpcDespawned.class, this::onNpcDespawned, 0f));
        manualSubscriptions.add(eventBus.register(GameTick.class, this::onGameTick, 0f));
        manualSubscriptions.add(eventBus.register(InteractingChanged.class, this::onInteractingChanged, 0f));
        manualSubscriptions.add(eventBus.register(MenuOptionClicked.class, this::onMenuOptionClicked, 0f));
    }

    private void unregisterEventSubscriptions() {
        for (EventBus.Subscriber subscriber : manualSubscriptions) {
            eventBus.unregister(subscriber);
        }
        manualSubscriptions.clear();
    }

    @Nullable
    public String consumeOutgoingOriginalMessage(ChatMessage chatMessage) {
        if (!config.showOriginalOutgoing() || !isOwnMessage(chatMessage)) {
            return null;
        }

        long now = System.currentTimeMillis();
        cleanupExpiredOutgoingMessages(now);

        OutgoingRestoreMessage matched = null;
        for (OutgoingRestoreMessage message : outgoingMessagesToRestore) {
            if (message.expiresAt < now) {
                continue;
            }

            if (message.expectedType == chatMessage.getType() && Objects.equals(message.sentMessage, chatMessage.getMessage())) {
                matched = message;
                break;
            }
        }

        if (matched == null) {
            return null;
        }

        outgoingMessagesToRestore.remove(matched);
        return matched.originalMessage;
    }

    private void translateAndSendOutgoingMessage(String originalText, int chatType, int clanTarget) {
        if (outgoingTranslationExecutor == null || outgoingTranslationExecutor.isShutdown()) {
            clientThread.invoke(() -> sendTranslatedChat(originalText, originalText, chatType, clanTarget));
            return;
        }

        CompletableFuture
                .supplyAsync(() -> translateOutgoingMessage(originalText), outgoingTranslationExecutor)
                .exceptionally(ex -> {
                    log.debug("Failed to translate outgoing chat message, sending original text.", ex);
                    return originalText;
                })
                .thenAccept(translatedText ->
                        clientThread.invoke(() -> sendTranslatedChat(originalText, translatedText, chatType, clanTarget)));
    }

    private String translateOutgoingMessage(String originalText) {
        if (!config.ApiConfig()) {
            return originalText;
        }
        return deepl.translateBlocking(
                originalText,
                config.getSelectedLanguage(),
                LangCodeSelectableList.ENGLISH,
                OUTGOING_TRANSLATION_TIMEOUT_MILLIS
        );
    }

    private void sendTranslatedChat(String originalMessage, String translatedMessage, int chatType, int clanTarget) {
        String messageToSend = translatedMessage;
        if (messageToSend == null || messageToSend.isBlank()) {
            messageToSend = originalMessage;
        }

        ChatMessageType expectedType = getExpectedOutgoingType(chatType);
        if (config.showOriginalOutgoing()
                && expectedType != null
                && !Objects.equals(messageToSend, originalMessage)) {
            outgoingMessagesToRestore.addLast(
                    new OutgoingRestoreMessage(
                            expectedType,
                            messageToSend,
                            originalMessage,
                            System.currentTimeMillis() + OUTGOING_RESTORE_TIMEOUT_MILLIS
                    )
            );
            cleanupExpiredOutgoingMessages(System.currentTimeMillis());
        }

        suppressOutgoingScriptCallback = true;
        try {
            client.runScript(ScriptID.CHAT_SEND, messageToSend, chatType, clanTarget, 0, -1);
        } finally {
            suppressOutgoingScriptCallback = false;
        }
    }

    private boolean shouldTransformOutgoingMessage(String originalText, int chatType) {
        if (originalText == null || originalText.trim().length() < OUTGOING_TRANSFORM_MIN_LENGTH) {
            return false;
        }
        if (!config.ApiConfig()) {
            return false;
        }
        if (chatType == CHATBOX_TYPE_CHEAT) {
            return false;
        }
        RuneLingualConfig.chatSelfConfig outgoingMode = getMyOutgoingConfig(chatType);
        return outgoingMode == RuneLingualConfig.chatSelfConfig.TRANSFORM
                || outgoingMode == RuneLingualConfig.chatSelfConfig.USE_API;
    }

    private RuneLingualConfig.chatSelfConfig getMyOutgoingConfig(int chatType) {
        switch (chatType) {
            case CHATBOX_TYPE_PUBLIC:
                return config.getMyPublicConfig();
            case CHATBOX_TYPE_FRIENDS:
                return config.getMyFcConfig();
            case CHATBOX_TYPE_CLAN:
                return config.getMyClanConfig();
            case CHATBOX_TYPE_GUEST_CLAN:
                return config.getMyGuestClanConfig();
            case CHATBOX_TYPE_GIM:
                return config.getMyGIMConfig();
            default:
                return RuneLingualConfig.chatSelfConfig.LEAVE_AS_IS;
        }
    }

    private ChatMessageType getExpectedOutgoingType(int chatType) {
        switch (chatType) {
            case CHATBOX_TYPE_PUBLIC:
                return ChatMessageType.PUBLICCHAT;
            case CHATBOX_TYPE_FRIENDS:
                return ChatMessageType.FRIENDSCHAT;
            case CHATBOX_TYPE_CLAN:
                return ChatMessageType.CLAN_CHAT;
            case CHATBOX_TYPE_GUEST_CLAN:
                return ChatMessageType.CLAN_GUEST_CHAT;
            case CHATBOX_TYPE_GIM:
                return ChatMessageType.CLAN_GIM_CHAT;
            default:
                return null;
        }
    }

    private void cleanupExpiredOutgoingMessages(long now) {
        while (true) {
            OutgoingRestoreMessage head = outgoingMessagesToRestore.peekFirst();
            if (head == null || head.expiresAt >= now) {
                return;
            }
            outgoingMessagesToRestore.pollFirst();
        }
    }

    private boolean isOwnMessage(ChatMessage chatMessage) {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null || localPlayer.getName() == null || chatMessage.getName() == null) {
            return false;
        }
        String localPlayerName = Colors.removeAllTags(localPlayer.getName());
        String senderName = Colors.removeAllTags(chatMessage.getName());
        return Objects.equals(localPlayerName, senderName);
    }

    private void queueUpdateAllOverrides()
    {
        clientThread.invoke(() -> {
            // Cross sprites and widget sprite cache are not setup until login screen
            if (client.getGameState().getState() < GameState.LOGIN_SCREEN.getState()) {
                return false;
            }
            updateAllOverrides();
            return true;
        });
    }

    private void updateAllOverrides() {
        spriteReplacer.initMap();
        spriteReplacer.replaceWidgetSprite();
    }

    @Override
    protected void shutDown() throws Exception {
        unregisterEventSubscriptions();
        if (outgoingTranslationExecutor != null) {
            outgoingTranslationExecutor.shutdownNow();
            outgoingTranslationExecutor = null;
        }
        outgoingMessagesToRestore.clear();

        clientToolBar.removeNavigation(navButton);
        overlayManager.remove(mouseTooltipOverlay);
        overlayManager.remove(deeplUsageOverlay);
        overlayManager.remove(chatInputOverlay);
        overlayManager.remove(chatInputCandidateOverlay);
        overlayManager.remove(menuEntryHighlightOverlay);
        h2Manager.closeConn();
        spriteReplacer.resetWidgetSprite();
    }


    @Provides
    RuneLingualConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RuneLingualConfig.class);
    }

    private void initLangFiles() {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            return;
        }
        //download necessary files
        downloader.setLangCode(targetLanguage.getLangCode());
        downloader.initDownloader();
    }

    public void restartPanel() {
        //update Language named folder (which is used to determine what language is selected)
        FileActions.deleteAllLangCodeNamedFile();
        FileActions.createLangCodeNamedFile(config.getSelectedLanguage());
        clientToolBar.removeNavigation(navButton);
        startPanel();
    }

    private void startPanel() {
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "globe.png");
        //panel.setTargetLanguage(config.getSelectedLanguage());
        panel = injector.getInstance(SidePanel.class);

        navButton = NavigationButton.builder()
                .tooltip("RuneLingual")
                .icon(icon)
                .priority(6)
                .panel(panel)
                .build();
        clientToolBar.addNavigation(navButton);
    }

    TileObject findTileObject(int x, int y, int id) {
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        Tile tile = tiles[client.getPlane()][x][y];
        if (tile != null) {
            for (GameObject gameObject : tile.getGameObjects()) {
                if (gameObject != null && gameObject.getId() == id) {
                    return gameObject;
                }
            }

            WallObject wallObject = tile.getWallObject();
            if (wallObject != null && wallObject.getId() == id) {
                return wallObject;
            }

            DecorativeObject decorativeObject = tile.getDecorativeObject();
            if (decorativeObject != null && decorativeObject.getId() == id) {
                return decorativeObject;
            }

            GroundObject groundObject = tile.getGroundObject();
            if (groundObject != null && groundObject.getId() == id) {
                return groundObject;
            }
        }
        return null;
    }

    @Nullable
    Actor getInteractedTarget() {
        return interactedNpc != null ? interactedNpc : client.getLocalPlayer().getInteracting();
    }

}
