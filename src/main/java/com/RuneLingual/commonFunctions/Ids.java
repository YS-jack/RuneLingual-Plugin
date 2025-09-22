package com.RuneLingual.commonFunctions;

import com.RuneLingual.Widgets.PartialTranslationManager;
import com.RuneLingual.Widgets.Widget2ModDict;
import lombok.Getter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID.*;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;

import com.RuneLingual.RuneLingualPlugin;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.RuneLingual.Widgets.PartialTranslationManager.PlaceholderType.*;

@Getter @Slf4j
public class Ids {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    Client client;
    @Getter
    private Widget2ModDict widget2ModDict;
    @Getter
    private PartialTranslationManager partialTranslationManager;

    @Inject
    public Ids(RuneLingualPlugin plugin, Widget2ModDict widget2ModDict, PartialTranslationManager partialTranslationManager) {
        this.plugin = plugin;
        this.client = plugin.getClient();
        this.widget2ModDict = widget2ModDict;
        this.partialTranslationManager = partialTranslationManager;
        initWidget2ModDict();
        initPartialTranslations();
    }

    // Ids of widgets
    // main tabs
    private final Set<Integer> widgetIdSetMainTabs = Set.of(ToplevelPreEoc.SIDE_PANELS, ToplevelOsrsStretch.SIDE_PANELS, Toplevel.SIDE_PANELS);
    private final int widgetIdAttackStyleTab = CombatInterface.UNIVERSE;
    private final int widgetIdSkillsTab = Stats.UNIVERSE;
    private final int widgetIdCharacterSummaryTab = AccountSummarySidepanel.SUMMARY_CONTENTS;
    private final int widgetIdQuestTab = Questlist.UNIVERSE;
    private final int widgetIdAchievementDiaryTab = AreaTask.TASKBOX;
    private final int widgetIdInventoryTab = Inventory.ITEMS;
    private final int widgetIdEquipmentTab = Wornitems.UNIVERSE;
    private final int widgetIdPrayerTab = Prayerbook.UNIVERSE;
    private final int widgetIdSpellBookTab = MagicSpellbook.UNIVERSE;
    private final int widgetIdGroupsTab = SideChannelsLarge.UNIVERSE;
    private final int widgetIdGroupTabNonGIM = 46333952;
    private final int widgetIdPvPArena = PvpArenaSidepanel.UNIVERSE;
    private final int widgetIdFriendsTab = Friends.UNIVERSE;
    private final int widgetIdIgnoreTab = Ignore.UNIVERSE;
    private final int widgetIdAccountManagementTab = Account.CONTENT;
    private final int widgetIdSettingsTab = SettingsSide.UNIVERSE;
    private final int widgetIdEmotesTab = Emote.UNIVERSE;
    private final int widgetIdMusicTab = Music.UNIVERSE;
    private final int widgetIdLogoutTab = Logout.UNIVERSE;
    private final int widgetIdWorldSwitcherTab = Worldswitcher.UNIVERSE;

    // dont translate at all, except menu option
    private final int widgetIdCharacterSummaryName = AccountSummarySidepanel.SUMMARY_PLAYER_NAME;
    private final int widgetIdGimGroupName = GimSidepanel.TITLE;
    private final int widgetIdClockGIM = SideChannelsLarge.CLOCK;
    private final int widgetIdClockNonGIM = SideChannels.CLOCK;
    private final int widgetIdMusicCurrent = Music.NOW_PLAYING_TEXT;
    private final int settingsSearchBarId = Settings.SEARCH_TEXT;
    private final int widgetIdCombatAchSearchBar = CaBosses.SEARCH_TEXT;
    private final int widgetIdBehindLoginScreen = ToplevelDisplay.LAYERS;

    //general interface
    private final int widgetIdSkillGuide = SkillGuide.UNIVERSE;
    private final Set<Integer> rootWidgetIdSet = Set.of(ToplevelPreEoc.GAMEFRAME, ToplevelOsrsStretch.GAMEFRAME, Toplevel.GAMEFRAME);

    /* example for adding set of widget ids
    private final Set<Integer> idSet4Raids_Colosseum = Set.of(
            1234, // widget id of parent containing text of display board for raids 1
            5678, // ...
            1234 // ...
    );
     */

