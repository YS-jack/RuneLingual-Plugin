package com.RuneLingual.MouseOverlays;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.MenuCapture;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import java.awt.*;
import java.awt.Point;
import java.util.*;
import java.util.List;
import javax.inject.Inject;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import static java.lang.Thread.sleep;

@Slf4j
public class MenuEntryHighlightOverlay extends Overlay {
    private final Client client;
    private final RuneLingualConfig config;
    private final PanelComponent panelComponent = new PanelComponent();
    @Inject
    private RuneLingualPlugin plugin;
    @Setter
    private static List<String> attemptedTranslation = Collections.synchronizedList(new ArrayList<>());

    @Inject
    public MenuEntryHighlightOverlay(Client client, RuneLingualConfig config) {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    // draw transparent square around the menu entry that is currently hovered over
    // only draw if the menu is open and the selected language needs character images
    @Override
    public Dimension render(Graphics2D graphics) {
        if (!client.isMenuOpen() || !plugin.getTargetLanguage().needsCharImages() || plugin.getTargetLanguage() == LangCodeSelectableList.ENGLISH) {
            return null;
        }

        // get the x, y, width of the menu
        int x = client.getMenu().getMenuX();
        int y = client.getMenu().getMenuY();
        int width = client.getMenu().getMenuWidth();
        log.info("Menu X: {}, Y: {}, Width: {}", x, y, width);
        panelComponent.getChildren().clear();
        panelComponent.setPreferredLocation(new Point(x, y));
        panelComponent.setPreferredSize(new Dimension(width,100));
        Color bgColor = new Color(127, 82, 33);
        Rectangle rectangle = new Rectangle(
                x,
                y,
                width,
                100
        );
        BackgroundComponent backgroundComponent = new BackgroundComponent(
                bgColor,
                rectangle,
                true
        );
        backgroundComponent.render(graphics);
        return null;
    }
}