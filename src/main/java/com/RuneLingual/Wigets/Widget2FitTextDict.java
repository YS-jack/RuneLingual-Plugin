package com.RuneLingual.Wigets;

import com.RuneLingual.RuneLingualPlugin;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;

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
        private Widget widget;
        private final int widgetId;
        private final boolean hasAdjacentSiblingWidget;
        private final boolean fixedTop;
        private final boolean fixedBottom;
        private final boolean fixedLeft;
        private final boolean fixedRight;
        private final int topPadding;
        private final int bottomPadding;
        private final int leftPadding;
        private final int rightPadding;

        public Widget2FitText(int widgetId, boolean hasAdjacentSiblingWidget, boolean fixedTop, boolean fixedBottom, boolean fixedLeft, boolean fixedRight, int topPadding, int bottomPadding, int leftPadding, int rightPadding){
            this.widgetId = widgetId;
            this.hasAdjacentSiblingWidget = hasAdjacentSiblingWidget;
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

    private enum Direction {
        RIGHT,
        LEFT,
        ABOVE,
        BELOW,
        INSIDE,
        OUTSIDE,
        DIAGONAL, // won't need to shift, nor is it inside or outside
    }

    public void add(int widgetId, boolean hasSiblingWidget , boolean fixedTop, boolean fixedBottom, boolean fixedLeft, boolean fixedRight, int topPadding, int bottomPadding, int leftPadding, int rightPadding) {
        Widget2FitText widget2FitText = new Widget2FitText(widgetId, hasSiblingWidget, fixedTop, fixedBottom, fixedLeft, fixedRight, topPadding, bottomPadding, leftPadding, rightPadding);
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
        widget2FitText.setWidget(widget);

        // change width/height of widget
        // if the widget doesn't have adjacent sibling widgets, make parent + sibling widgets larger/smaller by the same amount
        // else (if the widget has adjacent sibling widgets):
        // reposition depending on what side is fixed
        // shift sibling widgets in that direction
        // change parent width/height if needed
        // revalidate widgets

        if (!widget2FitText.fixedLeft || !widget2FitText.fixedRight) {
            //this.plugin.getWidgetsUtilRLingual().changeWidgetWidth(widget, newText, widget2FitText.leftPadding, widget2FitText.rightPadding);
        }


        if (!widget2FitText.fixedTop || !widget2FitText.fixedBottom) {
            int originalHeight = widget.getHeight();
            int originalY = widget.getRelativeY();
            int newHeight = getHeightToFit(widget2FitText, newText);
            int heightDiff = newHeight - originalHeight;
            if (originalHeight == newHeight) {
                return;
            }
            Widget parentWidget = widget.getParent();

            // if the widget doesn't have adjacent sibling widgets, make parent + sibling widgets larger/smaller by the same amount
            if (!widget2FitText.hasAdjacentSiblingWidget && widget.getParent() != null) {
                heightDiff = newHeight - originalHeight;

                // set new height for parent
                int newParentHeight = parentWidget.getHeight() + heightDiff;
                setWidgetHeightAbsolute(parentWidget, newParentHeight);

                // set new height for sibling widgets
                List<Widget> childWidgets = getAllChildWidget(parentWidget);
                for (Widget sibling : childWidgets) {
                    if (sibling != widget && sibling.getType() != 3) { // 3 seems to be the type for background widgets
                        int originalSiblingHeight = sibling.getHeight();
                        int newSiblingHeight = originalSiblingHeight + heightDiff;
                        setWidgetHeightAbsolute(sibling, newSiblingHeight);
                    }
                }
            } else {
                // reposition depending on what side is fixed, and resize
                Direction dirToShift = getVerticalDirToShift(widget2FitText);
                int newY = getNewShiftedY(widget, dirToShift, heightDiff); // relative position

                // shift sibling widgets in that direction
                List<Widget> childWidgets = getAllChildWidget(widget.getParent());
                for (Widget sibling : childWidgets) {
                    Direction dir = getDirTowards(widget, sibling);
                    if (dir == dirToShift || dir == Direction.INSIDE) {
                        shiftWidgetY(sibling, heightDiff, dirToShift);
                    } else if (dir == Direction.OUTSIDE) {
                        expandWidget(sibling, dirToShift, heightDiff);
                    }
                }

                setWidgetHeightAbsolute(widget, newHeight);
                setWidgetRelativeYPos(widget, newY);

                // change parent width/height if needed
                int siblingYCoverage = getSiblingsYCoverage(childWidgets);
                if (parentWidget.getHeight() < siblingYCoverage) {
                    setWidgetHeightAbsolute(parentWidget, siblingYCoverage);
                }

            }
        }
    }


    private int getHeightToFit(Widget2FitText widget2FitText, String newText) {
        int lineHeight = plugin.getConfig().getSelectedLanguage().getCharHeight();
        int numLines = newText.split("<br>").length;
        return lineHeight * numLines + widget2FitText.topPadding + widget2FitText.bottomPadding;
    }

    private void setWidgetWidthAbsolute(Widget widget, int width) {
        widget.setWidthMode(WidgetSizeMode.ABSOLUTE)
                .setOriginalWidth(width)
                .revalidate();
    }

    private void setWidgetHeightAbsolute(Widget widget, int height) {
        widget.setHeightMode(WidgetSizeMode.ABSOLUTE)
                .setOriginalHeight(height)
                .revalidate();
    }

    private void setWidgetRelativeXPos(Widget widget, int x) {
        widget.setOriginalX(x)
                .setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT)
                .revalidate();
    }
    private void setWidgetRelativeYPos(Widget widget, int y) {
        widget.setOriginalY(y)
                .setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP)
                .revalidate();
    }


    private Direction getVerticalDirToShift(Widget2FitText widget2FitText) {
        if (widget2FitText.fixedTop && widget2FitText.fixedBottom) {
            // can be difficult to determine which direction to shift, so hard coding

            // for spellbook tab hover text
            if (widget2FitText.getWidgetId() == plugin.getIds().getSpellbookTabHoverTextId()){
                // if the bottom right corner of widget is at the bottom of parent, shift above
                Widget widget = widget2FitText.getWidget();
                int parentHeight = widget.getParent().getHeight();
                if (widget.getRelativeY() + widget.getHeight() > parentHeight * 0.8) {
                    return Direction.ABOVE;
                } else {
                    return Direction.BELOW;// not tested
                }
            }
            return Direction.OUTSIDE; // shift both ways
        } else if (widget2FitText.fixedBottom) {
            return Direction.BELOW;
        } else if (widget2FitText.fixedTop) {
            return Direction.ABOVE;
        }
        return Direction.OUTSIDE;
    }

    // returns relative position
    private int getNewShiftedY(Widget widget, Direction dirToShift, int heightDiff) {
        int newY = getNewPos(widget, dirToShift, heightDiff);
        if (newY == -1) {
            int originalY = widget.getRelativeY();
            return originalY - heightDiff / 2;
        }
        return newY;
    }

    // returns relative position
    private int getNewX(Widget widget, Direction dirToShift, int widthDiff) {
        int newX = getNewPos(widget, dirToShift, widthDiff);
        if (newX == -1) {
            int originalX = widget.getRelativeX();
            return originalX - widthDiff / 2;
        }
        return newX;
    }

    private int getNewPos(Widget widget, Direction dirToShift, int diff) {
        int originalY = widget.getRelativeY();
        int originalX = widget.getRelativeX();
        int newPos;
        switch (dirToShift) {
            case ABOVE:
                newPos = originalY - diff;
                break;
            case BELOW:
                newPos = originalY + diff;
                break;
            case RIGHT:
                newPos = originalX + diff;
                break;
            case LEFT:
                newPos = originalX - diff;
                break;
            default:
                newPos = -1;
        }
        return newPos;
    }

    private List<Widget> getAllChildWidget(Widget widget) {
        List<Widget> childWidgets = new ArrayList<>(List.of(widget.getDynamicChildren()));
        childWidgets.addAll(List.of(widget.getStaticChildren()));
        childWidgets.addAll(List.of(widget.getNestedChildren()));
        return childWidgets;
    }

    // returns true if sibling is in the direction of widget and would overlap if widget moved in that direction
    private boolean isInDirection(Widget sibling, Widget widget, Direction dir) {
        int siblingTop = sibling.getRelativeY();
        int siblingBottom = sibling.getRelativeY() + sibling.getHeight();
        int siblingLeft = sibling.getRelativeX();
        int siblingRight = sibling.getRelativeX() + sibling.getWidth();
        int widgetTop = widget.getRelativeY();
        int widgetBottom = widget.getRelativeY() + widget.getHeight();
        int widgetLeft = widget.getRelativeX();
        int widgetRight = widget.getRelativeX() + widget.getWidth();

        switch (dir) {
            case ABOVE:
                return siblingBottom < widgetTop && widgetsOverlapHor(sibling, widget);
            case BELOW:
                return siblingTop > widgetBottom && widgetsOverlapHor(sibling, widget);
            case RIGHT:
                return siblingLeft > widgetRight && widgetsOverlapVer(sibling, widget);
            case LEFT:
                return siblingRight < widgetLeft && widgetsOverlapVer(sibling, widget);
            default:
                return false;
        }
    }

    private Direction getDirTowards(Widget baseWidget, Widget targetWidget) {
        int baseTop = baseWidget.getRelativeY();
        int baseBottom = baseWidget.getRelativeY() + baseWidget.getHeight();
        int baseLeft = baseWidget.getRelativeX();
        int baseRight = baseWidget.getRelativeX() + baseWidget.getWidth();
        int targetTop = targetWidget.getRelativeY();
        int targetBottom = targetWidget.getRelativeY() + targetWidget.getHeight();
        int targetLeft = targetWidget.getRelativeX();
        int targetRight = targetWidget.getRelativeX() + targetWidget.getWidth();
        if (targetBottom < baseTop && widgetsOverlapHor(baseWidget, targetWidget)) {
            return Direction.ABOVE;
        } else if (targetTop > baseBottom && widgetsOverlapHor(baseWidget, targetWidget)) {
            return Direction.BELOW;
        } else if (targetRight < baseLeft && widgetsOverlapVer(baseWidget, targetWidget)) {
            return Direction.LEFT;
        } else if (targetLeft > baseRight && widgetsOverlapVer(baseWidget, targetWidget)) {
            return Direction.RIGHT;
        } else if (targetTop >= baseTop && targetBottom <= baseBottom && targetLeft >= baseLeft && targetRight <= baseRight) {
            return Direction.INSIDE;
        } else if (targetTop < baseTop && targetBottom > baseBottom && targetLeft < baseLeft && targetRight > baseRight) {
            return Direction.OUTSIDE;
        } else {
            return Direction.DIAGONAL;
        }
    }

    private boolean widgetsOverlapHor(Widget w1, Widget w2) {
        int w1Left = w1.getRelativeX();
        int w1Right = w1.getRelativeX() + w1.getWidth();
        int w2Left = w2.getRelativeX();
        int w2Right = w2.getRelativeX() + w2.getWidth();
        return (w1Left > w2Left && w1Left < w2Right) || (w1Right > w2Left && w1Right < w2Right) || (w1Left < w2Left && w1Right > w2Right);
    }

    private boolean widgetsOverlapVer(Widget w1, Widget w2) {
        int w1Top = w1.getRelativeY();
        int w1Bottom = w1.getRelativeY() + w1.getHeight();
        int w2Top = w2.getRelativeY();
        int w2Bottom = w2.getRelativeY() + w2.getHeight();
        return (w1Top > w2Top && w1Top < w2Bottom) || (w1Bottom > w2Top && w1Bottom < w2Bottom) || (w1Top < w2Top && w1Bottom > w2Bottom);
    }

    private int getSiblingsYCoverage(List<Widget> siblings) {
        int minY = 999999;
        int maxY = 0;
        for (Widget sibling : siblings) {
            int siblingTop = sibling.getRelativeY();
            int siblingBottom = sibling.getRelativeY() + sibling.getHeight();
            if (siblingTop < minY) {
                minY = siblingTop;
            }
            if (siblingBottom > maxY) {
                maxY = siblingBottom;
            }
        }
        return maxY - minY;
    }

    private void expandWidget(Widget widget, Direction dir, int diff) {
        int originalY = widget.getRelativeY();
        int originalX = widget.getRelativeX();
        int originalHeight = widget.getHeight();
        int originalWidth = widget.getWidth();
        int newY = originalY;
        int newX = originalX;
        int newHeight = originalHeight;
        int newWidth = originalWidth;
        switch (dir) {
            case ABOVE:
                newY = originalY - diff;
                newHeight = originalHeight + diff;
                break;
            case BELOW:
                newHeight = originalHeight + diff;
                break;
            case RIGHT:
                newWidth = originalWidth + diff;
                break;
            case LEFT:
                newX = originalX - diff;
                newWidth = originalWidth + diff;
                break;
        }
        setWidgetHeightAbsolute(widget, newHeight);
        setWidgetWidthAbsolute(widget, newWidth);
        setWidgetRelativeXPos(widget, newX);
        setWidgetRelativeYPos(widget, newY);
        widget.setOriginalY(newY)
                .setOriginalX(newX)
                .setOriginalHeight(newHeight)
                .setOriginalWidth(newWidth)
                .revalidate();
    }

    private void shiftWidgetY(Widget widget, int diff, Direction dirToShift) {
        int originalSiblingY = widget.getRelativeY();
        int newSiblingY = getNewShiftedY(widget, dirToShift, diff); // relative position
        if (newSiblingY != originalSiblingY) {
            setWidgetRelativeYPos(widget, newSiblingY);
        }
    }

}