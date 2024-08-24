package com.RuneLingual.nonLatinChar;

import com.RuneLingual.commonFunctions.Colors;
import com.RuneLingual.RuneLingualPlugin;
import net.runelite.client.game.ChatIconManager;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.regex.Pattern;

public class GeneralFunctions {
    @Inject
    private CharImageInit charImageInit;
    @Inject
    private RuneLingualPlugin runeLingualPlugin;

    public String StringToTags(String string, Colors colors) {
        /*
        This function takes a string and a color and returns a string with the color tags
        But leave <img=??> tags as they are (they are already emojis)
        eg: こんにちは,<img=43>Lukyさん -> <img=1> <img=2> <img=3> <img=4> <img=5> <img=6> <img=43> <img=9>...

        order of conversion : String -> (for each char) -> char to codepoint -> get image name (eg: black--3021.png) ->
        -> get hash value of the char image from the hashmap
        -> get image Id in the chatIcon manager from the hash value
        -> create a tag with the image Id (eg: <img=1>)
        -> repeat to all characters in the string -> append all tags -> return
         */
        String pattern = "<img=[0-9]*>";
        String[] parts = string.split("(?=" + pattern + ")|(?<=" + pattern + ")");
        StringBuilder imgTagSb = new StringBuilder();
        for (String part : parts) {
            if (part.matches(pattern)) {
                imgTagSb.append(part);
                continue;
            }
            StringBuilder imgTagStrings = new StringBuilder();
            ChatIconManager chatIconManager = runeLingualPlugin.getChatIconManager();
            HashMap<String, Integer> map = runeLingualPlugin.getCharIds();
            for (int j = 0; j < part.length(); ) {

                int codePoint = part.codePointAt(j);
                if (codePoint == 160) {
                    imgTagStrings.append(" ");
                    j += 1;
                    continue;
                }
                String imgName = colors.getName() + "--" + codePoint + ".png";
                int hash = map.getOrDefault(imgName, -99);
                if (hash == -99) {
                    imgTagStrings.append("?");
                    j += Character.isHighSurrogate(part.charAt(j)) ? 2 : 1;
                }
                imgTagStrings.append("<img=");
                imgTagStrings.append(chatIconManager.chatIconIndex(hash));
                imgTagStrings.append(">");
                j += Character.isHighSurrogate(part.charAt(j)) ? 2 : 1;

            }
            imgTagSb.append(imgTagStrings);
        }
        return imgTagSb.toString();
    }

}
