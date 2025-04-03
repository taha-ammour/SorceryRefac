package org.example.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages sprite definitions and sprite sheets.
 * You can load sprite sheets, define sub-sprites (SpriteDefinition)
 * and then create new Sprite instances by either a unique ID or name.
 */
public class SpriteManager {
    // Map of loaded sprite sheets by name.
    private final Map<String, SpriteSheet> sheets = new HashMap<>();
    // Map of sprite definitions by integer ID and by string name.
    private final Map<Integer, SpriteDefinition> definitionsById = new HashMap<>();
    private final Map<String, SpriteDefinition> definitionsByName = new HashMap<>();

    // Map to track which sprites should always be created fresh (not from cache)
    private final Map<String, Boolean> dynamicSpriteFlags = new HashMap<>();

    /**
     * Loads a sprite sheet from a resource path and associates it with the given name.
     * Example: loadSpriteSheet("entities", "/textures/entities.png");
     */
    public void loadSpriteSheet(String sheetName, String resourcePath) {
        SpriteSheet sheet = new SpriteSheet(resourcePath);
        sheets.put(sheetName, sheet);
    }

    /**
     * Defines a sprite sub-region.
     *
     * @param id           Unique integer id (use -1 if not needed)
     * @param name         Unique name (or null if not needed)
     * @param sheetName    Name of the sprite sheet (must be loaded)
     * @param x            Left coordinate (pixels) in the sprite sheet.
     * @param y            Top coordinate (pixels) in the sprite sheet.
     * @param width        Width in pixels.
     * @param height       Height in pixels.
     * @param paletteCodes Array of 4 three-digit strings (e.g., {"100","250","000","555"})
     */
    public void defineSprite(int id, String name, String sheetName,
                             int x, int y, int width, int height, String[] paletteCodes) {
        defineSprite(id, name, sheetName, x, y, width, height, paletteCodes, false);
    }

    /**
     * Defines a sprite sub-region with dynamic flag control.
     *
     * @param id           Unique integer id (use -1 if not needed)
     * @param name         Unique name (or null if not needed)
     * @param sheetName    Name of the sprite sheet (must be loaded)
     * @param x            Left coordinate (pixels) in the sprite sheet.
     * @param y            Top coordinate (pixels) in the sprite sheet.
     * @param width        Width in pixels.
     * @param height       Height in pixels.
     * @param paletteCodes Array of 4 three-digit strings (e.g., {"100","250","000","555"})
     * @param isDynamic    If true, sprite will always be freshly created (not cached)
     */
    public void defineSprite(int id, String name, String sheetName,
                             int x, int y, int width, int height, String[] paletteCodes, boolean isDynamic) {
        SpriteDefinition def = new SpriteDefinition(sheetName, x, y, width, height, paletteCodes);
        if (id >= 0) {
            definitionsById.put(id, def);
        }
        if (name != null && !name.isEmpty()) {
            definitionsByName.put(name, def);
            // Mark sprite as dynamic if needed
            if (isDynamic) {
                dynamicSpriteFlags.put(name, true);
            }
        }
    }

    /**
     * Mark an existing sprite definition as dynamic (always created fresh, not cached)
     * @param name The name of the sprite
     */
    public void markSpriteAsDynamic(String name) {
        if (!definitionsByName.containsKey(name)) {
            throw new IllegalArgumentException("No sprite defined for name: " + name);
        }
        dynamicSpriteFlags.put(name, true);
    }

    /**
     * Creates and returns a new Sprite instance based on the definition identified by name.
     */
    public Sprite getSprite(String name) {
        SpriteDefinition def = definitionsByName.get(name);
        if (def == null) {
            throw new IllegalArgumentException("No sprite defined for name: " + name);
        }

        // Check if this sprite is marked as dynamic (should not use cache)
        boolean useCache = !dynamicSpriteFlags.getOrDefault(name, false);
        return createSpriteFromDefinition(def, useCache);
    }

    /**
     * Creates and returns a new Sprite instance based on the definition identified by id.
     */
    public Sprite getSprite(int id) {
        SpriteDefinition def = definitionsById.get(id);
        if (def == null) {
            throw new IllegalArgumentException("No sprite defined for id: " + id);
        }
        return createSpriteFromDefinition(def, true);
    }

    /**
     * Internal helper that creates a new Sprite using the given SpriteDefinition.
     * Automatically applies its palette.
     *
     * @param def The sprite definition to use
     * @param useCache Whether to use the sprite cache
     * @return A new or cached Sprite instance
     */
    private Sprite createSpriteFromDefinition(SpriteDefinition def, boolean useCache) {
        SpriteSheet sheet = sheets.get(def.sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("SpriteSheet '" + def.sheetName + "' is not loaded.");
        }

        Sprite sprite = sheet.getSprite(def.x, def.y, def.width, def.height, useCache);
        sprite.setPaletteFromCodes(def.paletteCodes);
        return sprite;
    }

    /**
     * Invalidates the cached sprite for the given name.
     * Next call to getSprite will create a fresh instance.
     */
    public void invalidateSprite(String name) {
        SpriteDefinition def = definitionsByName.get(name);
        if (def == null) {
            throw new IllegalArgumentException("No sprite defined for name: " + name);
        }

        SpriteSheet sheet = sheets.get(def.sheetName);
        if (sheet == null) {
            return;
        }

        sheet.invalidateSprite(def.x, def.y, def.width, def.height);
    }

    /**
     * Returns the total number of sprite definitions defined by ID.
     */
    public int getSpriteDefinitionCount() {
        return definitionsById.size();
    }

    /**
     * Returns the total number of sheets loaded.
     */
    public int getSheetCount() {
        return sheets.size();
    }

    /**
     * Gets a loaded sheet by name (e.g., "entities") if it exists.
     */
    public SpriteSheet getSheet(String sheetName) {
        SpriteSheet sheet = sheets.get(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("SpriteSheet '" + sheetName + "' is not loaded.");
        }
        return sheet;
    }

    /**
     * Updates the palette for a sprite definition.
     */
    public void updateDefinitionPalette(String spriteName, String[] newPalette) {
        SpriteDefinition def = definitionsByName.get(spriteName);
        if (def == null) {
            throw new IllegalArgumentException("No sprite defined for name: " + spriteName);
        }
        def.paletteCodes = newPalette;

        // Invalidate the sprite in the cache to ensure it's recreated with the new palette
        invalidateSprite(spriteName);
    }

    /**
     * Clears all sprite caches across all sheets.
     */
    public void clearAllCaches() {
        for (SpriteSheet sheet : sheets.values()) {
            sheet.clearCache();
        }
    }
}