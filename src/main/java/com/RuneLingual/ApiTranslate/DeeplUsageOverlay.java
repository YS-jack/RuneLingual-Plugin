package com.RuneLingual.ApiTranslate;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.TranslatingServiceSelectableList;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.NumberFormat;


public class DeeplUsageOverlay  extends Overlay {
    private Client client;
    private RuneLingualConfig config;
    private RuneLingualPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public DeeplUsageOverlay(Client client, RuneLingualPlugin plugin, RuneLingualConfig config) {
        setPosition(OverlayPosition.BOTTOM_RIGHT);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }
    /**
    * overlay for the number count by the DeepL API.
     * will show the number of characters translated by the DeepL API and the limit of the API.
     * if the API key is invalid, it will show a warning message.
     */
    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showUsageOverlayConfig())
            return null;

        int enCharSize = LangCodeSelectableList.ENGLISH.getCharWidth();
        int foreignCharSize = config.getSelectedLanguage().getCharWidth();
        boolean deeplKeyValid = plugin.getDeepl().isKeyValid();
        String deeplCount = Long.toString(plugin.getDeepl().getDeeplCount());
        String deeplLimit = Long.toString(plugin.getDeepl().getDeeplLimit());
        TranslatingServiceSelectableList selectedService = config.getApiServiceConfig();

        Color bgColorCount = new Color(80, 148, 144);
        Color bgColorInvalid = new Color(194, 93, 93);
        panelComponent.getChildren().clear();
        int len;
        if (selectedService == TranslatingServiceSelectableList.LibreTranslate) {
            boolean libreAvailable = plugin.getDeepl().canTranslateNow();
            if (libreAvailable) {
                NumberFormat nf = NumberFormat.getIntegerInstance();
                String chars = nf.format(plugin.getDeepl().getLibreTranslateCharCount());
                String reqs = nf.format(plugin.getDeepl().getLibreTranslateRequestCount());
                panelComponent.setBackgroundColor(bgColorCount);
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("LibreTranslate:")
                        .right(chars + " chars")
                        .build());
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Requests:")
                        .right(reqs)
                        .build());
                int line1 = ("LibreTranslate: " + chars + " chars").length() + 2;
                int line2 = ("Requests: " + reqs).length() + 2;
                len = Math.max(line1, line2) * enCharSize;
            } else {
                String errorMessage = "LibreTranslate unavailable.\nCheck URL/API key.";
                panelComponent.setBackgroundColor(bgColorInvalid);
                panelComponent.getChildren().add(LineComponent.builder()
                        .left(errorMessage)
                        .build());
                len = (getMaxLetters(errorMessage.split("\n")) + 2) * foreignCharSize;
            }
            panelComponent.setPreferredSize(new Dimension(len,0));
            return panelComponent.render(graphics);
        }

        if (deeplKeyValid) {
            panelComponent.setBackgroundColor(bgColorCount);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("DeepL:")
                    .right(deeplCount + " / " + deeplLimit)
                    .build());
            len = (deeplLimit.length()*2+10)*enCharSize;
        } else {
            String errorMessage = LangCodeSelectableList.getAPIErrorMessage(config.getSelectedLanguage());
            panelComponent.setBackgroundColor(bgColorInvalid);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(errorMessage)
                    .build());
            len = (getMaxLetters(errorMessage.split("\n"))+2)*foreignCharSize;
        }
        panelComponent.setPreferredSize(new Dimension(len,0));
        return panelComponent.render(graphics);
    }

    public static int getMaxLetters(String[] strings) {
        int maxLength = 0;
        for (String str : strings) {
            if (str != null) {
                maxLength = Math.max(maxLength, str.length());
            }
        }
        return maxLength;
    }
}
