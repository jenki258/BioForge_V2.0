package net.jenkimods.bioforge.api.guide;

import net.minecraft.network.chat.Style;

/** Preserve semantic colors and links while keeping text readable on the tablet panel. */
public final class TabletTextPalette {
    public static final int BACKGROUND = 0x0A2A34, TEXT = 0xA8F9FF;
    private TabletTextPalette() {}
    public static Style readable(Style style) {
        int color = style.getColor() == null ? TEXT : style.getColor().getValue();
        if (color == 0) color = TEXT;
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        while (contrast(color, BACKGROUND) < 4.5 && (r < 255 || g < 255 || b < 255)) {
            r = Math.min(255, r + 12); g = Math.min(255, g + 12); b = Math.min(255, b + 12);
            color = r << 16 | g << 8 | b;
        }
        return style.withColor(color);
    }
    public static double contrast(int first, int second) {
        double a = luminance(first), b = luminance(second);
        return (Math.max(a,b) + .05) / (Math.min(a,b) + .05);
    }
    private static double luminance(int color) {
        return .2126 * linear(color >> 16 & 255) + .7152 * linear(color >> 8 & 255) + .0722 * linear(color & 255);
    }
    private static double linear(int value) {
        double channel = value / 255D;
        return channel <= .04045 ? channel / 12.92 : Math.pow((channel + .055) / 1.055, 2.4);
    }
}
