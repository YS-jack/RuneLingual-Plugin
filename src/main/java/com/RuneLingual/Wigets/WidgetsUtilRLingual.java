package com.RuneLingual.Wigets;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import java.util.List;
import java.util.ArrayList;

public class WidgetsUtilRLingual
{
	@Inject
	private Client client;

	public Widget[] getAllChildren()
	{
		// Create a list to store widgets
		List<Widget> widgetList = new ArrayList<>();

		Widget[] roots = client.getWidgetRoots();
		for (Widget root : roots) {
			iterateWidgetsRecursive(root, widgetList);
		}

		// Convert the list to an array and return
		return widgetList.toArray(new Widget[0]);
	}


	public static List<Widget> getAllChildren(Widget widget)
	{
		// Create a list to store widgets
		List<Widget> widgetList = new ArrayList<>();
		
		// Call the recursive method to populate the list
		iterateWidgetsRecursive(widget, widgetList);
		
		// Convert the list to an array and return
		return widgetList;
	}
	
	private static void iterateWidgetsRecursive(Widget widget, List<Widget> widgetList)
	{
		// Check if the widget is not null and not hidden
		if(widget != null && !widget.isHidden())
		{
			// Add the widget to the list
			if(!widget.getText().isBlank() || !widget.getName().isBlank())
			{
				widgetList.add(widget);
			}
			
			Widget[] staticChildren = widget.getStaticChildren();
			if (staticChildren != null)
			{
				for (Widget child : staticChildren)
				{
					iterateWidgetsRecursive(child, widgetList);
				}
			}
			Widget[] dynamicChildren = widget.getDynamicChildren();
			if (dynamicChildren != null)
			{
				for (Widget child : dynamicChildren)
				{
					iterateWidgetsRecursive(child, widgetList);
				}
			}
			Widget[] nestedChildren = widget.getNestedChildren();
			if (nestedChildren != null)
			{
				for (Widget child : nestedChildren)
				{
					iterateWidgetsRecursive(child, widgetList);
				}
			}
		}
	}
}
