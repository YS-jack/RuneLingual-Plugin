package com.RuneLingual.ApiTranslate;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualConfig;
import com.RuneLingual.RuneLingualPlugin;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;


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

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showUsageOverlayConfig())
            return null;

        int enCharSize = LangCodeSelectableList.ENGLISH.getCharSize();
        int foreignCharSize = config.getSelectedLanguage().getCharSize();
        boolean deeplKeyValid = plugin.getDeepl().isKeyValid();
        String deeplCount = Long.toString(plugin.getDeepl().getDeeplCount());
        String deeplLimit = Long.toString(plugin.getDeepl().getDeeplLimit());
//        String googleCount = Long.toString(plugin.getApiTranslate().googleCount);
//        String googleLimit = Long.toString(plugin.getApiTranslate().googleLimit);
//        String azureCount = Long.toString(plugin.getApiTranslate().azureCount);
//        String azureLimit = Long.toString(plugin.getApiTranslate().azureLimit);

        Color bgColorCount = new Color(80, 148, 144);
        Color bgColorInvalid = new Color(194, 93, 93);
        panelComponent.getChildren().clear();
        int len;
        if (deeplKeyValid) {
            panelComponent.setBackgroundColor(bgColorCount);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("DeepL:")
                    .right(deeplCount + " / " + deeplLimit)
                    .build());
            len = (deeplLimit.length()*2+10)*enCharSize;
        } else {
            panelComponent.setBackgroundColor(bgColorInvalid);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("APIキーが無効です。\n有効なキーが入力されるまでは\n簡易翻訳が行われます。APIキーが有効であることと\n上限に到達していないことを確認してください")
                    .build());
            len = ("簡易翻訳が行われます。APIキーが有効であることと".length()+2)*foreignCharSize;
        }
        panelComponent.setPreferredSize(new Dimension(len,0));
        return panelComponent.render(graphics);
    }


}
