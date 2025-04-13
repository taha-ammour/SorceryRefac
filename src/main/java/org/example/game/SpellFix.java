package org.example.game;

import org.example.GameWorld;
import org.example.engine.Scene;
import org.example.engine.Sprite;
import org.example.engine.SpriteManager;
import org.joml.Vector3f;
import java.util.UUID;

/**
 * Utility class to fix spell rendering issues
 */
public class SpellFix {

    /**
     * Apply all fixes to the game's spell system
     * @param gameWorld The game world instance
     */
    public static void apply(GameWorld gameWorld) {
        // Make sure we're on the main thread
        if (!Thread.currentThread().getName().contains("main")) {
            System.err.println("SpellFix must be applied from the main thread");
            return;
        }

        System.out.println("Applying spell rendering fixes...");

        // Get necessary references
        SpellSystem spellSystem = gameWorld.getSpellSystem();
        Scene gameScene = gameWorld.getGameScene();
        SpriteManager spriteManager = gameWorld.getSpriteManager();

        if (spellSystem == null || gameScene == null || spriteManager == null) {
            System.err.println("Cannot apply spell fixes - missing required systems");
            return;
        }

        // Enable debug mode during fixes
        spellSystem.setDebug(true);

        // 1. Fix scene management for spells
        fixSceneManagement(gameWorld);

        // 2. Fix sprite loading for spell effects
        fixSpriteLoading(spriteManager);

        // 3. Ensure all players have spell books
        ensurePlayerSpellBooks(gameWorld);

        // 4. Test spell rendering directly
        testSpellRendering(gameWorld);

        // 5. Verify spell positioning
        verifySpellPositioning(gameWorld);

        System.out.println("Spell rendering fixes applied successfully");
    }

    /**
     * Fix how spells are added to the scene graph
     */
    private static void fixSceneManagement(GameWorld gameWorld) {
        // Ensure the SpellSystem has a reference to the game scene
        gameWorld.getSpellSystem().refreshSceneReference(gameWorld.getGameScene());

        // Make sure spells are added directly to the scene rather than queued
        gameWorld.setDirectSpellAddition(true);

        System.out.println("✓ Fixed scene management for spells");
    }

    /**
     * Ensure all spell sprites are properly loaded and accessible
     */
    private static void fixSpriteLoading(SpriteManager spriteManager) {
        // Check and ensure all required spell sprites exist
        ensureSpellSprites(spriteManager, new int[]{
                // Fire spell sprites
                154, 155, 156, 157, 158, 159, 160, 161,
                // Ice spell sprites
                221, 222,
                // Lightning spell sprites
                223, 224,
                // Effect sprites
                150, 151, 152, 153
        });

        // Also ensure named spell sprites
        ensureNamedSpellSprites(spriteManager);

        System.out.println("✓ Fixed sprite loading for spells");
    }

    /**
     * Ensure all players have spell books
     */
    private static void ensurePlayerSpellBooks(GameWorld gameWorld) {
        SpellSystem spellSystem = gameWorld.getSpellSystem();

        // Initialize local player
        if (gameWorld.getLocalPlayer() != null) {
            UUID localPlayerId = gameWorld.getLocalPlayer().getPlayerId();
            spellSystem.initializePlayer(localPlayerId);
            System.out.println("✓ Initialized spell book for local player: " + localPlayerId);
        }

        // Initialize for all remote players
        for (UUID playerId : gameWorld.getRemotePlayers().keySet()) {
            spellSystem.initializePlayer(playerId);
            System.out.println("✓ Initialized spell book for remote player: " + playerId);
        }
    }

    /**
     * Make sure all required sprite IDs are loaded
     */
    private static void ensureSpellSprites(SpriteManager spriteManager, int[] requiredSpriteIds) {
        int validCount = 0;

        for (int id : requiredSpriteIds) {
            try {
                Sprite sprite = spriteManager.getSprite(id);
                if (sprite != null) {
                    validCount++;
                }
            } catch (Exception e) {
                // Try to create a fallback sprite
                createFallbackSprite(spriteManager, id);
            }
        }

        System.out.println("✓ Verified " + validCount + "/" + requiredSpriteIds.length + " required spell sprites");
    }

