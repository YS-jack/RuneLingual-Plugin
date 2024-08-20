package com.RuneLingual.SQL;

import com.RuneLingual.RuneLingualPlugin;
import lombok.Getter;
import lombok.Setter;

import com.RuneLingual.SQL.SqlActions;
import com.RuneLingual.SQL.SqlVariables;

import javax.inject.Inject;

@Getter @Setter
public class SqlQuery {
    private String category;
    private String subCategory;
    private String source;
    private String english;
    private String translation;

    @Inject
    RuneLingualPlugin plugin;

    @Inject
    public SqlQuery(RuneLingualPlugin plugin){
        this.plugin = plugin;
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

        if (category != null && !category.isEmpty()){
            query += SqlVariables.columnCategory.getColumnName() + " = '" + category + "' AND ";
        }
        if (subCategory != null && !subCategory.isEmpty()){
            query += SqlVariables.columnSubCategory.getColumnName() + " = '" + subCategory + "' AND ";
        }
        if (source != null && !source.isEmpty()){
            query += SqlVariables.columnSource.getColumnName() + " = '" + source + "' AND ";
        }
        if (english != null && !english.isEmpty()){
            query += SqlVariables.columnEnglish.getColumnName() + " = '" + english + "' AND ";
        }
        if (translation != null && !translation.isEmpty()){
            query += SqlVariables.columnTranslation.getColumnName() + " = '" + translation + "' AND ";
        }

        if (query.endsWith("AND ")){
            query = query.substring(0, query.length() - 5);
            return query;
        }
        return null;
    }

    public void setEnCatSubcat(String english, String category, String subCategory){
        this.english = english;
        this.category = category;
        this.subCategory = subCategory;
    }

}
