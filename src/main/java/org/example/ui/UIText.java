package org.example.ui;

import org.example.engine.BatchedFontObject;
import org.example.engine.FontSheet;
import org.example.engine.Shader;
import org.joml.Matrix4f;

public class UIText extends UIComponent {
    private BatchedFontObject textObject;
    private FontSheet fontSheet;
    private Shader shader;

    // The complete text and the text currently being displayed (for animations).
    private String targetText;
    private String displayedText;

    // Animation state for typewriter effect.
    private boolean meshDirty = true;
    private float elapsedTime = 0f;
    private int currentCharCount = 0;
    private float charDelay = 0.1f; // Seconds delay per character.

    /**
     * Constructs a UIText element with a typewriter animation effect.
     *
     * @param fontSheet The font sheet used for text rendering.
     * @param shader    The shader used for rendering text.
     * @param text      The initial full text string.
     * @param x         Initial x-position.
     * @param y         Initial y-position.
     */
    public UIText(FontSheet fontSheet, Shader shader, String text, float x, float y) {
        super(x, y, 0, 0);
        this.fontSheet = fontSheet;
        this.shader = shader;
        this.targetText = text;
        this.displayedText = "";
        this.currentCharCount = 0;
        textObject = new BatchedFontObject(fontSheet, displayedText, x, y, 0, 0, 0xFFFFFF, 1.0f, shader);
    }

    /**
     * Set new text to animate. Resets the typewriter effect.
     *
     * @param newText The new full text string.
     */
    public void setText(String newText) {
        this.targetText = newText;
        this.currentCharCount = 0;
        this.elapsedTime = 0f;
        // Reset displayed text and update the underlying text object.
        this.displayedText = "";
        textObject.setText(displayedText);
        meshDirty = true;
    }

    /**
     * Update the text-specific animation (typewriter effect).
     *
     * @param deltaTime The elapsed time since last update (in seconds).
     */
    @Override
    public void update(float deltaTime) {
        // Typewriter effect: gradually reveal text one character at a time.
        if (currentCharCount < targetText.length()) {
            elapsedTime += deltaTime;
            if (elapsedTime >= charDelay) {
                int charsToAdd = (int)(elapsedTime / charDelay);
                elapsedTime %= charDelay;
                currentCharCount += charsToAdd;
                if (currentCharCount > targetText.length()) {
                    currentCharCount = targetText.length();
                }
                displayedText = targetText.substring(0, currentCharCount);
                textObject.setText(displayedText);
                meshDirty = true;
            }
        }
        // Optionally add additional text-specific animations or effects here,
        // such as blinking cursors, color transitions, or wave effects.
    }

    /**
     * Render the text element.
     *
     * @param viewProj The view-projection matrix used for rendering.
     */
    @Override
    public void render(Matrix4f viewProj) {
        if (!visible) {
            return;
        }
        // Calculate final position based on anchoring and padding.
        float finalX = x + paddingLeft;
        float finalY = y + paddingTop;
        finalX -= width * anchorX;
        finalY -= height * anchorY;
        textObject.setPosition(finalX, finalY, 0);
        textObject.render(viewProj);
    }
}
