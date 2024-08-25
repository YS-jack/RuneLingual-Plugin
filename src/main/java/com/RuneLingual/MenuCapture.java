package com.RuneLingual;

import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Transformer;
import com.RuneLingual.commonFunctions.Transformer.TransformOption;
import com.RuneLingual.SQL.SqlVariables;
import com.RuneLingual.SQL.SqlQuery;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;

import javax.inject.Inject;

import lombok.Setter;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import org.apache.commons.lang3.tuple.Pair;

import java.util.regex.Pattern;
import com.RuneLingual.debug.OutputToFile;
import com.RuneLingual.commonFunctions.Ids;

@Slf4j
public class MenuCapture
{
	@Inject
	private Client client;
	@Inject
	private RuneLingualPlugin plugin;
	
	@Setter
	private TranscriptManager actionTranslator;
	@Setter
	private TranscriptManager npcTranslator;
	@Setter
	private TranscriptManager objectTranslator;
	@Setter
	private TranscriptManager itemTranslator;
	
	@Setter
	private LogHandler logger;
	private boolean debugMessages = true;
	private final Colors colorObj = Colors.black;
    @Inject
	private Transformer transformer;
	@Inject
	private OutputToFile outputToFile;
	//private SqlVariables sqlVariables;

	@Inject
	public MenuCapture(RuneLingualPlugin plugin) {
		this.plugin = plugin;
	}
	
	// TODO: right click menu title 'Chose Options' - seems to not be directly editable

	public void handleOpenedMenu(MenuOpened event){
		MenuEntry[] menus = event.getMenuEntries();
		for(MenuEntry menu : menus){
			handleMenuEvent(new MenuEntryAdded(menu));
		}
	}
	
