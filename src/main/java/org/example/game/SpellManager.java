package org.example.game;

import org.example.engine.SpriteManager;
import org.example.game.Spells.AbstractSpell;
import org.example.game.Spells.PlayerSpellBook;
import org.joml.Matrix4f;

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

    public SpellManager(SpriteManager spriteManager) {
        this.spriteManager = spriteManager;
        this.playerSpellBooks = new HashMap<>();
        this.activeSpells = new ArrayList<>();
    }

    // Create a spell book for a player
    public void createPlayerSpellBook(String playerId) {
        playerSpellBooks.put(playerId, new PlayerSpellBook(playerId));
    }

    // Cast a spell for a player
    public SpellEntity castSpell(String playerId, String spellType, float x, float y) {
        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);
        if (spellBook == null) {
            System.out.println("No spell book for player: " + playerId);
            return null;
        }

        AbstractSpell spell = spellBook.getSpell(spellType);
        if (spell == null) {
            System.out.println("Player " + playerId + " does not have " + spellType + " spell");
            return null;
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

        // Create and track spell entity
        SpellEntity spellEntity = new SpellEntity(spriteManager, spell, entitySpellType, x, y);

        // Configure spell for better visibility during testing
        spellEntity.setMaxLifeTime(4.0f);     // Longer visibility
        spellEntity.setAnimationSpeed(0.3f);  // Slower animation for easier debugging

        activeSpells.add(spellEntity);
        System.out.println("Added spell to active spells. Total active: " + activeSpells.size());

        return spellEntity;
    }

    // Update all active spells
    public void updateSpells(float deltaTime) {
        // Remove inactive spells
        activeSpells.removeIf(spell -> !spell.isActive());

        // Update remaining spells
        for (SpellEntity spell : activeSpells) {
            spell.update(deltaTime);
        }
    }

    // Render all active spells
    public void renderSpells(Matrix4f viewProjectionMatrix) {
        for (SpellEntity spell : activeSpells) {
            spell.render(viewProjectionMatrix);
        }
    }

    // Upgrade a player's spell
    public void upgradeSpell(String playerId, String spellType) {
        PlayerSpellBook spellBook = playerSpellBooks.get(playerId);
        if (spellBook != null) {
            spellBook.upgradeSpell(spellType);
        }
    }

    // Get player's spell book
    public PlayerSpellBook getPlayerSpellBook(String playerId) {
        return playerSpellBooks.get(playerId);
    }

    // Get number of active spells
    public int getActiveSpellCount() {
        return activeSpells.size();
    }

    // Clear all active spells (e.g., when changing scenes)
    public void clearActiveSpells() {
        activeSpells.clear();
    }
}