package com.mnemonic;


import java.util.regex.Pattern;

public final class CommonConstants {

    public final static Pattern SPLITTER_PATTERN = Pattern.compile("\\w*=\\w*");

    private CommonConstants() {
        // nope
    }
}
