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

    public String[] getMatching(SqlVariables column, boolean searchAlike) {
        // create query -> execute -> return result
        String query = getSearchQuery();
        query = query.replace("*", column.getColumnName());
        String[][] result = plugin.getSqlActions().executeSearchQuery(query);
        if(result.length == 0 && searchAlike){
            return new String[]{getPlaceholderMatches()};
        }
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

    public void setMenuAcitons(String en, Colors defaultColor){
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

    public static String replaceSpecialSpaces(String input) {
        if(input == null){
            return null;
        }

        int[] specialSpaces = {32, 160, 8195, 8194, 8201, 8202, 8203, 12288};
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
