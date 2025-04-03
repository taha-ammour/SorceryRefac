package org.example.ui;

import org.example.engine.Animation;
import org.example.engine.Sprite;
import org.example.engine.SpriteManager;
import org.example.engine.ZOrderProvider;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Enhanced UI component that supports multiple sprite layers and animations.
 * Similar to LayeredCharacter but designed specifically for UI integration.
 */
public class UILayeredSprite extends UIComponent implements ZOrderProvider {
    private final SpriteManager spriteManager;
    private final List<Layer> layers = new ArrayList<>();
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float rotation = 0.0f;

    /**
     * Create a new UILayeredSprite
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param spriteManager Sprite manager for creating sprites
     */
    public UILayeredSprite(float x, float y, float width, float height, SpriteManager spriteManager) {
        super(x, y, width, height);
        this.spriteManager = spriteManager;
    }

    /**
     * Add a static sprite layer
     * @param layerName Name of the layer (for reference)
     * @param spriteName Name of the sprite to display
     * @param offsetX X offset from the component position
     * @param offsetY Y offset from the component position
     * @param zOrder Z order for layer ordering (higher values are drawn on top)
     * @return This instance for method chaining
     */
    public UILayeredSprite addLayer(String layerName, String spriteName, float offsetX, float offsetY, float zOrder) {
        Layer layer = new Layer(layerName, spriteName, null, offsetX, offsetY, zOrder);
        layers.add(layer);

        // Sort layers by Z order
        sortLayersByZ();

        // Mark the sprite as dynamic to prevent caching issues
        spriteManager.markSpriteAsDynamic(spriteName);

        return this;
    }

    /**
     * Add an animated sprite layer
     * @param layerName Name of the layer (for reference)
     * @param animation Animation to use for this layer
     * @param offsetX X offset from the component position
     * @param offsetY Y offset from the component position
     * @param zOrder Z order for layer ordering (higher values are drawn on top)
     * @return This instance for method chaining
     */
    public UILayeredSprite addLayer(String layerName, Animation animation, float offsetX, float offsetY, float zOrder) {
        Layer layer = new Layer(layerName, null, animation, offsetX, offsetY, zOrder);
        layers.add(layer);

        // Sort layers by Z order
        sortLayersByZ();

        return this;
    }

    /**
     * Remove a layer by name
     * @param layerName Name of the layer to remove
     * @return True if the layer was found and removed
     */
    public boolean removeLayer(String layerName) {
        boolean result = layers.removeIf(layer -> layer.layerName.equals(layerName));
        if (result) {
            // No need to resort, just removing a layer
        }
        return result;
    }

    /**
     * Get a layer by name
     * @param layerName Name of the layer to get
     * @return The layer if found, or null
     */
    public Layer getLayer(String layerName) {
        for (Layer layer : layers) {
            if (layer.layerName.equals(layerName)) {
                return layer;
            }
        }
        return null;
    }

    /**
     * Change the sprite for a static layer
     * @param layerName Name of the layer to update
     * @param spriteName New sprite name to use
     * @return True if the layer was found and updated
     */
    public boolean updateLayerSprite(String layerName, String spriteName) {
        Layer layer = getLayer(layerName);
        if (layer == null || layer.animation != null) {
            return false;
        }

        layer.spriteName = spriteName;
        layer.cachedSprite = null;
        layer.cachedSpriteName = null;

        // Mark the sprite as dynamic to prevent caching issues
        spriteManager.markSpriteAsDynamic(spriteName);

        return true;
    }

    /**
     * Change the animation for an animated layer
     * @param layerName Name of the layer to update
     * @param animation New animation to use
     * @return True if the layer was found and updated
     */
    public boolean updateLayerAnimation(String layerName, Animation animation) {
        Layer layer = getLayer(layerName);
        if (layer == null) {
            return false;
        }

        layer.animation = animation;
        layer.spriteName = null;
        layer.cachedSprite = null;
        layer.cachedSpriteName = null;

        return true;
    }

