package com.RuneLingual.Wigets;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;

import javax.inject.Inject;
import java.awt.*;
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

    private enum notFixedDir {
        HORIZONTAL,
        VERTICAL,
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

        if (!widget2FitText.fixedLeft || !widget2FitText.fixedRight) {
            fitWidgetInDirection(widget2FitText, newText, notFixedDir.HORIZONTAL);
        }
        if (!widget2FitText.fixedTop || !widget2FitText.fixedBottom) {
            fitWidgetInDirection(widget2FitText, newText, notFixedDir.VERTICAL);
        }
    }

    // change width/height of widget
    // if the widget doesn't have adjacent sibling widgets, make parent + sibling widgets larger/smaller by the same amount
    // else (if the widget has adjacent sibling widgets):
    // reposition depending on what side is fixed
    // shift sibling widgets in that direction
    // change parent width/height if needed
    // revalidate widgets
    private void fitWidgetInDirection(Widget2FitText widget2FitText, String newText, notFixedDir dirNotFixed) {
        Widget widget = widget2FitText.getWidget();
        int originalSize = (dirNotFixed == notFixedDir.HORIZONTAL) ? widget.getWidth() : widget.getHeight();
        int newSize = (dirNotFixed == notFixedDir.HORIZONTAL) ? getWidthToFit(widget2FitText, newText) :getHeightToFit(widget2FitText, newText);
        int originalPos = (dirNotFixed == notFixedDir.HORIZONTAL) ? widget.getRelativeX() : widget.getRelativeY();
        int sizeDiff = newSize - originalSize;
        if (originalSize == newSize) {
            return;
        }
        Widget parentWidget = widget.getParent();

        // if the widget doesn't have adjacent sibling widgets, make parent + sibling widgets larger/smaller by the same amount
        if (!widget2FitText.hasAdjacentSiblingWidget && widget.getParent() != null) {
            int originalParentPosition = (dirNotFixed == notFixedDir.HORIZONTAL) ? parentWidget.getRelativeX() : parentWidget.getRelativeY();
            int originalParentSize = (dirNotFixed == notFixedDir.HORIZONTAL) ? parentWidget.getWidth() : parentWidget.getHeight();
            sizeDiff = newSize - originalSize;

            // set new size and position for parent and widget
            if (dirNotFixed == notFixedDir.HORIZONTAL) {
                setWidgetWidthAbsolute(widget, newSize);
                int newParentSize = parentWidget.getWidth() + sizeDiff;
                setWidgetWidthAbsolute(parentWidget, newParentSize);
            } else {
                setWidgetHeightAbsolute(widget, newSize);
                int newParentSize = parentWidget.getHeight() + sizeDiff;
                setWidgetHeightAbsolute(parentWidget, newParentSize);
            }

            // reposition parent and the target widget
            if (dirNotFixed == notFixedDir.HORIZONTAL) {
                if (widget2FitText.fixedLeft && widget2FitText.fixedRight) {
                    int newParentPos = originalParentPosition - sizeDiff / 2;
                    if (newParentPos < 0) {
                        newParentPos = 0;
                    }
                    setWidgetRelativeXPos(parentWidget, newParentPos);
                    setWidgetRelativeXPos(widget, originalPos);
                } else if (widget2FitText.fixedLeft) {
                    setWidgetRelativeXPos(parentWidget, originalParentPosition);
                    setWidgetRelativeXPos(widget, originalPos);
                } else { // if (widget2FitText.fixedRight)
                    int newParentPos = originalParentPosition - sizeDiff;
                    if (newParentPos < 0) {
                        newParentPos = 0;
                    }
                    setWidgetRelativeXPos(parentWidget, newParentPos);
                    setWidgetRelativeXPos(widget, originalPos);
                }
            } else {
                if (widget2FitText.fixedTop && widget2FitText.fixedBottom) {
                    setWidgetRelativeYPos(parentWidget, originalParentPosition - sizeDiff / 2);
                    setWidgetRelativeYPos(widget, originalPos);
                } else if (widget2FitText.fixedTop) {
                    setWidgetRelativeYPos(parentWidget, originalParentPosition);
                    setWidgetRelativeYPos(widget, originalPos);
                } else { // if (widget2FitText.fixedBottom)
                    setWidgetRelativeYPos(parentWidget, originalParentPosition - sizeDiff);
                    setWidgetRelativeYPos(widget, originalPos );
                }
            }

            // set new size for sibling widgets
            List<Widget> childWidgets = getAllChildWidget(parentWidget);
            for (Widget sibling : childWidgets) {
                if (sibling != widget && sibling.getType() == 3) { // 3 seems to be the type for background widgets
                    int originalSiblingPosition = (dirNotFixed == notFixedDir.HORIZONTAL) ? sibling.getRelativeX() : sibling.getRelativeY();
                    if (dirNotFixed == notFixedDir.HORIZONTAL) {
                        // set new width for sibling
                        int originalSiblingWidth = sibling.getWidth();
                        int newSiblingWidth = originalSiblingWidth + sizeDiff;
                        setWidgetWidthAbsolute(sibling, newSiblingWidth);

                        // set new position for sibling
                        if (widget2FitText.fixedLeft && widget2FitText.fixedRight) {
                            setWidgetRelativeXPos(sibling, originalSiblingPosition);
                        } else if (widget2FitText.fixedLeft) {
                            setWidgetRelativeXPos(sibling, originalSiblingPosition);
                        } else { // if (widget2FitText.fixedRight)
                            setWidgetRelativeXPos(sibling, originalSiblingPosition);
                        }
                    } else {
                        // set new height for sibling
                        int originalSiblingHeight = sibling.getHeight();
                        int newSiblingHeight = originalSiblingHeight + sizeDiff;
                        setWidgetHeightAbsolute(sibling, newSiblingHeight);

                        // set new position for sibling
                        if (widget2FitText.fixedTop && widget2FitText.fixedBottom) {
                            setWidgetRelativeYPos(sibling, originalSiblingPosition);
                        } else if (widget2FitText.fixedTop) {
                            setWidgetRelativeYPos(sibling, originalSiblingPosition);
                        } else { // if (widget2FitText.fixedBottom)
                            setWidgetRelativeYPos(sibling, originalSiblingPosition);
                        }
                    }
                }
            }





        } else {
            // reposition depending on what side is fixed, and resize
            Direction dirToShift = getVerticalDirToShift(widget2FitText);

            // shift sibling widgets in that direction
            List<Widget> childWidgets = getAllChildWidget(widget.getParent());
            for (Widget sibling : childWidgets) {
                if (sibling.equals(widget)) {
                    continue;
                }
                Direction dir = getDirTowards(widget, sibling);
                if (dir == dirToShift || dir == Direction.INSIDE) {
                    shiftWidgetY(sibling, sizeDiff, dirToShift);
                } else if (dir == Direction.OUTSIDE) {
                    expandWidget(sibling, dirToShift, sizeDiff);
                }
            }

            // set new height for widget
            if (dirNotFixed == notFixedDir.HORIZONTAL) {
                setWidgetWidthAbsolute(widget, newSize);
                if (dirToShift == Direction.LEFT) { // if shifting upwards, shift the widget itself by the height difference
                    int newX = getNewShiftedX(widget, dirToShift, sizeDiff); // relative position
                    setWidgetRelativeXPos(widget, newX);
                }
            } else {
                setWidgetHeightAbsolute(widget, newSize);
                if (dirToShift == Direction.ABOVE) { // if shifting upwards, shift the widget itself by the height difference
                    int newY = getNewShiftedY(widget, dirToShift, sizeDiff); // relative position
                    setWidgetRelativeYPos(widget, newY);
                }
            }

            // change parent width/height if needed
            int siblingCoverage = getSiblingsCoverage(childWidgets, dirNotFixed);
            int parentCoverage = (dirNotFixed == notFixedDir.HORIZONTAL) ? parentWidget.getWidth() : parentWidget.getHeight();
            if (parentCoverage < siblingCoverage) {
                if (dirNotFixed == notFixedDir.HORIZONTAL) {
                    setWidgetWidthAbsolute(parentWidget, siblingCoverage);
                } else {
                    setWidgetHeightAbsolute(parentWidget, siblingCoverage);
                }
            }

        }
    }


    private int getHeightToFit(Widget2FitText widget2FitText, String newText) {
        int lineHeight = plugin.getConfig().getSelectedLanguage().getCharHeight();
        int numLines = newText.split("<br>").length;
        return lineHeight * numLines + widget2FitText.topPadding + widget2FitText.bottomPadding;
    }

    private int getWidthToFit(Widget2FitText widget2FitText, String newText) {
        // get the longest line, multiply by the width of selected language's character
        int longestLine = 0;

        String[] lines = newText.split("<br>");
        // count the number of characters, but if its char images, count the number of <img> tags, else
        if (!plugin.getConfig().getSelectedLanguage().needsCharImages()) {
            for (String line : lines) {
                if (line.length() > longestLine) {
                    longestLine = line.length();
                }
            }
            int latinCharWidth = LangCodeSelectableList.getLatinCharWidth(widget2FitText.getWidget(), plugin.getConfig().getSelectedLanguage());
            longestLine *= latinCharWidth;
        } else {
            for (String line : lines) {
                int imgCount = line.split("<img=").length - 1;
                int nonImgCount = line.replaceAll("<.*>", "").length();
                int lineLength = imgCount * plugin.getConfig().getSelectedLanguage().getCharWidth() +
                        nonImgCount * LangCodeSelectableList.ENGLISH.getCharWidth();
                if (lineLength > longestLine) {
                    longestLine = lineLength;
                }
            }
        }
        return longestLine + widget2FitText.leftPadding + widget2FitText.rightPadding;
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
        if (!widget2FitText.fixedTop && !widget2FitText.fixedBottom) {
            // can be difficult to determine which direction to shift, so hard coding

            // for spellbook tab hover text
            if (widget2FitText.getWidgetId() == plugin.getIds().getSpellbookTabHoverTextId()){
                // if the bottom edge of widget is at the bottom of parent, shift above
                Widget widget = widget2FitText.getWidget();
                int parentHeight = widget.getParent().getHeight();
                int widgetBottomY = widget.getRelativeY() + widget.getHeight();
                if (widgetBottomY > parentHeight /2) {
                    return Direction.ABOVE;
                } else {
                    return Direction.BELOW;// not tested
                }
            }
            return Direction.OUTSIDE; // shift both ways
        } else if (!widget2FitText.fixedBottom) {
            return Direction.BELOW;
        } else if (!widget2FitText.fixedTop) {
            return Direction.ABOVE;
        } else if (!widget2FitText.fixedLeft) {
            return Direction.LEFT;
        } else if (!widget2FitText.fixedRight) {
            return Direction.RIGHT;
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
    private int getNewShiftedX(Widget widget, Direction dirToShift, int widthDiff) {
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
        boolean widgetsOverlapHor = widgetsOverlapHor(baseWidget, targetWidget);
        boolean widgetsOverlapVer = widgetsOverlapVer(baseWidget, targetWidget);

        int overlapErrorPixels = 4; // widgets inside can be bigger by this amount even for bottom and right edges, even if they appear to be inside

        if (targetBottom - overlapErrorPixels < baseTop && widgetsOverlapHor) {
            return Direction.ABOVE;
        } else if (targetTop > baseBottom - overlapErrorPixels && widgetsOverlapHor) {
            return Direction.BELOW;
        } else if (targetRight - overlapErrorPixels < baseLeft && widgetsOverlapVer) {
            return Direction.LEFT;
        } else if (targetLeft > baseRight - overlapErrorPixels && widgetsOverlapVer) {
            return Direction.RIGHT;
        } else if (widgetsOverlapHor && widgetsOverlapVer) {
            if (isAnyEdgeInside(baseWidget, targetWidget, overlapErrorPixels)) {
                return Direction.INSIDE;
            } else if (targetTop <= baseTop && targetBottom >= baseBottom && targetLeft <= baseLeft && targetRight >= baseRight) {
                return Direction.OUTSIDE;
            }
        }
        return Direction.DIAGONAL;

    }

    private boolean widgetsOverlapHor(Widget w1, Widget w2) {
        int w1Left = w1.getRelativeX();
        int w1Right = w1.getRelativeX() + w1.getWidth();
        int w2Left = w2.getRelativeX();
        int w2Right = w2.getRelativeX() + w2.getWidth();
        return (w1Left > w2Left && w1Left < w2Right) || (w1Right > w2Left && w1Right < w2Right) || (w1Left < w2Left && w1Right > w2Right)
            || (w2Left > w1Left && w2Left < w1Right) || (w2Right > w1Left && w2Right < w1Right) || (w2Left < w1Left && w2Right > w1Right);
    }

    private boolean widgetsOverlapVer(Widget w1, Widget w2) {
        int w1Top = w1.getRelativeY();
        int w1Bottom = w1.getRelativeY() + w1.getHeight();
        int w2Top = w2.getRelativeY();
        int w2Bottom = w2.getRelativeY() + w2.getHeight();
        return (w1Top > w2Top && w1Top < w2Bottom) || (w1Bottom > w2Top && w1Bottom < w2Bottom) || (w1Top < w2Top && w1Bottom > w2Bottom)
            || (w2Top > w1Top && w2Top < w1Bottom) || (w2Bottom > w1Top && w2Bottom < w1Bottom) || (w2Top < w1Top && w2Bottom > w1Bottom);
    }

    private int getSiblingsCoverage(List<Widget> siblings, notFixedDir dirNotFixed) {
        int min = 999999;
        int max = 0;
        for (Widget sibling : siblings) {
            if (dirNotFixed == notFixedDir.HORIZONTAL) {
                int siblingLeft = sibling.getRelativeX();
                int siblingRight = sibling.getRelativeX() + sibling.getWidth();
                if (siblingLeft < min) {
                    min = siblingLeft;
                }
                if (siblingRight > max) {
                    max = siblingRight;
                }
            } else {
                int siblingTop = sibling.getRelativeY();
                int siblingBottom = sibling.getRelativeY() + sibling.getHeight();
                if (siblingTop < min) {
                    min = siblingTop;
                }
                if (siblingBottom > max) {
                    max = siblingBottom;
                }
            }
            int siblingTop = sibling.getRelativeY();
            int siblingBottom = sibling.getRelativeY() + sibling.getHeight();
            if (siblingTop < min) {
                min = siblingTop;
            }
            if (siblingBottom > max) {
                max = siblingBottom;
            }
        }
        return max - min;
    }

    private void expandWidget(Widget widget, Direction dir, int diff) {
//        if (widget.getType() == 5){ // type 5 seems to be icon sprites
//            return;
//        } // todo: remove comment after resolving dirtowards issue
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

    private boolean isAnyEdgeInside(Widget baseWgt, Widget targetWgt, int overlapErrorPixels) {
        return getNumCornerInside(baseWgt, targetWgt, overlapErrorPixels) > 0;
    }

    private int getNumCornerInside(Widget baseWgt, Widget targetWgt, int overlapErrorPixels) {
        //** for target bottom and right edges, consider overlapErrorPixels
        int baseTop = baseWgt.getRelativeY();
        int baseBottom = baseWgt.getRelativeY() + baseWgt.getHeight();
        int baseLeft = baseWgt.getRelativeX();
        int baseRight = baseWgt.getRelativeX() + baseWgt.getWidth();
        int targetTop = targetWgt.getRelativeY();
        int targetBottom = targetWgt.getRelativeY() + targetWgt.getHeight() - overlapErrorPixels;
        int targetLeft = targetWgt.getRelativeX();
        int targetRight = targetWgt.getRelativeX() + targetWgt.getWidth() - overlapErrorPixels;
        int count = 0;

        //** top and left edges can be on top of base widget's (so use <= or >= instead of < or >)
        // target's top left corner is inside base
        if (targetTop >= baseTop && targetTop < baseBottom && targetLeft >= baseLeft && targetLeft < baseRight) {
            count++;
        }
        // target's top right corner is inside base
        if (targetTop >= baseTop && targetTop < baseBottom && targetRight > baseLeft && targetRight < baseRight) {
            count++;
        }
        // target's bottom left corner is inside base
        if (targetBottom > baseTop && targetBottom < baseBottom && targetLeft >= baseLeft && targetLeft < baseRight) {
            count++;
        }
        // target's bottom right corner is inside base
        if (targetBottom > baseTop && targetBottom < baseBottom && targetRight > baseLeft && targetRight < baseRight) {
            count++;
        }
        return count;
    }

    private void shiftWidgetY(Widget widget, int diff, Direction dirToShift) {
        int originalSiblingY = widget.getRelativeY();
        int newSiblingY = getNewShiftedY(widget, dirToShift, diff); // relative position
        if (newSiblingY != originalSiblingY) {
            setWidgetRelativeYPos(widget, newSiblingY);
        }
    }

}