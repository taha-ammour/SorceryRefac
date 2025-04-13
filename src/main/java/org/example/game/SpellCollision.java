package org.example.game;

import org.example.engine.GameObject;
import org.example.engine.collision.Collidable;
import org.example.engine.collision.CollisionResult;

/**
 * Extension for SpellEntity class that implements the Collidable interface.
 * This allows spells to respond to collisions with players and other objects.
 */
public class SpellCollision implements Collidable {
    private final SpellEntity spell;
    private boolean hasHit = false;

    public SpellCollision(SpellEntity spell) {
        this.spell = spell;
    }

    @Override
    public void onCollision(GameObject other, CollisionResult result) {
        handleCollision(other);
    }

    @Override
    public void onTriggerEnter(GameObject other, CollisionResult result) {
        handleCollision(other);
    }

    @Override
    public void onTriggerExit(GameObject other) {
        // Not needed for spells
    }

    /**
     * Handle collision with another object
     */
    private void handleCollision(GameObject other) {
        if (hasHit) return; // Only hit once

        if (other instanceof Player) {
            Player player = (Player) other;

            // Don't hit the caster
            if (spell.getSpell() != null &&
                    spell.getSpell().getPlayerId().equals(player.getPlayerId().toString())) {
                return;
            }

            // Mark as hit
            hasHit = true;

            // Handle spell effect
            handleSpellEffect();

        }
    }

    /**
     * Handle spell effect when hitting something
     */
    private void handleSpellEffect() {
        if (spell == null) return;

        try {
            // Change the spell to "impact" variation for visual effect
            spell.setVariation("impact");

            // Reduce the lifetime to make it disappear soon
            spell.setMaxLifeTime(Math.min(spell.getMaxLifeTime(), 0.5f));

            // Stop movement
            spell.setMovementSpeed(0);

            // Could also create particle effects or play sounds here

        } catch (Exception e) {
            System.err.println("Error handling spell effect: " + e.getMessage());
        }
    }
}