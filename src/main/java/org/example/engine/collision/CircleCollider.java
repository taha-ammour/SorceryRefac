package org.example.engine.collision;

import org.example.engine.GameObject; /**
 * Circle-shaped collider
 */
public class CircleCollider extends Collider {
    private float radius;
    private float offsetX;
    private float offsetY;

    /**
     * Creates a new circle collider with the given radius
     */
    public CircleCollider(float radius) {
        super();
        this.radius = radius;
        this.offsetX = 0;
        this.offsetY = 0;
    }

    /**
     * Creates a new circle collider with the given radius and offset
     */
    public CircleCollider(float radius, float offsetX, float offsetY) {
        super();
        this.radius = radius;
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
        // Return a square bounds that contains the circle
        return new Bounds(
                position.x - radius,
                position.y - radius,
                radius * 2,
                radius * 2
        );
    }

    /**
     * Gets the X coordinate of the circle's center
     */
    public float getX() {
        return position.x;
    }

    /**
     * Gets the Y coordinate of the circle's center
     */
    public float getY() {
        return position.y;
    }

    /**
     * Gets the radius of the circle
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Sets the radius of the circle
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * Gets the X offset of the circle
     */
    public float getOffsetX() {
        return offsetX;
    }

    /**
     * Gets the Y offset of the circle
     */
    public float getOffsetY() {
        return offsetY;
    }

    /**
     * Sets the offset of the circle
     */
    public void setOffset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }
}
