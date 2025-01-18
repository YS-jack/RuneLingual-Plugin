package com.RuneLingual.Wigets;

import lombok.Getter;

import net.runelite.api.widgets.Widget;


import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WidgetChildrenPositionManager {
    private final Widget parentNode;
    private final Set<Node<Widget>> existingWidgets = new HashSet<>();

    public WidgetChildrenPositionManager(Widget parentNode) {
        this.parentNode = parentNode; // the parent widget of the widgets to be fit text and positioned
        initNodes();
    }

    protected enum RelativePosition {
        RIGHT,
        LEFT,
        ABOVE,
        BELOW,
        INSIDE,
        OUTSIDE
    }

    @Getter
    private static class Node<T> {
        private final Widget widget;
        private final Point topLeft;
        private final Point bottomRight;
        private final int originalWidth;
        private final int originalHeight;
        private final List<Node<T>> widgetOnRight = new ArrayList<>();
        private final List<Node<T>> widgetOnLeft = new ArrayList<>();
        private final List<Node<T>> widgetAbove = new ArrayList<>();
        private final List<Node<T>> widgetBelow = new ArrayList<>();
        private final List<Node<T>> widgetInside = new ArrayList<>();
        private final List<Node<T>> widgetOutside = new ArrayList<>();

        public Node(T widget) {
            this.widget = (Widget) widget;
            this.topLeft = new Point(this.widget.getOriginalX(), this.widget.getOriginalY());
            this.originalWidth = this.widget.getOriginalWidth();
            this.originalHeight = this.widget.getOriginalHeight();
            this.bottomRight = new Point(topLeft.x + originalWidth, topLeft.y + originalHeight);
        }

        
        private List<Widget> addWidgetsOnRight() {
            Widget parentWidget = widget.getParent();
            if (parentWidget == null) {
                return new ArrayList<>();
            }

            List<Widget> widgetsOnRight = new ArrayList<>();
            for (Widget sibling : parentWidget.getDynamicChildren()) {
                if (isOnTheRight(sibling) && sharesHeightRange(sibling)) {
                    widgetsOnRight.add(sibling);
                }
            }
            return widgetsOnRight;
        }
        private boolean isOnTheRight(Widget sibling) {
            return sibling.getOriginalX() > this.topLeft.x + this.originalWidth;
        }
        private boolean sharesHeightRange(Widget sibling) {
            int widgetTop = this.getTopY();
            int widgetBottom = this.getBottomY();
            int siblingTop = sibling.getOriginalY();
            int siblingBottom = sibling.getOriginalY() + sibling.getOriginalHeight();
            return (siblingTop > widgetTop && siblingTop < widgetBottom) // sibling's top edge is between widget's top and bottom edge
                 || (siblingBottom > widgetTop && siblingBottom < widgetBottom) // sibling's bottom edge is between widget's top and bottom edge
                 || (siblingTop < widgetTop && siblingBottom > widgetBottom); // sibling's top edge is above widget's top edge and sibling's bottom edge is below widget's bottom edge
        }

        private Point getTopRight(){
            return new Point(bottomRight.x, topLeft.y);
        }
        private Point getBottomLeft(){
            return new Point(topLeft.x, bottomRight.y);
        }
        private int getTopY(){
            return topLeft.y;
        }
        private int getBottomY(){
            return bottomRight.y;
        }
        private int getLeftX(){
            return topLeft.x;
        }
        private int getRightX(){
            return bottomRight.x;
        }

    }


    private void initNodes() {

        List<Widget> childWidgets = new ArrayList<>(List.of(parentNode.getDynamicChildren()));
        childWidgets.addAll(List.of(parentNode.getStaticChildren()));
        childWidgets.addAll(List.of(parentNode.getNestedChildren()));
        for (Widget childWidget : childWidgets) {
            Node<Widget> childNode = new Node<>(childWidget);
            existingWidgets.add(childNode);
            childNode.addWidgetsOnRight();

        }
    }

    private boolean isInside(Widget baseWidget, Widget targetWidget) {
        int baseX = baseWidget.getRelativeX();
        int baseY = baseWidget.getRelativeY();
        int baseWidth = baseWidget.getWidth();
        int baseHeight = baseWidget.getHeight();
        int targetX = targetWidget.getRelativeX();
        int targetY = targetWidget.getRelativeY();
        int targetWidth = targetWidget.getWidth();
        int targetHeight = targetWidget.getHeight();
        return targetX >= baseX && targetY >= baseY && targetX + targetWidth <= baseX + baseWidth && targetY + targetHeight <= baseY + baseHeight;
    }


}
