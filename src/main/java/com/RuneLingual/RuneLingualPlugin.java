package com.RuneLingual;

import com.RuneLingual.ApiTranslate.*;
import com.RuneLingual.ChatMessages.*;
import com.RuneLingual.MouseOverlays.MouseTooltipOverlay;
import com.RuneLingual.SQL.SqlActions;
import com.RuneLingual.SQL.SqlQuery;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.RuneLingual.nonLatin.*;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.game.ChatIconManager;


import lombok.Getter;

import com.RuneLingual.SidePanelComponents.SidePanel;
import com.RuneLingual.commonFunctions.FileActions;
import com.RuneLingual.prepareResources.Downloader;
import com.RuneLingual.commonFunctions.Ids;


import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
@PluginDescriptor(
	// Plugin name shown at plugin hub
	name = "RuneLingual",
	description = "All-in-one translation plugin for OSRS."
)

public class RuneLingualPlugin extends Plugin
{
	@Inject @Getter
	private Client client;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ClientToolbar clientToolBar;
	@Inject @Getter
	private ChatIconManager chatIconManager;
	@Getter
	private HashMap<String, Integer> charIds = new HashMap<>();    // colour-char(key) <-> CharIds(val)

	@Inject @Getter
	private RuneLingualConfig config;
	@Inject
	private CharImageInit charImageInit;

	@Getter
	private LangCodeSelectableList targetLanguage;
	@Getter
	private String selectedLanguageName;
	private TranscriptsFileManager dialogTranscriptManager = new TranscriptsFileManager();
	private TranscriptsFileManager actionTranscriptManager = new TranscriptsFileManager();
	private TranscriptsFileManager objectTranscriptManager = new TranscriptsFileManager();
	private TranscriptsFileManager itemTranscriptManager = new TranscriptsFileManager();
	
	// main modules
	@Inject @Getter
	private ChatCapture chatCapture;
	@Inject
	private DialogCapture dialogTranslator;
	@Inject @Getter
	private MenuCapture menuCapture;
	@Inject
	private GroundItems groundItemsTranslator;
	@Inject
	private MenuBar menuBar;

	@Inject @Getter
	private Downloader downloader;
	@Inject
	private SidePanel panel;
	private NavigationButton navButton;
	@Inject @Getter
	private GeneralFunctions generalFunctions;
	@Inject @Getter
	private FileNameAndPath fileNameAndPath = new FileNameAndPath();
	@Inject @Getter
	private SqlActions sqlActions;
	@Inject
	private SqlQuery sqlQuery;
	@Getter @Setter
	private String[] tsvFileNames;
	@Getter
	private String databaseUrl;
	@Getter @Setter
	private Connection conn;
	@Inject @Getter
	private Ids ids;
	@Inject
	private MouseTooltipOverlay mouseTooltipOverlay;
	@Inject @Getter
	private Deepl deepl;
	@Inject
	private DeeplUsageOverlay deeplUsageOverlay;
	@Inject @Getter
	private ChatInputRLingual chatInputRLingual;
	@Inject @Getter
	private ChatInputOverlay chatInputOverlay;
	@Inject
	private ChatInputCandidateOverlay chatInputCandidateOverlay;
	@Inject
	private OverheadCapture overheadCapture;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Starting...");
		//get selected language
		targetLanguage = config.getSelectedLanguage();
		// set database URL
		databaseUrl = "jdbc:h2:" + FileNameAndPath.getLocalBaseFolder() + File.separator + targetLanguage.getLangCode()
				+ File.separator + FileNameAndPath.getLocalSQLFileName();

		// check if online files have changed, if so download and update local files
		initLangFiles();

		//connect to database
		conn = DriverManager.getConnection(databaseUrl);

		// initiate overlays
		overlayManager.add(mouseTooltipOverlay);
		overlayManager.add(deeplUsageOverlay);
		overlayManager.add(chatInputOverlay);
		overlayManager.add(chatInputCandidateOverlay);


		// load image files
		charImageInit.loadCharImages();

		// side panel
		startPanel();


		//from here its old code
		// initializes transcript modules
		initTranscripts();
		loadTranscripts();
		
		// main dialog widget manager
		dialogTranslator.setLogger(this::pluginLog);
		dialogTranslator.setOriginalDialog(dialogTranscriptManager.originalTranscript);
		dialogTranslator.setTranslatedDialog(dialogTranscriptManager.translatedTranscript);
		
