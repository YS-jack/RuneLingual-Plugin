package com.RuneLingual.commonFunctions;

import com.RuneLingual.LangCodeSelectableList;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.RuneLite;

import java.io.File;

public class FileNameAndPath {
    @Getter
    private static final File localBaseFolder = new File(RuneLite.RUNELITE_DIR.getPath() + File.separator + "RuneLingual_resources");
    @Getter
    private static final String localSQLFileName = "transcript";


    public static String getLocalLangFolder(LangCodeSelectableList lang) {
        String langCode = lang.getLangCode();
        File localLangFolder = new File(localBaseFolder.getPath() + File.separator + langCode);
        return localLangFolder.getPath();
    }
}
