package com.RuneLingual;

import lombok.Getter;

import javax.inject.Inject;

public enum LangCodeSelectableList
{
    ENGLISH ("en", "EN","EN"),
    PORTUGUÊS_BRASILEIRO ("pt_br", "PT","PT-BR"),
    NORSK("no", "NB", "NB"),
    日本語("ja", "JA", "JA");
    // todo: add languages here

    @Getter
    private final String langCode;
    @Getter
    private final String deeplLangCodeSource;
    @Getter
    private final String deeplLangCodeTarget;

    @Inject
    LangCodeSelectableList(String langCode, String deeplCodeSrc, String deeplCodeTgt){
        this.langCode = langCode;
        this.deeplLangCodeSource = deeplCodeSrc;
        this.deeplLangCodeTarget = deeplCodeTgt;
    }

    public String getCode(){return this.langCode;}

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
