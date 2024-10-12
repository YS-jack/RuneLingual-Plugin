package com.RuneLingual.nonLatin;


import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.RuneLingualPlugin;
import com.RuneLingual.nonLatin.Japanese.UpdateChatInputJa;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class ChatInputRLingual {
    @Inject
    private RuneLingualPlugin plugin;
    @Inject @Getter
    private UpdateChatInputJa updateChatInputJa;

    @Inject
    public ChatInputRLingual(RuneLingualPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateChatInput() {
        if (plugin.getConfig().getSelectedLanguage().needsCharImages()){
            if(plugin.getConfig().getSelectedLanguage().equals(LangCodeSelectableList.日本語)){
                updateChatInputJa.updateInput();
            }
        }
    }

    public String transformChatText(String text) {
        if (plugin.getConfig().getSelectedLanguage().needsCharImages()){
            if(plugin.getConfig().getSelectedLanguage().equals(LangCodeSelectableList.日本語)){
                return updateChatInputJa.romJpTransform(text, false);
            }
        }
        return text;
    }
}
