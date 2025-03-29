package com.RuneLingual;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;

@Getter
public enum LangCodeSelectableList
{
    ENGLISH ("en", "EN","EN", 8, 11, 6, 6, false, false, false, false, true, false),
    //PORTUGUÊS_BRASILEIRO ("pt_br", "PT","PT-BR", 8, 11, 6, false, false, false, false, true),
    NORSK("no", "NB", "NB", 8, 12, 6, 6, false, false, false, false, true, false),
    日本語("ja", "JA", "JA", 11, 12, 12, 15, true, true, true, true, false, true),
    Русский("ru", "RU", "RU", 8, 12, 6, 6, true, false, true, false, true, false);
    // todo: add languages here

    private final String langCode;
    private final String deeplLangCodeSource;
    private final String deeplLangCodeTarget;
    private final int charWidth;
    private final int charHeight;
    private final int chatBoxCharWidth;
    private final int overlayCharWidth;

    @Getter(AccessLevel.NONE)
    private final boolean needCharImages;
    @Getter(AccessLevel.NONE)
    private final boolean swapMenuOptionAndTarget;
    @Getter(AccessLevel.NONE)
    private final boolean needInputOverlay;
    @Getter(AccessLevel.NONE)
    private final boolean needInputCandidateOverlay;
    @Getter(AccessLevel.NONE)
    private final boolean needSpaceBetweenWords;
    private final boolean chatButtonHorizontal;


    @Inject
    LangCodeSelectableList(String langCode, String deeplCodeSrc, String deeplCodeTgt,
                           int charWidth, int charHeight, int chatBoxCharWidth, int overlayCharWidth,
                           boolean needCharImages, boolean swapMenuOptionAndTarget,
                           boolean needInputOverlay, boolean needInputCandidateOverlay,
                           boolean needSpaceBetweenWords, boolean chatButtonHorizontal) {
        this.langCode = langCode;
        this.deeplLangCodeSource = deeplCodeSrc;
        this.deeplLangCodeTarget = deeplCodeTgt;
        this.charWidth = charWidth;
        this.charHeight = charHeight;
        this.chatBoxCharWidth = chatBoxCharWidth;
        this.overlayCharWidth = overlayCharWidth;
        this.needCharImages = needCharImages;
        this.swapMenuOptionAndTarget = swapMenuOptionAndTarget;
        this.needInputOverlay = needInputOverlay;
        this.needInputCandidateOverlay = needInputCandidateOverlay;
        this.needSpaceBetweenWords = needSpaceBetweenWords;
        this.chatButtonHorizontal = chatButtonHorizontal;
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

    public boolean needsSpaceBetweenWords() {
        return needSpaceBetweenWords;
    }

    public static int getLatinCharWidth(Widget widget, LangCodeSelectableList langCode) {
        /*
        494: 5 px
        495: 6 px
        496: 7 px
        */
        int fontId = widget.getFontId();
        if (fontId == 494) {
            return 5;
        } else if (fontId == 495) {
            return 6;
        } else if (fontId == 496) {
            return 7;
        }
        return langCode.getCharWidth();
    }
}
