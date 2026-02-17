package com.RuneLingual.Widgets;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.SQL.SqlQuery;
import com.RuneLingual.SQL.SqlVariables;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Ids;
import com.RuneLingual.commonFunctions.Transformer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.*;
import net.runelite.api.gameval.InterfaceID.*;

import javax.inject.Inject;
import java.awt.Rectangle;
import java.util.*;

import static com.RuneLingual.Widgets.WidgetsUtilRLingual.removeBrAndTags;

@Slf4j
public class WidgetCapture {
    private static final class BookPageState {
        private final int sourceHash;
        private final int renderedHash;
        private final List<List<String>> translatedLinesByColumn;

        private BookPageState(int sourceHash, int renderedHash, List<List<String>> translatedLinesByColumn) {
            this.sourceHash = sourceHash;
            this.renderedHash = renderedHash;
            this.translatedLinesByColumn = translatedLinesByColumn;
        }
    }

    private static int toInterfaceId(int componentId) {
        return WidgetUtil.componentToInterface(componentId);
    }

    private static final Set<Integer> API_BOOK_SCROLL_INTERFACE_IDS = Set.of(
            Book.UNIVERSE,
            IndexedBook.UNIVERSE,
            Bookofscrolls.UNIVERSE,
            Journalscroll.UNIVERSE,
            Questscroll.UNIVERSE,
            QuestscrollSpeedrun.UNIVERSE,
            Scroll.UNIVERSE,
            IiScroll.UNIVERSE,
            Longscroll.UNIVERSE,
            Eventscroll.UNIVERSE,
            HorrorPrayerbooks.UNIVERSE,
            SideJournal.UNIVERSE,
            Note.UNIVERSE,
            GhorrockRequisitionNote.UNIVERSE,
            toInterfaceId(Page.CONTENT_MODEL0),
            toInterfaceId(Questjournal.INFINITY),
            toInterfaceId(QuestjournalOverview.INFINITY),
            toInterfaceId(Letterscroll.ROOT_MODEL0),
            toInterfaceId(KingsLetterV2.KING_LETTER),
            toInterfaceId(Elem2MaintenanceScroll.KING_LETTER),
            toInterfaceId(SurokLetter1.ROOT_MODEL0),
            toInterfaceId(SurokLetter2.ROOT_MODEL0),
            toInterfaceId(ContactScrollBlood.ROOT_MODEL0),
            toInterfaceId(WiseOldManScroll.ROOT_MODEL0),
            toInterfaceId(ScrollGodfather.ROOT_MODEL0),
            toInterfaceId(BarbassaultScrollPl1.ROOT_MODEL0),
            toInterfaceId(BarbassaultScrollPl2.ROOT_MODEL0),
            toInterfaceId(BlastFurnacePlanScroll.CONTENTS_MODEL0),
            toInterfaceId(LostTribeSymbolBook.LOST_TRIBE_BOOK_COVER),
            toInterfaceId(PengClockworkBookInterface.BOOK),
            toInterfaceId(PogBolriesDiary.BACKGROUND_MODEL),
            toInterfaceId(PohBookcase.INFINITE),
            toInterfaceId(Messagescroll.ROOT_MODEL0),
            toInterfaceId(Messagescroll2.MESSAGESCROLL2_CLOSE),
            toInterfaceId(MessagescrollHandwriting.ROOT_MODEL0),
            toInterfaceId(MessagescrollHandwriting2.ROOT_MODEL0),
            toInterfaceId(MessagescrollHandwriting3.ROOT_MODEL0),
            toInterfaceId(TrailCluetext.ROOT_MODEL0),
            toInterfaceId(ChampionsScroll.ROOT_MODEL0),
            Messagebox.UNIVERSE,
            MessageboxTitled.UNIVERSE,
            MessageboxUrl.UNIVERSE,
            toInterfaceId(MmMessage.ROOT_MODEL0),
            toInterfaceId(Fairy2Message.ROOT_MODEL0),
            CluequestMap.UNIVERSE,
            CoaTablet1.UNIVERSE,
            CoaTablet2.UNIVERSE,
            CoaTablet3.UNIVERSE,
            CoaTablet4.UNIVERSE,
            EyegloCluePanel.UNIVERSE,
            toInterfaceId(EyegloGnomeMachineLocked.GNOME_MACHINE_BACKGROUND_LOCKED),
            toInterfaceId(EyegloGnomeMachineUnlocked.MACHINE_UNLOACKED_BACKGROUND),
            toInterfaceId(TrailClueEasyMap006.BG_SCROLL),
            toInterfaceId(TrailClueHardMap006.BG_SCROLL),
            toInterfaceId(TrailClueHardMap007.BG_SCROLL),
            toInterfaceId(TrailClueMediumMap008.BG_SCROLL),
            toInterfaceId(TrailClueMediumMap009.BG_SCROLL),
            toInterfaceId(TrailClueMediumMap010.BG_SCROLL),
            toInterfaceId(TrailClueMediumMap011.BG_SCROLL),
            toInterfaceId(TrailClueMediumMap012.BG_SCROLL),
            ChampionsLog.UNIVERSE,
            KillLog.UNIVERSE,
            FairyringsLog.UNIVERSE,
            SailingLog.UNIVERSE
    );

    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    Client client;
    @Inject
    private Transformer transformer;


