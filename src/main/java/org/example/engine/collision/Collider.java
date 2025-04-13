package org.example.engine.collision;

import org.example.engine.GameObject;
import org.joml.Vector2f;

public abstract class Collider {
    protected Vector2f position;
    protected int layer;
    protected boolean isTrigger;
    protected boolean isActive;

    public Collider() {
        this.position = new Vector2f();
        this.layer = 0; // Default layer
        this.isTrigger = false;
        this.isActive = true;
    }

    /**
     * Updates the collider position based on the game object
     */
    public abstract void updatePosition(GameObject gameObject);

    /**
     * Gets the bounds of the collider
     */
    public abstract Bounds getBounds();

    /**
     * Gets the position of the collider
     */
    public Vector2f getPosition() {
        return position;
    }

    /**
     * Sets the position of the collider
     */
    public void setPosition(float x, float y) {
        position.x = x;
        position.y = y;
    }

    /**
     * Gets the layer of the collider
     */
    public int getLayer() {
        return layer;
    }

    /**
     * Sets the layer of the collider
     */
    public void setLayer(int layer) {
        this.layer = layer;
    }

    /**
     * Checks if this collider is a trigger
     */
    public boolean isTrigger() {
        return isTrigger;
    }

    /**
     * Sets whether this collider is a trigger
     */
    public void setTrigger(boolean isTrigger) {
        this.isTrigger = isTrigger;
    }

    /**
     * Checks if this collider is active
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets whether this collider is active
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}