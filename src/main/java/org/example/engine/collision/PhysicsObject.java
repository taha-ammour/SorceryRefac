package org.example.engine.collision;

/**
 * Interface for objects that can have physics properties
 */
public interface PhysicsObject {
    /**
     * Gets the mass of the object
     */
    float getMass();

    /**
     * Checks if this object is static (immovable)
     */
    boolean isStatic();

    /**
     * Applies an impulse to the object
     */
    void applyImpulse(float x, float y);
}
