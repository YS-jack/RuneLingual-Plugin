package com.RuneLingual.Wigets;

import com.RuneLingual.RuneLingualPlugin;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class Widget2ResizeDict {
    private List<Widget2Resize> widgets2Resize = new ArrayList<>();
    @Inject
    private RuneLingualPlugin plugin;

    @Inject
    public Widget2ResizeDict(RuneLingualPlugin plugin) {
        this.plugin = plugin;
    }

    @Getter @Setter
    public static class Widget2Resize{
        private int widgetId;
        private boolean fixedTop;
        private boolean fixedBottom;
        private boolean fixedLeft;
        private boolean fixedRight;
        private int topPadding;
        private int bottomPadding;
        private int leftPadding;
        private int rightPadding;

        public Widget2Resize(int widgetId, boolean fixedTop, boolean fixedBottom, boolean fixedLeft, boolean fixedRight, int topPadding, int bottomPadding, int leftPadding, int rightPadding){
            this.widgetId = widgetId;
            this.fixedTop = fixedTop;
            this.fixedBottom = fixedBottom;
            this.fixedLeft = fixedLeft;
            this.fixedRight = fixedRight;
            this.topPadding = topPadding;
            this.bottomPadding = bottomPadding;
            this.leftPadding = leftPadding;
            this.rightPadding = rightPadding;
        }

    }

    public void add(int widgetId, boolean fixedTop, boolean fixedBottom, boolean fixedLeft, boolean fixedRight, int topPadding, int bottomPadding, int leftPadding, int rightPadding) {
        Widget2Resize widget2Resize = new Widget2Resize(widgetId, fixedTop, fixedBottom, fixedLeft, fixedRight, topPadding, bottomPadding, leftPadding, rightPadding);
        widgets2Resize.add(widget2Resize);
    }
    private boolean contains(int widgetId) {
        for (Widget2Resize widget2Resize : widgets2Resize) {
            if (widget2Resize.getWidgetId() == widgetId) {
                return true;
            }
        }
        return false;
    }

    public Widget2Resize getWidgets2Resize(int widgetId) {
        for (Widget2Resize widget2Resize : widgets2Resize) {
            if (widget2Resize.getWidgetId() == widgetId) {
                return widget2Resize;
            }
        }
        return null;
    }

    public void resizeWidgetIfNeeded(Widget widget, String newText) {
        int widgetId = widget.getId();
        if (!contains(widgetId)) {
            return;
        }
        resizeWidget(widget, newText);
    }


    public void resizeWidget(Widget widget, String newText) {
        int widgetId = widget.getId();

        Widget2Resize widget2Resize = getWidgets2Resize(widgetId);
        if (widget2Resize == null) {
            return;
        }

        // resize widget width
        if (!widget2Resize.fixedLeft || !widget2Resize.fixedRight) {
            this.plugin.getWidgetsUtilRLingual().changeWidgetWidth(widget, newText, widget2Resize.leftPadding, widget2Resize.rightPadding);
        }

        // resize widget height
        if (!widget2Resize.fixedTop || !widget2Resize.fixedBottom) {
            this.plugin.getWidgetsUtilRLingual().changeWidgetHeight(widget, newText, widget2Resize.topPadding, widget2Resize.bottomPadding);
        }
    }



}