    @Inject
    private WidgetsUtilRLingual widgetsUtilRLingual;
    @Inject
    private DialogTranslator dialogTranslator;
    @Inject
    private Ids ids;
    @Getter
    Set<String> pastTranslationResults = new HashSet<>();
    private final Set<Integer> loggedBookFallbackGroups = new HashSet<>();
    private final Set<Integer> processedBookLineParents = new HashSet<>();
    private final Map<Integer, BookPageState> bookPageCache = new HashMap<>();


    @Inject
    public WidgetCapture(RuneLingualPlugin plugin) {
        this.plugin = plugin;
        ids = this.plugin.getIds();
    }

    public void translateWidget() {
        if (plugin.getConfig().getInterfaceTextConfig() == RuneLingualConfig.ingameTranslationConfig.DONT_TRANSLATE
         && plugin.getConfig().getNpcDialogueConfig() == RuneLingualConfig.ingameTranslationConfig.DONT_TRANSLATE) {
            return;
        }
        processedBookLineParents.clear();
        Widget[] roots = client.getWidgetRoots();
        SqlQuery sqlQuery = new SqlQuery(this.plugin);
        for (Widget root : roots) {
            translateWidgetRecursive(root, sqlQuery);
        }
    }

    private void translateWidgetRecursive(Widget widget,SqlQuery sqlQuery) {
        int widgetId = widget.getId();

        // stop the recursion if the widget is hidden, outside the window or should be ignored
        // without the isOutsideWindow check, client will lag heavily when opening deep widget hierarchy, like combat achievement task list
//        if (widget.isHidden() || (!isInLobby() && isOutsideWindow(widget)) || ids.getWidgetIdNot2Translate().contains(widgetId)) {
//            return;
//        }
        if (widget.isHidden()) {
            return;
        }
        if (!isInLobby() && isOutsideWindow(widget)) {
            return;
        }
        if (ids.getWidgetIdNot2Translate().contains(widgetId)) {
            return;
        }
        if (ids.getWidgetIdNot2ApiTranslate().contains(widgetId)) {
            return;
        }

        int widgetGroup = WidgetUtil.componentToInterface(widgetId);
        modifySqlQuery4Widget(widget, sqlQuery);

        // recursive call
        for (Widget dynamicChild : widget.getDynamicChildren()) {
            translateWidgetRecursive(dynamicChild, sqlQuery);
        }
        for (Widget nestedChild : widget.getNestedChildren()) {
            translateWidgetRecursive(nestedChild, sqlQuery);
        }
        for (Widget staticChild : widget.getStaticChildren()) {
            translateWidgetRecursive(staticChild, sqlQuery);
        }

        // translate the widget text////////////////
        // dialogues are handled separately
        if (widgetGroup == WidgetUtil.componentToInterface(ChatLeft.NAME)
                || widgetGroup == WidgetUtil.componentToInterface(ChatRight.NAME)
                || widgetGroup == WidgetUtil.componentToInterface(Chatmenu.OPTIONS)) {
            dialogTranslator.handleDialogs(widget);
            //alignIfChatButton(widget);
            return;
        }

        boolean interfaceApiMode = plugin.getConfig().getInterfaceTextConfig() == RuneLingualConfig.ingameTranslationConfig.USE_API
                && plugin.getConfig().ApiConfig();
        boolean booksAndScrollsApiEnabled = plugin.getConfig().apiBooksAndScrolls();
        boolean isApiBookOrScrollWidget = booksAndScrollsApiEnabled
                && (API_BOOK_SCROLL_INTERFACE_IDS.contains(widgetGroup)
                || shouldUseBookOrScrollFallbackApi(widget, widgetGroup));

        // Book/scroll widgets do not always use WidgetType.TEXT + valid font ids.
        // Handle them with a dedicated API path based on "has translatable text".
        if (interfaceApiMode && isApiBookOrScrollWidget) {
            translateWidgetApiBookOrScroll(widget);
            return;
        }

        if (interfaceApiMode && shouldApiTranslateLobbyWidget(widget)) {
            translateWidgetApiLobby(widget);
            return;
        }

        if(shouldTranslateWidget(widget)) {
            if (interfaceApiMode) {
                return;
            }

            SqlQuery queryToPass = sqlQuery.copy();
            // replace sqlQuery if they are defined as item, npc, object, quest names
            Colors textColor = Colors.getColorFromHex(Colors.IntToHex(widget.getTextColor()));
            if (isChildWidgetOf(widget, ComponentID.CHATBOX_BUTTONS)) { // chat buttons
                queryToPass.setCategory(SqlVariables.categoryValue4Interface.getValue());
                queryToPass.setSubCategory(SqlVariables.subCategoryValue4ChatButtons.getValue());
            } else if (isChildWidgetOf(widget, ids.getLoginScreenId())) { // login screen
                queryToPass.setCategory(SqlVariables.categoryValue4Interface.getValue());
                queryToPass.setSubCategory(SqlVariables.subCategoryValue4LoginScreen.getValue());
            } else if (isChildWidgetOf(widget, ids.getWidgetIdCA())) {
                queryToPass.setCategory(SqlVariables.categoryValue4Interface.getValue());
                queryToPass.setSubCategory(SqlVariables.subCategoryValue4CA.getValue());
            }
            else {
                if (queryToPass.getCategory() == null) {
                    queryToPass.setCategory(SqlVariables.categoryValue4Interface.getValue());
                }
                if (queryToPass.getSubCategory() == null) {
                    queryToPass.setSubCategory(SqlVariables.subcategoryValue4GeneralUI.getValue());
                }
            }

            if (ids.getWidgetIdItemName().contains(widgetId)
                && !(widgetId == ComponentID.COMBAT_WEAPON_NAME && Objects.equals(widget.getText(), "Unarmed")) // "Unarmed" in combat tab is not an item
                ) {
                    String itemName = Colors.removeNonImgTags(widget.getText());
                    queryToPass.setItemName(itemName, textColor);
            } else if (ids.getWidgetIdNpcName().contains(widgetId)) {
                String npcName = Colors.removeNonImgTags(widget.getText());
                queryToPass.setNpcName(npcName, textColor);
            } else if (ids.getWidgetIdObjectName().contains(widgetId)) {
                String objectName = Colors.removeNonImgTags(widget.getText());
                queryToPass.setObjectName(objectName, textColor);
            } else if (ids.getWidgetIdQuestName().contains(widgetId)) {
                String questName = Colors.removeNonImgTags(widget.getText());
                queryToPass.setQuestName(questName, textColor);
            } else {
                queryToPass.setColor(textColor);
            }
            // debug: if the widget is the target for dumping
            ifIsDumpTarget_thenDump(widget, queryToPass);
            // translate the widget text
            translateWidgetText(widget, queryToPass);

            alignIfChatButton(widget);
        }
    }

