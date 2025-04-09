package org.example.game;

import org.example.Packets;
import org.example.engine.Scene;
import org.example.engine.SpriteManager;
import org.example.game.Spells.AbstractSpell;
import org.example.game.Spells.PlayerSpellBook;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * System that manages spell casting, spell books, and spell UI integration
 */
public class SpellSystem {
    private final SpriteManager spriteManager;
    private Scene gameScene;
    private final SpellManager spellManager;
    private final Map<UUID, PlayerSpellBook> playerSpellBooks = new HashMap<>();

    // Energy regeneration settings
    private final float energyRegenRate = 5.0f; // energy per second
    private final Map<UUID, Float> playerEnergy = new HashMap<>();
    private final float maxEnergy = 100.0f;

    // Cooldown tracking
    private final Map<UUID, Map<String, Float>> playerCooldowns = new HashMap<>();

    public SpellSystem(SpriteManager spriteManager, Scene gameScene) {
        this.spriteManager = spriteManager;
        this.gameScene = gameScene;
        this.spellManager = new SpellManager(spriteManager);

        // Set the game scene in the spell manager
        this.spellManager.setGameScene(gameScene);
    }

    /**
     * Initialize a player's spell system
     */
    public void initializePlayer(UUID playerId) {
        // Create spell book if not exists
        if (!playerSpellBooks.containsKey(playerId)) {
            PlayerSpellBook spellBook = new PlayerSpellBook(playerId.toString());
            playerSpellBooks.put(playerId, spellBook);

            // Initialize player energy and cooldowns
            playerEnergy.put(playerId, maxEnergy);
            playerCooldowns.put(playerId, new HashMap<>());

            // Create player spell book in the spell manager
            spellManager.createPlayerSpellBook(playerId.toString());

            // Add default spells - basic fire spell is added by default in PlayerSpellBook

            // Add ice spell at level 1 if needed
            if (!spellBook.hasSpell("ice")) {
                spellBook.addSpell("ice");
            }
        }
    }

    /**
     * Update the spell system (cooldowns, energy regeneration, etc)
     */
    public void update(float deltaTime) {
        // Update active spells
        spellManager.updateSpells(deltaTime);

        // Update player energy and cooldowns
        for (UUID playerId : playerSpellBooks.keySet()) {
            // Regenerate energy
            float currentEnergy = playerEnergy.getOrDefault(playerId, 0f);
            if (currentEnergy < maxEnergy) {
                currentEnergy = Math.min(maxEnergy, currentEnergy + (energyRegenRate * deltaTime));
                playerEnergy.put(playerId, currentEnergy);
            }

            // Update cooldowns
            Map<String, Float> cooldowns = playerCooldowns.get(playerId);
            if (cooldowns != null) {
                cooldowns.entrySet().removeIf(entry -> {
                    entry.setValue(entry.getValue() - deltaTime);
                    return entry.getValue() <= 0;
                });
            }
        }
    }

    /**
     * Render active spells
     */
    public void render(Matrix4f viewProjectionMatrix) {
        spellManager.renderSpells(viewProjectionMatrix);
    }

    /**
     * Cast a spell for the specified player
     */
    public SpellEntity castSpell(UUID playerId, String spellType, float x, float y) {
        AbstractSpell spell = null;
        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);

        // For remote players in multiplayer, create a spellbook if one doesn't exist
        if (spellBook == null) {
            // Only create automatic spellbooks for remote players in multiplayer
            // For local player, this should already exist
            System.out.println("Creating spell book for player: " + playerId);
            spellBook = new PlayerSpellBook(playerId.toString());
            playerSpellBooks.put(playerId, spellBook);
            playerEnergy.put(playerId, maxEnergy);
            playerCooldowns.put(playerId, new HashMap<>());
        }

        // Get the spell - don't automatically add it if it doesn't exist
        spell = spellBook.getSpell(spellType);
        if (spell == null) {
            System.out.println("Player " + playerId + " does not have " + spellType + " spell");
            return null; // Return null instead of adding the spell automatically
        }

        // Convert spell type to SpellEntity.SpellType
        SpellEntity.SpellType entitySpellType;
        try {
            entitySpellType = SpellEntity.SpellType.valueOf(spellType.toUpperCase());
            System.out.println("Creating spell of type: " + entitySpellType.name());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid spell type: " + spellType);
            return null;
        }

        // Use the spellManager to cast the spell
        SpellEntity spellEntity = spellManager.castSpell(playerId.toString(), spellType, x, y);

        return spellEntity;
    }

    /**
     * Upgrade a player's spell
     */
    public boolean upgradeSpell(UUID playerId, String spellType) {
        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);
        if (spellBook != null && spellBook.hasSpell(spellType)) {
            try {
                spellBook.upgradeSpell(spellType);
                return true;
            } catch (Exception e) {
                System.out.println("Failed to upgrade spell: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Process a spell cast action from the network
     */
    public void processSpellAction(Packets.GameAction action) {
        try {
            // Convert action type to spell type
            String spellType;
            switch (action.actionType) {
                case 1: spellType = "fire"; break;
                case 2: spellType = "ice"; break;
                case 3: spellType = "lightning"; break;
                default: return; // Invalid spell type
            }

            UUID playerId = UUID.fromString(action.playerId);
            castSpell(playerId, spellType, action.x, action.y);

        } catch (Exception e) {
            System.err.println("Error processing spell action: " + e.getMessage());
        }
    }

    /**
     * Get player's current energy
     */
    public float getPlayerEnergy(UUID playerId) {
        return playerEnergy.getOrDefault(playerId, 0f);
    }

    /**
     * Get player's max energy
     */
    public float getPlayerMaxEnergy() {
        return maxEnergy;
    }

    /**
     * Get spell cooldown remaining for a player's spell
     */
    public float getSpellCooldown(UUID playerId, String spellType) {
        Map<String, Float> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns != null) {
            return cooldowns.getOrDefault(spellType, 0f);
        }
        return 0f;
    }

    /**
     * Check if a player has a specific spell
     */
    public boolean playerHasSpell(UUID playerId, String spellType) {
        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);
        return spellBook != null && spellBook.hasSpell(spellType);
    }

    /**
     * Get a player's spell level
     */
    public int getSpellLevel(UUID playerId, String spellType) {
        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);
        return spellBook != null ? spellBook.getSpellLevel(spellType) : 0;
    }

    public void refreshSceneReference(Scene gameScene) {
        this.gameScene = gameScene;

        // Also update the spell manager
        if (spellManager != null) {
            spellManager.setGameScene(gameScene);
        }
    }

    /**
     * Get player's spell book
     * @param playerId The UUID of the player
     * @return The player's spell book, or null if it doesn't exist
     */
    public PlayerSpellBook getPlayerSpellBook(UUID playerId) {
        return playerSpellBooks.get(playerId);
    }
}