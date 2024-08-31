package com.RuneLingual;


import net.runelite.client.RuneLite;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.io.*;

@ConfigGroup(RuneLingualConfig.GROUP)
public interface RuneLingualConfig extends Config
{
	final int offset_section1 = 0;
	public String helpLink = "https://github.com/YS-jack/Runelingual-Transcripts/tree/original-main/draft/jp"; // todo: change this to the correct link
	@ConfigSection(
			name = "Language selection",
			description = "Select language",
			position = offset_section1,
			closedByDefault = false
	)
	String SECTION_BASIC_SETTINGS = "basicSettings";

	@ConfigItem(
			name = "\uD83D\uDDE3\uD83D\uDCAC\uD83C\uDF10",
			description = "Select the language to be translated to",
			keyName = "targetLang",
			position = offset_section1,
			section = SECTION_BASIC_SETTINGS
	)
	default LangCodeSelectableList getSelectedLanguage() {return LangCodeSelectableList.ENGLISH;}

	@ConfigItem(
			name = "Help Link (right click to reset)",
			description = "right click to reset",
			position = 1 + offset_section1,
			keyName = "enableRuneLingual",
			section = SECTION_BASIC_SETTINGS
	)
	default String getHelpLink() {return helpLink;} // getHelpLink shouldnt be used anywhere, instead use helpLink







	int offset_section2 = 20;
	@ConfigSection(
			name = "Game system text",
			description = "Options for game system texts",
			position = offset_section2,
			closedByDefault = false
	)
	String SECTION_GAME_SYSTEM_TEXT = "gameSystemText";

	enum ingameTranslationConfig
	{
		USE_LOCAL_DATA,
		USE_API,
		TRANSLITERATE,
		DONT_TRANSLATE,
	}

	@ConfigItem(
			name = "NPC Dialogue",
			description = "Option for NPC Dialogues",
			position = 1 + offset_section2,
			keyName = "npcDialogue",
			section = SECTION_GAME_SYSTEM_TEXT
	)
	default ingameTranslationConfig getNpcDialogue() {return ingameTranslationConfig.USE_LOCAL_DATA;}

	@ConfigItem(
			name = "Game Messages",
			description = "Option for game messages",
			position = 2 + offset_section2,
			keyName = "gameMessages",
			section = SECTION_GAME_SYSTEM_TEXT
	)
	default ingameTranslationConfig getGameMessages() {return ingameTranslationConfig.USE_LOCAL_DATA;}

	@ConfigItem(
			name = "Item Names",
			description = "Option for item names",
			position = 4 + offset_section2,
			keyName = "itemNames",
			section = SECTION_GAME_SYSTEM_TEXT
	)
	default ingameTranslationConfig getItemNames() {return ingameTranslationConfig.USE_LOCAL_DATA;}

	@ConfigItem(
			name = "NPC Names",
			description = "Option for NPC names",
			position = 5 + offset_section2,
			keyName = "NPCNames",
			section = SECTION_GAME_SYSTEM_TEXT
	)
	default ingameTranslationConfig getNPCNames() {return ingameTranslationConfig.USE_LOCAL_DATA;}

	@ConfigItem(
			name = "Object Names",
			description = "Option for object names",
			position = 6 + offset_section2,
			keyName = "objectNames",
			section = SECTION_GAME_SYSTEM_TEXT
	)
	default ingameTranslationConfig getObjectNames() {return ingameTranslationConfig.USE_LOCAL_DATA;}

	@ConfigItem(
			name = "Interfaces",
			description = "Option for interface texts",
			position = 7 + offset_section2,
			keyName = "interfaceText",
			section = SECTION_GAME_SYSTEM_TEXT
	)
	default ingameTranslationConfig getInterfaceText() {return ingameTranslationConfig.USE_LOCAL_DATA;}

	@ConfigItem(
			name = "Mouse Hover Text",
			description = "Option for texts next to the mouse",
			position = 8 + offset_section2,
			keyName = "mouseText",
			section = SECTION_GAME_SYSTEM_TEXT
	)
	default ingameTranslationConfig getMouseText() {return ingameTranslationConfig.USE_LOCAL_DATA;}

	@ConfigItem(
			name = "Enable Mouse Hover Text",
			description = "Option to toggle mouse hover texts",
			position = 9 + offset_section2,
			keyName = "overheadText",
			section = SECTION_GAME_SYSTEM_TEXT
	)
	default boolean getMouseHover() {return true;}






	enum chatConfig
	{
		TRANSFORM, //eg: watasi ha inu ga suki -> 私は犬が好き
		USE_API, // eg: I like dogs -> 私は犬が好き
		DONT_TRANSLATE, // eg: I like dogs -> I like dogs
	}
	enum chatSelfConfig
	{
		TRANSFORM,
		DONT_TRANSLATE,
	}

