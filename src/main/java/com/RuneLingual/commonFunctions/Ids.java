package com.RuneLingual.commonFunctions;

import lombok.Getter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;

import com.RuneLingual.RuneLingualPlugin;

import javax.inject.Inject;
import java.util.Set;

@Getter @Slf4j
public class Ids {
    @Inject
    public Ids(RuneLingualPlugin plugin) {
        this.plugin = plugin;
        this.client = plugin.getClient();
    }
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    Client client;

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
    private final int widgetIdAccountManagementTab = 7143424;
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

    private final Set<Integer> widgetIdDontRemoveBr = Set.of(
            38862892, // attack style tab's combat category/style display
            20971548, // skill tab's xp display
            7995417 // xp display on top right
    );


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
