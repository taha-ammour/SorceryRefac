package org.example.engine;

/**
 * Holds the data needed to create a Sprite:
 * the sprite sheet name, pixel region, and default palette codes.
 */
public class SpriteDefinition {
    public String sheetName;
    public int x, y;
    public int width, height;
    public String[] paletteCodes; // e.g., {"100", "250", "000", "555"}

    public SpriteDefinition(String sheetName, int x, int y, int width, int height, String[] paletteCodes) {
        this.sheetName = sheetName;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        if (paletteCodes == null || paletteCodes.length != 4) {
            throw new IllegalArgumentException("paletteCodes must be an array of 4 strings");
        }
        this.paletteCodes = paletteCodes;
    }
}