    // dont translate at all
    private final Set<Integer> widgetIdNot2Translate = Set.of(
            Chatbox.CHATDISPLAY,
            10617391,//some sort of background for chatbox
            widgetIdCharacterSummaryName,
            Ignore.LIST_CONTAINER,
            widgetIdGimGroupName, //gim group name in group tab
            widgetIdClockGIM, widgetIdClockNonGIM,
            //ComponentID.MUSIC_SCROLL_CONTAINER, // if music is not ignored here, having the music tab opened will drop fps
            //widgetIdMusicCurrent // may need to be ignored if clue solver reads this widget's value
            settingsSearchBarId,
            widgetIdCombatAchSearchBar,
            widgetIdBehindLoginScreen
    );

    // dont translate with api
    private final Set<Integer> widgetIdNot2ApiTranslate = Set.of(
            Music.FRAME, // if music is not ignored here, having the music tab opened will drop fps
            CaTasks.TASKS // combat achievement tasks
    );


    private final Set<Integer> widgetIdItemName = Set.of(
            CombatInterface.TITLE // combat weapon name in combat options
    );

    private final Set<Integer> widgetIdNpcName = Set.of(
            CaBosses.BOSSES_NAME, // the boss names in the "Combat Achievement - Bosses"
            CaBoss.BOSS_NAME // the boss names in the "Combat Achievement - Specific Bosses"
    );

    private final Set<Integer> widgetIdObjectName = Set.of(

    );

    private final Set<Integer> widgetIdQuestName = Set.of(
            Questlist.LIST // quest name in quest list
    );

    private final Set<Integer> widgetIdAnyTranslated = Set.of(// dont specify any categoriries

    );

    private final Set<Integer> widgetIdCA = Set.of(
            CaOverview.INFINITY, // combat achievement overview
            CaTasks.INFINITY, // combat achievement tasks
            CaBosses.INFINITY, // combat achievement Bosses
            CaRewards.INFINITY, // combat achievement Rewards
            CaBoss.INFINITY, // combat achievement specific bosses
            CaTasks.DROPDOWN_BOX  // combat achievement task filter drop down list
    );

    // other specific ids
    private final int attackStyleHoverTextId = CombatInterface.TOOLTIP;
    private final int prayerTabHoverTextId = Prayerbook.TOOLTIP;
    private final int spellbookTabHoverTextId = MagicSpellbook.TOOLTIP;
    private final int addFriendButtonId = Friends.UNIVERSE_TEXT4;
    private final int removeFriendButtonId = Friends.UNIVERSE_TEXT6;
    private final int skillsTabXpHoverTextId = Stats.TOOLTIP;
    private final int xpBarTopRightHoverTextId = XpDrops.TOOLTIP;
    private final int gimMemberNameId = GimSidepanel.MEMBERS; // show only if type = 4 and left aligned
    private final int groupTabPvPGroupMemberId = PvpArenaSidepanel.PLAYERLIST;
    private final int groupingGroupMemberNameId = Grouping.PLAYERLIST;
    private final int settingsHoverTextId = SettingsSide.TOOLTIP;
    private final int emotesHoverTextId = Emote.TOOLTIP;
    private final int worldSwitcherHoverTextId = Worldswitcher.TOOLTIP;
    private final int worldSwitcherWorldActivityId = Worldswitcher.INFO;
    private final int houseOptionsHoverTextId = PohOptions.TOOLTIP; // house options, from the settings tab, click house icon
    private final int houseOptionsTextOnID = PohOptions.ROOT_TEXT2; // in house options, the "On" text (can get <br> tag in some languages)
    private final int houseOptionsTextOffID = PohOptions.ROOT_TEXT3; // in house options, the "Off" text (can get <br> tag in some languages)
    private final int loginScreenId = WelcomeScreen.CONTENT;
    private final int loginBannerId = WelcomeScreen.BANNER_ARTCANVAS;

