package com.RuneLingual;

import lombok.Getter;

public enum LangCodeSelectableList
{
    ENGLISH ("en"),
    PORTUGUÊS_BRASILEIRO ("pt_br"),
    NORSK("no"),
    日本語("ja");
    // todo: add languages here

    @Getter
    private final String langCode;

    LangCodeSelectableList(String langCode){this.langCode = langCode;}

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
