package org.example.engine.ecs.components;

import org.example.engine.ecs.Component; /**
 * Component for health and damage
 */
public class HealthComponent extends Component {
    private int maxHealth = 100;
    private int currentHealth = 100;
    private boolean isInvulnerable = false;
    private float invulnerabilityTimer = 0;
    private boolean isDead = false;

    public HealthComponent() {
    }

    public HealthComponent(int maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        // If current health is greater than new max, cap it
        if (currentHealth > maxHealth) {
            currentHealth = maxHealth;
        }
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = Math.min(currentHealth, maxHealth);

        // Check if entity died
        if (this.currentHealth <= 0) {
            this.currentHealth = 0;
            isDead = true;
        }
    }

    public boolean damage(int amount) {
        if (isInvulnerable || isDead) {
            return false;
        }

        currentHealth -= amount;

        // Check if died
        if (currentHealth <= 0) {
            currentHealth = 0;
            isDead = true;
        }

        return true;
    }

    public void heal(int amount) {
        if (isDead) {
            return;
        }

        currentHealth = Math.min(currentHealth + amount, maxHealth);
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        isInvulnerable = invulnerable;
    }

    public void setInvulnerable(boolean invulnerable, float duration) {
        isInvulnerable = invulnerable;
        if (invulnerable) {
            invulnerabilityTimer = duration;
        } else {
            invulnerabilityTimer = 0;
        }
    }

    public void update(float deltaTime) {
        if (isInvulnerable && invulnerabilityTimer > 0) {
            invulnerabilityTimer -= deltaTime;
            if (invulnerabilityTimer <= 0) {
                isInvulnerable = false;
            }
        }
    }

    public boolean isDead() {
        return isDead;
    }

    public void revive(int health) {
        if (isDead) {
            isDead = false;
            currentHealth = Math.min(health, maxHealth);
        }
    }
}
