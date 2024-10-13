package com.RuneLingual;

import lombok.AccessLevel;
import lombok.Getter;

import javax.inject.Inject;

@Getter
public enum LangCodeSelectableList
{
    ENGLISH ("en", "EN","EN", 8, 6, false, false, false, false),
    PORTUGUÊS_BRASILEIRO ("pt_br", "PT","PT-BR", 8, 6, false, false, false, false),
    NORSK("no", "NB", "NB", 8, 6, false, false, false, false),
    日本語("ja", "JA", "JA", 15, 13, true, true, true, true);
    // todo: add languages here

    private final String langCode;
    private final String deeplLangCodeSource;
    private final String deeplLangCodeTarget;
    private final int charSize;
    private final int chatBoxCharSize;

    @Getter(AccessLevel.NONE)
    private final boolean needCharImages;
    @Getter(AccessLevel.NONE)
    private final boolean swapMenuOptionAndTarget;
    @Getter(AccessLevel.NONE)
    private final boolean needInputOverlay;
    @Getter(AccessLevel.NONE)
    private final boolean needInputCandidateOverlay;

    @Inject
    LangCodeSelectableList(String langCode, String deeplCodeSrc, String deeplCodeTgt, int charSize, int chatBoxCharSize,
                           boolean needCharImages, boolean swapMenuOptionAndTarget,
                           boolean needInputOverlay, boolean needInputCandidateOverlay){
        this.langCode = langCode;
        this.deeplLangCodeSource = deeplCodeSrc;
        this.deeplLangCodeTarget = deeplCodeTgt;
        this.charSize = charSize;
        this.chatBoxCharSize = chatBoxCharSize;
        this.needCharImages = needCharImages;
        this.swapMenuOptionAndTarget = swapMenuOptionAndTarget;
        this.needInputOverlay = needInputOverlay;
        this.needInputCandidateOverlay = needInputCandidateOverlay;
    }

    public boolean needsCharImages() {
        return needCharImages;
    }
    public boolean needsSwapMenuOptionAndTarget() {
        return swapMenuOptionAndTarget;
    }

    public boolean needsInputOverlay() {
        return needInputOverlay;
    }

    public boolean needsInputCandidateOverlay() {
        return needInputCandidateOverlay;
    }

}
