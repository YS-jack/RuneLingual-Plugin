package com.RuneLingual;

import lombok.Getter;

public enum TranslatingServiceSelectableList
{
    DeepL ("deepl"),
    DeepL_PRO ("deepl_pro"),
    LibreTranslate ("libretranslate");
//    GOOGLE_TRANSLATE ("google"),
//    OPENAI_GPT ("openai");

    @Getter
    private final String serviceName;

    TranslatingServiceSelectableList(String code){this.serviceName = code;}

    public String getService(){return this.serviceName;}

    public boolean isDeepLFamily() {
        return this == DeepL || this == DeepL_PRO;
    }
}
