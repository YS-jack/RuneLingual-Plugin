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
import java.util.List;
import java.util.ArrayList;

public class WidgetsUtilRLingual
{
	@Inject
	private Client client;
	@Inject
	private RuneLingualPlugin plugin;
	@Inject
	private GeneralFunctions generalFunctions;
	private List<String> stringTranslatingInThread = new ArrayList<>();
	@Inject
	private Ids ids;

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

	public void setWidgetText_NiceBr(Widget widget, String newText) {
		if (newText.contains("<br>")) {
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

	public void setWidgetText_ApiTranslation(Widget widget, String newText, Colors color){
		final String newText_withoutBrAndTags = removeBrAndTags(newText);
		String translatedText = plugin.getDeepl().translate(newText_withoutBrAndTags, LangCodeSelectableList.ENGLISH, plugin.getConfig().getSelectedLanguage());
		if(translatedText.equals(newText_withoutBrAndTags)) { // if the translation is the same as the original text, don't set the text
			return;
		}

		if (plugin.getTargetLanguage().needsCharImages()) {
			translatedText = generalFunctions.StringToTags(translatedText, color);
		}
		setWidgetText_NiceBr(widget, translatedText);
		stringTranslatingInThread.remove(newText);
		widget.setLineHeight(plugin.getConfig().getSelectedLanguage().getCharHeight());
	}

	private void setWidgetText_NiceBr_CharImages(Widget widget, String newText) { // todo: set to show overlay if the mouse is hovering and the widget is too small for the text to display
		// Set the text of the widget, but insert br considering the width of the widget
		int widgetWidth = widget.getWidth();
		int foreignWidth = plugin.getConfig().getSelectedLanguage().getCharWidth();
		int maxChars = widgetWidth / foreignWidth;
		// if language uses charImages and needs space between words
		if(plugin.getConfig().getSelectedLanguage().needsSpaceBetweenWords()) { // todo: test this when such language is added
			String[] words = newText.split(" ");
			StringBuilder newTextBuilder = new StringBuilder();
			int currentLineLength = 0;
			for(String word : words) {
				if(currentLineLength + word.length() > maxChars) {
					newTextBuilder.append("<br>");
					currentLineLength = 0;
				}
				newTextBuilder.append(word);
				currentLineLength += word.length();
			}
			newText = newTextBuilder.toString();
		} else { // if language uses charImages and doesn't need space between words
			String[] letters = newText.split("(?<=>)");
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
			newText = newTextBuilder.toString();
		}
		widget.setText(newText);
	}

	private void setWidgetText_NiceBr_NoCharImages(Widget widget, String newText) {
		if (!newText.contains(" ")) { // if there are no spaces, don't insert br
			widget.setText(newText);
			return;
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

		widget.setText(newText);
	}

	public static String removeBrAndTags(String str) {
		// replaces br with space
		String tmp = str.replaceAll("(?<=\\S)<br>(?=\\S)", " ");
		return Colors.removeNonImgTags(tmp);
	}
	public boolean isTranslatedWidget(String text) {
		return plugin.getWidgetCapture().pastTranslationResults.contains(text);
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
		return ids.getPartialTranslationManager().hasId(widget.getId())
				&& ids.getPartialTranslationManager().stringMatchesEnColVal(enColVal, widget.getId());
	}

	public boolean shouldPartiallyTranslateText(String string) {
		return ids.getPartialTranslationManager().stringMatchesEnColVal(string);
	}

	public String getEnColVal4PartialTranslation(Widget widget) {
		int widgetId = widget.getId();
		return ids.getPartialTranslationManager().getEnColVal(widgetId);
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
