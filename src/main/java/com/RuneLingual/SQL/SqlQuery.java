package com.RuneLingual.SQL;

import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import lombok.Getter;
import lombok.Setter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter @Setter
public class SqlQuery implements Cloneable{
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
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlQuery sqlQuery = (SqlQuery) o;
        return Objects.equals(english, sqlQuery.english) &&
                Objects.equals(category, sqlQuery.category) &&
                Objects.equals(subCategory, sqlQuery.subCategory) &&
                Objects.equals(source, sqlQuery.source) &&
                Objects.equals(translation, sqlQuery.translation);
    }
    @Override
    public int hashCode() {
        return Objects.hash(english, category, subCategory, source, translation);
    }
    public SqlQuery copy() {
        try {
            return (SqlQuery) this.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone not supported", e);
        }
    }

    public String[] getMatching(SqlVariables column, @Deprecated boolean searchAlike) {
        english = replaceSpecialSpaces(english);
        // cache results in memory so the same lookup hits the H2 file only once
        String cacheKey = column.getColumnName() + "|" + english + "|"
                + (category == null ? "" : category) + "|"
                + (subCategory == null ? "" : subCategory) + "|"
                + (source == null ? "" : source);
        String[] cached = plugin.getSqlActions().getCachedMatch(cacheKey);
        if (cached != null) {
            return cached;
        }
        String[] computed = computeMatching(column);
        plugin.getSqlActions().putCachedMatch(cacheKey, computed);
        return computed;
    }

    // Priority-based fallback chain for finding the best matching translation
    // P1: english + category + subCategory + source   P2: + category + subCategory   P3: + category
    // P4: english only, case-insensitive   P5: fuzzy (alphanumeric-only) + category + subCategory
    private String[] computeMatching(SqlVariables column) {
        String[][] result;

        // P1: Full 5-tuple exact match (english + category + subCategory + source)
        result = searchWithFields(column, true, true, true, false);
        if (result.length > 0) {
            return extractTranslations(result);
        }

        // P2: Relax source constraint (english + category + subCategory)
        result = searchWithFields(column, true, true, false, false);
        if (result.length > 0) {
            return extractTranslations(result);
        }

        // P3: Relax subCategory constraint (english + category)
        result = searchWithFields(column, true, false, false, false);
        if (result.length > 0) {
            return extractTranslations(result);
        }

        // P4: english only, case-insensitive match
        result = searchWithFields(column, false, false, false, true);
        if (result.length > 0) {
            return extractTranslations(result);
        }

        // P5: fuzzy match in english (strip all non-alphanumeric chars, compare case-insensitive) + category + subCategory
        // Handles cases where the database english and actual text differ in spaces or punctuation
        result = getFuzzyMatch(column);
        if (result.length > 0) {
            return extractTranslations(result);
        }
        return new String[0];
    }

    /**
     * Builds a SELECT query with the specified column constraints and executes it.
     * @param column The column to select
     * @param useCat Whether to include category in WHERE clause
     * @param useSubCat Whether to include subCategory in WHERE clause
     * @param useSource Whether to include source in WHERE clause
     * @param ignoreCase Whether to use case-insensitive comparison on English match
     * @return 2D result array from the query
     */
    private String[][] searchWithFields(SqlVariables column, boolean useCat, boolean useSubCat, boolean useSource, boolean ignoreCase) {
        String query = buildQueryForColumns(useCat, useSubCat, useSource, ignoreCase);
        if (query == null) {
            return new String[0][0];
        }
        query = query.replace("*", column.getColumnName());
        return plugin.getSqlActions().executeSearchQuery(query);
    }

    /**
     * Constructs a SQL SELECT query with the specified column constraints.
     * The WHERE clause always includes english, and optionally category, subCategory, and source.
     * @param useCat Whether to include category in WHERE clause
     * @param useSubCat Whether to include subCategory in WHERE clause
     * @param useSource Whether to include source in WHERE clause
     * @param ignoreCase Whether to use case-insensitive comparison on english
     * @return A complete SQL query string
     */
    private String buildQueryForColumns(boolean useCat, boolean useSubCat, boolean useSource, boolean ignoreCase) {
        List<String> clauses = new ArrayList<>();
        if (ignoreCase) {
            clauses.add("UPPER(" + SqlVariables.columnEnglish.getColumnName() + ") = UPPER('" + english.replace("'", "''") + "')");
        } else {
            clauses.add(SqlVariables.columnEnglish.getColumnName() + " = '" + english.replace("'", "''") + "'");
        }
        if (useCat && category != null && !category.isEmpty()) {
            clauses.add(SqlVariables.columnCategory.getColumnName() + " = '" + category + "'");
        }
        if (useSubCat && subCategory != null && !subCategory.isEmpty()) {
            clauses.add(SqlVariables.columnSubCategory.getColumnName() + " = '" + subCategory + "'");
        }
        if (useSource && source != null && !source.isEmpty()) {
            clauses.add(SqlVariables.columnSource.getColumnName() + " = '" + source + "'");
        }
        return "SELECT * FROM " + SqlActions.tableName + " WHERE " + String.join(" AND ", clauses);
    }

    /**
     * Extracts the first column (translation) from each row of a 2D result array.
     * @param result The 2D result array from a SQL query
     * @return A 1D array of the first column values
     */
    private String[] extractTranslations(String[][] result) {
        String[] translations = new String[result.length];
        for (int i = 0; i < result.length; i++) {
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

    /**
     * P5 fuzzy matching: fetch candidate rows from DB, then normalize both sides in Java
     * by stripping all non-alphanumeric characters and lowercasing.
     * This catches cases where the only differences are spaces, punctuation, or letter case.
     */
    private String[][] getFuzzyMatch(SqlVariables column) {
        // Require both category and subCategory (like P1-P2)
        if (category == null || category.isEmpty() || subCategory == null || subCategory.isEmpty()) {
            return new String[0][0];
        }

        String normalizedEnglish = normalizeForFuzzyMatch(english);
        if (normalizedEnglish.isEmpty()) {
            return new String[0][0];
        }

        // Build query with mandatory category + subCategory
        String query = "SELECT english, " + column.getColumnName() + " FROM " + SqlActions.tableName
                + " WHERE " + SqlVariables.columnCategory.getColumnName() + " = '" + category.replace("'", "''") + "'"
                + " AND " + SqlVariables.columnSubCategory.getColumnName() + " = '" + subCategory.replace("'", "''") + "'";

        String[][] results = plugin.getSqlActions().executeSearchQuery(query);
        if (results == null || results.length == 0) {
            return new String[0][0];
        }

        // Java-side fuzzy comparison on normalized text
        List<String[]> matches = new ArrayList<>();
        for (String[] row : results) {
            if (row.length >= 2 && row[0] != null) {
                String dbNormalized = normalizeForFuzzyMatch(row[0]);
                if (dbNormalized.equals(normalizedEnglish)) {
                    matches.add(new String[]{row[1]});
                }
            }
        }
        return matches.toArray(new String[0][0]);
    }

    /**
     * Keep only letters (a-z, A-Z) and digits (0-9), lowercase everything.
     * Used by P5 to compare text ignoring spaces, punctuation, and case differences.
     */
    private static String normalizeForFuzzyMatch(String str) {
        if (str == null) return "";
        return str.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    @Deprecated
    private String getPlaceholderMatches(){
        /*
        returns translation which includes placeholders at first that matches the english text,
        with the placeholders replaced with the corresponding english word/number.
        placeholders =  %s0, %s1,... for strings atleast 1 alphabet and 0 or more numbers/spaces
                        %d0, %d1,... for numbers (and only numbers)
        1. gets all records that contains placeholder values in English, and matches the query except for english
        if no matches with placeholders are found, returns the original english text
        2. returns the translation of the first match
        3. if no match is found, returns the original english text
        not tested for %s, nor tested throughly for %d
         */
        String[] placeholders = {"%s", "%d"};
        String query = getPlaceholderSearchQuery(placeholders);
        String[][] result = plugin.getSqlActions().executeSearchQuery(query);
        // returns a placeholder if no matches are found
        if (result == null || result.length == 0){
            return english;
        }
        for (String[] row : result){
            String englishWithPlaceholders = row[0];
            String translationWithPlaceholders = row[1];
            String replacedMatch = englishWithPlaceholders;
            // Replace placeholders
            // Replace placeholders for strings
            for (int i = 0; i < 100; i++) {
                String beforeReplace = replacedMatch;
                replacedMatch = replacedMatch.replace("%s" + i, "[ \\w]+");
                if (beforeReplace.equals(replacedMatch)){
                    break;
                }
            }

            // Replace placeholders for numbers
            for (int i = 0; i < 100; i++) {
                String beforeReplace = replacedMatch;
                replacedMatch = replacedMatch.replace("%d" + i, "\\d+");
                if (beforeReplace.equals(replacedMatch)){
                    break;
                }
            }

            replacedMatch = stringToRegex(replacedMatch);

            Pattern pattern = Pattern.compile(replacedMatch);
            Matcher matcher = pattern.matcher(this.english);
            if (!matcher.matches()){
                continue;
            }
            List<String> matchedStrings = new ArrayList<>();
            List<String> matchedNumbers = new ArrayList<>();
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group.matches("\\d+")) {
                    matchedStrings.add(group);
                } else if (group.matches("[ \\w]+")) {
                    matchedNumbers.add(group);
                }
            }

            // Replace placeholders in the translated text
            String translation = translationWithPlaceholders;
            for (int i = 0; i < matchedStrings.size(); i++) {
                translation = translation.replace("%s" + i, matchedStrings.get(i));
            }
            for (int i = 0; i < matchedNumbers.size(); i++) {
                translation = translation.replace("%d" + i, matchedNumbers.get(i));
            }
            return translation;
        }

        return english;
    }

    private String stringToRegex(String str){
        return str.replaceAll("([\\[\\](){}*+?^$.|])", "\\\\$1");
    }

    @Deprecated
    public String getSearchQuery() {
        english = replaceSpecialSpaces(english);

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

    @Deprecated
    public String getSearchQuery_IgnoreCase() {
        english = replaceSpecialSpaces(english);

        // creates query that matches all non-empty fields
        // returns null if no fields are filled
        String query = "SELECT * FROM " + SqlActions.tableName + " WHERE UPPER(";
        if (english != null && !english.isEmpty()){
            query += SqlVariables.columnEnglish.getColumnName() + ") = UPPER('" + english.replace("'","''") + "') AND ";
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

    @Deprecated
    public String getPlaceholderSearchQuery(String[] placeholders) {
        // creates query that matches all non-empty fields
        // returns null if no fields are filled
        // return only english
        String query = "SELECT english, translation FROM " + SqlActions.tableName + " WHERE (english LIKE '%\\%s%' OR english LIKE '%\\%d%') AND ";
        if (category != null && !category.isEmpty()){
            query += SqlVariables.columnCategory.getColumnName() + " = '" + category + "' AND ";
        }
        if (subCategory != null && !subCategory.isEmpty()){
            query += SqlVariables.columnSubCategory.getColumnName() + " = '" + subCategory + "' AND ";
        }
        if (source != null && !source.isEmpty()){
            query += SqlVariables.columnSource.getColumnName() + " = '" + source + "' AND ";
        }
        if (query.endsWith("AND ")){
            query = query.substring(0, query.length() - 5);
            return query;
        }
        //todo: add more here if columns to be filtered are added
        return query;
    }

    public void setEnCatSubcat(String english, String category, String subCategory, Colors defaultColor){
        this.english = english;
        this.category = category;
        this.subCategory = subCategory;
        this.color = defaultColor;
    }

    public void setItemName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4Name.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Item.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public boolean isItemNameQuery(){
        return english != null
                && Objects.equals(category, SqlVariables.categoryValue4Name.getValue())
                && Objects.equals(subCategory, SqlVariables.subcategoryValue4Item.getValue())
                && color != null;
    }

    public void setNpcName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4Name.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Npc.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setObjectName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4Name.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Obj.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setExamineTextItem(String en) {
        this.english = en;
        this.category = SqlVariables.categoryValue4Examine.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Item.getValue();
        this.color = Colors.black;
        this.source = null;
        this.translation = null;
    }
    public void setExamineTextNPC(String en) {
        this.english = en;
        this.category = SqlVariables.categoryValue4Examine.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Npc.getValue();
        this.color = Colors.black;
        this.source = null;
        this.translation = null;
    }
    public void setExamineTextObject(String en) {
        this.english = en;
        this.category = SqlVariables.categoryValue4Examine.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Obj.getValue();
        this.color = Colors.black;
        this.source = null;
        this.translation = null;
    }
    public void setGameMessage(String en){
        this.english = en;
        this.category = SqlVariables.categoryValue4GameMessage.getValue();
        this.subCategory = null;
        this.color = Colors.black;
        this.source = null;
        this.translation = null;
    }

    public void setMenuName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4Name.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Menu.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }



    public void setInventoryItemActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4InventActions.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Item.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setGroundItemActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4Actions.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Item.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setNpcActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4Actions.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Npc.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setObjectActions(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4Actions.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Obj.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setGenMenuAcitons(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4Actions.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Menu.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setPlayerActions(String en, Colors defualtColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4Actions.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Player.getValue();
        this.color = defualtColor;
        this.source = null;
        this.translation = null;
    }
    public void setPlayerLevel() {
        this.english = "level";
        this.category = SqlVariables.categoryValue4Name.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Level.getValue();
        this.source = null;
        this.translation = null;
    }

    public void setDialogue(String en, String npcTalkingTo, boolean speakerIsPlayer, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4Dialogue.getValue();
        this.subCategory = npcTalkingTo;
        this.color = defaultColor;
        if(speakerIsPlayer){
            this.source = "Player";
        } else {
            this.source = npcTalkingTo;
        }
        this.translation = null;
    }

    public void setQuestName(String en, Colors defaultColor){
        this.english = en;
        this.category = SqlVariables.categoryValue4Manual.getValue();
        this.subCategory = SqlVariables.subcategoryValue4Quest.getValue();
        this.color = defaultColor;
        this.source = null;
        this.translation = null;
    }

    public void setGeneralUI(String source){
        this.english = null;
        this.category = SqlVariables.categoryValue4Interface.getValue();
        this.subCategory = SqlVariables.subcategoryValue4GeneralUI.getValue();
        this.source = source;
        this.translation = null;
    }

    public static String replaceSpecialSpaces(String input) {
        if(input == null){
            return null;
        }

        int[] specialSpaces = {9, 32, 160, 8195, 8194, 8201, 8202, 8203, 12288};
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            int codePoint = input.codePointAt(i);
            boolean isSpecialSpace = false;

            for (int specialSpace : specialSpaces) {
                if (codePoint == specialSpace) {
                    isSpecialSpace = true;
                    break;
                }
            }

            if (isSpecialSpace) {
                result.append(' ');
            } else {
                result.appendCodePoint(codePoint);
            }
        }

        return result.toString();
    }

    /*
        * Replaces numbers in the input string with placeholders.
        * Numbers are replaced with <Num0>, <Num1>, <Num2>, etc.
        * For example, "Hello Asda123, how many 1s are there in 101?" becomes
        *              "Hello Asda<Num0>, how many <Num1>s are there in <Num2>?"
        * but if the number is between < and >, it is not replaced.
     */
    public static String replaceNumbersWithPlaceholders(String input) {
        if(input == null){
            return null;
        }

        StringBuilder result = new StringBuilder();
        int numberCount = 0;
        boolean lastCharWasNumber = false;
        Set<Character> punctuationMarks = Set.of('.', ',', '?', '!');
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '<'){// if its a start of a tag, find the end of the tag
                for (int j = i + 1; j < input.length(); j++){
                    if (input.charAt(j) == '>'){// if the end of the tag is found, append the tag and continue from the end of the tag
                        result.append(input, i, j + 1);
                        i = j;
                        break;
                    }
                    // if the end of the string is reached, or letters between <> is longer than 15, or there is at least 1 punctuation,
                    // consider '<' as a normal character
                    if (j == input.length() - 1 || j-i > 15 || punctuationMarks.contains(input.charAt(j))){
                        result.append(c);
                        break;
                    }
                }
            } else if (Character.isDigit(c)) {
                if (!lastCharWasNumber) {
                    result.append("<Num").append(numberCount).append(">");
                    numberCount++;
                }
                lastCharWasNumber = true;
            } else // if the number is a decimal number or a large number, continue appending the number
                if ((c == '.' || c == ',') && lastCharWasNumber && i < input.length() - 1 && Character.isDigit(input.charAt(i + 1))) {
                continue;
            } else {
                result.append(c);
                lastCharWasNumber = false;
            }
        }

        return result.toString();
    }

    /*
        * Replaces placeholders in the original text with numbers from the translated text.
        * Placeholders are <Num0>, <Num1>, <Num2>, etc.
        * For example, if the original text is "Hello Asda123, how many 1s are there in 101?"
        * and the translated text is "こんにちは、アスダ<Num0>さん、<Num2>の中に<Num1>はいくつありますか？",
        * the result will be "こんにちは、アスダ123さん、101の中に1はいくつありますか？"
        * but if the number is between < and >, it is not replaced.
     */
    public static String replacePlaceholdersWithNumbers(String originalText, String translatedText) {
        if (originalText == null || translatedText == null) {
            return null;
        }
        String[] numbers = getNumbers(originalText);
        for (int i = 0; i < numbers.length; i++) {
            translatedText = translatedText.replace("<Num" + i + ">", numbers[i]);
        }
        return translatedText;
    }

    /*
     * Extracts numbers from the input string.
     * Numbers are sequences of digits.
     * For example, "Hello Asda123, how many 1s are there in 101?" returns ["123", "1", "101"]
     */
    private static String[] getNumbers(String input) {
        if(input == null){
            return null;
        }

        List<String> numbers = new ArrayList<>();
        StringBuilder currentNumber = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '<') {
                for (int j = i + 1; j < input.length(); j++) {
                    if (input.charAt(j) == '>') {
                        i = j;
                        break;
                    }
                    // if the end of the string is reached, or letters between <> is longer than 15, consider '<' as a normal character
                    if (j == input.length() - 1 || j-i > 15){
                        break;
                    }
                }
            } else
            if (Character.isDigit(c)) {
                currentNumber.append(c);
            } else if ((c == '.' || c == ',') && currentNumber.length() > 0 && i < input.length() - 1 && Character.isDigit(input.charAt(i + 1))) {
                // Append the decimal point if the number is a decimal number or a large number
                currentNumber.append(c);
            } else {
                if (currentNumber.length() > 0) {
                    numbers.add(currentNumber.toString());
                    currentNumber = new StringBuilder();
                }
            }
        }

        if (currentNumber.length() > 0) {
            numbers.add(currentNumber.toString());
        }

        return numbers.toArray(new String[0]);
    }

}
