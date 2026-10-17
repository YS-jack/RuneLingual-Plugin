package com.RuneLingual.Widgets;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Ids;
import com.RuneLingual.commonFunctions.Transformer;
import com.RuneLingual.nonLatin.GeneralFunctions;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetSizeMode;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WidgetsUtilRLingual
{
	@Inject
	private Client client;
	@Inject
	private RuneLingualPlugin plugin;
	@Inject
	private GeneralFunctions generalFunctions;
	@Inject
	private Ids ids;

	private static final Pattern IMG_TAG = Pattern.compile("<img=([0-9]+)>");
	private final Map<Integer, Integer> charImageWidthsByTag = new HashMap<>();

	@Inject
	public WidgetsUtilRLingual(Client client, RuneLingualPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		this.ids = plugin.getIds();
	}

	public void setWidgetText_BrAsIs(Widget widget, String newText)
	{
		if (newText.equals(widget.getText())) // the texts will be the same if the widget has already been translated, or doesn't have a translation available
			return;
		// Set the text of the widget, but keep br as is
		widget.setText(newText);
	}

	// keep the translation's line breaks, but break each line again if it is wider than the widget
	// (eg. the spellbook tooltip: "description<br>Members only", where only the description is too long)
	public void setWidgetText_KeepBrAndFit(Widget widget, String newText) {
		String[] lines = newText.split("<br>", -1);
		setWidgetText_NiceBr(widget, "<autoBr>" + String.join("</autoBr><br><autoBr>", lines) + "</autoBr>");
	}

	public void setWidgetText_NiceBr(Widget widget, String newText) {
		if (newText.contains("<br>") && !newText.contains("<autoBr>")) { // if the text already has <br> and not specified to insert br automatically, don't insert br
			widget.setText(newText);
			return;
		}

		if (plugin.getConfig().getSelectedLanguage().needsCharImages())
			setWidgetText_NiceBr_CharImages(widget, newText);
		else
			setWidgetText_NiceBr_NoCharImages(widget, newText);
	}

	public void setWidgetText_NiceBr_apiTranslated(Widget widget, String newText) {
		if (newText.equals(widget.getText())) // the texts will be the same if the widget has already been translated, or doesn't have a translation available
			return;

		if (plugin.getConfig().getSelectedLanguage().needsCharImages())
			setWidgetText_NiceBr_CharImages(widget, newText);
		else
			setWidgetText_NiceBr_NoCharImages(widget, newText);
	}

	public void setWidgetText_ApiTranslation(Widget widget, String originalText, Colors color){
		final String text_withoutBrAndTags = Colors.removeNonImgTags(originalText);
		String translatedText = plugin.getDeepl().translate(text_withoutBrAndTags, LangCodeSelectableList.ENGLISH, plugin.getConfig().getSelectedLanguage());
		int originalLineHeight = widget.getLineHeight();
		if(translatedText.equals(text_withoutBrAndTags)) { // if the translation is the same as the original text, don't set the text
			return;
		}

		if (plugin.getTargetLanguage().needsCharImages()) {
			translatedText = generalFunctions.StringToTags(translatedText, color);
		}
		setWidgetText_NiceBr(widget, translatedText);
		widget.setLineHeight(originalLineHeight);
	}

	private void setWidgetText_NiceBr_CharImages(Widget widget, String newText) {
		// if newText doesnt have <br> tag at all, insert br automatically
		if (!newText.contains("<br>")) {
			newText = newText.replaceAll("<autoBr>|</autoBr>", ""); // remove <autoBr> tags
			newText = getWidgetText_NiceBr_CharImages(widget, newText);
			widget.setText(newText);
			return;
		}
		// if newText has <br> tag,
		// insert br automatically if it's inside <autoBr> tags, or doesnt have <br> tag at all
		String[] splitText = newText.split("(?=<autoBr>)|(?<=</autoBr>)");
		StringBuilder newTextBuilder = new StringBuilder();
		for (String part : splitText) {
			if (part.contains("<autoBr>")) {
				part = part.replaceAll("<autoBr>|</autoBr>", ""); // remove <autoBr> tags
				part = getWidgetText_NiceBr_CharImages(widget, part);
			}
			newTextBuilder.append(part);
		}
		newText = newTextBuilder.toString();
		// remove the first <br> if it's at the beginning of the text
		if (newText.matches("^<br>.*")) {
			newText = newText.substring(4);
		}
		// remove the last <br> if it's at the end of the text
		if (newText.matches(".*<br>$")) {
			newText = newText.substring(0, newText.length() - 4);
		}
		newText = newText.replaceAll("<br><br>", "<br>"); // remove double <br>
		widget.setText(newText);
	}

	private String getWidgetText_NiceBr_CharImages(Widget widget, String text){
		// Set the text of the widget, but insert br considering the width of the widget
		int widgetWidth = widget.getWidth();
		int foreignWidth = plugin.getConfig().getSelectedLanguage().getCharWidth();
		int maxChars = widgetWidth / foreignWidth;
		// if language uses charImages and needs space between words
		if(plugin.getConfig().getSelectedLanguage().needsSpaceBetweenWords()) { // todo: test this when such language is added
			String[] words = text.split("(?=\\s)");
			if (words.length == 1) { // if there are no spaces, don't insert br
				return text;
			}
			// measure in pixels: character images of latin letters, digits and symbols are much narrower than foreignWidth
			StringBuilder newTextBuilder = new StringBuilder();
			int currentLineWidth = 0;
			for(String word : words) {
				int wordWidth = getDisplayedWidth(word, foreignWidth);
				if(currentLineWidth > 0 && currentLineWidth + wordWidth > widgetWidth) {
					newTextBuilder.append("<br>");
					currentLineWidth = 0;
				}
				newTextBuilder.append(word);
				currentLineWidth += wordWidth;
			}
			text = newTextBuilder.toString();
		} else { // if language uses charImages and doesn't need space between words
			String[] letters = text.split("(?<=>)");
			StringBuilder newTextBuilder = new StringBuilder();
			int currentLineLength = 0;
			for(String letter : letters) {
				if(currentLineLength + 1 > maxChars) {
					newTextBuilder.append("<br>");
					currentLineLength = 0;
				}
				newTextBuilder.append(letter);
				currentLineLength++;
			}
			text = newTextBuilder.toString();
		}
		return text;
	}

	// width in pixels of text made of character images (<img=N>), as it will be drawn
	private int getDisplayedWidth(String text, int foreignWidth) {
		Map<Integer, Integer> widths = getCharImageWidthsByTag();
		String plain = text.replaceAll("<col=[0-9a-fA-F]+>|</col>", "");
		Matcher matcher = IMG_TAG.matcher(plain);
		int width = 0;
		int last = 0;
		while (matcher.find()) {
			width += (matcher.start() - last) * foreignWidth / 2; // plain characters, mostly spaces
			width += widths.getOrDefault(Integer.parseInt(matcher.group(1)), foreignWidth); // icons that are not character images use the default
			last = matcher.end();
		}
		return width + (plain.length() - last) * foreignWidth / 2;
	}

	// character image widths are stored by chat icon id; the <img=N> tags use the chat icon index
	private Map<Integer, Integer> getCharImageWidthsByTag() {
		Map<Integer, Integer> byId = plugin.getCharImageWidths();
		if (charImageWidthsByTag.size() != byId.size()) {
			charImageWidthsByTag.clear();
			byId.forEach((id, w) -> charImageWidthsByTag.put(plugin.getChatIconManager().chatIconIndex(id), w));
		}
		return charImageWidthsByTag;
	}

	private void setWidgetText_NiceBr_NoCharImages(Widget widget, String newText) {
		// if newText doesnt have <br> tag at all, insert br automatically
		if (!newText.contains("<br>")) {
			newText = newText.replaceAll("<autoBr>|</autoBr>", ""); // remove <autoBr> tags
			newText = getWidgetText_NiceBr_CharImages(widget, newText);
			widget.setText(newText);
			return;
		}
		// insert br automatically if
		// newText has <br> tag and is inside <autoBr> tags
		// or the text doesn't have <br> tag at all
		String[] splitText = newText.split("(?=<autoBr>)|(?<=</autoBr>)");
		StringBuilder newTextBuilder = new StringBuilder();
		for (String part : splitText) {
			if (part.contains("<autoBr>")) {
				part = part.replaceAll("<autoBr>|</autoBr>", ""); // remove <autoBr> tags
				part = getWidgetText_NiceBr_NoCharImages(widget, part);
			}
			newTextBuilder.append(part);
		}
		newText = newTextBuilder.toString();
		// remove the first <br> if it's at the beginning of the text
		if (newText.matches("^<br>.*")) {
			newText = newText.substring(4);
		}
		// remove the last <br> if it's at the end of the text
		if (newText.matches(".*<br>$")) {
			newText = newText.substring(0, newText.length() - 4);
		}
		newText = newText.replaceAll("<br><br>", "<br>"); // remove double <br>
		widget.setText(newText);
	}

	private String getWidgetText_NiceBr_NoCharImages(Widget widget, String newText) {
		if (!newText.contains(" ")) { // if there are no spaces, don't insert br
			return newText;
		}
		// Set the text of the widget, but insert br considering the width of the widget
		int widgetWidth = widget.getWidth();
		int foreignWidth = LangCodeSelectableList.getLatinCharWidth(widget, plugin.getConfig().getSelectedLanguage());
		int maxChars = widgetWidth / foreignWidth;

		if(plugin.getConfig().getSelectedLanguage().needsSpaceBetweenWords()) {
			String[] words = newText.split("(?=\\s)");
			StringBuilder newTextBuilder = new StringBuilder();
			int currentLineLength = 0;
			for(String word : words) {
				if(currentLineLength + word.length() > maxChars) {
					newTextBuilder.append("<br>");
					currentLineLength = 0;
				}
				if (currentLineLength == 0 && word.charAt(0) == ' ') {
					// remove space from the beginning of the word
					word = word.replaceFirst(" ", "");
				}
				newTextBuilder.append(word);
				currentLineLength += word.length();
			}
			newText = newTextBuilder.toString();
		} else {
			StringBuilder newTextBuilder = new StringBuilder();
			int currentLineLength = 0;
			for(int i = 0; i < newText.length(); i++) {
				if(currentLineLength + 1 > maxChars) {
					newTextBuilder.append("<br>");
					currentLineLength = 0;
				}
				newTextBuilder.append(newText.charAt(i));
				currentLineLength++;
			}
			newText = newTextBuilder.toString();
		}
		// remove the first <br> if it's at the beginning of the text
		if (newText.matches("^<br>.*")) {
			newText = newText.substring(4);
		}
		// remove the last <br> if it's at the end of the text
		if (newText.matches(".*<br>$")) {
			newText = newText.substring(0, newText.length() - 4);
		}
		newText = newText.replaceAll("<br><br>", "<br>"); // remove double <br>

		return newText;
	}

	public static String removeBrAndTags(String str) {
		// replaces br with space
		String tmp = str.replaceAll("(?<=\\S)<br>(?=\\S)", " ");
		return Colors.removeNonImgTags(tmp);
	}

	public void changeWidgetSize_ifNeeded(Widget widget) {
		int widgetId = widget.getId();
		String translatedText = widget.getText();

		//	resize widget if needed, dynamically depending on text length
		ids.getWidget2ModDict().resizeWidgetIfNeeded(widget, translatedText);

		// resize widget to fixed size, as defined in ids.getWidgetId2ChangeSize()
		if (ids.getWidgetId2FixedSize().containsKey(widgetId)) {// if is set to change to fixed size
			if (ids.getWidgetId2FixedSize().get(widgetId).getLeft() != null) {
				widget.setWidthMode(WidgetSizeMode.ABSOLUTE)
						.setOriginalWidth(ids.getWidgetId2FixedSize().get(widgetId).getLeft());
			}
			if (ids.getWidgetId2FixedSize().get(widgetId).getRight() != null) {
				widget.setHeightMode(WidgetSizeMode.ABSOLUTE)
						.setOriginalHeight(ids.getWidgetId2FixedSize().get(widgetId).getRight());
			}
			widget.revalidate();
		}
	}

	public boolean shouldPartiallyTranslateWidget(Widget widget) {
		String enColVal = Transformer.getEnglishColValFromText(widget.getText());
		boolean hasId = ids.getPartialTranslationManager().hasId(widget.getId());
		if (!hasId) {
			return false;
		}
		boolean stringMatch = ids.getPartialTranslationManager().doesStringMatchEnColVal(widget.getText());
		return stringMatch;
	}

	public boolean shouldPartiallyTranslateText(String string) {
		return ids.getPartialTranslationManager().doesStringMatchEnColVal(string);
	}

	public String getEnColVal4PartialTranslation(Widget widget, String text) {
		int widgetId = widget.getId();
		return ids.getPartialTranslationManager().getEnColVal(widgetId, text);
	}

	public String getMatchingEnColVal4PartialTranslation(String string) {
		return ids.getPartialTranslationManager().getMatchingEnColVal(string);
	}

	// set height of line for specified widgets, because they can be too small
	public void changeLineHeight(Widget widget) {
		int lineHeight = plugin.getConfig().getSelectedLanguage().getCharHeight();
		widget.setLineHeight(lineHeight);
	}

}
