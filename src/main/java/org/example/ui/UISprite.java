package org.example.ui;

import org.example.engine.Sprite;
import org.example.engine.SpriteManager;
import org.joml.Matrix4f;

/**
 * UI component for displaying sprites (like icons) in the UI
 */
public class UISprite extends UIComponent {
    private Sprite sprite;
    private float scale = 1.0f;
    private float rotation = 0.0f;
    private int color = 0xFFFFFF; // Default white
    private float alpha = 1.0f;

    /**
     * Create a new UI sprite component
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param spriteName Name of the sprite to display
     * @param spriteManager Sprite manager to get the sprite from
     */
    public UISprite(float x, float y, float width, float height, String spriteName, SpriteManager spriteManager) {
        super(x, y, width, height);
        this.sprite = spriteManager.getSprite(spriteName);

        // Set initial position
        sprite.setPosition(x, y);
    }

    /**
     * Create a UI sprite from an existing sprite
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param sprite The sprite to use
     */
    public UISprite(float x, float y, float width, float height, Sprite sprite) {
        super(x, y, width, height);
        this.sprite = sprite;

        // Set initial position
        sprite.setPosition(x, y);
    }

    @Override
    public void update(float deltaTime) {
        if (!visible) return;

        // Update sprite position if it changed
        if (layoutDirty) {
            // Calculate final position based on anchoring and padding
            float finalX = x + paddingLeft;
            float finalY = y + paddingTop;
            finalX -= width * anchorX;
            finalY -= height * anchorY;

            sprite.setPosition(finalX, finalY);
            layoutDirty = false;
        }
    }

    @Override
    public void render(Matrix4f viewProj) {
        if (!visible) return;

        // Render the sprite
        sprite.render(viewProj);
    }

    /**
     * Set the sprite's scale
     * @param scale Scale factor
     */
    public void setScale(float scale) {
        this.scale = scale;
        sprite.setScale(scale, scale);
    }

    /**
     * Set the sprite's rotation (in radians)
     * @param rotation Rotation angle in radians
     */
    public void setRotation(float rotation) {
        this.rotation = rotation;
        sprite.setRotation(rotation);
    }

    /**
     * Set the sprite's color and alpha
     * @param color Color as 0xRRGGBB
     * @param alpha Alpha value (0.0 to 1.0)
     */
    public void setColor(int color, float alpha) {
        this.color = color;
        this.alpha = alpha;
        sprite.setColor(color, alpha);
    }

    /**
     * Set the sprite's palette from color codes
     * @param paletteCodes Array of 4 color codes
     */
    public void setPalette(String[] paletteCodes) {
        sprite.setPaletteFromCodes(paletteCodes);
    }

    /**
     * Set the sprite's Z value (used for layering in the UI)
     * @param z Z value
     */
    public void setZ(float z) {
        sprite.setZ(z);
    }

    /**
     * Get the underlying sprite
     * @return The sprite
     */
    public Sprite getSprite() {
        return sprite;
    }
    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
        layoutDirty = true;
    }
}