		// chat translator handles game messages, contained also by the dialog transcript
		chatCapture.setLogger(this::pluginLog);
		chatCapture.setOriginalDialog(dialogTranscriptManager.originalTranscript);
		chatCapture.setTranslatedDialog(dialogTranscriptManager.translatedTranscript);
		//chatTranslator.setOnlineTranslator(this::temporaryTranslator);
		
//		menuCapture.setLogger(this::pluginLog);
//		menuCapture.setActionTranslator(actionTranscriptManager.translatedTranscript);
//		menuCapture.setNpcTranslator(dialogTranscriptManager.translatedTranscript);
//		menuCapture.setObjectTranslator(objectTranscriptManager.translatedTranscript);
//		menuCapture.setItemTranslator(itemTranscriptManager.translatedTranscript);
		// old code ends here (for this method)
		log.info("RuneLingual started!");
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event) throws Exception {
		overheadCapture.translateOverhead(event);
	}

	
	@Subscribe
	private void onBeforeRender(BeforeRender event)
	{
		if (targetLanguage == LangCodeSelectableList.ENGLISH) {
			return;
		}

		chatInputRLingual.updateChatInput();

		// this should be done on the onWidgetLoaded event
		// but something seems to change the contents right back
		// somewhere before the rendering process actually happens
		// so having this happen every game tick instead
		// of every client tick is actually less resource intensive



		// old code
		dialogTranslator.handleDialogs();
		for (Widget widgetRoot : client.getWidgetRoots()) {
			MenuCapture.remapWidget(widgetRoot);
		}

	}
	
	@Subscribe
	public void onItemQuantityChanged(ItemQuantityChanged itemQuantityChanged)
	{
		if (targetLanguage == LangCodeSelectableList.ENGLISH) {
			return;
		}
		groundItemsTranslator.handleGroundItems();
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{

//		MenuEntry[] ev = client.getMenuEntries();
//		for (MenuEntry e: ev ){
//			e.setOption(generalFunctions.StringToTags(testString, Colors.fromName("black")));
//		}

		menuCapture.handleOpenedMenu(event);
	}
	
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (targetLanguage == LangCodeSelectableList.ENGLISH) {
			return;
		}
		//menuTranslator.handleMenuEvent(event);
	}
	
	@Subscribe
	public void onChatMessage(ChatMessage event) throws Exception
	{
		if (targetLanguage == LangCodeSelectableList.ENGLISH) {
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN && client.getGameState() != GameState.HOPPING) {
			return;
		}
		chatCapture.handleChatMessage(event);
	}
	
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (targetLanguage == LangCodeSelectableList.ENGLISH) {
			return;
		}
		//transcriptManager.saveTranscript();
	}
	
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// if language is changed
		if(targetLanguage != config.getSelectedLanguage()){
			targetLanguage = config.getSelectedLanguage();
			initLangFiles();
			// todo: change the database URL and the connection to it
			databaseUrl = "jdbc:h2:" + FileNameAndPath.getLocalBaseFolder() + File.separator +
					targetLanguage.getLangCode() + File.separator + FileNameAndPath.getLocalSQLFileName();
			try {
				conn = DriverManager.getConnection(databaseUrl);
			} catch (Exception e){
				log.error("Error connecting to database: " + databaseUrl);
				targetLanguage = LangCodeSelectableList.ENGLISH;
				e.printStackTrace();
			}
			// download language files and structure language data
			clientToolBar.removeNavigation(navButton);
			boolean charImageChanged = initLangFiles();
			if(charImageChanged){
				charImageInit.loadCharImages();
			}

			// reset language specific variables

			overlayManager.remove(mouseTooltipOverlay);
			MouseTooltipOverlay.setAttemptedTranslation(new ArrayList<>());
			overlayManager.add(mouseTooltipOverlay);

			//reset deepl's past translations
			deepl = new Deepl(this);

			restartPanel();
		}

		// need this
		if(chatCapture != null)
		{
			chatCapture.updateConfigs();
		}

	}
	
	@Override
	protected void shutDown() throws Exception
	{
		clientToolBar.removeNavigation(navButton);
		overlayManager.remove(mouseTooltipOverlay);
		overlayManager.remove(deeplUsageOverlay);
		overlayManager.remove(chatInputOverlay);
		overlayManager.remove(chatInputCandidateOverlay);
		//transcriptManager.saveTranscript();
		log.info("RuneLingual plugin stopped!");
	}
	
	public void pluginLog(String contents)
	{
		log.info(contents);
	}
	
	private void initTranscripts()
	{
		dialogTranscriptManager.setLogger(this::pluginLog);
		dialogTranscriptManager.setFilePrefix("npc_dialog");
		actionTranscriptManager.setLogger(this::pluginLog);
		actionTranscriptManager.setFilePrefix("actions");
		objectTranscriptManager.setLogger(this::pluginLog);
		objectTranscriptManager.setFilePrefix("objects");
		itemTranscriptManager.setLogger(this::pluginLog);
		itemTranscriptManager.setFilePrefix("items");
	}
	
	private void loadTranscripts()
	{
		dialogTranscriptManager.loadTranscripts(targetLanguage.getLangCode());
		actionTranscriptManager.loadTranscripts(targetLanguage.getLangCode());
		objectTranscriptManager.loadTranscripts(targetLanguage.getLangCode());
		itemTranscriptManager.loadTranscripts(targetLanguage.getLangCode());
	}


	@Provides
	RuneLingualConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneLingualConfig.class);
	}

	private boolean initLangFiles(){
		//download necessary files
		downloader.setLangCode(targetLanguage.getLangCode());
        return downloader.initDownloader(targetLanguage.getLangCode());
	}

	public void restartPanel(){
		//update Language named folder (which is used to determine what language is selected)
		FileActions.deleteAllLangCodeNamedFile();
		FileActions.createLangCodeNamedFile(config.getSelectedLanguage());
		clientToolBar.removeNavigation(navButton);
		startPanel();
	}

	private void startPanel(){
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

}

