package com.RuneLingual.SQL;

import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import lombok.Getter;
import lombok.Setter;

import com.RuneLingual.SQL.SqlActions;
import com.RuneLingual.SQL.SqlVariables;

import javax.inject.Inject;

@Getter @Setter
public class SqlQuery {
    private String english; // the whole text, not a part of Colors.wordArray
    private String translation;
    private String category;
    private String subCategory;
    private String source;

    private Colors color;


    @Inject
    RuneLingualPlugin plugin;

    @Inject
    public SqlQuery(RuneLingualPlugin plugin){
        this.plugin = plugin;
        this.english = null;
        this.translation = null;
        this.category = null;
        this.subCategory = null;
        this.source = null;
        this.color = null;
    }

    public String[] getMatching(SqlVariables column) {
        // create query -> execute -> return result
        String query = getSearchQuery();
        query = query.replace("*", column.getColumnName());
        String[][] result = plugin.getSqlActions().executeSearchQuery(query);
        String[] translations = new String[result.length];
        for (int i = 0; i < result.length; i++){
            translations[i] = result[i][0];
        }
        return translations;
    }

    public String[] getMatching(SqlVariables[] columns) {
        // create query -> execute -> return result
        String query = getSearchQuery();
        String[] translations = new String[columns.length];
        for (int i = 0; i < columns.length; i++){
            query = query.replace("*", columns[i].getColumnName());
            String[][] result = plugin.getSqlActions().executeSearchQuery(query);
            translations[i] = result[0][0];
        }
        return translations;
    }

    public String getSearchQuery() {
        // creates query that matches all non-empty fields
        // returns null if no fields are filled
        String query = "SELECT * FROM " + SqlActions.tableName + " WHERE ";
        if (english != null && !english.isEmpty()){
            query += SqlVariables.columnEnglish.getColumnName() + " = '" + english.replace("'","''") + "' AND ";
        }
        if (category != null && !category.isEmpty()){
            query += SqlVariables.columnCategory.getColumnName() + " = '" + category + "' AND ";
        }
        if (subCategory != null && !subCategory.isEmpty()){
            query += SqlVariables.columnSubCategory.getColumnName() + " = '" + subCategory + "' AND ";
        }
        if (source != null && !source.isEmpty()){
            query += SqlVariables.columnSource.getColumnName() + " = '" + source + "' AND ";
        }

        if (translation != null && !translation.isEmpty()){
            query += SqlVariables.columnTranslation.getColumnName() + " = '" + translation.replace("'","''") + "' AND ";
        } //todo: add more here if columns to be filtered are added

        if (query.endsWith("AND ")){
            query = query.substring(0, query.length() - 5);
            return query;
        }
        return null;
    }

    public void setEnCatSubcat(String english, String category, String subCategory, Colors defaultColor){
        this.english = english;
        this.category = category;
        this.subCategory = subCategory;
        this.color = defaultColor;
    }

    public void setItemName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.nameInCategory.getValue();
        this.subCategory = SqlVariables.itemInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setNpcName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.nameInCategory.getValue();
        this.subCategory = SqlVariables.npcInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setObjectName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.nameInCategory.getValue();
        this.subCategory = SqlVariables.objInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setMenuName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.nameInCategory.getValue();
        this.subCategory = SqlVariables.menuInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }




    public void setInventoryItemActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.inventActionsInCategory.getValue();
        this.subCategory = SqlVariables.itemInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setGroundItemActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.actionsInCategory.getValue();
        this.subCategory = SqlVariables.itemInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setNpcActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.actionsInCategory.getValue();
        this.subCategory = SqlVariables.npcInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setObjectActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.actionsInCategory.getValue();
        this.subCategory = SqlVariables.objInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setMenuAcitons(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.actionsInCategory.getValue();
        this.subCategory = SqlVariables.menuInSubCategory.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setPlayerActions(String en, Colors defualtColor){
        this.english = en;
        this.category = SqlVariables.actionsInCategory.getValue();
        this.subCategory = SqlVariables.playerInSubCategory.getValue();
        this.color = defualtColor;
        this.source = null;
        this.translation = null;
    }
}