    // for English transcript to be split at <br> tags and added to the transcript
    // will reduce the number of translations needed
    // (below, "Next level at:" and "Remaining XP:" are only translated once instead of for every skill)
    // e.g
    // (original text) "Agility Xp:<br>Next level at:<br>Remaining XP:"
    // ->(split into) "Agility Xp:", "Next level at:", "Remaining XP:"
    // -> (translated to) "運動神経XP", "次レベル開始：", "残りXP："
    // -> (combine and set widget text as) "運動神経XP:<br>次レベル開始：<br>残りXP："
    private final Set<Integer> widgetId2SplitTextAtBr = Set.of(
            skillsTabXpHoverTextId, // skill tab's xp hover display
            xpBarTopRightHoverTextId // hover display of xp bar top right
    );

    // for transcript's break lines to be kept as is
    // useful for widgets that have multiple variables in one widget
    // can also use to prevent splitting at <br> tags if the text is to be kept as is
    // e.g (first line is level, second line is prayer name, third line is description, all in one widget)
    // (original text) "Level 22<br>Rapid Heal<br>2x restore rate for<br>Hitpoints stat."
    // -> (translated to) "レベル22<br>急激な回復<br>体力の回復速度を<br>２倍にする"
    // -> (set widget text as above)
    private final Set<Integer> widgetId2KeepBr = Set.of(
            prayerTabHoverTextId,
            spellbookTabHoverTextId,
            addFriendButtonId, removeFriendButtonId,
            settingsHoverTextId,
            emotesHoverTextId,
            worldSwitcherHoverTextId, worldSwitcherWorldActivityId,
            houseOptionsHoverTextId, houseOptionsTextOnID, houseOptionsTextOffID,
            Prayerbook.FILTERBUTTON, MagicSpellbook.FILTERBUTTON,
            GimSidepanel.TITLE, GimSidepanel.INVITE, GimSidepanel.SETTINGS,
            ChatchannelCurrent.CHANNELNAME, ChatchannelCurrent.CHANNELOWNER, ChatchannelCurrent.JOINLEAVE, ChatchannelCurrent.SETUP,
            ClansSidepanel.HEADER, ClansSidepanel.CHAT, ClansSidepanel.INFO,
            ClansGuestSidepanel.HEADER, ClansGuestSidepanel.CHAT, ClansGuestSidepanel.INFO, //ClansGuestSidepanel.SEARCH,
            Friends.LIST,
            Account.NAME_TEXT
    );

    private final Set<Integer> widgetIdChatButton2SetXTextAliLeft = Set.of(
            Chatbox.CHAT_GAME_TEXT1, // CHATBOX_TAB_GAME 's "Game" widget
            Chatbox.CHAT_PUBLIC_TEXT1, // CHATBOX_TAB_PUBLIC 's "Public" widget
            Chatbox.CHAT_PRIVATE_TEXT1, // CHATBOX_TAB_PRIVATE 's "Private" widget
            Chatbox.CHAT_FRIENDSCHAT_TEXT1, // CHATBOX_TAB_CHANNEL 's "Channel" widget
            Chatbox.CHAT_CLAN_TEXT1, // CHATBOX_TAB_CLAN 's "Clan" widget
            Chatbox.CHAT_TRADE_TEXT // CHATBOX_TAB_TRADE 's "Trade" widget
    );
    private final Set<Integer> widgetIdChatButton2SetXTextAliRight = Set.of(
            Chatbox.CHAT_GAME_FILTER, // CHATBOX_TAB_GAME 's setting widget
            Chatbox.CHAT_PUBLIC_FILTER, // CHATBOX_TAB_PUBLIC 's setting widget
            Chatbox.CHAT_PRIVATE_FILTER, // CHATBOX_TAB_PRIVATE 's setting widget
            Chatbox.CHAT_FRIENDSCHAT_FILTER, // CHATBOX_TAB_CHANNEL 's setting widget
            Chatbox.CHAT_CLAN_FILTER, // CHATBOX_TAB_CLAN 's setting widget
            Chatbox.CHAT_TRADE_FILTER // CHATBOX_TAB_TRADE 's setting widget
    );

