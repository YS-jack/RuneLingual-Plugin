package com.RuneLingual.Wigets;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Ids;
import com.RuneLingual.nonLatin.GeneralFunctions;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
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
		if (newText.equals(widget.getText())) // the texts will be the same if the widget has already been translated, or doesn't have a translation available
			return;

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
		// Set the text of the widget, but insert br considering the width of the widget
		int widgetWidth = widget.getWidth();
		int foreignWidth = plugin.getConfig().getSelectedLanguage().getCharWidth();
		int maxChars = widgetWidth / foreignWidth;

		if(plugin.getConfig().getSelectedLanguage().needsSpaceBetweenWords()) {
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
	public boolean isWidgetToFitText(Widget widget) {
		return ids.getWidget2FitTextDict().getWidgets2FitText(widget.getId()) != null;
	}
	public void changeWidgetSize_ifNeeded(Widget widget, String translatedText) {
		int widgetId = widget.getId();

		//	resize widget if needed, dynamically depending on text length
		ids.getWidget2FitTextDict().resizeWidgetIfNeeded(widget, translatedText);

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


	// set the width of the widget to fit the text
	// if the widget is the last type 4 widget among sibling widgets (=A), start the process below:
	// 		for every widget ( = X) under the same parent widget that: 1.needs resizing, 2. has been translated,
	// 			resize X to fit the text(1)
	//     		move all child widget of X  (2),
	// 			move all siblings in the direction it resized (3)
	// 			move all children of siblings that moved in (2) in the same direction(4),
	//     		then resize the parent in the same direction(5),
	//     		then repeat (1~5) for X = parent widget, recursively, until x is the parent of the widget A

	public void changeWidgetWidth(Widget widget, String newText, int leftPadding, int rightPadding) {
		Widget parentWidget = widget.getParent();
//		// if this widget is not the last type 4 widget among sibling widgets end here
//		if (!isLastType4Widget(widget) || parentWidget == null) {
//			return;
//		}
//
//		Widget parentWidget = widget.getParent();
//		for (Widget w: parentWidget.getDynamicChildren()){
//			if (isWidgetToFitText(w) && isTranslatedWidget(w.getText())) {
//				resizeAndReposition(w, parentWidget);
//			}
//		}
//		for (Widget w: parentWidget.getStaticChildren()){
//			if (isWidgetToFitText(w) && isTranslatedWidget(w.getText())) {
//				resizeAndReposition(w, parentWidget);
//			}
//		}
//		for (Widget w: parentWidget.getNestedChildren()){
//			if (isWidgetToFitText(w) && isTranslatedWidget(w.getText())) {
//				resizeAndReposition(w, parentWidget);
//			}
//		}

		// set the width of the widget to fit the text
		int newWidth = getWidgetNewWidth(newText);
		setWidgetWidthAbsolute(widget, newWidth);



		// else, set the width of the parent widget to fits all the children widgets

		// set parent + sibling widget's width
		if (widget.getParent() != null) {
			int newFamilyWidth = newWidth + leftPadding + rightPadding;
			parentWidget = widget.getParent();
			parentWidget.setWidthMode(WidgetSizeMode.ABSOLUTE)
					.setOriginalWidth(newFamilyWidth)
					.revalidate();
			// also set the sibling widget's width to the new width + padding
			Widget[] siblings = parentWidget.getDynamicChildren();
			for (Widget sibling : siblings) {
				if (sibling != widget && sibling.getType() == 3) { // 3 seems to be the type for background widgets
					sibling.setWidthMode(WidgetSizeMode.ABSOLUTE)
							.setOriginalWidth(newFamilyWidth)
							.revalidate();
				}
			}
		}
		// set the position
		widget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT)
				.setOriginalX(leftPadding)
				.revalidate();
	}

	private void resizeAndReposition(Widget widget, Widget parentWidget) {
		// save the original position and boundary of the widget

		// resize X to fit the text
		changeWidgetWidth(widget, widget.getText(),
				ids.getWidget2FitTextDict().getWidgets2FitText(widget.getId()).getLeftPadding(),
				ids.getWidget2FitTextDict().getWidgets2FitText(widget.getId()).getRightPadding());

		// get the new position and boundary of the widget

		// calculate how much it has moved in which direction

		// move all child widget of X
		// move all siblings in the direction it resized
		// move all children of siblings that moved in the same direction
		// then resize the parent in the same direction
		// then repeat for X = parent widget, recursively, until x is the parent of the widget A
		if (widget.getParent() != null && !widget.getParent().equals(parentWidget)) {
			resizeAndReposition(widget.getParent(), parentWidget);
		}

	}

	private int getWidgetNewWidth(String newText) {
		// get the longest line, multiply by the width of selected language's character
		int longestLine = 0;
		String[] lines = newText.split("<br>");
		// count the number of characters, but if its char images, count the number of <img> tags, else
		if (!plugin.getConfig().getSelectedLanguage().needsCharImages()) {
			for (String line : lines) {
				if (line.length() > longestLine) {
					longestLine = line.length() * LangCodeSelectableList.ENGLISH.getCharWidth();
				}
			}
		} else {
			for (String line : lines) {
				int imgCount = line.split("<img=").length - 1;
				int nonImgCount = line.replaceAll("<img=.*?>", "").length();
				if (imgCount > longestLine) {
					longestLine = imgCount * plugin.getConfig().getSelectedLanguage().getCharWidth() +
							nonImgCount * LangCodeSelectableList.ENGLISH.getCharWidth();
				}
			}
		}
		return longestLine;
	}

	private void setWidgetWidthAbsolute(Widget widget, int width) {
		widget.setWidthMode(WidgetSizeMode.ABSOLUTE)
				.setOriginalWidth(width)
				.revalidate();
	}

	private boolean isLastType4Widget(Widget widget) {
		Widget[] siblings = widget.getParent().getDynamicChildren();
		Widget lastType4Widget = null;
		for (Widget sibling: siblings){
			if (sibling.getType() == 4){
				lastType4Widget = sibling;
			}
		}
		return lastType4Widget == widget;
	}


	public void changeWidgetHeight(Widget widget, String newText, int topPadding, int bottomPadding) {
		// get the number of <br> tags, multiply by the height of selected language's character
		int newHeight = newText.split("<br>").length
				* plugin.getConfig().getSelectedLanguage().getCharHeight();

		// set parent + sibling widget's height
		if (widget.getParent() != null) {
			int newFamilyHeight = newHeight + topPadding + bottomPadding;
			Widget parentWidget = widget.getParent();
			parentWidget.setHeightMode(WidgetSizeMode.ABSOLUTE)
					.setOriginalHeight(newFamilyHeight)
					.revalidate();

			// also set the sibling widget's height to the new height + padding
			Widget[] siblings = parentWidget.getDynamicChildren();
			for (Widget sibling : siblings) {
				if (sibling != widget && sibling.getType() == 3) { // 3 seems to be the type for background widgets
					sibling.setHeightMode(WidgetSizeMode.ABSOLUTE)
							.setOriginalHeight(newFamilyHeight)
							.revalidate();
				}
			}
		}
		// set the height
		widget.setHeightMode(WidgetSizeMode.ABSOLUTE)
				.setOriginalHeight(newHeight)
				.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP)
				.setOriginalY(topPadding)
				.revalidate();
	}



	// set height of line for specified widgets, because they can be too small
	public void changeLineSize_ifNeeded(Widget widget) {
		if (ids.getWidgetId2SetLineHeight().contains(widget.getId())) {
			int lineHeight = plugin.getConfig().getSelectedLanguage().getCharHeight();
			widget.setLineHeight(lineHeight);
		}
	}

}