	final int offset_section3 = 40;
	@ConfigSection(
			name = "Chat messages",
			description = "Options for chat messages",
			position = offset_section3,
			closedByDefault = false
	)
	String SECTION_CHAT_MESSAGES = "chatMessages";

	@ConfigItem(
			name = "My Messages",
			description = "Option for your own messages",
			position = 1 + offset_section3,
			keyName = "myChat",
			section = SECTION_CHAT_MESSAGES
	)
	default chatSelfConfig getMyChat() {return chatSelfConfig.TRANSFORM;}

	@ConfigItem(
			name = "All Friends",
			description = "Option that applies to all friends",
			position = 2 + offset_section3,
			keyName = "allFriends",
			section = SECTION_CHAT_MESSAGES
	)
	default chatConfig getAllFriends() {return chatConfig.DONT_TRANSLATE;}

	@ConfigItem(
			name = "Public",
			description = "Option for public chat messages",
			position = 3 + offset_section3,
			keyName = "publicChat",
			section = SECTION_CHAT_MESSAGES
	)
	default chatConfig getPublicChat() {return chatConfig.DONT_TRANSLATE;}

	@ConfigItem(
			name = "Clan",
			description = "Option for clan chat messages",
			position = 4 + offset_section3,
			keyName = "clanChat",
			section = SECTION_CHAT_MESSAGES
	)
	default chatConfig getClanChat() {return chatConfig.DONT_TRANSLATE;}

	@ConfigItem(
			name = "Guest Clan",
			description = "Option for guest clan chat messages",
			position = 5 + offset_section3,
			keyName = "guestClanChat",
			section = SECTION_CHAT_MESSAGES
	)
	default chatConfig getGuestClanChat() {return chatConfig.DONT_TRANSLATE;}

	@ConfigItem(
			name = "Friends Chat",
			description = "Option for friends chat messages",
			position = 6 + offset_section3,
			keyName = "friendsChat",
			section = SECTION_CHAT_MESSAGES
	)
	default chatConfig getFriendsChat() {return chatConfig.DONT_TRANSLATE;}

	@ConfigItem(
			name = "GIM Group",
			description = "Option for GIM group chat messages",
			position = 7 + offset_section3,
			keyName = "GIMChat",
			section = SECTION_CHAT_MESSAGES
	)
	default chatConfig getGIMChat() {return chatConfig.DONT_TRANSLATE;}






	final int offset_section4 = 60;
	@ConfigSection(
			name = "Specific Player Settings",
			description = "Options for specific players",
			position = offset_section4,
			closedByDefault = false
	)
	String SECTION_SPECIFIC_PLAYER_SETTINGS = "specificPlayerSettings";

	@ConfigItem(
			name = "Don't translate",
			description = "Specific players to not translate",
			position = 1 + offset_section4,
			keyName = "specificDontTranslate",
			section = SECTION_SPECIFIC_PLAYER_SETTINGS
	)
	default String getSpecificDontTranslate() {return "enter player names here, separated by commas";}

	@ConfigItem(
			name = "Translate with APIs",
			description = "Specific players to translate using online translators",
			position = 2 + offset_section4,
			keyName = "specificApiTranslate",
			section = SECTION_SPECIFIC_PLAYER_SETTINGS
	)
	default String getSpecificApiTranslate() {return "enter player names here, separated by commas";}

	@ConfigItem(
			name = "Transform",
			description = "Specific players to transform",
			position = 3 + offset_section4,
			keyName = "specificTransform",
			section = SECTION_SPECIFIC_PLAYER_SETTINGS
	)
	default String getSpecificTransform() {return "enter player names here, separated by commas";}






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
			name = "Enable Online Translation",
			description = "whether to translate using online services",
			section = SECTION_CHAT_SETTINGS,
			keyName = "enableAPI",
			position = 2 + offset
	)
	default boolean allowAPI() {return false;}

	@ConfigItem(
			name = "Translating service",
			description = "Select your preferred translation service",
			section = SECTION_CHAT_SETTINGS,
			keyName = "translatingService",
			position = 3 + offset
	)
	default TranslatingServiceSelectableList getService() {return TranslatingServiceSelectableList.DeepL;}

	@ConfigItem(
			name = "Service API Key",
			description = "Your API key for the chosen translating service",
			section = SECTION_CHAT_SETTINGS,
			keyName = "APIKey",
			position = 4 + offset,
			secret = true
			//hidden = true
	)
	default String getAPIKey() {return "";}













	int offset_old_2 = 120;
	@ConfigSection(
		name = "General translation settings",
		description = "General translation settings",
		position = 2 + offset_old_2,
		closedByDefault = false
	)
	String SECTION_GENERAL_SETTINGS = "generalSettings";


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
