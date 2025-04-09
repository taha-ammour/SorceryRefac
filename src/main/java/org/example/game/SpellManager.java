package org.example.game;

import org.example.engine.LayeredCharacter;
import org.example.engine.Scene;
import org.example.engine.Sprite;
import org.example.engine.SpriteManager;
import org.example.game.Spells.AbstractSpell;
import org.example.game.Spells.PlayerSpellBook;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to manage spell casting and rendering
 */
public class SpellManager {
    private final SpriteManager spriteManager;
    private final Map<String, PlayerSpellBook> playerSpellBooks;
    private final List<SpellEntity> activeSpells;
    private Scene gameScene;

    public SpellManager(SpriteManager spriteManager) {
        this.spriteManager = spriteManager;
        this.playerSpellBooks = new HashMap<>();
        this.activeSpells = new ArrayList<>();
    }

    /**
     * Set the game scene for spell entity management
     */
    public void setGameScene(Scene scene) {
        this.gameScene = scene;
    }

    /**
     * Create a spell book for a player
     */
    public void createPlayerSpellBook(String playerId) {
        playerSpellBooks.put(playerId, new PlayerSpellBook(playerId));
    }

    /**
     * Cast a spell for a player, creating and adding the spell entity to the game
     */
    public SpellEntity castSpell(String playerId, String spellType, float x, float y) {
        // Get the player's spell book
        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);
        if (spellBook == null) {
            System.out.println("No spell book found for player: " + playerId);
            spellBook = new PlayerSpellBook(playerId);
            playerSpellBooks.put(playerId, spellBook);
        }

        // Get the spell from the spell book
        AbstractSpell spell = spellBook.getSpell(spellType);
        if (spell == null) {
            System.out.println("Player " + playerId + " does not have " + spellType + " spell");
            return null;
        }

        System.out.println("Casting " + spellType + " spell for player " + playerId + " at " + x + "," + y);

        try {
            // Create a spell entity with LayeredCharacter for better visibility
            SpellEntity spellEntity = createSpellEntity(spell, spellType, x, y);

            if (spellEntity != null) {
                // Add to active spells list
                activeSpells.add(spellEntity);
                System.out.println("Successfully added spell to active spells. Total active: " + activeSpells.size());

                // Add to game scene if available (this is crucial!)
                if (gameScene != null) {
                    gameScene.addGameObject(spellEntity);
                    System.out.println("Added spell entity to game scene");
                } else {
                    System.err.println("WARNING: Game scene is null, spell will not be rendered!");
                }

                return spellEntity;
            } else {
                System.err.println("Failed to create spell entity");
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error creating spell: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a spell entity with enhanced visuals
     */
    private SpellEntity createSpellEntity(AbstractSpell spell, String spellType, float x, float y) {
        // Convert spell type string to enum
        SpellEntity.SpellType entitySpellType;
        try {
            entitySpellType = SpellEntity.SpellType.valueOf(spellType.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid spell type: " + spellType);
            return null;
        }

        try {
            // Create the spell entity
            SpellEntity spellEntity = new SpellEntity(spriteManager, spell, entitySpellType, x, y);

            // Configure spell for better visibility
            spellEntity.setMaxLifeTime(4.0f);        // Longer duration for visibility
            spellEntity.setAnimationSpeed(0.2f);     // Slower animation for better effect

            // Set it to impact variation immediately for better visuals
            spellEntity.setVariation("impact");

            return spellEntity;
        } catch (Exception e) {
            System.err.println("Error creating spell entity: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Alternative method to create a spell with LayeredCharacter
     */
    private LayeredCharacter createSpellLayeredCharacter(String spellType, float x, float y) {
        LayeredCharacter spellChar = new LayeredCharacter(spriteManager);

        // Choose sprites based on spell type
        String baseSprite;
        String effectSprite;

        switch (spellType.toLowerCase()) {
            case "fire":
                baseSprite = "flame_spell_fil_ic_1";
                effectSprite = "spell_cast_fire_1";
                break;
            case "ice":
                baseSprite = "spread_spell_ic_1";
                effectSprite = "flame_spell_ic_1"; // Reuse but with blue color
                break;
            case "lightning":
                baseSprite = "type_spell_fl_ic_1";
                effectSprite = "spell_cast_fire_3"; // Reuse but with yellow color
                break;
            default:
                baseSprite = "flame_spell_fil_ic_1";
                effectSprite = "spell_cast_fire_1";
        }

        // Add layers
        spellChar.addLayer("base", baseSprite, 0, 0, 1.0f);
        spellChar.addLayer("effect", effectSprite, 0, 0, 0.5f);

        // Position and scale
        spellChar.setPosition(x, y, 10.0f); // High z value for visibility
        spellChar.setScale(3.0f, 3.0f);     // Larger size for visibility

        return spellChar;
    }

    /**
     * Update all active spells
     */
    public void updateSpells(float deltaTime) {
        // Store spells to remove to avoid concurrent modification
        List<SpellEntity> spellsToRemove = new ArrayList<>();

        // Update all spells
        for (SpellEntity spell : activeSpells) {
            spell.update(deltaTime);

            // Check if spell is no longer active
            if (!spell.isActive()) {
                spellsToRemove.add(spell);

                // Remove from scene if possible
                if (gameScene != null) {
                    gameScene.removeGameObject(spell);
                }
            }
        }

        // Remove inactive spells from tracking list
        activeSpells.removeAll(spellsToRemove);
    }

    /**
     * Render all active spells
     */
    public void renderSpells(Matrix4f viewProjectionMatrix) {
        for (SpellEntity spell : activeSpells) {
            try {
                spell.render(viewProjectionMatrix);
            } catch (Exception e) {
                System.err.println("Error rendering spell: " + e.getMessage());
            }
        }
    }

    /**
     * Upgrade a player's spell
     */
    public void upgradeSpell(String playerId, String spellType) {
        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);
        if (spellBook != null) {
            spellBook.upgradeSpell(spellType);
        }
    }

    /**
     * Get player's spell book
     */
    public PlayerSpellBook getPlayerSpellBook(String playerId) {
        return playerSpellBooks.get(playerId);
    }

    /**
     * Get number of active spells
     */
    public int getActiveSpellCount() {
        return activeSpells.size();
    }

    /**
     * Clear all active spells (e.g., when changing scenes)
     */
    public void clearActiveSpells() {
        // Remove from scene if possible
        if (gameScene != null) {
            for (SpellEntity spell : activeSpells) {
                gameScene.removeGameObject(spell);
            }
        }

        // Clear the list
        activeSpells.clear();
    }
}