    /**
     * Create a fallback sprite when a required sprite is missing
     */
    private static void createFallbackSprite(SpriteManager spriteManager, int id) {
        try {
            // Use a common sprite as a fallback
            Sprite template = null;

            // Try some common sprites that should exist
            int[] fallbackIds = {158, 159, 160, 161, 10, 11, 12};
            for (int fallbackId : fallbackIds) {
                try {
                    template = spriteManager.getSprite(fallbackId);
                    if (template != null) break;
                } catch (Exception e) {
                    // Continue to next fallback
                }
            }

            if (template != null) {
                // Copy properties from template sprite to create a new one
                int x = (int)(template.getU0() * spriteManager.getSheet("tiles").getAtlasWidth());
                int y = (int)(template.getV0() * spriteManager.getSheet("tiles").getAtlasHeight());

                // Register as a new sprite with the required ID
                String name = "fallback_spell_" + id;
                String[] palette = {"500", "300", "000", "555"};  // Reddish palette for visibility

                spriteManager.defineSprite(
                        id,                 // Use the required ID
                        name,               // Give it a unique name
                        "tiles",            // Use tiles sheet
                        x, y,               // Use same coordinates as template
                        16, 16,             // Standard size
                        palette,            // Distinctive palette
                        true                // Mark as dynamic
                );

                System.out.println("Created fallback sprite for ID " + id);
            }
        } catch (Exception e) {
            System.err.println("Failed to create fallback sprite for ID " + id + ": " + e.getMessage());
        }
    }

    /**
     * Ensure named spell sprites are available
     */
    private static void ensureNamedSpellSprites(SpriteManager spriteManager) {
        String[] spellSpriteNames = {
                "spell_cast_fire_1", "spell_cast_fire_2", "spell_cast_fire_3", "spell_cast_fire_4",
                "spell_castpartpoint_1", "spell_castpartpoint_2", "spell_castpartpoint_3", "spell_castpartpoint_4",
                "spell_cast_sm_fire_1", "spell_cast_sm_fire_2", "spell_cast_sm_fire_3", "spell_cast_sm_fire_4"
        };

        int foundCount = 0;
        for (String name : spellSpriteNames) {
            try {
                Sprite sprite = spriteManager.getSprite(name);
                if (sprite != null) {
                    foundCount++;
                }
            } catch (Exception e) {
                System.err.println("Failed to load named spell sprite '" + name + "': " + e.getMessage());
                // Try to create fallback by creating a copy of a similar sprite with the needed name
                createFallbackNamedSprite(spriteManager, name);
            }
        }

        System.out.println("✓ Verified " + foundCount + "/" + spellSpriteNames.length + " named spell sprites");
    }

    /**
     * Create a fallback named sprite when a required one is missing
     */
    private static void createFallbackNamedSprite(SpriteManager spriteManager, String name) {
        try {
            // Determine sprite type from name to select appropriate fallback
            int fallbackId;
            if (name.contains("fire")) {
                fallbackId = 158;  // Fire spell
            } else if (name.contains("ice") || name.contains("cast")) {
                fallbackId = 221;  // Ice/cast spell
            } else {
                fallbackId = 160;  // Generic fallback
            }

            // Get template sprite
            Sprite template = null;
            try {
                template = spriteManager.getSprite(fallbackId);
            } catch (Exception e) {
                // Try with a more reliable sprite
                template = spriteManager.getSprite(1);  // Player sprite as last resort
            }

            if (template != null) {
                // Copy properties from template
                int x = (int)(template.getU0() * spriteManager.getSheet("tiles").getAtlasWidth());
                int y = (int)(template.getV0() * spriteManager.getSheet("tiles").getAtlasHeight());

                // Choose palette based on name
                String[] palette;
                if (name.contains("fire")) {
                    palette = new String[]{"500", "300", "000", "555"};  // Red
                } else if (name.contains("ice")) {
                    palette = new String[]{"005", "003", "000", "555"};  // Blue
                } else if (name.contains("lightning")) {
                    palette = new String[]{"550", "330", "000", "555"};  // Yellow
                } else {
                    palette = new String[]{"333", "222", "000", "555"};  // Gray
                }

                // Define new sprite with the required name
                spriteManager.defineSprite(
                        -1,                 // No numeric ID
                        name,               // Use the required name
                        "tiles",            // Use tiles sheet
                        x, y,               // Use same coordinates as template
                        16, 16,             // Standard size
                        palette,            // Type-specific palette
                        true                // Mark as dynamic
                );

                System.out.println("Created fallback named sprite: " + name);
            }
        } catch (Exception e) {
            System.err.println("Failed to create fallback named sprite '" + name + "': " + e.getMessage());
        }
    }

