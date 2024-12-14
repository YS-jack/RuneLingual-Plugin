package com.RuneLingual.Wigets;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.commonFunctions.Transformer;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.RuneLingual.debug.OutputToFile.appendToFile;

public class WidgetCapture {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private WidgetsUtilRLingual widgetsUtilRLingual;
    @Inject
    private DialogTranslator dialogTranslator;
    @Inject
    Client client;

    @Inject
    public WidgetCapture(RuneLingualPlugin plugin){
        this.plugin = plugin;
    }

    public void translateWidget(){
        Widget[] roots = client.getWidgetRoots();
        for (Widget root : roots) {
            translateWidgetRecursive(root);
        }
    }

    private void translateWidgetRecursive(Widget widget) {
        int widgetId = widget.getId();

        List<Integer> widgetIdsToIgnore = Arrays.asList(ComponentID.CHATBOX_INPUT);
        // stop the recursion if the widget is hidden or should be ignored
        if (widget.isHidden() || widgetIdsToIgnore.contains(widgetId)) {
            return;
        }

        for (Widget dynamicChild : widget.getDynamicChildren()) {translateWidgetRecursive(dynamicChild);}
        for (Widget nestedChild : widget.getNestedChildren()) {translateWidgetRecursive(nestedChild);}
        for (Widget staticChild : widget.getStaticChildren()) {translateWidgetRecursive(staticChild);}

        int widgetGroup = WidgetUtil.componentToInterface(widget.getId());
        if (widgetGroup == InterfaceID.CHATBOX) {// skip all chatbox widgets for now TODO: chatbox buttons should be translated
            return;
        }

        if (isDumpTarget(widget)){
            return;
        }

        if (widgetGroup == InterfaceID.DIALOG_NPC
                || widgetGroup == InterfaceID.DIALOG_PLAYER
                || widgetGroup == InterfaceID.DIALOG_OPTION) {
            dialogTranslator.handleDialogs(widget);
            return;
        }
        if (!widget.getText().isEmpty()
            && !widget.getText().contains("<img=") // check if already translated to japanese. TODO: need something else after adding other languages
            && plugin.getConfig().getInterfaceTextConfig().equals(RuneLingualConfig.ingameTranslationConfig.USE_API)) {

        // if its only numbers and symbols dont do anything
        String re = "^[^\\p{Alpha}]+$";
        if (widget.getText().matches(re))
            return;

        Colors[] textColor = Colors.getColorArray(widget.getText(), Colors.black);
        // for now only translate interfaces and buttons with API
        widgetsUtilRLingual.setWidgetText_ApiTranslation(widget, widget.getText(), textColor[0]);

        }
    }

    private boolean isDumpTarget(Widget widget) {
        if (widget.getParent().getId() == 14024705 || widget.getParent().getId() == 14024714) { //parent of skill guide, or parent of element in list
            if (widget.getText().matches("\\d{1,2}"))
                return true;
            appendToFile(widget.getText() + "\t\t", "skillGuideDump.txt");
            return true;
        }
        return false;
    }
}

