package com.RuneLingual.Wigets;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Ids;
import com.RuneLingual.nonLatin.GeneralFunctions;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetSizeMode;

import javax.inject.Inject;
import java.util.List;
import java.util.ArrayList;
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
		if (widget.getId() == 20971547){
			System.out.println("Total level:<br> 75");
		}

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
		if(stringTranslatingInThread.contains(newText)) // skip if already translating with api
			return;
		stringTranslatingInThread.add(newText);
		Thread thread = new Thread(() -> {
			try {
				String translatedText = plugin.getDeepl().translate(newText_withoutBrAndTags, LangCodeSelectableList.ENGLISH, plugin.getConfig().getSelectedLanguage());
				if (plugin.getTargetLanguage().needsCharImages()) {
					translatedText = generalFunctions.StringToTags(translatedText, color);
				}
				setWidgetText_NiceBr(widget, translatedText);
				stringTranslatingInThread.remove(newText);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		thread.setDaemon(false);
		thread.start();
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
		return Colors.removeColorTag(tmp);
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

	public boolean shouldPartiallyTranslate(Widget widget) {
		return ids.getWidgetId2TranslatePartially().stream().anyMatch(pair -> pair.getLeft() == widget.getId());
	}
	public String getEnColVal4PartialTranslation(Widget widget) {
		int widgetId = widget.getId();
		for (var pair : ids.getWidgetId2TranslatePartially()) {
			if (pair.getLeft() == widgetId) {
				return pair.getRight(); // returns the text to translate, such as "Name: <playerName>"
			}
		}
		return null;
	}
	public String translatePartialTranslation(Widget widget, String translatedText, String originalText) {
		// for widgets like "Name: <playerName>" (found in accounts management tab), where only the part of the text should be translated
		// order:
		// originalText = "Name: Durial321", enColVal = "Name: <playerName>"
		// translatedText = "名前: <playerName>"
		// get : translatedText = "名前: Durial321"
		int widgetId = widget.getId();
		String enColVal = getEnColVal4PartialTranslation(widget);
		if (enColVal == null) {
			return translatedText;
		}
		// from the originalText and enColVal, get text replaced by tags like <playerName>
		String replacedText = getReplacedText(originalText, enColVal);
		if (replacedText.isEmpty()) {
			return translatedText;
		}
		// replace the translation's tag with the replaced text
		return translatedText.replaceAll("<.*?>", replacedText);
	}

	public String getReplacedText(String originalText, String enColVal) {
		// Regular expression to find text within <>
		Pattern pattern = Pattern.compile("<(.*?)>");
		Matcher matcher = pattern.matcher(enColVal);
		String replacedText = "";
		if (matcher.find()) {
			String tag = matcher.group(1); // Extract the text within <>
			String regex = enColVal.replace("<" + tag + ">", "(.*?)");
			Pattern pattern2 = Pattern.compile(regex);
			Matcher matcher2 = pattern2.matcher(originalText);

			if (matcher2.find()) {
				replacedText = matcher2.group(1); // Extract the replaced text
				System.out.println("Replaced Text: " + replacedText);
			}
		}
		return replacedText;
	}

	// set height of line for specified widgets, because they can be too small
	public void changeLineSize_ifNeeded(Widget widget) {
		if (ids.getWidgetId2SetLineHeight().contains(widget.getId())) {
			int lineHeight = plugin.getConfig().getSelectedLanguage().getCharHeight();
			widget.setLineHeight(lineHeight);
		}
	}

}
