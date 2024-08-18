package com.RuneLingual.nonLatinChar;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;

import com.RuneLingual.prepareResources.Downloader;
import com.RuneLingual.RuneLingualPlugin;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ChatIconManager;

import javax.imageio.ImageIO;
import javax.inject.Inject;

@Slf4j
public class CharImageInit {
    @Inject
    RuneLingualPlugin runeLingualPlugin;

    public void loadCharImages()
    {
        if(!runeLingualPlugin.getTargetLanguage().needCharImages()){
            return;
        }

        ChatIconManager chatIconManager = runeLingualPlugin.getChatIconManager();
        HashMap<String, Integer> charIds = runeLingualPlugin.getCharIds();

        String[] charNameArray = getCharList(); //list of all characters e.g.　black--3021.png

        Downloader downloader = runeLingualPlugin.getDownloader();
        String langCode = downloader.getLangCode();
        final String pathToChar = downloader.getLocalLangFolder().toString() + File.separator + "char_" + langCode;

        for (String imageName : charNameArray) {
            try {
                String fullPath = pathToChar + File.separator + imageName;
                File externalCharImg = new File(fullPath);
                final BufferedImage image = ImageIO.read(externalCharImg);

                final int charID = chatIconManager.registerChatIcon(image);
                charIds.put(imageName, charID);
            } catch (Exception e){log.info(String.valueOf(e));}
        }
        log.info("end of making character image hashmap");
    }


    public String[] getCharList() {//get list of names of all characters of every colours)
        Downloader downloader = runeLingualPlugin.getDownloader();
        String langCode = downloader.getLangCode();
        final String pathToChar = downloader.getLocalLangFolder().toString() + File.separator + "char_" + langCode;

        FilenameFilter pngFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".png");
            }
        };
        File colorDir = new File(pathToChar + "/");
        File[] files = colorDir.listFiles(pngFilter); //list of files that end with ".png"
        if (files == null){return null;}
        String[] charImageNames = new String[files.length];
        for (int j = 0; j < files.length; j++) {
            //charImagesFullPath[j] = colorDir.getAbsolutePath() + File.separator + files[j].getName();
            charImageNames[j] = files[j].getName();
        }
        return charImageNames;
    }
}
