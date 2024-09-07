package com.RuneLingual.nonLatin;


import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.nonLatin.Japanese.UpdateChatInputJa;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class chatInput {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private UpdateChatInputJa updateChatInputJa;

    public chatInput(RuneLingualPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateChatInput() {
        if (plugin.getConfig().getSelectedLanguage().needsCharImages()){
            if(plugin.getConfig().getSelectedLanguage().equals(LangCodeSelectableList.日本語)){
                updateChatInputJa.updateInput();
            }
        }
    }
}
