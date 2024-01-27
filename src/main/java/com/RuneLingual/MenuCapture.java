package com.RuneLingual;

import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;

import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;

public class MenuCapture
{
	@Inject
	private Client client;
	@Inject
	private RuneLingualConfig config;
	
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
	
	// TODO: right click menu title 'Chose Options' - seems to not be directly editable
	
	public void handleMenuEvent(MenuEntryAdded event)
	{
		// called whenever a right click menu is opened
		MenuEntry currentMenu = event.getMenuEntry();
		String menuAction = currentMenu.getOption();
		String menuTarget = currentMenu.getTarget();
		
		// some possible targets
		NPC targetNpc = currentMenu.getNpc();
		Player targetPlayer = currentMenu.getPlayer();
		int targetItem = currentMenu.getItemId();
		MenuAction menuType = currentMenu.getType();
		
		try
		{
			if(isPlayerMenu(menuType))
			{
				translateMenuAction("playeractions", event, menuTarget);
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
					
				}*/
				
			}
			
		}
		catch (Exception e)
		{
			if(debugMessages)
			{
				logger.log("Critical error happened while processing right click menus: " + e.getMessage());
			}
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
	
	private boolean isGeneralMenu(MenuAction action)
	{
		// checks if current action target is a menu that introduces general actions
		if(action.equals(MenuAction.CC_OP))
		{
			return true;
		}
		else if(action.equals(MenuAction.CC_OP_LOW_PRIORITY))
		{
			return true;
		}
		else if(action.equals(MenuAction.CANCEL))
		{
			return true;
		}
		else if(action.equals(MenuAction.WALK))
		{
			return true;
		}
		return false;
	}
	private boolean isObjectMenu(MenuAction action)
	{
		if(action.equals(MenuAction.EXAMINE_OBJECT))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_FIRST_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_SECOND_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_THIRD_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_THIRD_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_FOURTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_FIFTH_OPTION))
		{
			return true;
		}
		return false;
	}
	
	private boolean isNpcMenu(MenuAction action)
	{
		if(action.equals(MenuAction.EXAMINE_NPC))
		{
			return true;
		}
		else if(action.equals(MenuAction.NPC_FIRST_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.NPC_SECOND_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.NPC_THIRD_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.NPC_FOURTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.NPC_FIFTH_OPTION))
		{
			return true;
		}
		return false;
	}
	
	private boolean isItemMenu(MenuAction action)
	{
		if(action.equals(MenuAction.EXAMINE_ITEM_GROUND))
		{
			return true;
		}
		else if(action.equals(MenuAction.GROUND_ITEM_FIRST_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GROUND_ITEM_SECOND_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GROUND_ITEM_THIRD_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GROUND_ITEM_FOURTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GROUND_ITEM_FIFTH_OPTION))
		{
			return true;
		}
		return false;
	}
	
	private boolean isPlayerMenu(MenuAction action)
	{
		if(action.equals(MenuAction.PLAYER_FIRST_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_SECOND_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_THIRD_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_FOURTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_FIFTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_SIXTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_SEVENTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_EIGHTH_OPTION))
		{
			return true;
		}
		return false;
	}
}