    /**
     * Set a layer's Z order
     * @param layerName Name of the layer to update
     * @param zOrder New Z order value
     * @return True if the layer was found and updated
     */
    public boolean setLayerZOrder(String layerName, float zOrder) {
        Layer layer = getLayer(layerName);
        if (layer == null) {
            return false;
        }

        layer.zOrder = zOrder;

        // Re-sort layers after changing Z order
        sortLayersByZ();

        return true;
    }

    /**
     * Set a layer's X and Y offsets
     * @param layerName Name of the layer to update
     * @param offsetX New X offset value
     * @param offsetY New Y offset value
     * @return True if the layer was found and updated
     */
    public boolean setLayerOffset(String layerName, float offsetX, float offsetY) {
        Layer layer = getLayer(layerName);
        if (layer == null) {
            return false;
        }

        layer.offsetX = offsetX;
        layer.offsetY = offsetY;

        return true;
    }

    /**
     * Play all animations in all layers
     */
    public void playAllAnimations() {
        for (Layer layer : layers) {
            if (layer.animation != null) {
                layer.isPlaying = true;
            }
        }
    }

    /**
     * Stop all animations in all layers
     */
    public void stopAllAnimations() {
        for (Layer layer : layers) {
            if (layer.animation != null) {
                layer.isPlaying = false;
            }
        }
    }

    /**
     * Play animation in a specific layer
     * @param layerName Name of the layer to control
     * @return True if the layer was found and animation started
     */
    public boolean playLayerAnimation(String layerName) {
        Layer layer = getLayer(layerName);
        if (layer == null || layer.animation == null) {
            return false;
        }

        layer.isPlaying = true;
        return true;
    }

    /**
     * Stop animation in a specific layer
     * @param layerName Name of the layer to control
     * @return True if the layer was found and animation stopped
     */
    public boolean stopLayerAnimation(String layerName) {
        Layer layer = getLayer(layerName);
        if (layer == null || layer.animation == null) {
            return false;
        }

        layer.isPlaying = false;
        return true;
    }

    /**
     * Set the sprite's scale
     * @param scaleX Scale factor on X axis
     * @param scaleY Scale factor on Y axis
     */
    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /**
     * Set the sprite's rotation (in radians)
     * @param rotation Rotation angle in radians
     */
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    /**
     * Get the sprite's current rotation
     * @return Current rotation in radians
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * Get the sprite's current X scale
     * @return Current X scale factor
     */
    public float getScaleX() {
        return scaleX;
    }

    /**
     * Get the sprite's current Y scale
     * @return Current Y scale factor
     */
    public float getScaleY() {
        return scaleY;
    }

    /**
     * Set the palette for a layer
     * @param layerName Name of the layer to update
     * @param paletteCodes Array of 4 palette codes
     * @return True if the layer was found and palette set
     */
    public boolean setLayerPalette(String layerName, String[] paletteCodes) {
        Layer layer = getLayer(layerName);
        if (layer == null) {
            return false;
        }

        layer.palette = paletteCodes;

        // Apply palette to cached sprite if it exists
        if (layer.cachedSprite != null) {
            layer.cachedSprite.setPaletteFromCodes(paletteCodes);
        }

        return true;
    }

    /**
     * Set the color and alpha for a layer
     * @param layerName Name of the layer to update
     * @param color Color as 0xRRGGBB
     * @param alpha Alpha value (0.0 to 1.0)
     * @return True if the layer was found and color set
     */
    public boolean setLayerColor(String layerName, int color, float alpha) {
        Layer layer = getLayer(layerName);
        if (layer == null) {
            return false;
        }

        layer.color = color;
        layer.alpha = alpha;

        // Apply color to cached sprite if it exists
        if (layer.cachedSprite != null) {
            layer.cachedSprite.setColor(color, alpha);
        }

        return true;
    }

    /**
     * Sort layers by Z-order (ascending) for proper rendering sequence
     * Layers with higher Z values will be drawn on top of lower Z values
     */
    private void sortLayersByZ() {
        layers.sort(Comparator.comparingDouble(layer -> layer.zOrder));
    }

    @Override
    public float getRenderingPriority() {
        // Return the minimum z-order of its layers
        return layers.stream()
                .map(Layer::getZOrder)
                .min(Float::compare)
                .orElse(0.0f);
    }

