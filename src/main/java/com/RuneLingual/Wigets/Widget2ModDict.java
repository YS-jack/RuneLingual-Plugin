package com.RuneLingual.Wigets;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class Widget2ModDict {
    private List<Widget2Mod> widgets2Mod = new ArrayList<>();
    @Inject
    private RuneLingualPlugin plugin;

    @Inject
    public Widget2ModDict(RuneLingualPlugin plugin) {
        this.plugin = plugin;
    }

    @Getter @Setter
    public static class Widget2Mod {
        private Widget widget;
        private final int errorPixels;
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

        public Widget2Mod(int widgetId, int errorPixels, boolean hasAdjacentSiblingWidget, boolean fixedTop, boolean fixedBottom, boolean fixedLeft, boolean fixedRight, int topPadding, int bottomPadding, int leftPadding, int rightPadding){
            this.widgetId = widgetId;
            this.errorPixels = errorPixels;
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
        ALMOST_SAME // for widgets that are almost the same size and position, +/- the error pixels

    }

    private enum notFixedDir {
        HORIZONTAL,
        VERTICAL,
    }

    public void add(int widgetId, int errorPixels, boolean hasSiblingWidget , boolean fixedTop, boolean fixedBottom, boolean fixedLeft, boolean fixedRight, int topPadding, int bottomPadding, int leftPadding, int rightPadding) {
        Widget2Mod widget2Mod = new Widget2Mod(widgetId, errorPixels, hasSiblingWidget, fixedTop, fixedBottom, fixedLeft, fixedRight, topPadding, bottomPadding, leftPadding, rightPadding);
        widgets2Mod.add(widget2Mod);
    }
    private boolean contains(int widgetId) {
        for (Widget2Mod widget2Mod : widgets2Mod) {
            if (widget2Mod.getWidgetId() == widgetId) {
                return true;
            }
        }
        return false;
    }

    public Widget2Mod getWidgets2Mod(int widgetId) {
        for (Widget2Mod widget2Mod : widgets2Mod) {
            if (widget2Mod.getWidgetId() == widgetId) {
                return widget2Mod;
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
        Widget2Mod widget2Mod = getWidgets2Mod(widgetId);
        if (widget2Mod == null) {
            return;
        }
        widget2Mod.setWidget(widget);

        if (!widget2Mod.fixedLeft || !widget2Mod.fixedRight) {
            fitWidgetInDirection(widget2Mod, newText, notFixedDir.HORIZONTAL);
        }
        if (!widget2Mod.fixedTop || !widget2Mod.fixedBottom) {
            fitWidgetInDirection(widget2Mod, newText, notFixedDir.VERTICAL);
        }
    }

    // change width/height of widget
    // if the widget doesn't have adjacent sibling widgets, make parent + sibling widgets larger/smaller by the same amount
    // else (if the widget has adjacent sibling widgets):
    // reposition depending on what side is fixed
    // shift sibling widgets in that direction
    // change parent width/height if needed
    // revalidate widgets
    private void fitWidgetInDirection(Widget2Mod widget2Mod, String newText, notFixedDir dirNotFixed) {
        Widget widget = widget2Mod.getWidget();
        int originalSize = (dirNotFixed == notFixedDir.HORIZONTAL) ? widget.getWidth() : widget.getHeight();
        int newSize = (dirNotFixed == notFixedDir.HORIZONTAL) ? getWidthToFit(widget2Mod, widget2Mod.widget, newText) :getHeightToFit(widget2Mod, newText);
        int originalPos = (dirNotFixed == notFixedDir.HORIZONTAL) ? widget.getRelativeX() : widget.getRelativeY();
        int sizeDiff = newSize - originalSize;
//        if (originalSize == newSize) {
//            return;
//        }
        Widget parentWidget = widget.getParent();

        // if the widget doesn't have adjacent sibling widgets, make parent + sibling widgets larger/smaller by the same amount
        if (!widget2Mod.hasAdjacentSiblingWidget && widget.getParent() != null) {
            int originalParentPosition = (dirNotFixed == notFixedDir.HORIZONTAL) ? parentWidget.getRelativeX() : parentWidget.getRelativeY();
            int originalParentSize = (dirNotFixed == notFixedDir.HORIZONTAL) ? parentWidget.getWidth() : parentWidget.getHeight();
            int newParentSize = originalParentSize + sizeDiff;
            // set new size and position for parent and widget
            if (dirNotFixed == notFixedDir.HORIZONTAL) {
                setWidgetWidthAbsolute(widget, newSize);

                setWidgetWidthAbsolute(parentWidget, newParentSize);
            } else {
                setWidgetHeightAbsolute(widget, newSize);
                setWidgetHeightAbsolute(parentWidget, newParentSize);
            }

            // reposition parent and the target widget
            if (dirNotFixed == notFixedDir.HORIZONTAL) {
                if (widget2Mod.fixedLeft && widget2Mod.fixedRight) {
                    int newParentPos = originalParentPosition - sizeDiff / 2;
                    if (newParentPos < 0) {
                        newParentPos = 0;
                    }
                    setWidgetRelativeXPos(parentWidget, newParentPos);
                    setWidgetRelativeXPos(widget, originalPos);
                } else if (widget2Mod.fixedLeft) {
                    setWidgetRelativeXPos(parentWidget, originalParentPosition);
                    setWidgetRelativeXPos(widget, originalPos);
                } else { // if (widget2Mod.fixedRight)
                    int newParentPos = originalParentPosition - sizeDiff;
                    if (newParentPos < 0) {
                        newParentPos = 0;
                    }
                    setWidgetRelativeXPos(parentWidget, newParentPos);
                    setWidgetRelativeXPos(widget, originalPos);
                }
            } else {
                if (widget2Mod.fixedTop && widget2Mod.fixedBottom) {
                    setWidgetRelativeYPos(parentWidget, originalParentPosition - sizeDiff / 2);
                    setWidgetRelativeYPos(widget, originalPos);
                } else if (widget2Mod.fixedTop) {
                    setWidgetRelativeYPos(parentWidget, originalParentPosition);
                    setWidgetRelativeYPos(widget, originalPos);
                } else { // if (widget2Mod.fixedBottom)
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
                        if (widget2Mod.fixedLeft && widget2Mod.fixedRight) {
                            setWidgetRelativeXPos(sibling, originalSiblingPosition);
                        } else if (widget2Mod.fixedLeft) {
                            setWidgetRelativeXPos(sibling, originalSiblingPosition);
                        } else { // if (widget2Mod.fixedRight)
                            setWidgetRelativeXPos(sibling, originalSiblingPosition);
                        }
                    } else {
                        // set new height for sibling
                        int originalSiblingHeight = sibling.getHeight();
                        int newSiblingHeight = originalSiblingHeight + sizeDiff;
                        setWidgetHeightAbsolute(sibling, newSiblingHeight);

                        // set new position for sibling
                        if (widget2Mod.fixedTop && widget2Mod.fixedBottom) {
                            setWidgetRelativeYPos(sibling, originalSiblingPosition);
                        } else if (widget2Mod.fixedTop) {
                            setWidgetRelativeYPos(sibling, originalSiblingPosition);
                        } else { // if (widget2Mod.fixedBottom)
                            setWidgetRelativeYPos(sibling, originalSiblingPosition);
                        }
                    }
                }
            }

        } else {
            List<Widget> childWidgets = getAllChildWidget(widget.getParent());
            // update size in cases like the xp hover text in skills tab, where the widget is in the same row as the sibling
            // in this case even though the sibling widget is ALMOST_SAME, the sibling widget text's width + widget's text width should be the new size,
            // instead of expanding the sibling widget to fit the widget's size
            if (dirNotFixed == notFixedDir.HORIZONTAL) {
                newSize += getAdjacentTextSiblingSize(childWidgets, widget, dirNotFixed, widget2Mod);
                sizeDiff = newSize - originalSize;
            }
            // reposition depending on what side is fixed, and resize
            Direction dirToShift = getDirToShift(widget2Mod, dirNotFixed);
            // shift sibling widgets in that direction
            for (Widget sibling : childWidgets) {
                if (sibling.equals(widget)) {
                    continue;
                }
                Direction dir = getDirTowards(widget, sibling, widget2Mod);
                if (dir == dirToShift) {
                    shiftWidget(sibling, sizeDiff, dirToShift);
                } else if (dir == Direction.OUTSIDE || dir == Direction.INSIDE || dir == Direction.ALMOST_SAME) {
                    moveAndExpandWidget(sibling, dirToShift, sizeDiff);
                }
            }

//             resize and reposition the target widget
            if (dirNotFixed == notFixedDir.HORIZONTAL) {
                originalPos = widget.getRelativeX(); // delete later
                setWidgetWidthAbsolute(widget, newSize);
                if (dirToShift == Direction.LEFT) { // if shifting left, shift the widget itself by the difference
                    originalPos = getNewShiftedPos(originalPos, dirToShift, sizeDiff); // relative position
                }
                setWidgetRelativeXPos(widget, originalPos);
            } else {
                setWidgetHeightAbsolute(widget, newSize);
                if (dirToShift == Direction.ABOVE) { // if shifting upwards, shift the widget itself by the difference
                    originalPos = getNewShiftedPos(originalPos, dirToShift, sizeDiff); // relative position
                }
                setWidgetRelativeYPos(widget, originalPos);
            }


            // change parent width/height to fit all the widget and sibling widgets
            int originalParentPosition = (dirNotFixed == notFixedDir.HORIZONTAL) ? parentWidget.getRelativeX() : parentWidget.getRelativeY();
            int siblingCoverage = getSiblingsCoverage(childWidgets, dirNotFixed);
            int parentCoverage = (dirNotFixed == notFixedDir.HORIZONTAL) ? parentWidget.getWidth() : parentWidget.getHeight();
            if (parentCoverage < siblingCoverage) {
                if (dirNotFixed == notFixedDir.HORIZONTAL) {
                    if (dirToShift == Direction.LEFT) {
                        int newParentX = originalParentPosition - sizeDiff;
                        setWidgetRelativeXPos(parentWidget, newParentX);
                    }
                    setWidgetWidthAbsolute(parentWidget, siblingCoverage);

                } else {
                    if (dirToShift == Direction.ABOVE) {
                        int newParentY = originalParentPosition - sizeDiff;
                        setWidgetRelativeYPos(parentWidget, newParentY);
                    }
                    setWidgetHeightAbsolute(parentWidget, siblingCoverage);
                }
            }

            // if any of the parent's corners are outside of the parent's parent, resize and reposition parent and child widgets

            // also, if the parent width/height (children coverage) is larger than parent's parent,
            // resize and reposition parent and child widgets
            if (dirNotFixed == notFixedDir.HORIZONTAL) {
                int parentWidth = parentWidget.getWidth();
                int grandPWidth = parentWidget.getParent().getWidth();
                boolean isLeftOutside = parentWidget.getRelativeX() < 0;
                boolean isRightOutside = parentWidget.getRelativeX() + parentWidth > grandPWidth;

                if (isLeftOutside && isRightOutside) {
                    // parent size is bigger than parent's parent. need to make parent and sibling widgets smaller to fit. not checked
                    setWidgetRelativeXPos(parentWidget, 0);
                    setWidgetWidthAbsolute(parentWidget, grandPWidth);
                    setAllChildFitPar(parentWidget, notFixedDir.HORIZONTAL, widget2Mod);
                }
                if (isLeftOutside) {
                    setWidgetRelativeXPos(parentWidget, 0);
                    if (parentWidth > grandPWidth) {
                        // if the parent widget is larger than the parent, make it the same size as the parent
                        setWidgetWidthAbsolute(parentWidget, grandPWidth);
                    }
                    setAllChildFitPar(parentWidget, notFixedDir.HORIZONTAL, widget2Mod);
                }
                if (isRightOutside) {
                    if (parentWidth <= grandPWidth) {
                        setWidgetRelativeXPos(parentWidget, grandPWidth - parentWidth);
                        setWidgetWidthAbsolute(parentWidget, grandPWidth);
                    } else {
                        setWidgetWidthAbsolute(parentWidget, grandPWidth);
                        setWidgetRelativeXPos(parentWidget, 0);
                    }
                    setAllChildFitPar(parentWidget, notFixedDir.HORIZONTAL, widget2Mod);
                }
            } else {
                boolean isTopOutside = parentWidget.getRelativeY() < 0;
                boolean isBottomOutside = parentWidget.getRelativeY() + parentWidget.getHeight() > parentWidget.getParent().getHeight();
                if (isTopOutside && isBottomOutside) { // need to make parent and sibling widgets smaller to fit, not checked
                    setWidgetRelativeYPos(parentWidget, 0);
                    setWidgetHeightAbsolute(parentWidget, parentWidget.getParent().getHeight());
                    setAllChildFitPar(parentWidget, notFixedDir.VERTICAL, widget2Mod);
                }
                if (isTopOutside) {
                    setWidgetRelativeYPos(parentWidget, 0);
                    if (parentWidget.getHeight() > parentWidget.getParent().getHeight()) {
                        // if the parent widget is larger than the parent, make it the same size as the parent
                        setWidgetHeightAbsolute(parentWidget, parentWidget.getParent().getHeight());
                    }
                    setAllChildFitPar(parentWidget, notFixedDir.VERTICAL, widget2Mod);
                }
                if (isBottomOutside) {
                    setWidgetRelativeYPos(parentWidget, parentWidget.getParent().getHeight() - parentWidget.getHeight());
                    if (parentWidget.getHeight() > parentWidget.getParent().getHeight()) {
                        // if the parent widget is larger than the parent, make it the same size as the parent
                        setWidgetHeightAbsolute(parentWidget, parentWidget.getParent().getHeight());
                    }
                    setAllChildFitPar(parentWidget, notFixedDir.VERTICAL, widget2Mod);
                }
            }
        }
    }

    private int getAdjacentTextSiblingSize(List<Widget> childWidgets, Widget widget, notFixedDir dirNotFixed, Widget2Mod widget2Mod) {
        int maxSize = 0;
        for (Widget sibling : childWidgets) {
            if (sibling.equals(widget)) {
                continue;
            }
            String siblingText = sibling.getText();
            if (siblingText == null || siblingText.isEmpty()) {
                continue;
            }
            int newSize = 0;
            Direction dir = getDirTowards(widget, sibling, widget2Mod);
            if (dir == Direction.ALMOST_SAME// if the widgets share similar boundary
                    && sibling.getType() == 4 && sibling.getText() != null && !sibling.getText().isEmpty() // and the sibling is a text widget
                    && Math.pow(sibling.getXTextAlignment() - widget.getXTextAlignment(), 2) == 4) { // and the sibling is aligned to the left or right of the widget
                // they are likely to be in the same row, like the xp hover text in skills tab
                if (dirNotFixed == notFixedDir.HORIZONTAL) {
                    newSize = getWidthToFit(widget2Mod, widget2Mod.widget, siblingText);
                } else {
                    newSize = getHeightToFit(widget2Mod, siblingText);
                }
            }
            if (newSize > maxSize) {
                maxSize = newSize;
            }
        }
        return maxSize;
    }

    private int getHeightToFit(Widget2Mod widget2Mod, String newText) {
        int lineHeight = plugin.getConfig().getSelectedLanguage().getCharHeight();
        int numLines = newText.split("<br>").length;
        return lineHeight * numLines + widget2Mod.topPadding + widget2Mod.bottomPadding;
    }

    // give widget as argument to get width for
    // give widget2Mod as argument to get padding
    private int getWidthToFit(Widget2Mod widget2Mod, Widget widget, String newText) {
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
            int latinCharWidth = LangCodeSelectableList.getLatinCharWidth(widget, plugin.getConfig().getSelectedLanguage());
            longestLine *= latinCharWidth;
        } else {
            for (String line : lines) {
                int imgCount = line.split("<img=").length - 1;
                int nonImgCount = line.replaceAll("<.*>", "").length();
                int lineLength = imgCount * plugin.getConfig().getSelectedLanguage().getCharWidth() +
                        nonImgCount * LangCodeSelectableList.getLatinCharWidth(widget, LangCodeSelectableList.ENGLISH);
                if (lineLength > longestLine) {
                    longestLine = lineLength;
                }
            }
        }
        return longestLine + widget2Mod.leftPadding + widget2Mod.rightPadding;
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


    private Direction getDirToShift(Widget2Mod widget2Mod, notFixedDir dirNotFixed) {
        // can be difficult to determine which direction to shift, so hard coding

        // for spellbook tab hover text
        if (widget2Mod.getWidgetId() == plugin.getIds().getSpellbookTabHoverTextId()){
            // if the bottom edge of widget is at the bottom of parent, shift above
            Widget widget = widget2Mod.getWidget();
            int parentHeight = widget.getParent().getHeight();
            int widgetBottomY = widget.getRelativeY() + widget.getHeight();
            if (widgetBottomY > parentHeight /2) {
                return Direction.ABOVE;
            } else {
                return Direction.BELOW;// not tested
            }
        }

         if (widget2Mod.getWidgetId() == plugin.getIds().getSkillsTabXpHoverTextId()) {
            if (dirNotFixed == notFixedDir.VERTICAL) {
                int Y = widget2Mod.getWidget().getRelativeY();
                if (Y < 170) {
                    return Direction.BELOW;
                } else {
                    return Direction.ABOVE;
                }
            } else {
                int parentWidth = widget2Mod.getWidget().getParent().getWidth();
                int X = widget2Mod.getWidget().getRelativeX();
                if (X < parentWidth / 4) {
                    return Direction.RIGHT;
                } else {
                    return Direction.LEFT;
                }
            }
        }

        if (dirNotFixed == notFixedDir.VERTICAL) {
            if (!widget2Mod.fixedBottom) {
                return Direction.BELOW;
            } else if (!widget2Mod.fixedTop) {
                return Direction.ABOVE;
            }
        } else {
            if (!widget2Mod.fixedLeft) {
                return Direction.LEFT;
            } else if (!widget2Mod.fixedRight) {
                return Direction.RIGHT;
            }
        }
        return Direction.OUTSIDE;
    }

    // returns relative position
    private int getNewShiftedPos(int originalPos, Direction dirToShift, int heightDiff) {
        int newY = getNewPos(originalPos, dirToShift, heightDiff);
        if (newY == -1) {
            return originalPos - heightDiff / 2;
        }
        return newY;
    }


    private int getNewPos(int originalPos, Direction dirToShift, int diff) {
        int newPos;
        switch (dirToShift) {
            case ABOVE:
                newPos = originalPos - diff;
                break;
            case BELOW:
                newPos = originalPos + diff;
                break;
            case RIGHT:
                newPos = originalPos + diff;
                break;
            case LEFT:
                newPos = originalPos - diff;
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

    private Direction getDirTowards(Widget baseWidget, Widget targetWidget, Widget2Mod widget2Mod) {
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

        int overlapErrorPixels = widget2Mod.getErrorPixels(); // widgets inside can be bigger by this amount even for bottom and right edges, even if they appear to be inside

        if (targetBottom - overlapErrorPixels < baseTop && widgetsOverlapHor) {
            return Direction.ABOVE;
        } else if (targetTop > baseBottom - overlapErrorPixels && widgetsOverlapHor) {
            return Direction.BELOW;
        } else if (targetRight - overlapErrorPixels < baseLeft && widgetsOverlapVer) {
            return Direction.LEFT;
        } else if (targetLeft > baseRight - overlapErrorPixels && widgetsOverlapVer) {
            return Direction.RIGHT;
        } else if (widgetsOverlapHor && widgetsOverlapVer) {
            if (Math.abs(baseTop - targetTop) < overlapErrorPixels
                    && Math.abs(baseBottom - targetBottom) < overlapErrorPixels
                    && Math.abs(baseLeft - targetLeft) < overlapErrorPixels
                    && Math.abs(baseRight - targetRight) < overlapErrorPixels) {
                return Direction.ALMOST_SAME;
            } else
            if (isAnyEdgeInside(baseWidget, targetWidget, overlapErrorPixels)) {
                return Direction.INSIDE;
            } else if (targetTop <= baseTop && targetBottom >= baseBottom && targetLeft <= baseLeft && targetRight >= baseRight) {
                return Direction.OUTSIDE;
            }
        }
        return Direction.DIAGONAL;

    }

    private boolean widgetsOverlapHor(Widget w1, Widget w2) {
        // if the widgets share the same horizontal space, return true
        int w1Left = w1.getRelativeX();
        int w1Right = w1.getRelativeX() + w1.getWidth();
        int w2Left = w2.getRelativeX();
        int w2Right = w2.getRelativeX() + w2.getWidth();
        return (w1Left >= w2Left && w1Left < w2Right) || (w1Right >= w2Left && w1Right < w2Right) || (w1Left < w2Left && w1Right >= w2Right)
            || (w2Left >= w1Left && w2Left < w1Right) || (w2Right >= w1Left && w2Right < w1Right) || (w2Left < w1Left && w2Right >= w1Right);
    }

    private boolean widgetsOverlapVer(Widget w1, Widget w2) {
        // if the widgets share the same vertical space, return true
        int w1Top = w1.getRelativeY();
        int w1Bottom = w1.getRelativeY() + w1.getHeight();
        int w2Top = w2.getRelativeY();
        int w2Bottom = w2.getRelativeY() + w2.getHeight();
        return (w1Top >= w2Top && w1Top < w2Bottom) || (w1Bottom >= w2Top && w1Bottom < w2Bottom) || (w1Top < w2Top && w1Bottom >= w2Bottom)
            || (w2Top >= w1Top && w2Top < w1Bottom) || (w2Bottom >= w1Top && w2Bottom < w1Bottom) || (w2Top < w1Top && w2Bottom >= w1Bottom);
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


    private void moveAndExpandWidget(Widget widget, Direction dir, int diff) {
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

    private void shiftWidget(Widget widget, int diff, Direction dirToShift) {
        if (dirToShift == Direction.ABOVE || dirToShift == Direction.BELOW) {
            int originalY = widget.getRelativeY();
            int newY = getNewShiftedPos(widget.getRelativeY(), dirToShift, diff); // relative position
            if (newY != originalY) {
                setWidgetRelativeYPos(widget, newY);
            }
        } else {
            int originalX = widget.getRelativeX();
            int newX = getNewShiftedPos(widget.getRelativeX(), dirToShift, diff); // relative position
            if (newX != originalX) {
                setWidgetRelativeXPos(widget, newX);
            }
        }
    }

    private void setAllChildFitPar(Widget parentWidget, notFixedDir notFixedDir, Widget2Mod widget2Mod) {
        List<Widget> childWidgets = getAllChildWidget(parentWidget);
        for (Widget sibling : childWidgets) {
            // if the sibling widget is larger than the parent widget, make it the same size as the parent widget
            if (notFixedDir == Widget2ModDict.notFixedDir.HORIZONTAL
                    && sibling.getWidth() > parentWidget.getWidth()) {
                int newSiblingWidth = parentWidget.getWidth();
                if (sibling.getType() == 4 && sibling.getXTextAlignment() == WidgetTextAlignment.RIGHT) {
                    // (subtract right padding for right aligned texts)
                    newSiblingWidth -= widget2Mod.rightPadding;
                }
                setWidgetWidthAbsolute(sibling, newSiblingWidth);
            }
            // if the sibling widget is larger than the parent widget, make it the same size as the parent widget
            if (notFixedDir == Widget2ModDict.notFixedDir.VERTICAL
                    && sibling.getHeight() > parentWidget.getHeight()) {
                int newSiblingHeight = parentWidget.getHeight();
                if (sibling.getType() == 4 && sibling.getYTextAlignment() == WidgetTextAlignment.BOTTOM) {
                    // (subtract bottom padding for bottom aligned texts)
                    newSiblingHeight -= widget2Mod.bottomPadding;
                }
                setWidgetHeightAbsolute(sibling, newSiblingHeight);

            }
        }
    }

}