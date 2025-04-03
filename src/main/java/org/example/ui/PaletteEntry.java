package org.example.ui;

public class PaletteEntry {
    public String spriteName;
    public String[] colorCodes; // For example: {"003", "224", "112", "555"}

    public PaletteEntry(String spriteName, String[] colorCodes) {
        this.spriteName = spriteName;
        this.colorCodes = colorCodes;
    }
}