    private void translateWidgetApi(Widget widget) {
        String text = widget.getText();
        Colors color = Colors.getColorArray(widget.getText(), Colors.getColorFromHex(Colors.IntToHex(widget.getTextColor())))[0];
        widgetsUtilRLingual.setWidgetText_ApiTranslation(widget, text, color);
        widgetsUtilRLingual.changeLineHeight(widget);
        widgetsUtilRLingual.changeWidgetSize_ifNeeded(widget);
    }

    private void translateWidgetApiBookOrScroll(Widget widget) {
        String text = widget.getText();
        if (!shouldTranslateTextForBookOrScrollApi(text)) {
            return;
        }

        if (isLikelyBookLineWidget(widget)) {
            translateBookLineWidgetsByParent(widget);
            return;
        }
        translateWidgetApi(widget);
    }

    private boolean shouldTranslateTextForBookOrScrollApi(String text) {
        if (text == null) {
            return false;
        }
        String modifiedText = Colors.removeAllTags(text).trim();
        if (modifiedText.isEmpty()) {
            return false;
        }
        if (!modifiedText.matches(".*[a-zA-Z].*")) {
            return false;
        }
        return true;
    }

    private boolean shouldApiTranslateLobbyWidget(Widget widget) {
        if (!plugin.getConfig().apiLobbyScreen() || !isInLobby()) {
            return false;
        }
        if (!shouldTranslateWidget(widget)) {
            return false;
        }

        String sourceText = Colors.removeNonImgTags(widget.getText()).trim();
        if (sourceText.isEmpty() || !sourceText.matches(".*[a-zA-Z].*")) {
            return false;
        }
        if (plugin.getDeepl().getDeeplPastTranslationManager().getTranslationResults().contains(sourceText)) {
            return false;
        }
        return looksLikeEnglishSource(sourceText);
    }

    private void translateWidgetApiLobby(Widget widget) {
        String sourceText = Colors.removeNonImgTags(widget.getText()).trim();
        if (sourceText.isEmpty()) {
            return;
        }

        String translatedText = plugin.getDeepl().translate(
                sourceText,
                LangCodeSelectableList.ENGLISH,
                plugin.getConfig().getSelectedLanguage()
        );
        if (translatedText.equals(sourceText)) {
            return;
        }

        Colors color = Colors.getColorArray(widget.getText(), Colors.getColorFromHex(Colors.IntToHex(widget.getTextColor())))[0];
        widgetsUtilRLingual.setWidgetText_ApiTranslatedResolved(widget, translatedText, color, false);
        widgetsUtilRLingual.changeLineHeight(widget);
        widgetsUtilRLingual.changeWidgetSize_ifNeeded(widget);
    }

    private boolean isLikelyBookLineWidget(Widget widget) {
        if (!hasBookPageNumberSibling(widget)) {
            return false;
        }
        return widget.getHeight() > 0 && widget.getHeight() <= 45
                && widget.getWidth() >= 60;
    }

    private void translateBookLineWidgetsByParent(Widget seedWidget) {
        Widget parent = seedWidget.getParent();
        if (parent == null) {
            translateWidgetApi(seedWidget);
            return;
        }

        int parentId = parent.getId();
        if (!processedBookLineParents.add(parentId)) {
            return;
        }

        List<Widget> bookLineWidgets = collectBookLineWidgets(parent);
        if (bookLineWidgets.size() < 2) {
            translateWidgetApi(seedWidget);
            return;
        }

        List<List<Widget>> columns = splitIntoColumns(bookLineWidgets);
        for (List<Widget> column : columns) {
            column.sort(Comparator.comparingInt(w -> w.getBounds().y));
        }

        String sourceText = buildBookPageSourceText(columns);
        if (sourceText.isEmpty()) {
            return;
        }

        int sourceHash = sourceText.hashCode();
        int currentRenderedHash = getTextHash(bookLineWidgets);
        BookPageState cachedState = bookPageCache.get(parentId);

        if (cachedState != null) {
            if (currentRenderedHash == cachedState.renderedHash) {
                return;
            }
            if (sourceHash == cachedState.sourceHash || !looksLikeEnglishSource(sourceText)) {
                applyPageLines(columns, cachedState.translatedLinesByColumn);
                return;
            }
        }

        if (!looksLikeEnglishSource(sourceText)) {
            return;
        }

        String translatedText = plugin.getDeepl().translate(
                sourceText,
                LangCodeSelectableList.ENGLISH,
                plugin.getConfig().getSelectedLanguage()
        );
        if (translatedText.equals(sourceText)) {
            return;
        }

        List<List<String>> translatedLinesByColumn = distributeTranslatedTextAcrossColumns(translatedText, columns);
        applyPageLines(columns, translatedLinesByColumn);
        int renderedHash = getTextHash(bookLineWidgets);
        bookPageCache.put(parentId, new BookPageState(sourceHash, renderedHash, deepCopyLines(translatedLinesByColumn)));
    }

