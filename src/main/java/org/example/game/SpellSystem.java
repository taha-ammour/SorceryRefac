package org.example.game;

import org.example.Packets;
import org.example.engine.Scene;
import org.example.engine.SpriteManager;
import org.example.game.Spells.PlayerSpellBook;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * System that manages spell casting, spell books, and spell UI integration
 */
public class SpellSystem {
    private final SpriteManager spriteManager;
    private final Scene gameScene;
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
    public void render(org.joml.Matrix4f viewProjectionMatrix) {
        spellManager.renderSpells(viewProjectionMatrix);
    }

    /**
     * Cast a spell for the specified player
     */
    public boolean castSpell(UUID playerId, String spellType, float x, float y) {
        // Check for spell book
        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);
        if (spellBook == null) {
            System.out.println("No spell book for player: " + playerId);
            return false;
        }

        // Check spell existence
        if (!spellBook.hasSpell(spellType)) {
            System.out.println("Player does not have spell: " + spellType);
            return false;
        }

        // Check energy
        float energyCost = spellBook.getSpell(spellType).getEnergyCost();
        float currentEnergy = playerEnergy.getOrDefault(playerId, 0f);
        if (currentEnergy < energyCost) {
            System.out.println("Not enough energy to cast " + spellType + ". Need " + energyCost + ", have " + currentEnergy);
            return false;
        }

        // Check cooldown
        Map<String, Float> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns != null && cooldowns.containsKey(spellType)) {
            float remainingCooldown = cooldowns.get(spellType);
            if (remainingCooldown > 0) {
                System.out.println(spellType + " on cooldown for " + remainingCooldown + " seconds");
                return false;
            }
        }

        // Cast the spell
        System.out.println("⚡ Casting " + spellType + " spell at " + x + "," + y);
        SpellEntity spellEntity = spellManager.castSpell(playerId.toString(), spellType, x, y);

        if (spellEntity != null) {
            System.out.println("✓ Spell entity created successfully");

            // Add to scene - make sure it's added next frame to avoid OpenGL context issues
            // Use addGameObjectNextFrame to ensure it's added safely
            gameScene.addGameObject(spellEntity);

            // Deduct energy
            playerEnergy.put(playerId, currentEnergy - energyCost);

            // Start cooldown
            double cooldown = spellBook.getSpell(spellType).getCooldown();
            if (cooldowns != null) {
                cooldowns.put(spellType, (float)cooldown);
            }

            return true;
        } else {
            System.err.println("× Failed to create spell entity");
        }

        return false;
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
}