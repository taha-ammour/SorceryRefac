package org.example.game;

import org.example.engine.GameObject;
import org.example.engine.collision.Collidable;
import org.example.engine.collision.CollisionResult;
import org.example.engine.collision.PhysicsObject;
import org.joml.Vector3f;

/**
 * Extension for Player class that implements collision-related interfaces.
 * This approach allows us to add collision behavior without directly modifying the Player class.
 */
public class PlayerCollision implements Collidable, PhysicsObject {
    private final Player player;
    private float mass = 10.0f;
    private boolean isStatic = false;
    private boolean isImmune = false;
    private float immuneTimer = 0;
    private final float immuneDuration = 1.0f; // 1 second of immunity after being hit

    public PlayerCollision(Player player) {
        this.player = player;
    }

    @Override
    public void onCollision(GameObject other, CollisionResult result) {
        // Handle collision with other objects
        if (other instanceof SpellEntity) {
            handleSpellCollision((SpellEntity)other);
        }
    }

    @Override
    public void onTriggerEnter(GameObject other, CollisionResult result) {
        // Handle trigger events (like entering a spell's area effect)
        if (other instanceof SpellEntity) {
            handleSpellCollision((SpellEntity)other);
        }
    }

    @Override
    public void onTriggerExit(GameObject other) {
        // Handle exiting a trigger zone if needed
    }

    /**
     * Handle collision with a spell
     */
    private void handleSpellCollision(SpellEntity spell) {
        // Skip if player is immune or spell is from this player
        if (isImmune || spell.getSpell() == null) {
            return;
        }

        // Check if this is our own spell
        if (spell.getSpell().getPlayerId().equals(player.getPlayerId().toString())) {
            return; // Don't damage yourself with your own spells
        }

        // Apply spell effect to player
        applySpellEffect(spell);

        // Set temporary immunity to prevent spam damage
        setImmune(true);
    }

    /**
     * Apply a spell's effect to the player
     */
    private void applySpellEffect(SpellEntity spell) {
        if (spell == null || !player.isAlive()) return;

        try {
            // Get damage from spell
            double damage = 0;
            if (spell.getSpell() != null) {
                damage = spell.getSpell().getDamage();
            } else {
                // Default damage if spell doesn't have damage value
                damage = 10.0;
            }

            // Apply damage to player
            int currentHealth = player.getHealth();
            int newHealth = Math.max(0, currentHealth - (int)damage);
            player.setHealth(newHealth);

            // Apply knockback force based on spell type
            float knockbackForce = 50.0f;

            switch (spell.getSpellType()) {
                case FIRE:
                    knockbackForce = 30.0f;
                    break;
                case ICE:
                    knockbackForce = 20.0f;
                    // Ice could slow player movement
                    player.setMoveSpeed(player.getMoveSpeed() * 0.7f);
                    break;
                case LIGHTNING:
                    knockbackForce = 40.0f;
                    break;
            }

            // Apply knockback in the direction from spell to player
            Vector3f spellPos = new Vector3f(spell.getX(), spell.getY(), 0);
            Vector3f playerPos = player.getPosition();
            Vector3f direction = new Vector3f(playerPos).sub(spellPos);

            // Normalize and scale by knockback force
            if (direction.length() > 0.001f) {
                direction.normalize().mul(knockbackForce);
                applyImpulse(direction.x, direction.y);
            }

            // Check if player died
            if (newHealth <= 0 && player.isAlive()) {
                player.setAlive(false);
                // You could trigger death animation or respawn logic here
            }

        } catch (Exception e) {
            System.err.println("Error applying spell effect: " + e.getMessage());
        }
    }

    /**
     * Update method to be called from Player's update
     */
    public void update(float deltaTime) {
        // Update immunity timer
        if (isImmune) {
            immuneTimer -= deltaTime;
            if (immuneTimer <= 0) {
                isImmune = false;
            }
        }
    }

    /**
     * Set player immune state
     */
    public void setImmune(boolean immune) {
        this.isImmune = immune;
        if (immune) {
            immuneTimer = immuneDuration;
        }
    }

    /**
     * Check if player is currently immune
     */
    public boolean isImmune() {
        return isImmune;
    }

    @Override
    public float getMass() {
        return mass;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public void applyImpulse(float x, float y) {
        if (isStatic) return;

        // Apply impulse by moving the player
        Vector3f position = player.getPosition();
        position.x += x * (1.0f / mass);
        position.y += y * (1.0f / mass);
        player.setPosition(position.x, position.y, position.z);
    }
}