    private List<Widget> collectBookLineWidgets(Widget parent) {
        List<Widget> result = new ArrayList<>();
        for (Widget child : parent.getDynamicChildren()) {
            if (isLikelyBookLineWidget(child) && shouldTranslateTextForBookOrScrollApi(child.getText())) {
                result.add(child);
            }
        }
        for (Widget child : parent.getNestedChildren()) {
            if (isLikelyBookLineWidget(child) && shouldTranslateTextForBookOrScrollApi(child.getText())) {
                result.add(child);
            }
        }
        for (Widget child : parent.getStaticChildren()) {
            if (isLikelyBookLineWidget(child) && shouldTranslateTextForBookOrScrollApi(child.getText())) {
                result.add(child);
            }
        }
        return result;
    }

    private List<List<Widget>> splitIntoColumns(List<Widget> bookLineWidgets) {
        if (bookLineWidgets.size() < 2) {
            return Collections.singletonList(bookLineWidgets);
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (Widget widget : bookLineWidgets) {
            Rectangle bounds = widget.getBounds();
            minX = Math.min(minX, bounds.x);
            maxX = Math.max(maxX, bounds.x);
        }

        if (maxX - minX < 120) {
            return Collections.singletonList(bookLineWidgets);
        }

        int splitX = minX + ((maxX - minX) / 2);
        List<Widget> left = new ArrayList<>();
        List<Widget> right = new ArrayList<>();

        for (Widget widget : bookLineWidgets) {
            Rectangle bounds = widget.getBounds();
            int centerX = bounds.x + (Math.max(bounds.width, 1) / 2);
            if (centerX <= splitX) {
                left.add(widget);
            } else {
                right.add(widget);
            }
        }

        if (left.isEmpty() || right.isEmpty()) {
            return Collections.singletonList(bookLineWidgets);
        }

        List<List<Widget>> columns = new ArrayList<>(2);
        columns.add(left);
        columns.add(right);
        return columns;
    }

    private String buildBookPageSourceText(List<List<Widget>> columns) {
        List<String> columnTexts = new ArrayList<>();
        for (List<Widget> column : columns) {
            List<String> sourceLines = new ArrayList<>();
            for (Widget widget : column) {
                String text = Colors.removeAllTags(widget.getText()).trim();
                if (!text.isEmpty()) {
                    sourceLines.add(text);
                }
            }
            if (!sourceLines.isEmpty()) {
                columnTexts.add(String.join(" ", sourceLines));
            }
        }
        return String.join(" ", columnTexts).trim();
    }

    private List<List<String>> distributeTranslatedTextAcrossColumns(String translatedText, List<List<Widget>> columns) {
        List<Integer> slotCapacities = new ArrayList<>();
        List<Integer> columnLineCounts = new ArrayList<>();

        for (List<Widget> column : columns) {
            int lineCount = column.size();
            columnLineCounts.add(lineCount);

            int minWidth = column.stream().mapToInt(Widget::getWidth).filter(w -> w > 0).min().orElse(120);
            int charWidth = Math.max(1, LangCodeSelectableList.getLatinCharWidth(column.get(0), plugin.getConfig().getSelectedLanguage()));
            int maxCharsPerLine = Math.max(6, (int) Math.floor((minWidth / (double) charWidth) * 0.72));
            for (int i = 0; i < lineCount; i++) {
                slotCapacities.add(maxCharsPerLine);
            }
        }

        List<String> slotLines = wrapTextToCapacitySlots(translatedText, slotCapacities);
        List<List<String>> linesByColumn = new ArrayList<>();
        int cursor = 0;
        for (int columnIndex = 0; columnIndex < columnLineCounts.size(); columnIndex++) {
            int count = columnLineCounts.get(columnIndex);
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                lines.add(cursor < slotLines.size() ? slotLines.get(cursor) : "");
                cursor++;
            }
            linesByColumn.add(lines);
        }
        return linesByColumn;
    }

