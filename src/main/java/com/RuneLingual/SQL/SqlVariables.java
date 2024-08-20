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
    manualInCategory("manual","category"),
    actionsInCategory("actions","category"),
    lvlUpMessageInCategory("lvl_up_Message","category"),
    inventActionsInCategory("inventoryActions","category"),

    itemInSubCategory("item","sub_category"),
    npcInSubCategory("npc","sub_category"),
    objInSubCategory("object","sub_category"),

    playerInSource("player","source"),;


    private final String value;
    private final String columnName;

    SqlVariables(String val, String columnName) {
        this.value = val;
        this.columnName = columnName;
    }

}
