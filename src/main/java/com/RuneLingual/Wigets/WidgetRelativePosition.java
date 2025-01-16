package com.RuneLingual.Wigets;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class WidgetRelativePosition {
    private Widget parentWidget;

    public WidgetRelativePosition(Widget parentWidget) {
        this.parentWidget = parentWidget;
        setWidgetNode();
    }

    protected enum RelativePosition {
        RIGHT,
        LEFT,
        ABOVE,
        BELOW,
        INSIDE,
        OUTSIDE
    }

    @Setter @Getter
    private static class Node<T> {
        private T widget;
        private List<Node<T>> widgetOnRight;
        private List<Node<T>> widgetOnLeft;
        private List<Node<T>> widgetAbove;
        private List<Node<T>> widgetBelow;
        private List<Node<T>> widgetInside;
        private List<Node<T>> widgetOutside;
        private Pair<Integer,Integer> topLeft;
        private Pair<Integer,Integer> bottomRight;

        public Node(T widget) {
            this.widget = widget;
        }
        public  void addChild(Node<T> child, RelativePosition relativePosition) {
            switch (relativePosition) {
                case RIGHT:
                    widgetOnRight.add(child);
                    break;
                case LEFT:
                    widgetOnLeft.add(child);
                    break;
                case ABOVE:
                    widgetAbove.add(child);
                    break;
                case BELOW:
                    widgetBelow.add(child);
                    break;
                case INSIDE:
                    widgetInside.add(child);
                    break;
                case OUTSIDE:
                    widgetOutside.add(child);
                    break;
            }
        }
        public void removeChild(Node<T> child, RelativePosition relativePosition) {
            switch (relativePosition) {
                case RIGHT:
                    widgetOnRight.remove(child);
                    break;
                case LEFT:
                    widgetOnLeft.remove(child);
                    break;
                case ABOVE:
                    widgetAbove.remove(child);
                    break;
                case BELOW:
                    widgetBelow.remove(child);
                    break;
                case INSIDE:
                    widgetInside.remove(child);
                    break;
                case OUTSIDE:
                    widgetOutside.remove(child);
                    break;
            }
        }
        public Pair<Integer,Integer> getTopRight(){
            return Pair.of(topLeft.getLeft(), bottomRight.getRight());
        }
        public Pair<Integer,Integer> getBottomLeft(){
            return Pair.of(bottomRight.getLeft(), topLeft.getRight());
        }
    }

    private void setWidgetNode() {
        Node<Widget> widgetNode = new Node<>(parentWidget);
        Widget[] dynamicChildren = parentWidget.getDynamicChildren();
        Widget[] staticChildren = parentWidget.getStaticChildren();
        Widget[] nestedChildren = parentWidget.getNestedChildren();
    }



}
