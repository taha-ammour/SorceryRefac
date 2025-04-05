package org.example.engine;

import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LayeredCharacter extends GameObject implements ZOrderProvider {
    private final SpriteManager spriteManager;
    private final List<Layer> layers = new ArrayList<>();
    private float x, y, z; // character position; z controls overall draw order
    private float scaleX = 1.0f; // Added scale for X axis
    private float scaleY = 1.0f; // Added scale for Y axis

    // Fields to track movement and direction.
    private boolean isMoving = false;
    // A direction string used for static layers (e.g. body sprite).
    private String currentDirection = "player_sprite_d"; // default direction

    public LayeredCharacter(SpriteManager manager) {
        this.spriteManager = manager;
    }

    /**
     * Adds a layer with an Animation.
     */
    public void addLayer(String layerName, Animation animation, float offsetX, float offsetY, float offsetZ) {
        Layer layer = new Layer(layerName, animation, offsetX, offsetY, offsetZ);
        layers.add(layer);

        // Sort layers by z-order after adding
        sortLayersByZ();
    }

    /**
     * Adds a static layer.
     */
    public void addLayer(String layerName, String staticSpriteName, float offsetX, float offsetY, float offsetZ) {
        // Mark sprite as dynamic if it's a directional sprite to prevent caching issues
        if (staticSpriteName.contains("_d") || staticSpriteName.contains("_u") ||
                staticSpriteName.contains("_l") || staticSpriteName.contains("_r")) {
            spriteManager.markSpriteAsDynamic(staticSpriteName);
        }

        Layer layer = new Layer(layerName, staticSpriteName, offsetX, offsetY, offsetZ);
        layers.add(layer);

        // Sort layers by z-order after adding
        sortLayersByZ();
    }

    /**
     * Sort layers by z-offset for proper rendering order
     */
    private void sortLayersByZ() {
        layers.sort(Comparator.comparingDouble(layer -> layer.offsetZ));
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Set the scale of the character (applies to all layers)
     * @param scaleX X-axis scale factor
     * @param scaleY Y-axis scale factor
     */
    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    // Allow setting movement state.
    public void setMoving(boolean moving) {
        this.isMoving = moving;
    }

    // Set the current direction (for static layers that depend on direction).
    public void setDirection(String directionSpriteName) {
        if (directionSpriteName.equals(this.currentDirection)) {
            return; // No change needed
        }

        this.currentDirection = directionSpriteName;

        // For each layer that depends on direction, update its sprite
        for (Layer layer : layers) {
            if (layer.layerName.equalsIgnoreCase("body")) {
                layer.staticSpriteName = directionSpriteName;

                // Make sure this sprite is marked as dynamic
                spriteManager.markSpriteAsDynamic(directionSpriteName);

                // Invalidate cached sprite
                if (layer.cachedSprite != null) {
                    layer.cachedSprite = null;
                    layer.cachedSpriteName = null;
                }
            }
        }
    }

    // Set a layer's z offset individually.
    public void setLayerZOffset(String layerName, float offsetZ) {
        for (Layer layer : layers) {
            if (layer.layerName.equalsIgnoreCase(layerName)) {
                layer.offsetZ = offsetZ;
            }
        }
        // Resort layers to maintain correct drawing order
        sortLayersByZ();
    }

    @Override
    public void update(float deltaTime) {
        // Update each layer's animation only if moving.
        for (Layer layer : layers) {
            if (layer.animation != null && isMoving) {
                layer.animation.update(deltaTime);
                // Invalidate cached sprite since animation frame changed
                layer.cachedSprite = null;
                layer.cachedSpriteName = null;
            }
        }
    }

    @Override
    public void render(Matrix4f viewProj) {
        // Render each layer in order of z-offset
        for (Layer layer : layers) {
            Sprite sprite = null;
            String spriteName = null;

            // If this layer is animated, use the current frame.
            if (layer.animation != null) {
                spriteName = layer.animation.getCurrentFrameName();

                // Only get a new sprite if the sprite changed or cache is invalid
                if (!spriteName.equals(layer.cachedSpriteName) || layer.cachedSprite == null) {
                    sprite = spriteManager.getSprite(spriteName);
                    layer.cachedSpriteName = spriteName;
                    layer.cachedSprite = sprite;
                } else {
                    sprite = layer.cachedSprite;
                }
            } else {
                // For static layers
                spriteName = layer.staticSpriteName;

                // Only get a new sprite if the sprite changed or cache is invalid
                if (!spriteName.equals(layer.cachedSpriteName) || layer.cachedSprite == null) {
                    sprite = spriteManager.getSprite(spriteName);
                    layer.cachedSpriteName = spriteName;
                    layer.cachedSprite = sprite;
                } else {
                    sprite = layer.cachedSprite;
                }
            }

            // Set sprite position and draw order based on character and layer offsets.
            sprite.setPosition(x + layer.offsetX, y + layer.offsetY);
            sprite.setZ(z + layer.offsetZ);

            // Apply character scale to the sprite
            sprite.setScale(scaleX, scaleY);

            sprite.render(viewProj);
        }
    }

    @Override
    public void cleanup() {
        // Clean up layers
        for (Layer layer : layers) {
            if (layer.cachedSprite != null) {
                // Note: actual sprite cleanup should be handled by SpriteManager/SpriteSheet
                layer.cachedSprite = null;
            }
        }
        layers.clear();
    }

    // Get the character's overall z value (for scene sorting)
    @Override
    public float getZ() {
        return z;
    }

    public void setFlipX(boolean flip) {
        // Apply flip to all layers
        for (Layer layer : layers) {
            if (layer.cachedSprite != null) {
                layer.cachedSprite.setFlipX(flip);
            }
        }
    }

    public void setFlipY(boolean flip) {
        // Apply flip to all layers
        for (Layer layer : layers) {
            if (layer.cachedSprite != null) {
                layer.cachedSprite.setFlipY(flip);
            }
        }
    }

    // Get the character's current scale
    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    private static class Layer {
        String layerName;
        Animation animation;
        String staticSpriteName;
        float offsetX, offsetY, offsetZ;

        // Cached sprite information to avoid repeated lookups.
        Sprite cachedSprite;
        String cachedSpriteName;

        // Constructor for animated layer.
        Layer(String layerName, Animation animation, float offsetX, float offsetY, float offsetZ) {
            this.layerName = layerName;
            this.animation = animation;
            this.staticSpriteName = null;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
        }

        // Constructor for static layer.
        Layer(String layerName, String staticSpriteName, float offsetX, float offsetY, float offsetZ) {
            this.layerName = layerName;
            this.animation = null;
            this.staticSpriteName = staticSpriteName;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
        }
    }
}