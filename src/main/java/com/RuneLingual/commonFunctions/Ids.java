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
    private final int widgetIdFriendsTab = 28114944;
    private final int widgetIdIgnoreTab = 28311552;
    private final int widgetIdAccountManagementTab = 7143445;
    private final int widgetIdSettingsTab = 7602176;
    private final int widgetIdEmotesTab = ComponentID.EMOTES_WINDOW;
    private final int widgetIdMusicTab = ComponentID.MUSIC_CONTAINER;
    private final int widgetIdLogoutTab = 11927552;
    private final int widgetIdWorldSwitcherTab = 4521984;

    // dont translate at all, except menu option
    private final int widgetIdGimGroupName = 47579137;
    private final int widgetIdGimMemberList = 47579140;
    private final int widgetIdIgnoreNameList = ComponentID.IGNORE_LIST_FULL_CONTAINER;

    //dont translate names, but translate World ### and "Offline"
    private final int widgetIdFriendsNameList = ComponentID.FRIEND_LIST_FULL_CONTAINER;

    //general interface
    private final int widgetIdSkillGuide = 14024705;

    /* example for adding set of widget ids
    private final Set<Integer> idSet4Raids_Colosseum = Set.of(
            1234, // widget id of parent containing text of display board for raids 1
            5678, // ...
            1234 // ...
    );
     */

    private final Set<Integer> widgetIdPlayerName = Set.of(
            46661633,//character summary player name
            28311561,//ignore list
            458764//friends chat list
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
    private final int attackStyleHoverTextId = 38862892;
    private final int prayerTabHoverTextId = 35455015;
    private final int spellbookTabHoverTextId = 14287050;
    private final int friendsTabPlayerNameTextId = 28114955;
    private final int playerNameInAccManTab = 7143474;
    private final int addFriendButtonId = 28114959;
    private final int removeFriendButtonId = 28114961;
    private final int skillsTabXpHoverTextId = 20971548;
    private final int xpBarTopRightHoverTextId = 7995417;

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
            addFriendButtonId, removeFriendButtonId
    );

//    private final Set<Integer> widgetId2SetLineHeight = Set.of(
//            46661634, // character summary tab's category texts
//            35455018, // prayer tab's filter texts
//            14287046, // spellbook tab filter texts
//            addFriendButtonId, removeFriendButtonId // friends tab's add friend and remove friend button texts
//    );

    // widget ids to change the width of, because some widget have room and also needs more
    // each value's meaning: Map<widgetId, Pair<newWidth, newHeight>>
    private final Map<Integer, Pair<Integer, Integer>> widgetId2FixedSize = Map.ofEntries(
        //Map.entry(widget_id, Pair.of(newWidth, newHeight))
        Map.entry(16973826, Pair.of(110, null)) // the achievement diary tab's location names
    );

    // widget ids to resize to match the text inside it, mostly for hover displays like prayer's hover descriptions
    // sibling widgets = other widgets under the same parent, which contains text ( and should be type 4)
    private void initWidget2ModDict() {
        // widget2ModDict.add(widgetId, error pixels, has Sibling Widget, fixed top, fixed bottom, fixed left, fixed right, top padding, bottom padding, left padding, right padding)
        widget2ModDict.add(attackStyleHoverTextId, 4, false, true, false, false, false, 1, 3, 2, 2); // spellbook tab's hover text
        widget2ModDict.add(skillsTabXpHoverTextId, 4, true, false, false, false, false, 3, 3, 3, 3); // skill tab's xp hover display
        widget2ModDict.add(prayerTabHoverTextId, 4,false, true, false, true, false, 3, 3, 3, 3);
        widget2ModDict.add(spellbookTabHoverTextId, 4,true, false, false, true, true, 2, 2, 2, 2);
    }

    private void initPartialTranslations() {
        /* usage:
        partialTranslationManager.addPartialTranslation(
                widgetId,
                List.of("fixed text part 1", "fixed text part 2", "fixed text part 3"),
                List.of(placeholder_type1, placeholder_type2)
         */
        partialTranslationManager.addPartialTranslation(
                playerNameInAccManTab,
                List.of("Name: "),
                List.of(PLAYER_NAME)
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
