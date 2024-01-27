package com.RuneLingual;

import static com.RuneLingual.WidgetsUtil.getAllChildren;

import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class MenuBar
{
	@Inject
	private Client client;
	@Inject
	private RuneLingualConfig config;
	
	@Setter
	private TranscriptManager interfaceTranslator;
	
	// logging control
	@Setter
	private LogHandler logger;
	private boolean logTranslations;
	private boolean logErrors;
	private boolean logCaptures;
	
	// configs - translation control
	private boolean translateMenus;
	
	private List<Widget> worldMapWidgetsLoaded;
	
	public MenuBar(RuneLingualConfig config, Client client)
	{
		this.client = client;
		this.config = config;
		
		this.logErrors = true;
		this.logTranslations = false;
		this.logCaptures = false;
		
		this.translateMenus = true;
	}
	
	public void handleCharacterSummaryTab()
	{
		Widget characterSummaryContainer = client.getWidget(ComponentID.CHARACTER_SUMMARY_CONTAINER);
		List<Widget> summaryChildren = getAllChildren(characterSummaryContainer);
		
		// ensure the widget is valid
		if(summaryChildren.size() > 0)
		{
			// translates quest list
			for(Widget widget : summaryChildren)
			{
				int widgetId = widget.getId();
				
				if(hasTextContents(widget))
				{
					// handles widget name translation
					String originalQuestTitle = widget.getName();
					String questTitle;
					try
					{
						questTitle = interfaceTranslator.getText(
								"charactersummary",
								originalQuestTitle,
								true);
					}
					catch(Exception e)
					{
						questTitle = originalQuestTitle;
						if(logErrors)
						{
							logger.log("Could not translate("
						        + e.getMessage()
						        + "):"
						        + originalQuestTitle);
						}
					}
					if(questTitle.length() > 0)
					{
						replaceNameById(widgetId, questTitle);
					}
					
					// handles widget text translation
					String originalQuestText = widget.getText();
					String questText;
					try
					{
						questText = interfaceTranslator.getText(
							"charactersummary",
							originalQuestText,
							true);
					}
					catch(Exception e)
					{
						questText = originalQuestText;
						if(logErrors)
						{
							logger.log("Could not translate("
						        + e.getMessage()
						        + "):"
						        + originalQuestText);
						}
					}
					if(questText.length() > 0)
					{
						replaceTextById(widgetId, questText);
					}
				}
			}
		}
	}
	
	public void handleQuestMenuTab()
	{
		Widget questListBox = client.getWidget(ComponentID.QUEST_LIST_BOX);
		List<Widget> questListBoxChildren = getAllChildren(questListBox);
		
		// ensure the widget is valid
		if(questListBoxChildren.size() > 0)
		{
			// translates quest list
			for(Widget questWidget : questListBoxChildren)
			{
				int widgetId = questWidget.getId();
				if(hasTextContents(questWidget))
				{
					/* not to be confused with the quest diary itself
					* there should be a special treatment for those under
					* this plugin's DiaryCapture module */
					String originalQuestTitle = questWidget.getName();
					String questTitle;
					try
					{
						questTitle = interfaceTranslator.getText(
							"quests",
							originalQuestTitle,
							true);
					}
					catch(Exception e)
					{
						questTitle = originalQuestTitle;
						if(logErrors)
						{
							logger.log("Could not translate("
						        + e.getMessage()
						        + "):"
						        + originalQuestTitle);
						}
					}
					if(questTitle.length() > 0)
					{
						replaceNameById(widgetId, questTitle);
					}
					
					String originalQuestText = questWidget.getText();
					String questText;
					try
					{
						questText = interfaceTranslator.getText(
							"quests",
							originalQuestTitle,
							true);
					}
					catch(Exception e)
					{
						questText = originalQuestTitle;
						if(logErrors)
						{
							logger.log("Could not translate("
						        + e.getMessage()
						        + "):"
						        + originalQuestText);
						}
					}
					if(questText.length() > 0)
					{
						replaceTextById(widgetId, questText);
					}
				}
			}
		}
	}
	
	public void handleAchievementDiaryTab()
	{
		/* not to be confused with the achievement diary journal interface
		 * there should be a special treatment for that somewhere else */
		Widget achievementDiaryBox = client.getWidget(ComponentID.ACHIEVEMENT_DIARY_CONTAINER);
		if(achievementDiaryBox == null)
		{
			if(logErrors)
			{
				logger.log("Could not retrieve achievement diary container widget! Null widget!");
			}
			return;
		}
		
		Widget achievementDiaryParent = achievementDiaryBox.getParent();
		List<Widget> achievementChildren = getAllChildren(achievementDiaryParent);
		
		// ensures the list widget is valid
		if(achievementChildren.size() > 0)
		{
			for(Widget currentWidget : achievementChildren)
			{
				int widgetId = currentWidget.getId();
				if(hasTextContents(currentWidget))
				{
					String originalWidgetTitle = currentWidget.getName();
					String widgetTitle;
					try
					{
						widgetTitle = interfaceTranslator.getText(
							"achievementdiary",
							originalWidgetTitle,
							true);
					}
					catch(Exception e)
					{
						widgetTitle = originalWidgetTitle;
						if(logErrors)
						{
							logger.log("Could not translate("
						        + e.getMessage()
						        + "):"
						        + originalWidgetTitle);
						}
					}
					if(widgetTitle.length() > 0)
					{
						replaceNameById(widgetId, widgetTitle);
					}
					
					String originalWidgetText = currentWidget.getText();
					String widgetText;
					try
					{
						widgetText = interfaceTranslator.getText(
							"achievementdiary",
							originalWidgetText,
							true);
					}
					catch(Exception e)
					{
						widgetText = originalWidgetText;
						if(logErrors)
						{
							logger.log("Could not translate("
						        + e.getMessage()
						        + "):"
						        + originalWidgetText);
						}
					}
					if(widgetText.length() > 0)
					{
						replaceTextById(widgetId, widgetText);
					}
				}
			}
		}
		else
		{
			if(logErrors)
			{
				logger.log("Could not retrieve achievement diary children widgets!"
			        + " Widget list is null!");
			}
		}
	}
	
	public void handleWorldMap()
	{
		/* In the current way this widget is structured, it is
		actually better to iterate between World Map's tooltip,
		since it is on second level on the widget's hierarchy
		I've found no other supported widget for this */
		try
		{
			Widget worldMapToolTip = client.getWidget(ComponentID.WORLD_MAP_TOOLTIP);
			Widget worldMap = worldMapToolTip.getParent();
			List<Widget> worldMapComponents = getAllChildren(worldMap);
			
			if(worldMapComponents.size() != 0 && worldMapComponents.equals(worldMapWidgetsLoaded))
			{
				worldMapWidgetsLoaded = worldMapComponents;
			}
			
			for(Widget widget : worldMapWidgetsLoaded)
			{
				int widgetId = widget.getId();
				
				String widgetName = widget.getName();
				if(widgetName.length() > 0)
				{
					String newName = translateContents("worldmap", widgetName);
					replaceNameById(widgetId, newName);
				}
				
				String widgetText = widget.getText();
				if(widgetText.length() > 0)
				{
					String newText = translateContents("worldmap", widgetText);
					replaceTextById(widgetId, newText);
				}
			}
		}
		catch(Exception e)
		{
			if(logErrors)
			{
				logger.log("Could not translate one or more world map widgets :" + e.getMessage());
			}
		}
		
	}
	
	private void replaceTextById(int widgetId, String newContents)
	{
		try
		{
			client.getWidget(widgetId).setText(newContents);
		}
		catch(Exception e)
		{
			if(logErrors)
			{
				logger.log("Could not replace widget(ID "
			        + widgetId
			        + ") text contents! Exception captured: "
			        + e.getMessage());
			}
		}
	}
	private void replaceNameById(int widgetId, String newContents)
	{
		try
		{
			client.getWidget(widgetId).setName(newContents);
		}
		catch(Exception e)
		{
			if(logErrors)
			{
				logger.log("Could not replace widget(ID "
		            + widgetId
			        + ") name! Exception captured: "
					+ e.getMessage());
			}
		}
	}
	private String translateContents(String contentSource, String originalContents)
	{
		try
		{
			return interfaceTranslator.getText(contentSource, originalContents, true);
		}
		catch(Exception e)
		{
			if(logErrors)
			{
				logger.log("Could not translate '"
			        + contentSource
		            + "' contents: "
			        + originalContents
			        + "("
			        + e.getMessage()
					+ ")");
			}
			return originalContents;
		}
	}
	private boolean hasTextContents(Widget widget)
	{
		if(widget != null)
		{
			if(widget.getText().length() > 0)
			{
				return true;
			}
			if(widget.getName().length() > 0)
			{
				return true;
			}
		}
		return false;
	}
}
