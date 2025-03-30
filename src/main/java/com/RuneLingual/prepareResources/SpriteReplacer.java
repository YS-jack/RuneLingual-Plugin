package com.RuneLingual.prepareResources;

import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.commonFunctions.Ids;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.SpritePixels;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ImageUtil;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class SpriteReplacer {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;


    private final Map<String, Integer> fileNameToWidgetId = new java.util.HashMap<>();

    public void initMap() {
        Ids ids = plugin.getIds();
        String fileLoginBanner = plugin.getFileNameAndPath().getLocalLangFolder() + File.separator + "loginScreenSprite_" + plugin.getConfig().getSelectedLanguage().getLangCode() +".png";
        int loginBannerId = ids.getLoginBannerId();
        fileNameToWidgetId.put(fileLoginBanner, loginBannerId);
    }

    public void replaceWidgetSprite() {
        for(Map.Entry<String, Integer> entry : fileNameToWidgetId.entrySet()) {
            String file = entry.getKey();
            int widgetId = entry.getValue();
            SpritePixels sprite = getFileSpritePixels(file);
            if(sprite == null) {
                log.debug("Sprite is null for file: {}", file);
                continue;
            }
            client.getWidgetSpriteOverrides().put(widgetId, sprite);
        }
    }

    public void resetWidgetSprite() {
        clientThread.invokeLater(() ->
        {
            for(Map.Entry<String, Integer> entry : fileNameToWidgetId.entrySet()) {
                int widgetId = entry.getValue();
                client.getWidgetSpriteOverrides().remove(widgetId);
            }
        });
    }

    private SpritePixels getFileSpritePixels(String file)
    {
        try
        {
            log.debug("Loading: {}", file);
            File externalImage = new File(file);
            BufferedImage image = ImageIO.read(externalImage);
            return ImageUtil.getImageSpritePixels(image, client);
        }
        catch (RuntimeException ex)
        {
            log.debug("Unable to load image: ", ex);
        } catch (IOException e) {
            log.debug("Unable to load image: ", e);
        }
        return null;
    }
}