	public void handleMenuEvent(MenuEntryAdded event) {
		// called whenever a right click menu is opened
		MenuEntry currentMenu = event.getMenuEntry();
		String[] newMenus = translateMenuAction(currentMenu);
		String newTarget = newMenus[0];
		String newOption = newMenus[1];

		if(newOption != null) {
			if (newTarget != null && !newTarget.isEmpty()) {
				currentMenu.setTarget(newTarget);
			} else {
				// if target is empty, remove the target part of the menu entry
				currentMenu.setTarget("");
			}
			currentMenu.setOption(newOption);
		}
		//old codes
		/*
		try
		{
			if(isPlayerMenu(menuType))
			{
				translateMenuAction("playeractions", event, menuAction);
			}
			else if(isNpcMenu(menuType))
			{
				translateMenuAction("npcactions", event, menuAction);

				// translates npc name
				try
				{
					int combatLevel = targetNpc.getCombatLevel();
					if(combatLevel > 0)
					{
						// attackable npcs
						int levelIndicatorIndex = menuTarget.indexOf('(');
						if(levelIndicatorIndex != -1)
						{  // npc has a combat level
							String actualName = menuTarget.substring(0, levelIndicatorIndex);
							String newName = npcTranslator.getName(actualName, true);

							String levelIndicator = actionTranslator.getText("npcactions", "level", true);
							newName += " (" + levelIndicator + "-" + combatLevel + ")";
							event.getMenuEntry().setTarget(newName);
						}
						else
						{  // npc does not have a combat level
							String newName = npcTranslator.getName(menuTarget, true);
							event.getMenuEntry().setTarget(newName);
						}
					}
					else
					{  // non attackable npcs
						String newName = npcTranslator.getName(menuTarget, true);
						event.getMenuEntry().setTarget(newName);
					}

				}
				catch(Exception f)
				{
					if(debugMessages)
					{
						logger.log("Could not translate npc name: "
			                + menuTarget
			                + " - "
				            + f.getMessage());
					}
				}

			}
			else if(isWidgetOnSomething(menuType))
			{
				Pair<String, String> result = convertWidgetOnSomething(currentMenu);
				String itemName = result.getLeft();
				String useOnX = result.getRight();
				String newName = itemTranslator.getText("items", itemName, true);
				if (menuType.equals(MenuAction.WIDGET_TARGET_ON_NPC))
				{
					try
					{
						int combatLevel = targetNpc.getCombatLevel();
						if(combatLevel > 0)
						{
							// attackable npcs
							int levelIndicatorIndex = useOnX.indexOf('(');
							if(levelIndicatorIndex != -1)
							{  // npc has a combat level
								String actualName = useOnX.substring(0, levelIndicatorIndex);
								String NPCname = npcTranslator.getName(actualName, true);

								String levelIndicator = actionTranslator.getText("npcactions", "level", true);
								useOnX = NPCname + " (" + levelIndicator + "-" + combatLevel + ")";
								//event.getMenuEntry().setTarget(newName);
							}
							else
							{  // npc does not have a combat level
								useOnX = npcTranslator.getName(useOnX, true);
							}
						}
						else
						{  // non attackable npcs
							useOnX = npcTranslator.getName(useOnX, true);
						}
					}
					catch(Exception f)
					{
						if(debugMessages)
						{
							logger.log("Could not translate npc name: "
									+ menuTarget
									+ " - "
									+ f.getMessage());
						}
					}
				}
				else if (menuType.equals(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT))
				{
					useOnX = objectTranslator.getText("objects", useOnX, true);
				}
				else if (menuType.equals(MenuAction.WIDGET_TARGET_ON_WIDGET) || menuType.equals(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM))
				{
					useOnX = itemTranslator.getText("items", useOnX, true);
				}
				translateMenuAction("iteminterfaceactions", event, menuAction);
				event.getMenuEntry().setTarget(newName + " -> " + useOnX);
			}
			else if(isObjectMenu(menuType))
			{
				translateItemName("objects", event, menuTarget);
				translateMenuAction("objectactions", event, menuAction);
			}
			else if(isItemMenu(menuType))
			{  // ground item
				translateItemName("items", event, menuTarget);
				translateMenuAction("itemactions", event, menuAction);
			}
			else if(targetItem != -1)
			{  // inventory item
				translateItemName("items", event, menuTarget);
				translateMenuAction("iteminterfaceactions", event, menuAction);
			}
			else if(isGeneralMenu(menuType))
			{
				try
				{
					String newAction = actionTranslator.getText("generalactions", menuAction, true);
					event.getMenuEntry().setOption(newAction);
				}
				catch(Exception f)
				{
					if(debugMessages)
					{
						logger.log("Could not translate action: " + f.getMessage());
					}
				}
				try
				{
					translateItemName("items", event, menuTarget);
					translateMenuAction("iteminterfaceaction", event, menuAction);
				}
				catch(Exception f)
				{
					if(debugMessages)
					{
						logger.log("Could not translate action: " + f.getMessage());
					}
				}
			}
			else
			{
				// TODO: this
				// nor a player or npc
				logger.log("Menu action:"
			           + menuAction
			           + " - Menu target:"
			           + menuTarget
			           + "type:"
			           + event.getMenuEntry().getType());

				/*
				// tries to translate general actions
				try
				{
					String newAction = actionTranslator.getTranslatedText("generalactions", menuAction, true);
					event.getMenuEntry().setOption(newAction);
				}
				catch(Exception f)
				{

					logger.logger("Could not translate action: " + f.getMessage());

				} end comment here with * and /

			}

		}
		catch (Exception e)
		{
			if(debugMessages)
			{
				logger.log("Critical error happened while processing right click menus: " + e.getMessage());
			}
		}
		*/
	}

