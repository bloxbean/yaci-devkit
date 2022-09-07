package com.bloxbean.cardano.yacicli.commands.tail.themes;

public class ColorModeFactory {
    private static ColorMode colorMode;

    public static void setColorMode(String mode) {
        if ("light".equals(mode))
            colorMode = new LightColorMode();
        else
            colorMode = new DarkColorMode();
    }

    public static ColorMode getColorMode() {
        if (colorMode == null)
            return new DarkColorMode();
        else
            return colorMode;
    }
}
