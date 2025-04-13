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

    // Debug mode
    private boolean debug = false;

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

            // Add ice spell at level 1 if needed
            if (!spellBook.hasSpell("ice")) {
                spellBook.addSpell("ice");
            }

            // Add lightning spell at level 1 if needed
            if (!spellBook.hasSpell("lightning")) {
                spellBook.addSpell("lightning");
            }

            if (debug) {
                System.out.println("Initialized player " + playerId + " with spell system");
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
     * @return The created spell entity or null if failed
     */
    public SpellEntity castSpell(UUID playerId, String spellType, float x, float y) {
        // Check energy and cooldown constraints
        if (!canCastSpell(playerId, spellType)) {
            if (debug) {
                System.out.println("Player " + playerId + " cannot cast " + spellType +
                        " (energy or cooldown constraints)");
            }
            return null;
        }

        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);

        // For remote players in multiplayer, create a spellbook if one doesn't exist
        if (spellBook == null) {
            if (debug) {
                System.out.println("Creating spell book for player: " + playerId);
            }

            spellBook = new PlayerSpellBook(playerId.toString());
            playerSpellBooks.put(playerId, spellBook);
            playerEnergy.put(playerId, maxEnergy);
            playerCooldowns.put(playerId, new HashMap<>());
        }

        // Get the spell - don't automatically add it if it doesn't exist
        AbstractSpell spell = spellBook.getSpell(spellType);
        if (spell == null) {
            if (debug) {
                System.out.println("Player " + playerId + " does not have " + spellType + " spell");
            }
            return null;
        }


        // Convert spell type to SpellEntity.SpellType
        SpellEntity.SpellType entitySpellType;
        try {
            entitySpellType = SpellEntity.SpellType.valueOf(spellType.toUpperCase());
            if (debug) {
                System.out.println("Creating spell of type: " + entitySpellType.name());
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid spell type: " + spellType);
            return null;
        }

        // Apply cost and cooldown
        applySpellEffects(playerId, spellType, spell);

        // Use the spellManager to cast the spell
        SpellEntity spellEntity = spellManager.castSpell(playerId.toString(), spellType, x, y);

        return spellEntity;
    }

    /**
     * Check if a player can cast a spell (energy and cooldown constraints)
     */
    private boolean canCastSpell(UUID playerId, String spellType) {
        // Get the player's spell
        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);
        if (spellBook == null || !spellBook.hasSpell(spellType)) {
            return false;
        }

        AbstractSpell spell = spellBook.getSpell(spellType);
        if (spell == null) {
            return false;
        }

        // Check energy
        float playerEnergyAvailable = playerEnergy.getOrDefault(playerId, 0f);
        if (playerEnergyAvailable < spell.getEnergyCost()) {
            if (debug) {
                System.out.println("Not enough energy to cast " + spellType +
                        " (needs " + spell.getEnergyCost() + ", has " + playerEnergyAvailable + ")");
            }
            return false;
        }

        // Check cooldown
        Map<String, Float> cooldowns = playerCooldowns.getOrDefault(playerId, new HashMap<>());
        float remainingCooldown = cooldowns.getOrDefault(spellType, 0f);
        if (remainingCooldown > 0) {
            if (debug) {
                System.out.println("Spell " + spellType + " still on cooldown: " +
                        String.format("%.1f", remainingCooldown) + "s remaining");
            }
            return false;
        }

        return true;
    }

    /**
     * Apply spell effects (energy cost and cooldown)
     */
    private void applySpellEffects(UUID playerId, String spellType, AbstractSpell spell) {
        // Apply energy cost
        float currentEnergy = playerEnergy.getOrDefault(playerId, maxEnergy);
        currentEnergy = Math.max(0, currentEnergy - spell.getEnergyCost());
        playerEnergy.put(playerId, currentEnergy);

        // Apply cooldown
        Map<String, Float> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns == null) {
            cooldowns = new HashMap<>();
            playerCooldowns.put(playerId, cooldowns);
        }

        // Set cooldown based on spell's cooldown value
        cooldowns.put(spellType, (float)spell.getCooldown());

        if (debug) {
            System.out.println("Applied effects for " + spellType + " spell: -" +
                    spell.getEnergyCost() + " energy, " + spell.getCooldown() + "s cooldown");
        }
    }

    /**
     * Upgrade a player's spell
     */
    public boolean upgradeSpell(UUID playerId, String spellType) {
        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);
        if (spellBook != null && spellBook.hasSpell(spellType)) {
            try {
                spellBook.upgradeSpell(spellType);
                if (debug) {
                    System.out.println("Upgraded " + spellType + " spell for player " + playerId);
                }
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

            // For network actions, we don't enforce energy/cooldown constraints
            // since those should have been applied on the sender's side

            PlayerSpellBook spellBook = playerSpellBooks.get(playerId);
            if (spellBook == null) {
                // Create a spell book for remote players if needed
                spellBook = new PlayerSpellBook(playerId.toString());
                playerSpellBooks.put(playerId, spellBook);
                playerEnergy.put(playerId, maxEnergy);
                playerCooldowns.put(playerId, new HashMap<>());

                // Ensure they have all the basic spells
                if (!spellBook.hasSpell(spellType)) {
                    spellBook.addSpell(spellType);
                }
            }

            // Cast the spell at the specified location
            SpellEntity spellEntity = spellManager.castSpell(playerId.toString(), spellType, action.x, action.y);

            // If the spell was created successfully and we have a game scene, add it
            if (spellEntity != null && gameScene != null) {
                gameScene.addGameObject(spellEntity);
                if (debug) {
                    System.out.println("Added network spell to scene: " + spellType);
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing spell action: " + e.getMessage());
        }
    }

    /**
     * Create a spell directly without player association
     * Useful for testing and effects
     */
    public SpellEntity createDirectSpell(String spellType, float x, float y) {
        try {
            // Convert spell type string to enum
            SpellEntity.SpellType entitySpellType = SpellEntity.SpellType.valueOf(spellType.toUpperCase());

            // Create a spell entity directly without going through a player
            SpellEntity spellEntity = new SpellEntity(spriteManager, null, entitySpellType, x, y);

            // Configure spell with reasonable default values
            spellEntity.setMaxLifeTime(4.0f);
            spellEntity.setAnimationSpeed(0.2f);
            spellEntity.setVariation("impact");

            // Add to game scene if available
            if (gameScene != null) {
                gameScene.addGameObject(spellEntity);
                if (debug) {
                    System.out.println("Added direct spell to scene: " + spellType);
                }
            }

            return spellEntity;
        } catch (Exception e) {
            System.err.println("Error creating direct spell: " + e.getMessage());
            return null;
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

    /**
     * Refresh the game scene reference
     */
    public void refreshSceneReference(Scene gameScene) {
        this.gameScene = gameScene;

        // Also update the spell manager
        if (spellManager != null) {
            spellManager.setGameScene(gameScene);
            if (debug) {
                System.out.println("Updated game scene reference in SpellSystem and SpellManager");
            }
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

    /**
     * Modify player energy directly
     * @param playerId Player UUID
     * @param amount Amount to add (positive) or subtract (negative)
     * @return New energy value
     */
    public float modifyPlayerEnergy(UUID playerId, float amount) {
        float currentEnergy = playerEnergy.getOrDefault(playerId, maxEnergy);
        float newEnergy = Math.max(0, Math.min(maxEnergy, currentEnergy + amount));
        playerEnergy.put(playerId, newEnergy);
        return newEnergy;
    }

    /**
     * Reset all cooldowns for a player
     * Useful for powerups or testing
     */
    public void resetCooldowns(UUID playerId) {
        Map<String, Float> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns != null) {
            cooldowns.clear();
            if (debug) {
                System.out.println("Reset all cooldowns for player " + playerId);
            }
        }
    }

    public int getActiveSpellCount() {
        return spellManager != null ? spellManager.getActiveSpellCount() : 0;
    }

    /**
     * Set debug mode
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
        if (spellManager != null) {
            spellManager.setDebug(debug);
        }
    }
}