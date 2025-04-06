package org.example.game.Spells;

import java.util.HashMap;
import java.util.Map;

public class PlayerSpellBook {
    private final String playerId;
    private final Map<String, AbstractSpell> spells;

    public PlayerSpellBook(String playerId) {
        this.playerId = playerId;
        this.spells = new HashMap<>();

        addSpell("fire");
    }

    // Buy (or add) a spell of the given type
    public void addSpell(String type) {
        AbstractSpell spell = SpellFactory.createSpell(type, playerId);
        spells.put(type.toLowerCase(), spell);
    }

    // Get the spell by type
    public AbstractSpell getSpell(String type) {
        return spells.get(type.toLowerCase());
    }

    // Upgrade a spell by type
    public void upgradeSpell(String type) {
        AbstractSpell spell = spells.get(type.toLowerCase());
        if (spell != null) {
            try {
                AbstractSpell upgradedSpell = spell.upgrade();
                spells.put(type.toLowerCase(), upgradedSpell);
                System.out.println("Upgraded " + type + " spell to level " + upgradedSpell.getLevel());
            } catch (IllegalStateException e) {
                System.out.println("Cannot upgrade " + type + " spell: " + e.getMessage());
            }
        } else {
            System.out.println("No spell of type " + type + " exists for player " + playerId);
        }
    }

    public Map<String, AbstractSpell> getSpells() {
        return spells;
    }

    // Convenience method to check if player has a specific spell
    public boolean hasSpell(String type) {
        return spells.containsKey(type.toLowerCase());
    }

    // Get the level of a specific spell, or 0 if not owned
    public int getSpellLevel(String type) {
        AbstractSpell spell = spells.get(type.toLowerCase());
        return spell != null ? spell.getLevel() : 0;
    }
}