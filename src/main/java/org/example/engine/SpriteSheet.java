package org.example.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps a large atlas texture and provides sub-sprites by pixel region.
 * The atlas is loaded from a classpath resource with color keying applied by default.
 */
public class SpriteSheet {
    private final Texture atlasTexture;
    private final int atlasWidth;
    private final int atlasHeight;

    // Cache key will now include additional parameters to differentiate between similar sprites
    private final Map<String, Sprite> spriteCache = new HashMap<>();

    /**
     * Loads the texture atlas from a classpath resource.
     * @param resourcePath Resource path (e.g., "/textures/entities.png")
     */
    public SpriteSheet(String resourcePath) {
        // Apply color keying so that any pink pixels become transparent.
        atlasTexture = new Texture(resourcePath, false, 0xFFFFFF);
        atlasWidth = atlasTexture.getWidth();
        atlasHeight = atlasTexture.getHeight();
    }

    /**
     * Retrieves a sub-region of the atlas as a Sprite.
     * @param x The left pixel coordinate in the atlas.
     * @param y The top pixel coordinate in the atlas.
     * @param width The width of the region in pixels.
     * @param height The height of the region in pixels.
     * @return A fresh Sprite referencing that sub-region.
     */
    public Sprite getSprite(int x, int y, int width, int height) {
        return getSprite(x, y, width, height, true);
    }

    /**
     * Retrieves a sub-region of the atlas as a Sprite with caching control.
     * @param x The left pixel coordinate in the atlas.
     * @param y The top pixel coordinate in the atlas.
     * @param width The width of the region in pixels.
     * @param height The height of the region in pixels.
     * @param useCache Whether to use/store the sprite in cache.
     * @return A Sprite referencing that sub-region.
     */
    public Sprite getSprite(int x, int y, int width, int height, boolean useCache) {
        String key = x + ":" + y + ":" + width + ":" + height;

        if (useCache && spriteCache.containsKey(key)) {
            return spriteCache.get(key);
        }

        // Convert pixel coordinates to normalized UV coordinates [0,1]
        float u0 = x / (float) atlasWidth;
        float v0 = y / (float) atlasHeight;
        float u1 = (x + width) / (float) atlasWidth;
        float v1 = (y + height) / (float) atlasHeight;

        Sprite sprite = new Sprite(atlasTexture, u0, v0, u1, v1, width, height);

        if (useCache) {
            spriteCache.put(key, sprite);
        }

        return sprite;
    }

    /**
     * Removes a specific sprite from the cache.
     * @param x The left pixel coordinate in the atlas.
     * @param y The top pixel coordinate in the atlas.
     * @param width The width of the region in pixels.
     * @param height The height of the region in pixels.
     */
    public void invalidateSprite(int x, int y, int width, int height) {
        String key = x + ":" + y + ":" + width + ":" + height;
        spriteCache.remove(key);
    }

    /**
     * Convenience method for extracting a sprite from a regular grid.
     * @param col The column index (starting at 0).
     * @param row The row index (starting at 0).
     * @param tileWidth The width of each tile in pixels.
     * @param tileHeight The height of each tile in pixels.
     * @return A Sprite representing the tile at the specified grid location.
     */
    public Sprite getSpriteByGrid(int col, int row, int tileWidth, int tileHeight) {
        int x = col * tileWidth;
        int y = row * tileHeight;
        return getSprite(x, y, tileWidth, tileHeight);
    }

    /**
     * Convenience method for extracting a sprite from a regular grid with caching control.
     * @param col The column index (starting at 0).
     * @param row The row index (starting at 0).
     * @param tileWidth The width of each tile in pixels.
     * @param tileHeight The height of each tile in pixels.
     * @param useCache Whether to use/store the sprite in cache.
     * @return A Sprite representing the tile at the specified grid location.
     */
    public Sprite getSpriteByGrid(int col, int row, int tileWidth, int tileHeight, boolean useCache) {
        int x = col * tileWidth;
        int y = row * tileHeight;
        return getSprite(x, y, tileWidth, tileHeight, useCache);
    }

    /**
     * Returns the atlas texture.
     * @return The Texture used by the sprite sheet.
     */
    public Texture getAtlasTexture() {
        return atlasTexture;
    }

    public void bindTexture() {
        getAtlasTexture().bind();
    }

    public int getAtlasHeight() {
        return atlasHeight;
    }

    public int getAtlasWidth() {
        return atlasWidth;
    }

    /**
     * Clears the entire sprite cache.
     */
    public void clearCache() {
        spriteCache.clear();
    }
}