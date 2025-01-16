package com.RuneLingual.Wigets;

import com.RuneLingual.RuneLingualPlugin;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class Widget2FitTextDict {
    private List<Widget2FitText> widgets2FitText = new ArrayList<>();
    @Inject
    private RuneLingualPlugin plugin;

    @Inject
    public Widget2FitTextDict(RuneLingualPlugin plugin) {
        this.plugin = plugin;
    }

    @Getter @Setter
    public static class Widget2FitText {
        private int widgetId;
        private boolean fixedTop;
        private boolean fixedBottom;
        private boolean fixedLeft;
        private boolean fixedRight;
        private int topPadding;
        private int bottomPadding;
        private int leftPadding;
        private int rightPadding;

        public Widget2FitText(int widgetId, boolean fixedTop, boolean fixedBottom, boolean fixedLeft, boolean fixedRight, int topPadding, int bottomPadding, int leftPadding, int rightPadding){
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
        Widget2FitText widget2FitText = new Widget2FitText(widgetId, fixedTop, fixedBottom, fixedLeft, fixedRight, topPadding, bottomPadding, leftPadding, rightPadding);
        widgets2FitText.add(widget2FitText);
    }
    private boolean contains(int widgetId) {
        for (Widget2FitText widget2FitText : widgets2FitText) {
            if (widget2FitText.getWidgetId() == widgetId) {
                return true;
            }
        }
        return false;
    }

    public Widget2FitText getWidgets2FitText(int widgetId) {
        for (Widget2FitText widget2FitText : widgets2FitText) {
            if (widget2FitText.getWidgetId() == widgetId) {
                return widget2FitText;
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

        Widget2FitText widget2FitText = getWidgets2FitText(widgetId);
        if (widget2FitText == null) {
            return;
        }

        // resize widget width
        if (!widget2FitText.fixedLeft || !widget2FitText.fixedRight) {
            this.plugin.getWidgetsUtilRLingual().changeWidgetWidth(widget, newText, widget2FitText.leftPadding, widget2FitText.rightPadding);
        }

        // resize widget height
        if (!widget2FitText.fixedTop || !widget2FitText.fixedBottom) {
            this.plugin.getWidgetsUtilRLingual().changeWidgetHeight(widget, newText, widget2FitText.topPadding, widget2FitText.bottomPadding);
        }
    }



}