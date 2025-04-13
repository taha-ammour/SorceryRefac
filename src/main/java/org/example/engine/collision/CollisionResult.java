package org.example.engine.collision;

import org.joml.Vector2f;

public class CollisionResult {
    private boolean colliding;
    private Vector2f normal;
    private float depth;
    private Vector2f contactPoint;

    public CollisionResult(boolean colliding) {
        this.colliding = colliding;
        this.normal = new Vector2f();
        this.depth = 0;
        this.contactPoint = new Vector2f();
    }

    /**
     * Copy constructor
     */
    public CollisionResult(CollisionResult other) {
        this.colliding = other.colliding;
        this.normal = new Vector2f(other.normal);
        this.depth = other.depth;
        this.contactPoint = new Vector2f(other.contactPoint);
    }

    /**
     * Checks if a collision occurred
     */
    public boolean isColliding() {
        return colliding;
    }

    /**
     * Gets the collision normal
     */
    public Vector2f getNormal() {
        return normal;
    }

    /**
     * Sets the collision normal
     */
    public void setNormal(Vector2f normal) {
        this.normal.set(normal);
    }

    /**
     * Gets the collision depth
     */
    public float getDepth() {
        return depth;
    }

    /**
     * Sets the collision depth
     */
    public void setDepth(float depth) {
        this.depth = depth;
    }

    /**
     * Gets the contact point
     */
    public Vector2f getContactPoint() {
        return contactPoint;
    }

    /**
     * Sets the contact point
     */
    public void setContactPoint(Vector2f contactPoint) {
        this.contactPoint.set(contactPoint);
    }

    /**
     * Flips the direction of the collision (for use with the second collider)
     */
    public void flipDirection() {
        normal.x = -normal.x;
        normal.y = -normal.y;
    }
}