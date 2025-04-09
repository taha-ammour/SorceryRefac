package org.example.game;

import org.example.GameWorld;
import org.example.engine.Scene;
import org.example.engine.Sprite;
import org.example.engine.SpriteManager;
import org.example.game.SpellEntity;
import org.example.game.SpellSystem;
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
        SpriteManager spriteManager = gameWorld.getSpriteManager(); // Use gameWorld's getSpriteManager instead

        if (spellSystem == null || gameScene == null || spriteManager == null) {
            System.err.println("Cannot apply spell fixes - missing required systems");
            return;
        }

        // 1. Fix scene management for spells
        fixSceneManagement(gameWorld);

        // 2. Fix sprite loading for spell effects
        fixSpriteLoading(spriteManager);

        // 3. Test spell rendering directly
        testSpellRendering(gameWorld);

        System.out.println("Spell rendering fixes applied successfully");
    }

    /**
     * Fix how spells are added to the scene graph
     */
    private static void fixSceneManagement(GameWorld gameWorld) {
        // Ensure the SpellManager has a reference to the game scene
        gameWorld.getSpellSystem().refreshSceneReference(gameWorld.getGameScene());

        // Make sure spells are added directly to the scene rather than queued
        gameWorld.setDirectSpellAddition(true);
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
    }

    /**
     * Make sure all required sprite IDs are loaded
     */
    private static void ensureSpellSprites(SpriteManager spriteManager, int[] requiredSpriteIds) {
        for (int id : requiredSpriteIds) {
            try {
                Sprite sprite = spriteManager.getSprite(id);
                System.out.println("Verified sprite ID " + id + " exists");
            } catch (Exception e) {
                System.err.println("Failed to load required spell sprite ID " + id + ": " + e.getMessage());
            }
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

        for (String name : spellSpriteNames) {
            try {
                Sprite sprite = spriteManager.getSprite(name);
                System.out.println("Verified sprite '" + name + "' exists");
            } catch (Exception e) {
                System.err.println("Failed to load named spell sprite '" + name + "': " + e.getMessage());
            }
        }
    }

    /**
     * Test spell rendering by creating a test spell directly
     */
    private static void testSpellRendering(GameWorld gameWorld) {
        if (gameWorld.getLocalPlayer() == null) {
            System.out.println("Cannot test spell rendering - local player not found");
            return;
        }

        UUID playerId = gameWorld.getLocalPlayer().getPlayerId();
        Vector3f playerPos = gameWorld.getLocalPlayer().getPosition();

        // Create a test spell at the player's position
        SpellEntity testSpell = gameWorld.getSpellSystem().castSpell(
                playerId, "fire", playerPos.x + 50, playerPos.y - 50);

        if (testSpell != null) {
            System.out.println("Test spell created successfully at " + playerPos.x + "," + playerPos.y);

            // Force add to scene immediately
            gameWorld.getGameScene().addGameObject(testSpell);
        } else {
            System.err.println("Failed to create test spell");
        }
    }
}