    // widget ids to change the width of, because some widget have room and also needs more
    // each value's meaning: Map<widgetId, Pair<newWidth, newHeight>>
    private final Map<Integer, Pair<Integer, Integer>> widgetId2FixedSize = Map.ofEntries(
        //Map.entry(widget_id, Pair.of(newWidth, newHeight))
        Map.entry(AreaTask.TASKBOX, Pair.of(110, null)) // the achievement diary tab's location names
    );

    // ids of widgets to resize to match the text inside it, mostly for hover displays like prayer's hover descriptions
    // sibling widgets = other widgets under the same parent, which contains text
    private void initWidget2ModDict() {
        widget2ModDict.add(attackStyleHoverTextId, 4, false, true, false, false, false, 2, 3, 2, 2);
        widget2ModDict.add(skillsTabXpHoverTextId, 4, true, false, false, false, false, 3, 3, 3, 3); // skill tab's xp hover display
        widget2ModDict.add(prayerTabHoverTextId, 4,false, true, false, false, false, 3, 3, 2, 0);
        widget2ModDict.add(spellbookTabHoverTextId, 4,true, false, false, true, true, 2, 2, 2, 2);
        widget2ModDict.add(settingsHoverTextId, 4, false, true, false, false, false, 2, 2, 2, 2);
        widget2ModDict.add(emotesHoverTextId, 4, false, true, false, false, false, 2, 2, 2, 2);
        widget2ModDict.add(worldSwitcherHoverTextId, 4, false, false, false, false, false, 0, 2, 0, 2);
        widget2ModDict.add(houseOptionsHoverTextId, 4, false, true, false, false, false, 2, 2, 2, 0);
    }

    // partial translations
    private final int playerNameInAccManTab = Account.NAME_TEXT;
    private final int widgetIdPvPArenaNan = PvpArenaSidepanel.PLAYERLIST;//group tab, click PvP Arena button in top right while in an unrelated guest clan
    private final int widgetIdInOtherActivityChannel = Grouping.PLAYERLIST;// group tab, select an activity in drop down menu, click join, then select another activity
    private final int playerNameInCombAch = CaOverview.CA_PERSONAL_HEADER;// the player name in "Overview" of combat achievements tab
    private final int topBossInCombAch = CaOverview.CA_PERSONAL_CONTENT;// CA overview > the texts on right side, under text with player name
    private final int monsterTargetNameInCombAch = CaTasks.TASKS_MONSTER;// "Monster: ..." in combat achievements tab (tasks)
    private final int bossSpecificCA = CaBoss.FRAME;// combat achievements - specific bosses' title at the top