    @Override
    public void update(float deltaTime) {
        if (!visible) return;

        // Update animations for all layers
        for (Layer layer : layers) {
            if (layer.animation != null && layer.isPlaying) {
                layer.animation.update(deltaTime);
                // Animation changed, clear cached sprite
                layer.cachedSprite = null;
                layer.cachedSpriteName = null;
            }
        }

        // Update layout if dirty
        if (layoutDirty) {
            // Nothing special needed for now
            layoutDirty = false;
        }
    }

    @Override
    public void render(Matrix4f viewProj) {
        if (!visible) return;

        // Calculate final position based on anchoring and padding
        float finalX = x + paddingLeft;
        float finalY = y + paddingTop;
        finalX -= width * anchorX;
        finalY -= height * anchorY;

        // Since we've sorted the layers by Z-order, we'll render in that order
        // This ensures proper layering regardless of how the underlying sprite system works
        for (Layer layer : layers) {
            Sprite sprite = null;

            // Get the sprite for this layer (either from animation or static sprite)
            if (layer.animation != null) {
                // Get sprite from current animation frame
                String spriteName = layer.animation.getCurrentFrameName();
                if (!spriteName.equals(layer.cachedSpriteName) || layer.cachedSprite == null) {
                    sprite = spriteManager.getSprite(spriteName);
                    layer.cachedSprite = sprite;
                    layer.cachedSpriteName = spriteName;

                    // Apply palette and color if set
                    if (layer.palette != null) {
                        sprite.setPaletteFromCodes(layer.palette);
                    }
                    sprite.setColor(layer.color, layer.alpha);
                } else {
                    sprite = layer.cachedSprite;
                }
            } else if (layer.spriteName != null) {
                // Static sprite
                String spriteName = layer.spriteName;
                if (!spriteName.equals(layer.cachedSpriteName) || layer.cachedSprite == null) {
                    sprite = spriteManager.getSprite(spriteName);
                    layer.cachedSprite = sprite;
                    layer.cachedSpriteName = spriteName;

                    // Apply palette and color if set
                    if (layer.palette != null) {
                        sprite.setPaletteFromCodes(layer.palette);
                    }
                    sprite.setColor(layer.color, layer.alpha);
                } else {
                    sprite = layer.cachedSprite;
                }
            }

            // If we have a valid sprite, render it
            if (sprite != null) {
                // Position and transform the sprite
                sprite.setPosition(finalX + layer.offsetX, finalY + layer.offsetY);

                // IMPORTANT: We use the absolute Z value from the layer
                // This is critical for proper rendering order
                sprite.setZ(layer.zOrder);

                sprite.setRotation(rotation);
                sprite.setScale(scaleX, scaleY);

                // Render the sprite
                sprite.render(viewProj);
            }
        }
    }

    @Override
    public float getZ() {
        return layers.stream()
                .map(Layer::getZOrder)
                .min(Float::compare)
                .orElse(0.0f);
    }

    public List<Layer> getLayers() {
        return new ArrayList<>(layers);
    }

    /**
     * Inner class to hold layer data
     */
    public class Layer {
        private String layerName;
        private String spriteName;
        private Animation animation;
        private float offsetX, offsetY;
        private float zOrder; // Higher values = drawn on top
        private boolean isPlaying = true;

        // Cache for sprite lookup optimization
        private Sprite cachedSprite;
        private String cachedSpriteName;

        // Layer appearance
        private String[] palette;
        private int color = 0xFFFFFF;
        private float alpha = 1.0f;

        public Layer(String layerName, String spriteName, Animation animation,
                     float offsetX, float offsetY, float zOrder) {
            this.layerName = layerName;
            this.spriteName = spriteName;
            this.animation = animation;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.zOrder = zOrder;
        }

        public String getLayerName() {
            return layerName;
        }

        public String getSpriteName() {
            return spriteName;
        }

        public Animation getAnimation() {
            return animation;
        }

        public float getOffsetX() {
            return offsetX;
        }

        public float getOffsetY() {
            return offsetY;
        }

        public float getZOrder() {
            return zOrder;
        }

        public boolean isPlaying() {
            return isPlaying;
        }

        public void setPlaying(boolean playing) {
            isPlaying = playing;
        }
    }
}