package org.example.engine.collision;

import org.example.engine.GameObject;

public interface Collidable {
    /**
     * Called when this object collides with another object
     * @param other The other game object
     * @param result The collision result with details about the collision
     */
    void onCollision(GameObject other, CollisionResult result);

    /**
     * Called when this object enters a trigger collider
     * @param other The other game object
     * @param result The collision result with details about the trigger
     */
    void onTriggerEnter(GameObject other, CollisionResult result);

    /**
     * Called when this object exits a trigger collider
     * @param other The other game object
     */
    void onTriggerExit(GameObject other);
}