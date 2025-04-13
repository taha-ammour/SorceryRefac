package org.example.engine.collision;

public interface CollisionHandler {
    /**
     * Checks for collision between two colliders
     */
    CollisionResult checkCollision(Collider a, Collider b);
}