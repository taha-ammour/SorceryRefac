package org.example.engine;
import org.joml.Matrix4f;

public class FontSheet {

    // Font flag constants (same as in the C code)
    public static final int FONT_NONE     = 0;
    public static final int FONT_ITALIC   = 1;
    public static final int FONT_DOUBLED  = 1 << 1;
    public static final int FONT_CENTER_X = 1 << 2;

    // Each glyph is 8x8 pixels (with 9–pixel line spacing on subsequent lines)
    private final int glyphWidth  = 8;
    private final int glyphHeight = 8;

    // The font atlas (sprite sheet).
    private final SpriteSheet fontAtlas;

    private final String[] colorPalette = new String[]{"555", "555", "555", "555"};

    // This Sprite is used to retrieve a default shader.
    private Sprite sprite;

    // Layout for mapping characters to glyph positions.
    private static final String[] FONT_LAYOUT = {
            "ABCDEFGHIJKLMNOP",
            "QRSTUVWXYZ",
            "1234567890.,;:/\\",
            "!?@#$%^&*()[]+-=",
            "<>{}'\"_|" + "\u00DB"
    };

    // Scale factor for glyph spacing and size
    private float scale = 4.0F;

    /**
     * Constructs the FontSheet by loading the font atlas.
     */
    public FontSheet() {
        // Do not shadow the field; assign directly.
        fontAtlas = new SpriteSheet("/textures/font.png");
        // Create a default sprite from the atlas to retrieve its shader.
        // We assume the atlas contains the font arranged in a grid;
        // here we take the glyph at grid position (0,0) with the standard glyph size.
        sprite = fontAtlas.getSpriteByGrid(0, 0, glyphWidth, glyphHeight);
    }