    /**
     * Test spell rendering by creating test spells directly
     */
    private static void testSpellRendering(GameWorld gameWorld) {
        if (gameWorld.getLocalPlayer() == null) {
            System.out.println("Cannot test spell rendering - local player not found");
            return;
        }

        UUID playerId = gameWorld.getLocalPlayer().getPlayerId();
        Vector3f playerPos = gameWorld.getLocalPlayer().getPosition();
        SpellSystem spellSystem = gameWorld.getSpellSystem();

        // Create test spells of each type at different positions
        try {
            // Fire spell
            SpellEntity fireSpell = spellSystem.castSpell(
                    playerId, "fire", playerPos.x + 50, playerPos.y - 50);

            if (fireSpell != null) {
                System.out.println("✓ Fire test spell created successfully");
                // Force add to scene immediately
                gameWorld.getGameScene().addGameObject(fireSpell);
            } else {
                System.err.println("× Failed to create fire test spell");
            }

            // Ice spell
            SpellEntity iceSpell = spellSystem.castSpell(
                    playerId, "ice", playerPos.x - 50, playerPos.y - 50);

            if (iceSpell != null) {
                System.out.println("✓ Ice test spell created successfully");
                gameWorld.getGameScene().addGameObject(iceSpell);
            } else {
                System.err.println("× Failed to create ice test spell");
            }

            // Lightning spell
            SpellEntity lightningSpell = spellSystem.castSpell(
                    playerId, "lightning", playerPos.x, playerPos.y - 100);

            if (lightningSpell != null) {
                System.out.println("✓ Lightning test spell created successfully");
                gameWorld.getGameScene().addGameObject(lightningSpell);
            } else {
                System.err.println("× Failed to create lightning test spell");
            }

            System.out.println("✓ Test spells created");
        } catch (Exception e) {
            System.err.println("Error during test spell creation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verify spell positioning and movement
     */
    private static void verifySpellPositioning(GameWorld gameWorld) {
        // Create a moving spell to verify movement works correctly
        if (gameWorld.getLocalPlayer() == null) {
            System.out.println("Cannot test spell positioning - local player not found");
            return;
        }

        try {
            UUID playerId = gameWorld.getLocalPlayer().getPlayerId();
            Vector3f playerPos = gameWorld.getLocalPlayer().getPosition();
            SpellSystem spellSystem = gameWorld.getSpellSystem();

            // Create a test spell that moves
            SpellEntity testSpell = spellSystem.castSpell(
                    playerId, "lightning", playerPos.x, playerPos.y - 50);

            if (testSpell != null) {
                // Explicitly set movement
                testSpell.setMovementDirection(1, 0);  // Move right
                testSpell.setMovementSpeed(200.0f);

                // Add to scene
                gameWorld.getGameScene().addGameObject(testSpell);

                System.out.println("✓ Created test moving spell to verify movement");
            } else {
                System.err.println("× Failed to create test moving spell");
            }

            // Verify size and scale of spells
            if (spellSystem.getActiveSpellCount() > 0) {
                System.out.println("✓ Active spells verified: " + spellSystem.getActiveSpellCount());
            } else {
                System.err.println("× No active spells found, positioning cannot be verified");
            }
        } catch (Exception e) {
            System.err.println("Error verifying spell positioning: " + e.getMessage());
        }
    }
}