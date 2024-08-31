package com.RuneLingual;

import lombok.Getter;

import javax.inject.Inject;

public enum LangCodeSelectableList
{
    ENGLISH ("en", "EN","EN", 8),
    PORTUGUÊS_BRASILEIRO ("pt_br", "PT","PT-BR", 8),
    NORSK("no", "NB", "NB", 8),
    日本語("ja", "JA", "JA", 15);
    // todo: add languages here

    @Getter
    private final String langCode;
    @Getter
    private final String deeplLangCodeSource;
    @Getter
    private final String deeplLangCodeTarget;
    @Getter
    private final int charSize;

    @Inject
    LangCodeSelectableList(String langCode, String deeplCodeSrc, String deeplCodeTgt, int charSize){
        this.langCode = langCode;
        this.deeplLangCodeSource = deeplCodeSrc;
        this.deeplLangCodeTarget = deeplCodeTgt;
        this.charSize = charSize;
    }

    public boolean needCharImages(){
        if (this == 日本語){ // todo: when adding new languages, add them here if they need char images
            return true;
        }
        return false;
    }

    public boolean swapMenuOptionAndTarget(){
        if (this == 日本語){ // todo: when adding new languages, add them here if they should swap menu entries (if noun comes before verb)
            return true;
        }
        return false;
    }


}