	private String[] translateMenuAction(MenuEntry currentMenu) {
		/*
		returns: String[] {newTarget, newOption}

		target and option examples
		option: Enable prayer reordering	target: <col=ff9040></col>
		option: Add-10<col=ff9040>		target: <col=ff9040>Pollnivneach teleport</col>
		 */
		MenuAction menuType = currentMenu.getType();
		Colors optionColor = Colors.white;

		String menuTarget = currentMenu.getTarget();  // eg. <col=ffff00>Sand Crab<col=ff00>  (level-15)     eg2. <col=ff9040>Dramen staff</col>
		String[] targetWordArray = Colors.getWordArray(menuTarget); // eg. ["Sand Crab", " (level-15)"]
		Colors[] targetColorArray = Colors.getColorArray(menuTarget, Colors.white); // eg. [Colors.yellow, Colors.green]

		String menuOption = currentMenu.getOption(); // doesnt seem to have color tags, always white? eg. Attack
		String[] actionWordArray = Colors.getWordArray(menuOption); // eg. ["Attack"]
		Colors[] actionColorArray = Colors.getColorArray(menuOption, optionColor);

		SqlQuery targetSqlQuery = new SqlQuery(this.plugin);
		SqlQuery actionSqlQuery = new SqlQuery(this.plugin);

		String newTarget = "";
		String newOption = "";

		// for debug purposes
		if(!isWalkOrCancel(menuType)){
			// do nothing
			printMenuEntry(currentMenu);
			if(!isNpcMenu(menuType) && !isObjectMenu(menuType)
					&& !isItemOnGround(menuType) && !isItemInWidget(currentMenu) && !isPlayerMenu(menuType)){
				//outputToFile.menuTarget(menuTarget,SqlVariables.menuInSubCategory.getValue(), "");
				//outputToFile.menuOption(menuOption,SqlVariables.menuInSubCategory.getValue(), "");
			}
		}

		// translate the target
		if(isWalkOrCancel(menuType)) {// needs to be checked
			actionSqlQuery.setEnglish(menuOption);
			actionSqlQuery.setCategory(SqlVariables.actionsInCategory.getValue());

			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.TRANSLATE_LOCAL, actionSqlQuery);

			if(Colors.removeColorTag(menuTarget).isEmpty()) {
				newTarget = "";
			} else {
				targetSqlQuery.setEnglish(targetWordArray[0]);
				targetSqlQuery.setCategory(SqlVariables.nameInCategory.getValue());
				targetSqlQuery.setSubCategory(SqlVariables.menuInSubCategory.getValue());

				newTarget = transformer.transform(targetWordArray, targetColorArray, TransformOption.TRANSLATE_LOCAL, targetSqlQuery);
			}
		}else if (isPlayerMenu(menuType)){
			//leave name as is (but replace to char image if needed)
			String playerName = targetWordArray[0];
			String translatedName = transformer.transform(playerName, Colors.white, TransformOption.AS_IS, null);

			// translate the level part. in database, "(level-%number%)" is stored as "(level-%number%)	name	player	" as tsv, in order of english, category, subcategory
			SqlQuery targetQueryLevel = new SqlQuery(this.plugin);
			String translatedLevel = transformer.transform(targetWordArray[1], targetColorArray[1],
					TransformOption.AS_IS,// todo: after the above todo, change the second option to TranslateLocal
					targetQueryLevel);

			newTarget = translatedName + translatedLevel;

			// set action as usual
			actionSqlQuery.setPlayerActions(menuOption, Colors.white);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.TRANSLATE_LOCAL, actionSqlQuery);
		} else if(isNpcMenu(menuType)) { // need to split into npcs with and without level
			// todo: get translation option from settings
			targetSqlQuery.setNpcName(menuTarget, Colors.yellow);
			actionSqlQuery.setNpcActions(menuOption, optionColor);

			targetColorArray = Colors.getColorArray(menuTarget, Colors.yellow); //default color is not the same as initial definition

			newTarget = transformer.transform(targetWordArray, targetColorArray, TransformOption.TRANSLATE_LOCAL, targetSqlQuery);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.TRANSLATE_LOCAL, actionSqlQuery);
		} else if(isObjectMenu(menuType)){
			targetSqlQuery.setObjectName(menuTarget, Colors.lightblue);
			actionSqlQuery.setObjectActions(menuOption, optionColor);

			targetColorArray = Colors.getColorArray(menuTarget, Colors.lightblue); //default color is not the same as initial definition

			newTarget = transformer.transform(targetWordArray, targetColorArray, TransformOption.TRANSLATE_LOCAL, targetSqlQuery);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.TRANSLATE_LOCAL, actionSqlQuery);
		} else if(isItemOnGround(menuType)){ // needs checking
			//printMenuEntry(event);
			targetSqlQuery.setItemName(menuTarget, Colors.orange);
			actionSqlQuery.setGroundItemActions(menuOption, optionColor);

			targetColorArray = Colors.getColorArray(menuTarget, Colors.orange); //default color is not the same as initial definition

			newTarget = transformer.transform(targetWordArray, targetColorArray, TransformOption.TRANSLATE_LOCAL, targetSqlQuery);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.TRANSLATE_LOCAL, actionSqlQuery);
		} else if(isItemInWidget(currentMenu)){ // either in inventory or in equipment
			//printMenuEntry(currentMenu);
			targetSqlQuery.setItemName(menuTarget, Colors.orange);
			actionSqlQuery.setInventoryItemActions(menuOption, optionColor);

			targetColorArray = Colors.getColorArray(menuTarget, Colors.orange); //default color is not the same as initial definition

			newTarget = transformer.transform(targetWordArray, targetColorArray, TransformOption.TRANSLATE_LOCAL, targetSqlQuery);
			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.TRANSLATE_LOCAL, actionSqlQuery);
		} else if(isWidgetOnSomething(menuType)){ // needs checking
//			// eg. "Use" -> "Brug"
//			optionSqlQuery = new SqlQuery();
//			optionSqlQuery.setEnglish(menuOption);
//			optionSqlQuery.setCategory(SqlVariables.actionsInCategory.getValue());
//
//			// eg. "Dramen staff -> Sand Crab"
//			Pair<String, String> result = convertWidgetOnSomething(currentMenu);
//			String itemName = result.getLeft();
//			String useOnX = result.getRight();
//			targetSqlVar = List.of(SqlVariables.nameInCategory, SqlVariables.itemInSubCategory);
//			newTarget = transformer.transform(new String[]{itemName, useOnX}, new Colors[]{Colors.white, Colors.white}, TransformOption.AS_IS, targetSqlVar);

		} else { // is a general menu
			// check what widget it is in, then set the source column value accordingly
			String source = setSourceAccordingToOpenTab(currentMenu);

			targetSqlQuery.setMenuName(menuTarget, Colors.orange);
			targetSqlQuery.setSource(source);
			actionSqlQuery.setMenuAcitons(menuOption, optionColor);
			actionSqlQuery.setSource(source);

			// for debug purposes
			//outputToFile.menuTarget(menuTarget,SqlVariables.menuInSubCategory.getValue(), source);
			//outputToFile.menuOption(menuOption,SqlVariables.menuInSubCategory.getValue(), source);

			newOption = transformer.transform(actionWordArray, actionColorArray, TransformOption.TRANSLATE_LOCAL, actionSqlQuery);

			if(Colors.removeColorTag(menuTarget).isEmpty()) {
				newTarget = "";
			} else {
				targetSqlQuery.setEnglish(targetWordArray[0]);
				targetSqlQuery.setCategory(SqlVariables.nameInCategory.getValue());
				targetSqlQuery.setSubCategory(SqlVariables.menuInSubCategory.getValue());
				targetSqlQuery.setSource(source);

				targetColorArray = Colors.getColorArray(menuTarget, Colors.orange); //default color is not the same as initial definition
				newTarget = transformer.transform(targetWordArray, targetColorArray, TransformOption.TRANSLATE_LOCAL, targetSqlQuery);
			}

		}

		// swap out the translated menu action and target.
		// reorder them if it is grammatically correct to do so in that language
		if(this.plugin.getTargetLanguage().swapMenuOptionAndTarget()) {
			String temp = newOption;
			newOption = newTarget;
			newTarget = temp;
		}
		return new String[]{newTarget, newOption};
	}

	private String setSourceAccordingToOpenTab(MenuEntry menu){
		String source = "";
		Ids ids = this.plugin.getIds();
		if(isChildWidgetOf(ids.getCombatOptionParentWidgetId(), menu)){
			source = SqlVariables.combatOptionsTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getSkillsTabParentWidgetId(),menu)){
			source = SqlVariables.skillsTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getCharacterSummaryTabWidgetId(),menu)){
			source = SqlVariables.characterSummaryTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getQuestTabParentWidgetId(), menu)){
			source = SqlVariables.questListTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getAchievementDiaryTabParentWidgetId(),menu)){
			source = SqlVariables.achievementDiaryTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getInventoryTabParentWidgetId(),menu)){
			source = SqlVariables.inventTabInSource.getValue();
		}  else if(isChildWidgetOf(ids.getEquipmentTabParentWidgetId(),menu)){
			source = SqlVariables.wornEquipmentTabInSource.getValue();
		}else if(isChildWidgetOf(ids.getPrayerTabParentWidgetId(),menu)){
			source = SqlVariables.prayerTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getSpellBookTabParentWidgetId(), menu)){
			source = SqlVariables.spellBookTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getGroupsTabParentWidgetId(), menu)){
			source = SqlVariables.groupTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getFriendsTabParentWidgetId(), menu)){
			source = SqlVariables.friendsTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getIgnoreTabParentWidgetId(), menu)){
			source = SqlVariables.ignoreTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getAccountManagementTabParentWidgetId(), menu)){
			source = SqlVariables.accountManagementTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getSettingsTabParentWidgetId(), menu)){
			source = SqlVariables.settingsTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getLogoutTabParentWidgetId(), menu)){
			source = SqlVariables.logoutTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getWorldSwitcherTabParentWidgetId(), menu)) {
			source = SqlVariables.worldSwitcherTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getEmotesTabParentWidgetId(), menu)){
			source = SqlVariables.emotesTabInSource.getValue();
		} else if(isChildWidgetOf(ids.getMusicTabParentWidgetId(), menu)){
			source = SqlVariables.musicTabInSource.getValue();
		}
		log.info("source: " + source);
		return source;
	}

	private void printMenuEntry(MenuEntry menuEntry)
	{
		String target = menuEntry.getTarget();
		String option = menuEntry.getOption();
		MenuAction type = menuEntry.getType();
		log.info("option: " + option + ", target: " + target + ", type: " + type);
	}

	static void mapWidgetText(Widget[] childComponents) {
		for (Widget component : childComponents) {
			remapWidget(component);
			String text = component.getText();
			if (text.isEmpty())
				continue;
			RemapWidgetText(component, text);
		}
	}
	static void remapWidget(Widget widget) {
		final int groupId = WidgetInfo.TO_GROUP(widget.getId());
		final int CHAT_MESSAGE = 162, PRIVATE_MESSAGE = 163, FRIENDS_LIST = 429;

		if (groupId == CHAT_MESSAGE || groupId == PRIVATE_MESSAGE || groupId == FRIENDS_LIST)
			return;

		Widget[] children = widget.getDynamicChildren();
		if (children == null)
			return;

		Widget[] childComponents = widget.getDynamicChildren();
		if (childComponents != null)
			mapWidgetText(childComponents);

		childComponents = widget.getStaticChildren();
		if (childComponents != null)
			mapWidgetText(childComponents);

		childComponents = widget.getNestedChildren();
		if (childComponents != null)
			mapWidgetText(childComponents);
	}
	static void RemapWidgetText(Widget component, String text)
	{
		if (component.getText().contains("Rapid"))
		{
			component.setText(text.replace("Rapid", "Hurtig"));
		}
	}

	private void translateItemName(String source, MenuEntryAdded entryAdded, String target)
	{
		if(target.length() == 0)
		{
			return;
		}
		
		// translates item name
		try
		{
			String newName = target;
			if(source.equals("items"))
			{
				newName = itemTranslator.getText(source, target, true);
				entryAdded.getMenuEntry().setTarget(newName);
			}
			else if(source.equals("objects"))
			{
				newName = objectTranslator.getText(source, target, true);
				entryAdded.getMenuEntry().setTarget(newName);
			}
		}
		catch(Exception f)
		{
			if(debugMessages)
			{
				logger.log("Could not translate '"
		            + source
			        + "' name: "
		            + target
					+ " - "
					+ f.getMessage());
			}
		}
	}
	private void translateMenuAction(String actionSource, MenuEntryAdded entryAdded, String target)
	{
		// translates menu action
		try
		{
			String newAction = actionTranslator.getText(actionSource, target, true);
			entryAdded.getMenuEntry().setOption(newAction);
		}
		catch(Exception f)
		{
			// if current action is not from the informed category
			// checks if it is a generic action
			if(!actionSource.equals("generalactions"))
			{
				try
				{
					translateMenuAction("generalactions", entryAdded, target);
				}
				catch(Exception g)
				{
					if(debugMessages)
					{
						logger.log("Could not translate menu '"
					        + actionSource
					        + "' action: "
					        + target
					        + " - "
					        + f.getMessage()
							+ " - "
							+ g.getMessage());
					}
				}
			}
			else if(debugMessages)
			{
				logger.log("Could not translate general action menu entry: "
		            + target
			        + " - "
			        + f.getMessage());
			}
		}
	}
	private Pair<String, String> convertWidgetOnSomething(MenuEntry entry)
	{
		String menuTarget = entry.getTarget();
		String[] parts = menuTarget.split(" -> ");
		String itemName = parts[0];
		String useOnName = parts[1];
		return Pair.of(itemName, useOnName);
	}

	private boolean hasLevel(String target)
	{
		// check if target contains <col=(numbers and alphabets)>(level-(*\d)). such as "<col=ffffff>Mama Layla<col=ffff00>(level-3000)"
		Pattern re = Pattern.compile(".+<col=[a-zA-Z0-9]+>\\s*\\(level-\\d+\\)");
		return re.matcher(target).find();
	}



	private boolean isWalkOrCancel(MenuAction action)
	{
		return ((action.equals(MenuAction.CANCEL))
				|| (action.equals(MenuAction.WALK)));
	}

	private boolean isGeneralMenu(MenuEntry menuEntry)
	{
		MenuAction action = menuEntry.getType();
		// checks if current action target is a menu that introduces general actions
		return  (!isItemInWidget(menuEntry) &&
				(action.equals(MenuAction.CC_OP)
				|| action.equals(MenuAction.CC_OP_LOW_PRIORITY)))
				|| isWalkOrCancel(action)
				|| action.equals(MenuAction.RUNELITE_OVERLAY)
				|| action.equals(MenuAction.RUNELITE);
	}
	private boolean isObjectMenu(MenuAction action)
	{
		return ((action.equals(MenuAction.EXAMINE_OBJECT))
				|| (action.equals(MenuAction.GAME_OBJECT_FIRST_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_SECOND_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_THIRD_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_FOURTH_OPTION))
				|| (action.equals(MenuAction.GAME_OBJECT_FIFTH_OPTION)));
	}
	private boolean isNpcMenu(MenuAction action)
	{
		return ((action.equals(MenuAction.EXAMINE_NPC))
				|| (action.equals(MenuAction.NPC_FIRST_OPTION))
				|| (action.equals(MenuAction.NPC_SECOND_OPTION))
				|| (action.equals(MenuAction.NPC_THIRD_OPTION))
				|| (action.equals(MenuAction.NPC_FOURTH_OPTION))
				|| (action.equals(MenuAction.NPC_FIFTH_OPTION)));
	}
	private boolean isItemOnGround(MenuAction action)
	{
		return ((action.equals(MenuAction.EXAMINE_ITEM_GROUND))
				|| (action.equals(MenuAction.GROUND_ITEM_FIRST_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_SECOND_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_THIRD_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_FOURTH_OPTION))
				|| (action.equals(MenuAction.GROUND_ITEM_FIFTH_OPTION)));
	}
	private boolean isPlayerMenu(MenuAction action)
	{
		return ((action.equals(MenuAction.PLAYER_FIRST_OPTION))
				|| (action.equals(MenuAction.PLAYER_SECOND_OPTION))
				|| (action.equals(MenuAction.PLAYER_THIRD_OPTION))
				|| (action.equals(MenuAction.PLAYER_FOURTH_OPTION))
				|| (action.equals(MenuAction.PLAYER_FIFTH_OPTION))
				|| (action.equals(MenuAction.PLAYER_SIXTH_OPTION))
				|| (action.equals(MenuAction.PLAYER_SEVENTH_OPTION))
				|| (action.equals(MenuAction.PLAYER_EIGHTH_OPTION))
				|| (action.equals(MenuAction.RUNELITE_PLAYER)));
	}
	private boolean isWidgetOnSomething(MenuAction action)
	{
		return ((action.equals(MenuAction.WIDGET_TARGET_ON_WIDGET))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_NPC))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM))
				|| (action.equals(MenuAction.WIDGET_TARGET_ON_PLAYER)));
	}

	private boolean isItemInWidget(MenuEntry menuEntry){ // todo: needs checking
		MenuAction action = menuEntry.getType();
		String target = menuEntry.getTarget();
		target = Colors.removeColorTag(target);
		if(target.isEmpty()){
			return false;
		}

		SqlQuery sqlQuery = new SqlQuery(this.plugin);
		sqlQuery.setItemName(target, Colors.orange);


		return sqlQuery.getMatching(SqlVariables.columnEnglish).length > 0 &&
				(action.equals(MenuAction.CC_OP)
				|| action.equals(MenuAction.CC_OP_LOW_PRIORITY)
				|| action.equals(MenuAction.WIDGET_TARGET)
				);

	}

	private Widget getWidgetOfMenu(MenuEntry menuEntry){
		return client.getWidget(menuEntry.getParam1());
	}

	private boolean isChildWidgetOf(int widgetIdToCheck, MenuEntry menuEntry){
		if(widgetIdToCheck == -1){
			return false;
		}
		Widget widget = client.getWidget(menuEntry.getParam1());

		while(widget != null){
			if(widget.getId() == widgetIdToCheck){
				return true;
			}
			widget = widget.getParent();
		}
		return false;
	}


}
