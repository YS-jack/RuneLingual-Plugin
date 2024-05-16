package com.RuneLingual;

import net.runelite.client.RuneLite;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.io.File;

@ConfigGroup(RuneLingualConfig.GROUP)
public interface RuneLingualConfig extends Config
{
	String GROUP = "lingualConfig";
	@ConfigSection(
			name = "Dynamic translating",
			description = "Online translation options",
			position = 1,
			closedByDefault = false
	)
	String SECTION_CHAT_SETTINGS = "chatSettings";

	@ConfigItem(
		name = "Service API Key",
		description = "Your API key for the chosen translating service",
		section = "chatSettings",
		keyName = "APIKey",
		position = 1
	)
	String APIKey();

	@ConfigItem(
			name = "Translating service",
			description = "Select your preferred translation service",
			section = "chatSettings",
			keyName = "translatingService",
			position = 2
	)
	default TranslatingServiceSelectableList getService() {return TranslatingServiceSelectableList.GOOGLE_TRANSLATE;}

	default String getAPIKey() {return APIKey();}
	@ConfigItem(
			name = "Enable dynamic translating",
			description = "Mostly for player messages",
			section = "chatSettings",
			keyName = "enableAPI",
			position = 2
	)
	default boolean allowAPI() {return false;}

	@ConfigSection(
		name = "General translation settings",
		description = "General translation settings",
		position = 2,
		closedByDefault = false
	)
	String SECTION_GENERAL_SETTINGS = "generalSettings";

	@ConfigItem(
		name = "Target language",
		description = "Select the language to be translated to",
		section = "generalSettings",
		keyName = "targetLang",
		position = 1
	)
	default LangCodeSelectableList presetLang() {return LangCodeSelectableList.PORTUGUÊS_BRASILEIRO;}

	@ConfigItem(
			name = "Public chat",
			description = "Requires a valid API key",
			section = "generalSettings",
			keyName = "translatePublic",
			position = 2
	)
	default boolean getAllowPublic() {return false;}
	
	@ConfigItem(
			name = "Friends chat",
			description = "Requires a valid API key",
			section = "generalSettings",
			keyName = "translateFriends",
			position = 3
	)
	default boolean getAllowFriends() {return false;}
	
	@ConfigItem(
			name = "Clan chats/channels",
			description = "Requires a valid API key",
			section = "generalSettings",
			keyName = "translateClan",
			position = 4
	)
	default boolean getAllowClan() {return false;}
	
	@ConfigItem(
			name = "Your own chat messages",
			description = "With this other players should receive your messages in english",
			section = "generalSettings",
			keyName = "allowLocalPlayerTranslate",
			position = 5
	)
	default boolean getAllowLocal() {return false;}

	@ConfigItem(
			name = "Game messages",
			description = "Translate all game messages",
			section = "generalSettings",
			keyName = "allowGameTranslate",
			position = 6
	)
	default boolean getAllowGame() {return true;}
	
	@ConfigItem(
			name = "Game spam messages",
			description = "Translate all spammy game messages",
			section = "generalSettings",
			keyName = "allowSpamTranslate",
			position = 7
	)
	default boolean getAllowSpam() {return true;}
	@ConfigItem(
			name = "Item names",
			description = "Change item names",
			section = "generalSettings",
			keyName = "allowItemTranslate",
			position = 8
	)
	default boolean getAllowItems() {return true;}
	
	@ConfigItem(
			name = "Change NPC names",
			description = "Translate NPC names",
			section = "generalSettings",
			keyName = "allowNameTranslate",
			position = 9
	)
	default boolean getAllowName() {return true;}
	
	@ConfigItem(
			name = "Overhead messages",
			description = "Translate overhead messages",
			section = "generalSettings",
			keyName = "allowOverHeadTranslate",
			position = 7
	)
	default boolean getAllowOverHead() {return false;}

	@ConfigItem(
			name = "Local file location",
			description = "Location of the files to be translated",
			keyName = "fileLocation",
			position = 200
	)
	default String getFileLocation() {return RuneLite.RUNELITE_DIR.getPath() + File.separator + "RuneLingual_resources";}


}