    /**
     * Returns the width in pixels of the entire string (ignoring control codes),
     * scaled by the current scale factor.
     */
    public int stringWidth(String text) {
        int acc = 0, max = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '$' && i + 3 < text.length()) {
                if (text.charAt(i + 1) == '$') {
                    acc += glyphWidth;
                    i += 2;
                } else {
                    i += 4;
                }
            } else if (c == '\n') {
                if (acc > max) {
                    max = acc;
                }
                acc = 0;
                i++;
            } else if (c == '\t') {
                acc += 32;
                i++;
            } else {
                acc += glyphWidth;
                i++;
            }
        }
        return Math.round(Math.max(acc, max) * scale);
    }

    /**
     * Returns the height in pixels for the string, scaled by the current scale factor.
     */
    public int stringHeight(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return Math.round((glyphHeight + (lines - 1) * 9) * scale);
    }

    /**
     * Helper to compute the width of the first line (ignoring control codes), scaled.
     */
    private int firstLineWidth(String text, int start) {
        int acc = 0;
        int i = start;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '$' && i + 3 < text.length()) {
                if (text.charAt(i + 1) == '$') {
                    acc += glyphWidth;
                    i += 2;
                } else {
                    i += 4;
                }
            } else if (c == '\n') {
                break;
            } else if (c == '\t') {
                acc += 32;
                i++;
            } else {
                acc += glyphWidth;
                i++;
            }
        }
        return Math.round(acc * scale);
    }

    /**
     * Returns the Sprite for a given character and flags.
     */
    private Sprite getGlyphSprite(char ch, int flags) {
        ch = Character.toUpperCase(ch);
        int row = -1;
        int col = -1;
        for (int i = 0; i < FONT_LAYOUT.length; i++) {
            int pos = FONT_LAYOUT[i].indexOf(ch);
            if (pos != -1) {
                row = i;
                col = pos;
                break;
            }
        }
        if (row == -1) {
            System.err.println("Warning: unfont-able character '" + ch + "'");
            row = 0;
            col = 0;
        }
        if ((flags & FONT_ITALIC) != 0 && ch >= 'A' && ch <= 'Z') {
            row += 5;
        }
        // Retrieve a sprite from the font atlas using grid coordinates.
        sprite = fontAtlas.getSpriteByGrid(col, row, glyphWidth, glyphHeight);
        return sprite;
    }

    /**
     * Renders the given text string.
     *
     * @param text         The string to render (may include control codes).
     * @param viewProj     The view–projection matrix.
     * @param x            The x–coordinate (in pixels) where text begins.
     * @param y            The y–coordinate (in pixels) at the bottom of the text.
     * @param z            Depth value.
     * @param flags        Initial font flags.
     * @param defaultColor Default text color as 0xRRGGBB.
     * @param alpha        Opacity [0..1].
     */
    public void renderText(String text, Matrix4f viewProj,
                           float x, float y, float z,
                           int flags, int defaultColor, float alpha) {
        int currentFlags = flags;
        int currentColor = defaultColor;

        // Compute scaled starting positions.
        int totalHeight = stringHeight(text);
        float startX = x;
        float startY = y + totalHeight - Math.round(glyphHeight * scale);

        int lineWidth = firstLineWidth(text, 0);
        float offsetX = 0;
        if ((currentFlags & FONT_CENTER_X) != 0) {
            int overallWidth = stringWidth(text);
            offsetX = (overallWidth - lineWidth) / 2.0f;
        }
        float offsetY = 0;
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '$' && i + 3 < text.length()) {
                if (text.charAt(i + 1) == '$') {
                    c = '$';
                    i += 2;
                } else {
                    String code = text.substring(i + 1, i + 4);
                    switch (code) {
                        case "ITA":
                            currentFlags |= FONT_ITALIC;
                            break;
                        case "REG":
                            currentFlags &= ~FONT_ITALIC;
                            break;
                        case "SIN":
                            currentFlags &= ~FONT_DOUBLED;
                            break;
                        case "DBL":
                            currentFlags |= FONT_DOUBLED;
                            break;
                        case "RES":
                            currentFlags &= ~(FONT_ITALIC | FONT_DOUBLED);
                            break;
                        case "CTX":
                            currentFlags |= FONT_CENTER_X;
                            break;
                        case "PCT":
                            c = '%';
                            break;
                        default:
                            if (code.matches("[0-5]{3}")) {
                                int r = code.charAt(0) - '0';
                                int g = code.charAt(1) - '0';
                                int b = code.charAt(2) - '0';
                                r *= 51;
                                g *= 51;
                                b *= 51;
                                currentColor = (r << 16) | (g << 8) | b;
                            }
                            break;
                    }
                    i += 4;
                    continue;
                }
            }

            if (Character.isWhitespace(c)) {
                if (c == '\n') {
                    offsetY += Math.round(9 * scale);
                    if ((currentFlags & FONT_CENTER_X) != 0) {
                        int lineW = firstLineWidth(text, i + 1);
                        int overallWidth = stringWidth(text);
                        offsetX = (overallWidth - lineW) / 2.0f;
                    } else {
                        offsetX = 0;
                    }
                } else if (c == '\t') {
                    offsetX += Math.round(32 * scale);
                } else {
                    offsetX += Math.round(glyphWidth * scale);
                }
                i++;
                continue;
            }

            Sprite glyph = getGlyphSprite(c, currentFlags);
            glyph.setPosition(startX + offsetX, startY + offsetY);
            glyph.setZ(z);
            glyph.setPaletteFromCodes(colorPalette);
            glyph.setColor(currentColor, alpha);
            glyph.setScale(scale, scale);
            glyph.render(viewProj);

            if ((currentFlags & FONT_DOUBLED) != 0) {
                Sprite glyphDbl = getGlyphSprite(c, currentFlags & ~FONT_DOUBLED);
                glyphDbl.setPosition(startX + offsetX + Math.round(1 * scale), startY + offsetY - Math.round(1 * scale));
                glyphDbl.setZ(z + 0.001f);
                glyphDbl.setColor(darkenColor(currentColor, 2), alpha);
                glyphDbl.setScale(scale, scale);
                glyphDbl.render(viewProj);
            }

            offsetX += Math.round(glyphWidth * scale);
            i++;
        }
    }

    public GlyphQuad getGlyphQuad(char ch, int flags) {
        ch = Character.toUpperCase(ch);
        int row = -1;
        int col = -1;
        for (int i = 0; i < FONT_LAYOUT.length; i++) {
            int pos = FONT_LAYOUT[i].indexOf(ch);
            if (pos != -1) {
                row = i;
                col = pos;
                break;
            }
        }
        if (row == -1) {
            System.err.println("Warning: unfont-able character '" + ch + "'");
            row = 0;
            col = 0;
        }
        if ((flags & FONT_ITALIC) != 0 && ch >= 'A' && ch <= 'Z') {
            row += 5;
        }
        int atlasWidth = fontAtlas.getAtlasWidth();
        int atlasHeight = fontAtlas.getAtlasHeight();
        float u1 = (col * glyphWidth) / (float) atlasWidth;
        float v1 = (row * glyphHeight) / (float) atlasHeight;
        float u2 = ((col + 1) * glyphWidth) / (float) atlasWidth;
        float v2 = ((row + 1) * glyphHeight) / (float) atlasHeight;
        return new GlyphQuad(u1, v1, u2, v2);
    }

    /**
     * Utility method that darkens an RGB color.
     */
    private int darkenColor(int color, int amount) {
        int r = Math.max(((color >> 16) & 0xFF) - amount, 0);
        int g = Math.max(((color >> 8) & 0xFF) - amount, 0);
        int b = Math.max((color & 0xFF) - amount, 0);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Sets the scale factor.
     */
    public void setScale(float ss) {
        this.scale = ss;
    }

    public float getScale() {
        return scale;
    }

    public SpriteSheet getFontAtlas(){
        return fontAtlas;
    }

    public int getGlyphWidth() {
        return glyphWidth;
    }

    public int getGlyphHeight() {
        return glyphHeight;
    }

    public Shader getSpriteShader() {
        return sprite.getShader();
    }
}
