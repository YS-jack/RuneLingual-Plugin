package com.RuneLingual;


import net.runelite.client.RuneLite;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import org.checkerframework.checker.units.qual.C;

import java.io.*;

@ConfigGroup(RuneLingualConfig.GROUP)
public interface RuneLingualConfig extends Config
{
	public String helpLink = "https://github.com/YS-jack/Runelingual-Transcripts/tree/original-main/draft/jp"; // todo: change this to the correct link
	@ConfigSection(
			name = "Basic settings",
			description = "Select language",
			position = 0,
			closedByDefault = false
	)
	String SECTION_BASIC_SETTINGS = "basicSettings";
    @ConfigItem(
			name = "\uD83D\uDDE3\uD83D\uDCAC\uD83C\uDF10",
			description = "Select language",
			position = 0,
			keyName = "languageSelection",
			section = SECTION_BASIC_SETTINGS
	) default LangCodeSelectableList getSelectedLanguage() {return LangCodeSelectableList.ENGLISH;}

	@ConfigItem(
			name = "info (right click to reset)",
			description = "right click to reset",
			position = 1,
			keyName = "enableRuneLingual",
			section = SECTION_BASIC_SETTINGS
	)
	default String getHelpLink() {return helpLink;}

	final int offset = 100;
	String GROUP = "lingualConfig";
	@ConfigSection(
			name = "Dynamic translating",
			description = "Online translation options",
			position = 1 + offset,
			closedByDefault = false
	)
	String SECTION_CHAT_SETTINGS = "chatSettings";

	@ConfigItem(
		name = "Service API Key",
		description = "Your API key for the chosen translating service",
		section = "chatSettings",
		keyName = "APIKey",
		position = 1 + offset
	)
	String APIKey();

	@ConfigItem(
			name = "Translating service",
			description = "Select your preferred translation service",
			section = "chatSettings",
			keyName = "translatingService",
			position = 2 + offset
	)
	default TranslatingServiceSelectableList getService() {return TranslatingServiceSelectableList.GOOGLE_TRANSLATE;}

	default String getAPIKey() {return APIKey();}
	@ConfigItem(
			name = "Enable dynamic translating",
			description = "Mostly for player messages",
			section = "chatSettings",
			keyName = "enableAPI",
			position = 2 + offset
	)
	default boolean allowAPI() {return false;}

	@ConfigSection(
		name = "General translation settings",
		description = "General translation settings",
		position = 2 + offset,
		closedByDefault = false
	)
	String SECTION_GENERAL_SETTINGS = "generalSettings";

	@ConfigItem(
		name = "Target language",
		description = "Select the language to be translated to",
		section = "generalSettings",
		keyName = "targetLang",
		position = 1 + offset
	)
	default LangCodeSelectableList presetLang() {return LangCodeSelectableList.PORTUGUÊS_BRASILEIRO;}

	@ConfigItem(
			name = "Public chat",
			description = "Requires a valid API key",
			section = "generalSettings",
			keyName = "translatePublic",
			position = 2 + offset
	)
	default boolean getAllowPublic() {return false;}

	@ConfigItem(
			name = "Friends chat",
			description = "Requires a valid API key",
			section = "generalSettings",
			keyName = "translateFriends",
			position = 3 + offset
	)
	default boolean getAllowFriends() {return false;}

	@ConfigItem(
			name = "Clan chats/channels",
			description = "Requires a valid API key",
			section = "generalSettings",
			keyName = "translateClan",
			position = 4 + offset
	)
	default boolean getAllowClan() {return false;}

	@ConfigItem(
			name = "Your own chat messages",
			description = "With this other players should receive your messages in english",
			section = "generalSettings",
			keyName = "allowLocalPlayerTranslate",
			position = 5 + offset
	)
	default boolean getAllowLocal() {return false;}

	@ConfigItem(
			name = "Game messages",
			description = "Translate all game messages",
			section = "generalSettings",
			keyName = "allowGameTranslate",
			position = 6 + offset
	)
	default boolean getAllowGame() {return true;}

	@ConfigItem(
			name = "Game spam messages",
			description = "Translate all spammy game messages",
			section = "generalSettings",
			keyName = "allowSpamTranslate",
			position = 7 + offset
	)
	default boolean getAllowSpam() {return true;}
	@ConfigItem(
			name = "Item names",
			description = "Change item names",
			section = "generalSettings",
			keyName = "allowItemTranslate",
			position = 8 + offset
	)
	default boolean getAllowItems() {return true;}

	@ConfigItem(
			name = "Change NPC names",
			description = "Translate NPC names",
			section = "generalSettings",
			keyName = "allowNameTranslate",
			position = 9 + offset
	)
	default boolean getAllowName() {return true;}

	@ConfigItem(
			name = "Overhead messages",
			description = "Translate overhead messages",
			section = "generalSettings",
			keyName = "allowOverHeadTranslate",
			position = 7 + offset
	)
	default boolean getAllowOverHead() {return false;}

	@ConfigItem(
			name = "Local file location",
			description = "Location of the files to be translated",
			keyName = "fileLocation",
			position = 200 + offset
	)
	default String getFileLocation() {return RuneLite.RUNELITE_DIR.getPath() + File.separator + "RuneLingual_resources";}


}
