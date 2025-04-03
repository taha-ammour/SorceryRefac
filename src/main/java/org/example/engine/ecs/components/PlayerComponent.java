package org.example.engine.ecs.components;

import org.example.engine.ecs.Component;
import java.util.UUID;

/**
 * Component that stores player-specific data
 */
public class PlayerComponent extends Component {
    private String username;
    private String color;
    private boolean isLocalPlayer;
    private int health = 100;
    private int energy = 100;
    private int armor = 1;
    private boolean isAlive = true;

    // For networking
    private UUID networkId;
    private boolean needsSync = false;

    public PlayerComponent(String username, String color, boolean isLocalPlayer) {
        this.username = username;
        this.color = color;
        this.isLocalPlayer = isLocalPlayer;
        this.networkId = UUID.randomUUID();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isLocalPlayer() {
        return isLocalPlayer;
    }

    public void setLocalPlayer(boolean localPlayer) {
        isLocalPlayer = localPlayer;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
        needsSync = true;
    }

    public void damage(int amount) {
        health -= amount;
        if (health <= 0) {
            health = 0;
            isAlive = false;
        }
        needsSync = true;
    }

    public void heal(int amount) {
        health += amount;
        if (health > 100) {
            health = 100;
        }
        needsSync = true;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
        needsSync = true;
    }

    public void useEnergy(int amount) {
        energy -= amount;
        if (energy < 0) {
            energy = 0;
        }
        needsSync = true;
    }

    public void rechargeEnergy(int amount) {
        energy += amount;
        if (energy > 100) {
            energy = 100;
        }
        needsSync = true;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = armor;
        needsSync = true;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
        needsSync = true;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    public boolean needsSync() {
        return needsSync;
    }

    public void clearSyncFlag() {
        needsSync = false;
    }

    public void markForSync() {
        needsSync = true;
    }
}

