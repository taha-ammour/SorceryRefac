package org.example.engine;

import org.joml.Matrix4f;

public class FontObject extends GameObject {
    private final FontSheet fontSheet;
    private final String text;
    private final float x;
    private final float y;
    private final float z;
    private final int flags;
    private final int defaultColor;
    private final float alpha;

    /**
     * Constructs a new TextGameObject.
     *
     * @param fontSheet    the FontSheet instance to use for text rendering.
     * @param text         the text string (may include control codes).
     * @param x            x-coordinate for text rendering.
     * @param y            y-coordinate for text rendering.
     * @param z            depth value.
     * @param flags        initial font flags (e.g. FontSheet.FONT_NONE, FontSheet.FONT_CENTER_X).
     * @param defaultColor default text color in 0xRRGGBB format.
     * @param alpha        opacity (0.0 to 1.0).
     */
    public FontObject(FontSheet fontSheet, String text, float x, float y, float z, int flags, int defaultColor, float alpha) {
        this.fontSheet = fontSheet;
        this.text = text;
        this.x = x;
        this.y = y;
        this.z = z;
        this.flags = flags;
        this.defaultColor = defaultColor;
        this.alpha = alpha;
    }

    @Override
    public void update(float deltaTime) {
        // For static text, no update logic is required.
    }

    @Override
    public void render(Matrix4f viewProj) {
        // Delegate rendering to the FontSheet.
        fontSheet.renderText(text, viewProj, x, y, z, flags, defaultColor, alpha);
    }

    @Override
    public void cleanup() {
        // Cleanup resources if necessary.
    }
}