    private List<String> wrapTextToCapacitySlots(String text, List<Integer> capacities) {
        if (capacities.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalizedWords = new ArrayList<>();
        for (String word : text.trim().split("\\s+")) {
            if (!word.isEmpty()) {
                normalizedWords.add(word);
            }
        }

        List<String> lines = new ArrayList<>(Collections.nCopies(capacities.size(), ""));
        int wordIndex = 0;

        for (int slot = 0; slot < capacities.size(); slot++) {
            int capacity = Math.max(1, capacities.get(slot));
            StringBuilder line = new StringBuilder();

            while (wordIndex < normalizedWords.size()) {
                String word = normalizedWords.get(wordIndex);
                if (word.length() > capacity) {
                    if (line.length() == 0) {
                        line.append(word, 0, capacity);
                        normalizedWords.set(wordIndex, word.substring(capacity));
                    }
                    break;
                }

                if (line.length() == 0) {
                    line.append(word);
                    wordIndex++;
                    continue;
                }

                if (line.length() + 1 + word.length() <= capacity) {
                    line.append(' ').append(word);
                    wordIndex++;
                } else {
                    break;
                }
            }

            lines.set(slot, line.toString());
        }

        if (wordIndex < normalizedWords.size()) {
            StringBuilder tail = new StringBuilder(lines.get(lines.size() - 1));
            for (int i = wordIndex; i < normalizedWords.size(); i++) {
                String word = normalizedWords.get(i);
                if (word.isEmpty()) {
                    continue;
                }
                if (tail.length() > 0) {
                    tail.append(' ');
                }
                tail.append(word);
            }
            lines.set(lines.size() - 1, tail.toString());
        }

        return lines;
    }

    private void applyPageLines(List<List<Widget>> columns, List<List<String>> translatedLinesByColumn) {
        int columnCount = Math.min(columns.size(), translatedLinesByColumn.size());
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            List<Widget> widgets = columns.get(columnIndex);
            List<String> lines = translatedLinesByColumn.get(columnIndex);
            for (int i = 0; i < widgets.size(); i++) {
                String newLine = i < lines.size() ? lines.get(i) : "";
                widgets.get(i).setText(newLine);
            }
        }
    }

