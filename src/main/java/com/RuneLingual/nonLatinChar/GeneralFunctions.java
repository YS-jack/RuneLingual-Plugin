package com.RuneLingual.nonLatinChar;

import com.RuneLingual.nonLatinChar.CharImageInit;
import com.RuneLingual.nonLatinChar.Colors;
import com.RuneLingual.RuneLingualPlugin;
import net.runelite.client.game.ChatIconManager;

import javax.inject.Inject;
import java.util.HashMap;

public class GeneralFunctions {
    @Inject
    private CharImageInit charImageInit;
    @Inject
    private RuneLingualPlugin runeLingualPlugin;

    public String StringToTags(String string, Colors colors) {
        /*
        This function takes a string and a color and returns a string with the color tags
        eg: こんにちは -> <img=1> <img=2> <img=3> <img=4> <img=5> <img=6>

        order of conversion : String -> (for each char) -> char to codepoint -> get image name (eg: black--3021.png) ->
        -> get hash value of the char image from the hashmap
        -> get image Id in the chatIcon manager from the hash value
        -> create a tag with the image Id (eg: <img=1>)
        -> repeat to all characters in the string -> append all tags -> return
         */

        StringBuilder imgTagStrings = new StringBuilder();
        ChatIconManager chatIconManager = runeLingualPlugin.getChatIconManager();
        HashMap<String, Integer> map = runeLingualPlugin.getCharIds();
        for (int j = 0; j < string.length(); ) {

            int codePoint = string.codePointAt(j);
            String imgName = colors.getName() + "--" + codePoint + ".png";
            int hash = map.getOrDefault(imgName, -99);
            if (hash == -99) {
                imgTagStrings.append("?");
                j += Character.isHighSurrogate(string.charAt(j)) ? 2 : 1;
            }
            imgTagStrings.append("<img=");
            imgTagStrings.append(chatIconManager.chatIconIndex(hash));
            imgTagStrings.append(">");
            j += Character.isHighSurrogate(string.charAt(j)) ? 2 : 1;

        }
        return imgTagStrings.toString();
    }

}
