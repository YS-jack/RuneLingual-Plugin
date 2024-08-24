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

    dialogueInCategory("dialogue","category"), // string value for "dialogue" in category column
    examineInCategory("examine","category"),
    nameInCategory("name","category"),
    //manualInCategory("manual","category"),//probably wont use
    actionsInCategory("actions","category"),
    lvlUpMessageInCategory("lvl_up_Message","category"),
    inventActionsInCategory("inventoryActions","category"), // this is for every menu entries in the main panel(Inventory, Worn Equipment, friends list, etc.


    itemInSubCategory("item","sub_category"),
    npcInSubCategory("npc","sub_category"),
    objInSubCategory("object","sub_category"),
    levelInSubCategory("level","sub_category"), // for "(level-%d)" of player or npcs with levels, category should be "name"
    menuInSubCategory("menu","sub_category"), // for widgets that are not buttons nor interface, such as one of the skills in skill list tab, name of tabs ("Combat Options", "Quest List")

    playerInSource("player","source"), // for player options, such as report, trade, follow, etc.
    ;


    private final String value;
    private final String columnName;

    SqlVariables(String val, String columnName) {
        this.value = val;
        this.columnName = columnName;
    }

}
