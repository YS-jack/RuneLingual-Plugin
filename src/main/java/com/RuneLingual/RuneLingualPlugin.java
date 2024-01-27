package com.RuneLingual;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.VarClientInt;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigClient;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import lombok.Getter;
import lombok.Setter;

@Slf4j
@PluginDescriptor(
	// Plugin name shown at plugin hub
	name = "RuneLingual",
	description = "All-in-one translation plugin for OSRS."
)

public class RuneLingualPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private RuneLingualConfig config;

	private LangCodeSelectableList targetLanguage;
	private TranscriptsFileManager dialogTranscriptManager = new TranscriptsFileManager();
	private TranscriptsFileManager actionTranscriptManager = new TranscriptsFileManager();
	private TranscriptsFileManager objectTranscriptManager = new TranscriptsFileManager();
	private TranscriptsFileManager itemTranscriptManager = new TranscriptsFileManager();
	
	// main modules
	@Inject
	private ChatCapture chatTranslator;
	@Inject
	private DialogCapture dialogTranslator;
	@Inject
	private MenuCapture menuTranslator;
	@Inject
	private GroundItems groundItemsTranslator;
	@Inject
	private MenuBar menuBar;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Starting...");

		targetLanguage = config.presetLang();
		log.info(targetLanguage.getCode());
		
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
		
		log.info("RuneLingual started!");
	}
	
	@Subscribe
	private void onBeforeRender(BeforeRender event)
	{
		// this should be done on the onWidgetLoaded event
		// but something seems to change the contents right back
		// somewhere before the rendering process actually happens
		// so having this happen every game tick instead
		// of every client tick is actually less resource intensive
		dialogTranslator.handleDialogs();
		
		int currentHudTab = client.getVarcIntValue(VarClientInt.INVENTORY_TAB);
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
		groundItemsTranslator.handleGroundItems();
	}
	
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		menuTranslator.handleMenuEvent(event);
	}
	
	@Subscribe
	public void onChatMessage(ChatMessage event) throws Exception
	{
		chatTranslator.handleChatMessage(event);
	}
	
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		//transcriptManager.saveTranscript();
	}
	
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
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
		dialogTranscriptManager.loadTranscripts();
		actionTranscriptManager.loadTranscripts();
		objectTranscriptManager.loadTranscripts();
		itemTranscriptManager.loadTranscripts();
	}

	@Provides
	RuneLingualConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneLingualConfig.class);
	}
}

