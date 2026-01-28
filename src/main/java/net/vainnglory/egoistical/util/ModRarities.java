package net.vainnglory.egoistical.util;

import net.minecraft.util.Formatting;

import net.minecraft.text.TextColor;

public enum ModRarities {

    GOLDEN(TextColor.fromRgb(0xE8AB55)),
    ENDER(TextColor.fromRgb(0x0D5959));

    public final TextColor color;

    ModRarities(TextColor color) {
        this.color = color;
    }
}
