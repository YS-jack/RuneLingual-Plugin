package com.RuneLingual.SQL;

import lombok.Getter;

@Getter
public enum SqlVariables {
    // SQL Variables
    columnEnglish("","english"),// name of column in the database
    columnTranslation("","translation"),
    columnCategory("","category"),
    columnSubCategory("","sub_category"),
    columnSource("","source"),

    // possible category values
    categoryValue4Dialogue("dialogue","category"), // string value for "dialogue" in category column
    categoryValue4Examine("examine","category"),
    categoryValue4GameMessage("gameText","category"), // for game text, such as "Welcome to old School RuneScape."
    categoryValue4Name("name","category"),
    categoryValue4Manual("manual","category"),//probably wont use
    categoryValue4Actions("actions","category"),
    categoryValue4LvlUpMessage("lvl_up_Message","category"),
    categoryValue4InventActions("inventoryActions","category"), // this is for every menu entries in the main panel(Inventory, Worn Equipment, friends list, etc.
    categoryValue4Interface("interface","category"), // for interfaces like buttons and widgets

    // possible sub_category values
    subcategoryValue4Item("item","sub_category"),
    subcategoryValue4Npc("npc","sub_category"),
    subcategoryValue4Obj("object","sub_category"),
    subcategoryValue4Level("level","sub_category"), // for "(level-%d)" of player or npcs with levels, category should be "name"
    subcategoryValue4Menu("menu","sub_category"), // for widgets that are not buttons nor interface, such as one of the skills in skill list tab, name of tabs ("Combat Options", "Quest List")
    subcategoryValue4Player("player","sub_category"), // for player options, such as report, trade, follow, etc.
    subcategoryValue4Quest("quest","sub_category"),
    subcategoryValue4Tab("genActions","sub_category"), // for general actions such as deposit all, sell-5, etc.
    subcategoryValue4GeneralUI("generalUI","sub_category"), // most if not all interfaces will have this subcategory value
    subcategoryValue4MainTabs("mainTabs","sub_category"), // for main tabs, such as combat options, skills, character summary, etc.
    subCategoryValue4ChatButtons("chatButtons","sub_category"), // for chat buttons, such as report, trade, On, Off, Hide, etc.
    subCategoryValue4LoginScreen("loginScreen","sub_category"),
    subCategoryValue4CA("combAchvmt","sub_category"), // for combat achievements and achievement diary


    // possible source values
    sourceValue4Player("player","source"), // for player options, such as report, trade, follow, etc.
    //  for tabs
    sourceValue4CombatOptionsTab("combatOptionsTab","source"), // for combat options, attack styles etc. query eg: Block	actions	menu	combatOption
    sourceValue4SkillsTab("skillsTab","source"), // for skills tab
    sourceValue4CharacterSummaryTab("characterSummaryTab","source"), // for character summary tab
    sourceValue4QuestListTab("questListTab","source"), // for quest list tab
    sourceValue4AchievementDiaryTab("achievementDiaryTab","source"), // for achievement Diary Tab
    sourceValue4InventTab("inventTab","source"), // for inventory tab
    sourceValue4WornEquipmentTab("wornEquipmentTab","source"), // for worn equipment tab
    sourceValue4PrayerTab("prayerTab","source"), // for prayer tab
    sourceValue4SpellBookTab("spellBookTab","source"), // for spell book tab
    sourceValue4GroupTab("groupTab","source"), // for group tab
    sourceValue4FriendsTab("friendsTab","source"), // for friends tab
    sourceValue4IgnoreTab("ignoreTab","source"), // for ignore tab
    sourceValue4AccountManagementTab("accountManagementTab","source"), // for account management tab
    sourceValue4SettingsTab("settingsTab","source"), // for settings tab
    sourceValue4LogoutTab("logoutTab","source"), // for logout tab
    sourceValue4WorldSwitcherTab("worldSwitcherTab","source"), // for world switcher
    sourceValue4EmotesTab("emotesTab","source"), // for emotes tab
    sourceValue4MusicTab("musicTab","source"), // for music tab

    // for interfaces
    sourceValue4SkillGuideInterface("skillGuide","source"), // for skill guide interface
    // add more here, with comments for what they are for
    // name should be "sourceValue4" + the name of the source
    // the second value "source" should not be changed, as it is the name of the column in the database
    ;

    private final String value;
    private final String columnName;

    SqlVariables(String val, String columnName) {
        this.value = val;
        this.columnName = columnName;
    }

}
