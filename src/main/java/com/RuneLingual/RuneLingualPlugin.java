package com.RuneLingual;

import com.RuneLingual.MouseOverlays.MouseTooltipOverlay;
import com.RuneLingual.SQL.SqlActions;
import com.RuneLingual.SQL.SqlQuery;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
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
import com.RuneLingual.nonLatinChar.CharImageInit;
import com.RuneLingual.nonLatinChar.GeneralFunctions;
import com.RuneLingual.commonFunctions.Ids;

import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
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
	@Inject
	private ChatCapture chatTranslator;
	@Inject
	private DialogCapture dialogTranslator;
	@Inject @Getter
	private MenuCapture menuTranslator;
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

	@Override
	protected void startUp() throws Exception
	{
		log.info("Starting...");
		//get selected language
		targetLanguage = config.getSelectedLanguage();
		// check if online files have changed, if so download and update local files
		initLangFiles();
		// set database URL
		databaseUrl = "jdbc:h2:" + FileNameAndPath.getLocalBaseFolder() + File.separator + targetLanguage.getCode() + File.separator + FileNameAndPath.getLocalSQLFileName();
		//connect to database
		conn = DriverManager.getConnection(databaseUrl);

		// initiate overlays
		overlayManager.add(mouseTooltipOverlay);


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
		chatTranslator.setLogger(this::pluginLog);
		chatTranslator.setOriginalDialog(dialogTranscriptManager.originalTranscript);
		chatTranslator.setTranslatedDialog(dialogTranscriptManager.translatedTranscript);
		//chatTranslator.setOnlineTranslator(this::temporaryTranslator);
		
		menuTranslator.setLogger(this::pluginLog);
		menuTranslator.setActionTranslator(actionTranscriptManager.translatedTranscript);
		menuTranslator.setNpcTranslator(dialogTranscriptManager.translatedTranscript);
		menuTranslator.setObjectTranslator(objectTranscriptManager.translatedTranscript);
		menuTranslator.setItemTranslator(itemTranscriptManager.translatedTranscript);
		// old code ends here (for this method)
		log.info("RuneLingual started!");
	}
	
	@Subscribe
	private void onBeforeRender(BeforeRender event)
	{
		if (targetLanguage == LangCodeSelectableList.ENGLISH) {
			return;
		}

		// this should be done on the onWidgetLoaded event
		// but something seems to change the contents right back
		// somewhere before the rendering process actually happens
		// so having this happen every game tick instead
		// of every client tick is actually less resource intensive
		dialogTranslator.handleDialogs();
		for (Widget widgetRoot : client.getWidgetRoots()) {
			MenuCapture.remapWidget(widgetRoot);
		}
		int currentHudTab = -1; // client.getVarcIntValue(VarClientInt.INVENTORY_TAB);
		switch(currentHudTab)
		{
			case 0:
			{
				System.out.println("combat opt");
				break;
			}
			case 1:
			{
				System.out.println("skills");
				break;
			}
			case 2:
			{
				System.out.println("quest");
				menuBar.handleQuestMenuTab();
				break;
			}
			case 3:
			{
				System.out.println("inv");
				break;
			}
			default:
			{
				break;
			}
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
		menuTranslator.handleOpenedMenu(event);
	}
	
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (targetLanguage == LangCodeSelectableList.ENGLISH) {
			return;
		}

//		MenuEntry[] ev = client.getMenuEntries();
//		for (MenuEntry e: ev ){
//			e.setOption(generalFunctions.StringToTags("蘇生　贖罪", Colors.fromName("black")));
//		}
//		menuTranslator.handleMenuEvent(event);
	}
	
	@Subscribe
	public void onChatMessage(ChatMessage event) throws Exception
	{
		if (targetLanguage == LangCodeSelectableList.ENGLISH) {
			return;
		}
		chatTranslator.handleChatMessage(event);
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
		if(targetLanguage != config.getSelectedLanguage()){ // if language is changed
			targetLanguage = config.getSelectedLanguage();
			initLangFiles();
			// todo: change the database URL and the connection to it
			databaseUrl = "jdbc:h2:" + FileNameAndPath.getLocalBaseFolder() + File.separator + targetLanguage.getCode() + File.separator + FileNameAndPath.getLocalSQLFileName();
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


			restartPanel();
		}

		// below are some old code


		if(dialogTranscriptManager != null)
		{
			if(dialogTranscriptManager.isChanged())
			{
				dialogTranscriptManager.saveOriginalTranscript();
			}
		}
		
		if(chatTranslator != null)
		{
			chatTranslator.updateConfigs();
		}

	}
	
	@Override
	protected void shutDown() throws Exception
	{
		clientToolBar.removeNavigation(navButton);
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
		dialogTranscriptManager.loadTranscripts(targetLanguage.getCode());
		actionTranscriptManager.loadTranscripts(targetLanguage.getCode());
		objectTranscriptManager.loadTranscripts(targetLanguage.getCode());
		itemTranscriptManager.loadTranscripts(targetLanguage.getCode());
	}


	@Provides
	RuneLingualConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneLingualConfig.class);
	}

	private boolean initLangFiles(){
		//download necessary files
		downloader.setLangCode(targetLanguage.getCode());
        return downloader.initDownloader(targetLanguage.getCode());
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