    private List<List<String>> deepCopyLines(List<List<String>> linesByColumn) {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> lines : linesByColumn) {
            copy.add(new ArrayList<>(lines));
        }
        return copy;
    }

    private boolean looksLikeEnglishSource(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return false;
        }

        int asciiLetters = 0;
        int accentedLetters = 0;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
                asciiLetters++;
            } else if ((ch >= 192 && ch <= 255) || ch == 'ã' || ch == 'õ' || ch == 'ç' || ch == 'á' || ch == 'é'
                    || ch == 'í' || ch == 'ó' || ch == 'ú' || ch == 'â' || ch == 'ê' || ch == 'ô') {
                accentedLetters++;
            }
        }

        if (asciiLetters == 0) {
            return false;
        }
        return accentedLetters * 4 < asciiLetters;
    }


    private int getTextHash(List<Widget> widgets) {
        StringBuilder builder = new StringBuilder();
        widgets.stream()
                .sorted(Comparator.comparingInt((Widget w) -> w.getBounds().x).thenComparingInt(w -> w.getBounds().y))
                .forEach(w -> builder.append(w.getId()).append('=').append(w.getText()).append('\n'));
        return builder.toString().hashCode();
    }

    private boolean shouldUseBookOrScrollFallbackApi(Widget widget, int widgetGroup) {
        String text = widget.getText();
        if (!shouldTranslateTextForBookOrScrollApi(text)) {
            return false;
        }

        // Don't target chat widgets with long text.
        if (isChildWidgetOf(widget, Chatbox.CHATDISPLAY)
                || isChildWidgetOf(widget, ComponentID.CHATBOX_BUTTONS)
                || widget.getId() == Chatbox.SCROLLAREA) {
            return false;
        }

        String strippedText = Colors.removeAllTags(text).trim();
        int width = widget.getWidth();
        int height = widget.getHeight();
        boolean isLongTextBlock = strippedText.length() >= 80;
        boolean looksLikeBookLine = hasBookPageNumberSibling(widget)
                && strippedText.length() >= 8
                && width >= 60
                && height >= 10
                && height <= 40;
        boolean looksLikeReadablePanelLine = isInsideLargeReadablePanel(widget)
                && strippedText.length() >= 16
                && width >= 80
                && height >= 10
                && height <= 45;

        if (!isLongTextBlock && !looksLikeBookLine && !looksLikeReadablePanelLine) {
            return false;
        }

        if (loggedBookFallbackGroups.add(widgetGroup)) {
            String preview = strippedText.length() > 64 ? strippedText.substring(0, 64) + "..." : strippedText;
            log.info("RuneLingual book/scroll API fallback matched group={}, widgetId={}, type={}, font={}, size={}x{}, text='{}'",
                    widgetGroup, widget.getId(), widget.getType(), widget.getFontId(), width, height, preview);
        }
        return true;
    }

    private boolean hasBookPageNumberSibling(Widget widget) {
        Widget parent = widget.getParent();
        if (parent == null) {
            return false;
        }

        int pageLikeSiblings = 0;
        for (Widget child : parent.getDynamicChildren()) {
            if (isPageNumberLike(child)) {
                pageLikeSiblings++;
            }
        }
        for (Widget child : parent.getNestedChildren()) {
            if (isPageNumberLike(child)) {
                pageLikeSiblings++;
            }
        }
        for (Widget child : parent.getStaticChildren()) {
            if (isPageNumberLike(child)) {
                pageLikeSiblings++;
            }
        }
        return pageLikeSiblings >= 2;
    }

    private boolean isPageNumberLike(Widget widget) {
        if (widget == null || widget.getText() == null) {
            return false;
        }
        String t = Colors.removeAllTags(widget.getText()).trim();
        if (!t.matches("\\d{1,2}")) {
            return false;
        }
        return widget.getWidth() <= 40 && widget.getHeight() <= 30;
    }

    private boolean isInsideLargeReadablePanel(Widget widget) {
        Widget parent = widget.getParent();
        while (parent != null) {
            if (parent.getWidth() >= 260 && parent.getHeight() >= 160) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private void modifySqlQuery4Widget(Widget widget, SqlQuery sqlQuery) {
        sqlQuery.setColor(Colors.getColorFromHex(Colors.IntToHex(widget.getTextColor())));
        int widgetId = widget.getId();
        if (widgetId == ids.getWidgetIdSkillGuide()) { //Id for parent of skill guide, or parent of element in list
            sqlQuery.setGeneralUI(SqlVariables.sourceValue4SkillGuideInterface.getValue());
        }
        /* example of using Sets:
        if (ids.getWidgetIdPlayerName().contains(widgetId)) {
            sqlQuery.setPlayerName(widget.getText(), sqlQuery.getColor());
        }
         */
        // add more general UIs here

        // if one of the main tabs, set the category and subcategory. main tabs = combat options, skills tab, etc.
        if (ids.getWidgetIdSetMainTabs().contains(widgetId)) {
            sqlQuery.setCategory(SqlVariables.categoryValue4Interface.getValue());
            sqlQuery.setSubCategory(SqlVariables.subcategoryValue4MainTabs.getValue());
        }
        // if one of the main tabs, set the source as the tab name
        if (sqlQuery.getCategory() != null && sqlQuery.getCategory().equals(SqlVariables.categoryValue4Interface.getValue())
                && sqlQuery.getSubCategory() != null && sqlQuery.getSubCategory().equals(SqlVariables.subcategoryValue4MainTabs.getValue())) {
            // set the source as the tab name
            if (widgetId == ids.getWidgetIdAttackStyleTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4CombatOptionsTab.getValue());
            } else if (widgetId == ids.getWidgetIdSkillsTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4SkillsTab.getValue());
            } else if (widgetId == ids.getWidgetIdCharacterSummaryTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4CharacterSummaryTab.getValue());
            } else if (widgetId == ids.getWidgetIdQuestTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4QuestListTab.getValue());
            } else if (widgetId == ids.getWidgetIdAchievementDiaryTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4AchievementDiaryTab.getValue());
            } else if (widgetId == ids.getWidgetIdPrayerTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4PrayerTab.getValue());
            } else if (widgetId == ids.getWidgetIdSpellBookTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4SpellBookTab.getValue());
            } else if (widgetId == ids.getWidgetIdGroupsTab() || widgetId == ids.getWidgetIdGroupTabNonGIM() || widgetId == ids.getWidgetIdPvPArena()) {
                sqlQuery.setSource(SqlVariables.sourceValue4GroupTab.getValue());
            } else if (widgetId == ids.getWidgetIdFriendsTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4FriendsTab.getValue());
            } else if (widgetId == ids.getWidgetIdIgnoreTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4IgnoreTab.getValue());
            } else if (widgetId == ids.getWidgetIdAccountManagementTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4AccountManagementTab.getValue());
            } else if (widgetId == ids.getWidgetIdSettingsTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4SettingsTab.getValue());
            } else if (widgetId == ids.getWidgetIdEmotesTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4EmotesTab.getValue());
            } else if (widgetId == ids.getWidgetIdMusicTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4MusicTab.getValue());
            } else if (widgetId == ids.getWidgetIdLogoutTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4LogoutTab.getValue());
            } else if (widgetId == ids.getWidgetIdWorldSwitcherTab()) {
                sqlQuery.setSource(SqlVariables.sourceValue4WorldSwitcherTab.getValue());
            }
        }
    }

    private void translateWidgetText(Widget widget, SqlQuery sqlQuery) {
        int widgetId = widget.getId();
        String originalText = widget.getText();
        String textToTranslate = getEnglishColValFromWidget(widget);
        String translatedText;
        if (ids.getWidgetIdAnyTranslated().contains(widgetId)) {
            sqlQuery.setCategory(null);
            sqlQuery.setSubCategory(null);
            sqlQuery.setSource(null);
            translatedText = getTranslationFromQuery(sqlQuery, originalText, textToTranslate);
        } else if (widgetsUtilRLingual.shouldPartiallyTranslateWidget(widget)) {
            // for widgets like "Name: <playerName>" (found in accounts management tab), where only the part of the text should be translated
            // order:
            // textToTranslate = "Name: <playerName>" -> translatedText = "名前: <playerName>" -> translatedText = "名前: Durial321"
            String translationWithPlaceHolder = getTranslationFromQuery(sqlQuery, originalText, textToTranslate);
            translatedText = ids.getPartialTranslationManager().translateWidget(widget, translationWithPlaceHolder, originalText, sqlQuery.getColor());
        } else if (!ids.getWidgetId2SplitTextAtBr().contains(widgetId)// for most cases
            && !ids.getWidgetId2KeepBr().contains(widgetId)) {
            translatedText = getTranslationFromQuery(sqlQuery, originalText, textToTranslate);
        } else if (ids.getWidgetId2SplitTextAtBr().contains(widgetId)){// for widgets that have <br> in the text and should be kept where they are, translate each line separately
            String[] textList = textToTranslate.split("<br>");
            String[] originalTextList = originalText.split("<br>");
            StringBuilder translatedTextBuilder = new StringBuilder();

            for (int i = 0; i < textList.length; i++) {
                String text = textList[i];
                String originalTextLine = originalTextList[i];
                String translatedTextPart = getTranslationFromQuery(sqlQuery, originalTextLine, text);
                if (translatedTextPart == null) { // if translation failed
                    return;
                }
                translatedTextBuilder.append(translatedTextPart);
                if (i != textList.length - 1) { // if it's not the last line, add <br>
                    translatedTextBuilder.append("<br>");
                }
            }

            translatedText = translatedTextBuilder.toString();
        } else { // if(ids.getWidgetId2KeepBr().contains(widgetId))
            // for widgets that have <br> in the text and should be kept where they are, translate the whole text
            translatedText = getTranslationFromQuery(sqlQuery, originalText, textToTranslate);
        }

        // translation was not available

        if(translatedText == null){ // if the translation is the same as the original with <br>
            return;
        }
        String originalWithoutBr = removeBrAndTags(originalText);
        String translationWithoutBr = removeBrAndTags(translatedText);
        if(Objects.equals(translatedText, originalText) // if the translation is the same as the original
                || originalWithoutBr.equals(translationWithoutBr)){ // if the translation is the same as the original without <br>
            return;
        }

        pastTranslationResults.add(translatedText);

        if (ids.getWidgetId2SplitTextAtBr().contains(widgetId)
                || ids.getWidgetId2KeepBr().contains(widgetId)
                || ids.getWidgetId2FixedSize().containsKey(widgetId)) {
            widgetsUtilRLingual.setWidgetText_BrAsIs(widget, translatedText);
        } else {
            widgetsUtilRLingual.setWidgetText_NiceBr(widget, translatedText);
        }
        widgetsUtilRLingual.changeLineHeight(widget);
        widgetsUtilRLingual.changeWidgetSize_ifNeeded(widget);
    }

    private String getTranslationFromQuery(SqlQuery sqlQuery, String originalText, String textToTranslate) {
        sqlQuery.setEnglish(textToTranslate);
        Transformer.TransformOption option = Transformer.TransformOption.TRANSLATE_LOCAL;
        return transformer.transformWithPlaceholders(originalText, textToTranslate, option, sqlQuery);
    }


    private boolean shouldTranslateWidget(Widget widget) {
        int widgetId = widget.getId();

        return shouldTranslateText(widget.getText())
                && widget.getType() == WidgetType.TEXT
                && widget.getFontId() != -1 // if font id is -1 it's probably not shown
                && !ids.getWidgetIdNot2Translate().contains(widgetId)
                && !isWidgetIdNot2Translate(widget);
    }

    public boolean isWidgetIdNot2Translate(Widget widget) {
        int widgetId = widget.getId();
        boolean isFriendsListNames = ComponentID.FRIEND_LIST_NAMES_CONTAINER == widgetId
                    && widget.getXTextAlignment() == WidgetTextAlignment.LEFT;
        boolean isGimMemberNames = ids.getGimMemberNameId() == widgetId
                    && widget.getXTextAlignment() == WidgetTextAlignment.LEFT
                    && widget.getTextColor() == 0xffffff; // if not white text its "Vacancy". use color because "Vacancy" could be player name
        boolean isFriendsChatList = ComponentID.FRIENDS_CHAT_LIST == widgetId
                    && widget.getType() == WidgetType.TEXT && widget.getTextColor() == 0xffffff; // check colors so its not world #
        boolean isFcTitleOrOwner = (ComponentID.FRIENDS_CHAT_TITLE == widgetId || ComponentID.FRIENDS_CHAT_OWNER == widgetId)
                && client.getFriendsChatManager() != null;
        // if its orange text, its clan name, world, member count etc,
        // but if its grey its "Your Clan" and "No current clan" which is displayed when not in a clan
        boolean isClanName = ComponentID.CLAN_HEADER == widgetId
                    && widget.getTextColor() == 0xe6981f;
        boolean isClanMemberName = ComponentID.CLAN_MEMBERS == widgetId
                    && widget.getTextColor() == 0xffffff;
        boolean isGuesClanName = ComponentID.CLAN_GUEST_HEADER == widgetId
                    && widget.getTextColor() == 0xe6981f;
        boolean isGuestClanMemberName = ComponentID.CLAN_GUEST_MEMBERS == widgetId
                    && widget.getTextColor() == 0xffffff;
        boolean isPvPMemberName = ids.getGroupTabPvPGroupMemberId() == widgetId
                    && (widget.getTextColor() == 0xffffff || widget.getTextColor() == 0x9f9f9f);
        boolean isGroupingGroupMemberName = ids.getGroupingGroupMemberNameId() == widgetId
                && widget.getTextColor() == 0xffffff;
        boolean isChatBoxLine = Chatbox.SCROLLAREA == widgetId;
        return isFriendsListNames || isGimMemberNames || isFcTitleOrOwner || isFriendsChatList || isClanMemberName || isClanName
                || isGuesClanName || isGuestClanMemberName || isPvPMemberName || isGroupingGroupMemberName || isChatBoxLine;
    }

    /* check if the text should be translated
     * returns true if the text contains letters excluding tags, has at least 1 alphabet, and has not been translated
     */
    private boolean shouldTranslateText(String text) {
        String modifiedText = text.trim();
        modifiedText = Colors.removeAllTags(modifiedText);
        return !modifiedText.isEmpty()
                && !pastTranslationResults.contains(text)
                && modifiedText.matches(".*[a-zA-Z].*")
                && !plugin.getConfig().getInterfaceTextConfig().equals(RuneLingualConfig.ingameTranslationConfig.DONT_TRANSLATE);
    }

    /*
      * get the English text from the widget that should be identical to the corresponding sql column value
      * used when creating the dump file for manual translation
      * and when searching for English added manually originating from the dump file
     */
    public String getEnglishColValFromWidget(Widget widget) {
        String text = widget.getText();
        if (text == null) {
            return "";
        }

        text = SqlQuery.replaceSpecialSpaces(text);
        text = Colors.getEnumeratedColorWord(text);
        text = SqlQuery.replaceNumbersWithPlaceholders(text);
        if (!ids.getWidgetId2SplitTextAtBr().contains(widget.getId())
                && !ids.getWidgetId2KeepBr().contains(widget.getId())) {
            text = text.replace(" <br>", " ");
            text = text.replace("<br> ", " ");
            text = text.replace("<br>", " ");
        }

        // special case: if the text should only be partially translated
        if (widgetsUtilRLingual.shouldPartiallyTranslateWidget(widget)) {
            String enColVal = ids.getPartialTranslationManager().getMatchingEnColVal(widget.getText());
            return widgetsUtilRLingual.getEnColVal4PartialTranslation(widget, enColVal);
        }

        return text;
    }



    // used for creating the English transcript used for manual translation
    private void ifIsDumpTarget_thenDump(Widget widget, SqlQuery sqlQuery) {
        if (!plugin.getConfig().enableLoggingWidget()) {
            return;
        }
//        if (sqlQuery.getSource() != null &&
//                (sqlQuery.getSource().equals(SqlVariables.sourceValue4FriendsTab.getValue())
//        || sqlQuery.getSource().equals(SqlVariables.sourceValue4IgnoreTab.getValue())
//        || sqlQuery.getSource().equals(SqlVariables.sourceValue4AccountManagementTab.getValue()))) {
        //if (sqlQuery.getSource() != null && sqlQuery.getSource().equals(SqlVariables.sourceValue4MusicTab.getValue())){
        if (isChildWidgetOf(widget, ids.getWidgetIdCA())){// change for new dump category
            if (widget.getText() == null || !shouldTranslateWidget(widget)) {
                return;
            }
            String fileName = "combAchvmt.txt"; // change for new dump category
            String textToDump = getEnglishColValFromWidget(widget);

            // for partial translation
            if (widgetsUtilRLingual.shouldPartiallyTranslateWidget(widget)) {
                textToDump = widgetsUtilRLingual.getEnColVal4PartialTranslation(widget, ids.getPartialTranslationManager().getMatchingEnColVal(widget.getText()));
            }
            String category = sqlQuery.getCategory();
            String subCategory = sqlQuery.getSubCategory();
            String source = sqlQuery.getSource();
            if (category == null) {
                category = "";
            }
            if (subCategory == null) {
                subCategory = "";
            }
            if (source == null) {
                source = "";
            }
            if (ids.getWidgetIdAnyTranslated().contains(widget.getId())) {
                return;
                //appendIfNotExistToFile(textToDump + "\t\t\t", fileName);
            } else if (ids.getWidgetId2SplitTextAtBr().contains(widget.getId())) {
                String[] textList = textToDump.split("<br>");
                for (String text : textList) {
                    plugin.getOutputToFile().appendIfNotExistToFile(text + "\t" + category +
                            "\t" + subCategory +
                            "\t" + source, fileName);
                }
            } else {
                plugin.getOutputToFile().appendIfNotExistToFile(textToDump + "\t" + category +
                        "\t" + subCategory +
                        "\t" + source, fileName);
            }
        }

    }

    private boolean isChildWidgetOf(Widget widget, int parentWidgetId) {
        Widget parent = widget.getParent();
        while (parent != null) {
            if (parent.getId() == parentWidgetId) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private boolean isChildWidgetOf(Widget widget, Set<Integer> parentWidgetIds) {
        Widget parent = widget.getParent();
        while (parent != null) {
            if (parentWidgetIds.contains(parent.getId())) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private void alignIfChatButton(Widget widget) {
        int widgetId = widget.getId();
        if (plugin.getConfig().getSelectedLanguage().isChatButtonHorizontal()) {
            if (ids.getWidgetIdChatButtonName().contains(widgetId)) { // if chat button is set to be horizontal, the button name is to be left aligned
                widget.setXTextAlignment(WidgetTextAlignment.LEFT);
            } else if (ids.getWidgetIdChatButtonFilterType().contains(widgetId)) { // the filter type is to be right aligned
                widget.setXTextAlignment(WidgetTextAlignment.RIGHT);
            }
        } else {
            String text = widget.getText();
            if (ids.getWidgetIdChatButtonName().contains(widgetId) && !text.contains("<br>")) {
                widget.setText(text+"<br>"); // add <br> to the button name to place this above the filter type
                widget.setXTextAlignment(WidgetTextAlignment.CENTER);
            } else if (ids.getWidgetIdChatButtonFilterType().contains(widgetId) && !text.contains("<br>")) {
                widget.setText("<br>"+text); // add <br> to the filter type to place this below the button name
                widget.setXTextAlignment(WidgetTextAlignment.CENTER);
            }
        }
    }

    private boolean isOutsideWindow(Widget widget) {
        Widget canvasWidget = null;
        for (int id : ids.getRootWidgetIdSet()) {
            Widget w = client.getWidget(id);
            if (w != null && !w.isHidden()) {
                canvasWidget = w;
            }
        }

        if (canvasWidget == null) {
            return true;
        }
        Rectangle canvasRec = canvasWidget.getBounds();
        Rectangle widgetRec = widget.getBounds();
        return widgetRec.x + widgetRec.width < canvasRec.x
                || widgetRec.x > canvasRec.x + canvasRec.width
                || widgetRec.y + widgetRec.height < canvasRec.y
                || widgetRec.y > canvasRec.y + canvasRec.height;
    }

    private boolean isInLobby() {
        Widget loginWidget = client.getWidget(ids.getLoginScreenId());
        return loginWidget != null && !loginWidget.isHidden();
    }
}
