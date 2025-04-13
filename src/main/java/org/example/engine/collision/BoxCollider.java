package org.example.engine.collision;

import org.example.engine.GameObject;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Box-shaped collider (axis-aligned bounding box)
 */
public class BoxCollider extends Collider {
    private float width;
    private float height;
    private float offsetX;
    private float offsetY;

    /**
     * Creates a new box collider with the given dimensions
     */
    public BoxCollider(float width, float height) {
        super();
        this.width = width;
        this.height = height;
        this.offsetX = 0;
        this.offsetY = 0;
    }

    /**
     * Creates a new box collider with the given dimensions and offset
     */
    public BoxCollider(float width, float height, float offsetX, float offsetY) {
        super();
        this.width = width;
        this.height = height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public void updatePosition(GameObject gameObject) {
        // Use the game object's position and apply offset
        org.joml.Vector3f objectPos = null;

        // Handle different position methods depending on the object type
        if (gameObject instanceof org.example.game.Player) {
            objectPos = ((org.example.game.Player) gameObject).getPosition();
        } else if (gameObject instanceof org.example.game.SpellEntity) {
            float x = ((org.example.game.SpellEntity) gameObject).getX();
            float y = ((org.example.game.SpellEntity) gameObject).getY();
            objectPos = new org.joml.Vector3f(x, y, 0);
        } else {
            // Default behavior - try to access a position field or method using reflection
            try {
                java.lang.reflect.Method getPositionMethod = gameObject.getClass().getMethod("getPosition");
                objectPos = (org.joml.Vector3f) getPositionMethod.invoke(gameObject);
            } catch (Exception e) {
                // If we can't get the position, use (0,0) as a fallback
                objectPos = new org.joml.Vector3f(0, 0, 0);
            }
        }

        if (objectPos != null) {
            position.x = objectPos.x + offsetX;
            position.y = objectPos.y + offsetY;
        }
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(position.x, position.y, width, height);
    }

    /**
     * Gets the X coordinate of the box
     */
    public float getX() {
        return position.x;
    }

    /**
     * Gets the Y coordinate of the box
     */
    public float getY() {
        return position.y;
    }

    /**
     * Gets the width of the box
     */
    public float getWidth() {
        return width;
    }

    /**
     * Gets the height of the box
     */
    public float getHeight() {
        return height;
    }

    /**
     * Sets the dimensions of the box
     */
    public void setDimensions(float width, float height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Gets the X offset of the box
     */
    public float getOffsetX() {
        return offsetX;
    }

    /**
     * Gets the Y offset of the box
     */
    public float getOffsetY() {
        return offsetY;
    }

    /**
     * Sets the offset of the box
     */
    public void setOffset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }
}

