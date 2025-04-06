package org.example.engine;

/**
 * Extension for SpriteManager to add functionality for Sprite copying and dynamic sprite creation
 */
public class SpriteManagerExtension {

    /**
     * Creates a new sprite with the given parameters
     *
     * @param spriteManager The sprite manager to use
     * @param name The name for the new sprite
     * @param sheetName The sprite sheet name
     * @param x X coordinate on the sprite sheet
     * @param y Y coordinate on the sprite sheet
     * @param width Width of the sprite
     * @param height Height of the sprite
     * @param palette Color palette for the sprite
     * @return The created sprite
     */
    public static Sprite createSprite(SpriteManager spriteManager, String name, String sheetName,
                                      int x, int y, int width, int height, String[] palette) {
        // Define sprite
        spriteManager.defineSprite(-1, name, sheetName, x, y, width, height, palette, true);

        // Get the sprite
        return spriteManager.getSprite(name);
    }

    /**
     * Creates a sprite copy with a new name
     *
     * @param spriteManager The sprite manager to use
     * @param originalName Original sprite name
     * @param newName New sprite name
     * @param palette New palette (or null to keep original)
     * @return The created sprite
     */
    public static Sprite copySprite(SpriteManager spriteManager, String originalName, String newName, String[] palette) {
        try {
            // Get original sprite
            Sprite originalSprite = spriteManager.getSprite(originalName);

            // Get sprite sheet information
            SpriteSheet sheet = spriteManager.getSheet("entities"); // Assuming "entities" sheet

            // Calculate pixel coordinates from UV
            int atlasWidth = sheet.getAtlasWidth();
            int atlasHeight = sheet.getAtlasHeight();

            int x = (int)(originalSprite.getU0() * atlasWidth);
            int y = (int)(originalSprite.getV0() * atlasHeight);

            // Calculate width and height from UV
            int width = (int)((originalSprite.getU1() - originalSprite.getU0()) * atlasWidth);
            int height = (int)((originalSprite.getV1() - originalSprite.getV0()) * atlasHeight);

            // Use original palette if not provided
            String[] finalPalette = palette;
            if (finalPalette == null) {
                // This is a placeholder - we don't have direct access to sprite's palette
                // In a real implementation, you would extract palette from the original sprite
                finalPalette = new String[]{"000", "333", "222", "555"};
            }

            // Create and return the new sprite
            return createSprite(spriteManager, newName, "entities", x, y, width, height, finalPalette);

        } catch (Exception e) {
            System.err.println("Failed to copy sprite: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a sprite specifically for spell effects
     */
    public static Sprite createSpellSprite(SpriteManager spriteManager, String spellType, int frameIndex) {
        // Define coordinates based on spell type and frame
        int x = 0;
        int y = 0;
        String sheetName = "tiles"; // Spell effects are typically on the tiles sheet
        String[] palette;

        switch(spellType.toLowerCase()) {
            case "fire":
                // Apply bright fire colors
                palette = new String[]{"000", "500", "530", "550"};

                // Small fire spell animation frames (small size)
                if (frameIndex >= 0 && frameIndex < 2) {
                    x = (frameIndex * 8) + 32;
                    y = 40;
                }
                // Large fire spell animation frames
                else {
                    x = ((frameIndex - 2) * 8);
                    y = 32;
                }
                break;

            case "ice":
                // Apply ice blue colors
                palette = new String[]{"000", "035", "045", "055"};

                // Use spread spell effects
                x = 24 + (frameIndex % 2) * 8;
                y = 72 - (frameIndex / 2) * 8;
                break;

            case "lightning":
                // Apply lightning colors
                palette = new String[]{"000", "550", "551", "555"};

                // Use lightning-like effects
                x = 48 + (frameIndex % 2) * 8;
                y = 72 - (frameIndex / 2) * 8;
                break;

            default:
                // Default to a generic spell effect with neutral colors
                palette = new String[]{"000", "333", "444", "555"};
                x = 0;
                y = 64;
        }

        // Create unique sprite name for this frame
        String spriteName = "spell_" + spellType + "_" + frameIndex + "_" + System.currentTimeMillis();

        System.out.println("Creating spell sprite: " + spriteName + " at " + x + "," + y + " with palette for " + spellType);

        try {
            // Create a new sprite instance with the specified parameters
            Sprite sprite = createSprite(spriteManager, spriteName, sheetName, x, y, 8, 8, palette);

            // Additional setup to ensure visibility
            if (sprite != null) {
                sprite.setZ(-30.0f);  // Set high Z for visibility
                sprite.setScale(3.0f, 3.0f);  // Make it larger
            }

            return sprite;
        } catch (Exception e) {
            System.err.println("Error creating spell sprite: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}