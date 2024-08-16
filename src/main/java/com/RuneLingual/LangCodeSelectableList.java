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


}
