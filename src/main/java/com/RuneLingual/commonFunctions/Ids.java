package com.RuneLingual.commonFunctions;

import com.RuneLingual.Widgets.PartialTranslationManager;
import com.RuneLingual.Widgets.Widget2ModDict;
import lombok.Getter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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
    private final int widgetIdMainTabs = 10747976;
    private final int widgetIdAttackStyleTab = 38862848;
    private final int widgetIdSkillsTab = ComponentID.SKILLS_CONTAINER;
    private final int widgetIdCharacterSummaryTab = ComponentID.CHARACTER_SUMMARY_CONTAINER;
    private final int widgetIdQuestTab = ComponentID.QUEST_LIST_BOX;
    private final int widgetIdAchievementDiaryTab = ComponentID.ACHIEVEMENT_DIARY_CONTAINER;
    private final int widgetIdInventoryTab = ComponentID.INVENTORY_CONTAINER;
    private final int widgetIdEquipmentTab = ComponentID.EQUIPMENT_INVENTORY_ITEM_CONTAINER;
    private final int widgetIdPrayerTab = ComponentID.PRAYER_PARENT;
    private final int widgetIdSpellBookTab = ComponentID.SPELLBOOK_PARENT;
    private final int widgetIdGroupsTab = 47644672;
    private final int widgetIdGroupTabNonGIM = 46333952;
    private final int widgetIdPvPArena = 49938432;
    private final int widgetIdFriendsTab = 28114944;
    private final int widgetIdIgnoreTab = 28311552;
    private final int widgetIdAccountManagementTab = 7143445;
    private final int widgetIdSettingsTab = 7602176;
    private final int widgetIdEmotesTab = ComponentID.EMOTES_WINDOW;
    private final int widgetIdMusicTab = ComponentID.MUSIC_CONTAINER;
    private final int widgetIdLogoutTab = 11927552;
    private final int widgetIdWorldSwitcherTab = 4521984;

    // dont translate at all, except menu option
    private final int widgetIdCharacterSummaryName = 46661633;
    private final int widgetIdGimGroupName = 47579137;
    private final int widgetIdClockGIM = 47644681;
    private final int widgetIdClockNonGIM = 46333960;
    private final int widgetIdMusicCurrent = 15663113;
    private final int settingsSearchBarId = 8781833;

    //general interface
    private final int widgetIdSkillGuide = 14024705;

    /* example for adding set of widget ids
    private final Set<Integer> idSet4Raids_Colosseum = Set.of(
            1234, // widget id of parent containing text of display board for raids 1
            5678, // ...
            1234 // ...
    );
     */

    // dont translate at all
    private final Set<Integer> widgetIdNot2Translate = Set.of(
            ComponentID.CHATBOX_TRANSPARENT_BACKGROUND_LINES,
            10617391,//some sort of background for chatbox
            widgetIdCharacterSummaryName,
            ComponentID.IGNORE_LIST_FULL_CONTAINER,
            widgetIdGimGroupName, //gim group name in group tab
            widgetIdClockGIM, widgetIdClockNonGIM,
            //ComponentID.MUSIC_SCROLL_CONTAINER, // if music is not ignored here, having the music tab opened will drop fps
            //widgetIdMusicCurrent // may need to be ignored if clue solver reads this widget's value
            settingsSearchBarId
    );

    // dont translate with api
    private final Set<Integer> widgetIdNot2ApiTranslate = Set.of(
            ComponentID.MUSIC_SCROLL_CONTAINER // if music is not ignored here, having the music tab opened will drop fps

    );


    private final Set<Integer> widgetIdItemName = Set.of(
            ComponentID.COMBAT_WEAPON_NAME // combat weapon name in combat options
    );

    private final Set<Integer> widgetIdNpcName = Set.of(

    );

    private final Set<Integer> widgetIdObjectName = Set.of(

    );

    private final Set<Integer> widgetIdQuestName = Set.of(
            26148871 // quest name in quest list
    );

    private final Set<Integer> widgetIdAnyTranslated = Set.of(// dont specify any categoriries
            46923790 // the boss names in the "Combat Achievement - Bosses"
    );

    private final Set<Integer> widgetIdCA = Set.of(
            46989312, // combat achievement overview
            46858241, // combat achievement tasks
            46923776, // combat achievement Bosses
            46792705, // combat achievement Rewards
            46727169 // combat achievement specific bosses
    );

    // other specific ids
    private final int attackStyleHoverTextId = 38862892;
    private final int prayerTabHoverTextId = 35455015;
    private final int spellbookTabHoverTextId = 14287050;
    private final int addFriendButtonId = 28114959;
    private final int removeFriendButtonId = 28114961;
    private final int skillsTabXpHoverTextId = 20971548;
    private final int xpBarTopRightHoverTextId = 7995417;
    private final int gimMemberNameId = 47579142; // show only if type = 4 and left aligned
    private final int groupTabPvPGroupMemberId = 49938440;
    private final int groupingGroupMemberNameId = 4980752;
    private final int settingsHoverTextId = 7602219;
    private final int emotesHoverTextId = 14155781;
    private final int worldSwitcherHoverTextId = 4522010;
    private final int worldSwitcherWorldActivityId = 4522003;
    private final int houseOptionsHoverTextId = 24248346; // house options, from the settings tab, click house icon
    private final int houseOptionsTextOnID = 24248322; // in house options, the "On" text (can get <br> tag in some languages)
    private final int houseOptionsTextOffID = 24248323; // in house options, the "Off" text (can get <br> tag in some languages)
    private final int loginScreenId = 24772610;
    private final int loginBannerId = 24772687;

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

    // for English transcript to be kept as is
    // useful for widgets that have multiple variables in one widget
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
            houseOptionsHoverTextId, houseOptionsTextOnID, houseOptionsTextOffID
    );

    private final Set<Integer> widgetIdChatButton2SetXTextAliLeft = Set.of(
            10616841, // CHATBOX_TAB_GAME 's "Game" widget
            10616845, // CHATBOX_TAB_PUBLIC 's "Public" widget
            10616849, // CHATBOX_TAB_PRIVATE 's "Private" widget
            10616853, // CHATBOX_TAB_CHANNEL 's "Channel" widget
            10616857, // CHATBOX_TAB_CLAN 's "Clan" widget
            10616861 // CHATBOX_TAB_TRADE 's "Trade" widget
    );
    private final Set<Integer> widgetIdChatButton2SetXTextAliRight = Set.of(
            10616842, // CHATBOX_TAB_GAME 's setting widget
            10616846, // CHATBOX_TAB_PUBLIC 's setting widget
            10616850, // CHATBOX_TAB_PRIVATE 's setting widget
            10616854, // CHATBOX_TAB_CHANNEL 's setting widget
            10616858, // CHATBOX_TAB_CLAN 's setting widget
            10616862 // CHATBOX_TAB_TRADE 's setting widget
    );

    // widget ids to change the width of, because some widget have room and also needs more
    // each value's meaning: Map<widgetId, Pair<newWidth, newHeight>>
    private final Map<Integer, Pair<Integer, Integer>> widgetId2FixedSize = Map.ofEntries(
        //Map.entry(widget_id, Pair.of(newWidth, newHeight))
        Map.entry(16973826, Pair.of(110, null)) // the achievement diary tab's location names
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
    private final int playerNameInAccManTab = 7143474;
    private final int widgetIdPvPArenaNan = 49938440;//group tab, click PvP Arena button in top right while in an unrelated guest clan
    private final int widgetIdInOtherActivityChannel = 4980752;// group tab, select an activity in drop down menu, click join, then select another activity
    private final int playerNameInCombAch = 46989317;// the player name in "Overview" of combat achievements tab
    private final int topBossInCombAch = 46989326;// the texts under Combat Profile section in "Overview" of combat achievements tab
    private final int monsterTargetNameInCombAch = 46858252;// "Monster: ..." in combat achievements tab (tasks)

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
                List.of(ANY_TRANSLATED)// monster name, but includes non npc names like "Royal Titans", "Wintertodt"
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