    private void initPartialTranslations() {
        /* use for when part of a text should not be translated / translated as item name, object name etc, and other parts should be translated by translator/api
         * for placeholder types, use PLAYER_NAME, ITEM_NAME, NPC_NAME, OBJECT_NAME, QUEST_NAME
         * use PLAYER_NAME for any text that shouldn't be translated, ITEM_NAME for item names, etc.
         *
        partialTranslationManager.addPartialTranslation(
                widgetId, (give any number if its not for widget)
                List.of("fixed text part 1", "fixed text part 2", "fixed text part 3"),
                List.of(placeholder_type1, placeholder_type2)
                * );
         (text = fixed text part 1 placeholder_type1 fixed text part 2 placeholder_type2 fixed text part 3)
         */
        partialTranslationManager.addPartialTranslation(
                playerNameInAccManTab,
                List.of("Name: "),
                List.of(PLAYER_NAME)
        );
        partialTranslationManager.addPartialTranslation(
                widgetIdPvPArenaNan,
                List.of("You have currently loaded <colNum0>", "</col>, which is not a PvP Arena group."),
                List.of(PLAYER_NAME)// clan name goes here
        );
        partialTranslationManager.addPartialTranslation(
                widgetIdInOtherActivityChannel,
                List.of("You are currently talking in the <colNum0>","</col> channel."),
                List.of(ANY_TRANSLATED)// activity name goes here
        );
        partialTranslationManager.addPartialTranslation( // menu option for join button in group tab's activity tab
                0,
                List.of("Join <colNum0>","</col> channel"),
                List.of(ANY_TRANSLATED)// activity name goes here
        );
        partialTranslationManager.addPartialTranslation( // menu option for leave button in group tab's activity tab, displayed after joining
                0,
                List.of("Leave <colNum0>","</col> channel"),
                List.of(ANY_TRANSLATED)// activity name goes here
        );
        partialTranslationManager.addPartialTranslation( // menu option for teleport button in group tab's activity tab
                0,
                List.of("Teleport to <colNum0>","</col>"),
                List.of(ANY_TRANSLATED)// activity name goes here
        );
        partialTranslationManager.addPartialTranslation(
                topBossInCombAch,
                List.of("", " (<Num0>)"),
                List.of(ANY_TRANSLATED)// boss names. ANY_TRANSLATED and not NPC_NAME because it's not always npc name, eg Wintertodt)
        );
        partialTranslationManager.addPartialTranslation(
                playerNameInCombAch,
                List.of("Combat Profile - "),
                List.of(PLAYER_NAME)
        );
        partialTranslationManager.addPartialTranslation(
                monsterTargetNameInCombAch,
                List.of("Monster: <colNum0>","</col>"),
                List.of(NPC_NAME)// monster name, but includes non npc names like "Royal Titans", "Wintertodt"
        );
        partialTranslationManager.addPartialTranslation(
                bossSpecificCA,
                List.of("Combat Achievements - "),
                List.of(NPC_NAME)// npc names include non-npcs like "chambers of zeric", "theatre of blood"
        );
        partialTranslationManager.addPartialTranslation(
                0,
                List.of("Add friend "),
                List.of(PLAYER_NAME)// for menu action in group tab, activity tab after joining an fc and right clicking player there
        );
        partialTranslationManager.addPartialTranslation(
                0,
                List.of("Add ignore "),
                List.of(PLAYER_NAME)// for menu action in group tab, activity tab after joining an fc and right clicking player there
        );
        partialTranslationManager.addPartialTranslation(
                0,
                List.of("Remove friend "),
                List.of(PLAYER_NAME)// for menu action in group tab, activity tab after joining an fc and right clicking player there
        );
        partialTranslationManager.addPartialTranslation(
                0,
                List.of("Remove ignore "),
                List.of(PLAYER_NAME)// for menu action in group tab, activity tab after joining an fc and right clicking player there
        );

        // to add placeholder at the beginning of the text, add an empty string to the fixedTextParts
        // eg.  partialTranslationManager.addPartialTranslation(
        //                playerNameInAccManTab,
        //                List.of("", "is his name."),
        //                List.of(PLAYER_NAME)
        //        );

        // add more partial translations here
    }

    public int getCombatOptionParentWidgetId() {
        Widget w = client.getWidget(ComponentID.COMBAT_LEVEL);
        if(w != null) {
            return w.getParentId();
        }
        //log.info("parent of ComponentID.COMBAT_LEVEL is null");
        return -1;
    }

    public int getWidgetIdAchievementDiaryTab() {
        Widget w = client.getWidget(ComponentID.ACHIEVEMENT_DIARY_CONTAINER);
        if(w != null) {
            return w.getParent().getParent().getParentId();
        }
        //log.info("parent^3 of ComponentID.ACHIEVEMENT_DIARY_CONTAINER is null");
        return -1;
    }

    public int getFriendsTabParentWidgetId() {
        Widget w = client.getWidget(ComponentID.FRIEND_LIST_TITLE);
        if(w != null) {
            return w.getParentId();
        }
        //log.info("parent of ComponentID.FRIEND_LIST_TITLE is null");
        return -1;
    }

    public int getIgnoreTabParentWidgetId() {
        Widget w = client.getWidget(ComponentID.IGNORE_LIST_TITLE);
        if(w != null) {
            return w.getParentId();
        }
        //log.info("parent of ComponentID.IGNORE_LIST_TITLE is null");
        return -1;
    }

    public int getWidgetIdCharacterSummaryTab(){
        Widget w = client.getWidget(ComponentID.CHARACTER_SUMMARY_CONTAINER);
        if(w != null) {
            return w.getParentId();
        }
        //log.info("parent of ComponentID.CHARACTER_SUMMARY_CONTAINER is null");
        return -1;
    }

}
