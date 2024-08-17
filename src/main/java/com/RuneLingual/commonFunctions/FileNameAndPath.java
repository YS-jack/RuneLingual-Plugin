package com.RuneLingual.commonFunctions;

import lombok.Getter;
import net.runelite.client.RuneLite;

import java.io.File;

public class FileNameAndPath {
    @Getter
    private static final File localBaseFolder = new File(RuneLite.RUNELITE_DIR.getPath() + File.separator + "RuneLingual_resources");
    @Getter
    private static final String localSQLFileName = "transcript";
}
