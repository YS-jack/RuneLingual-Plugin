package com.RuneLingual.Wigets;

import com.RuneLingual.RuneLingualPlugin;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;

import javax.inject.Inject;

public class WidgetCapture {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private WidgetsUtilRLingual widgetsUtilRLingual;
    @Inject
    private DialogTranslator dialogTranslator;

    @Inject
    public WidgetCapture(RuneLingualPlugin plugin){
        this.plugin = plugin;
    }

    public void translateWidget(){
        Widget[] validWidgets = widgetsUtilRLingual.getAllChildren();
        for(Widget validWidget : validWidgets){
            int widgetGroup = WidgetUtil.componentToInterface(validWidget.getId());

            if(widgetGroup == InterfaceID.DIALOG_NPC
                || widgetGroup == InterfaceID.DIALOG_PLAYER
                || widgetGroup == InterfaceID.DIALOG_OPTION){
                dialogTranslator.handleDialogs(validWidget);
            }
        }
    }
}
