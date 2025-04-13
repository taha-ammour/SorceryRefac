package org.example.engine.collision;

import org.example.engine.GameObject;

public class Collision {
    private final GameObject objectA;
    private final GameObject objectB;
    private final Collider colliderA;
    private final Collider colliderB;
    private final CollisionResult result;

    public Collision(GameObject objectA, GameObject objectB, Collider colliderA, Collider colliderB, CollisionResult result) {
        this.objectA = objectA;
        this.objectB = objectB;
        this.colliderA = colliderA;
        this.colliderB = colliderB;
        this.result = result;
    }

    /**
     * Gets the first game object in the collision
     */
    public GameObject getObjectA() {
        return objectA;
    }

    /**
     * Gets the second game object in the collision
     */
    public GameObject getObjectB() {
        return objectB;
    }

    /**
     * Gets the first collider in the collision
     */
    public Collider getColliderA() {
        return colliderA;
    }

    /**
     * Gets the second collider in the collision
     */
    public Collider getColliderB() {
        return colliderB;
    }

    /**
     * Gets the collision result
     */
    public CollisionResult getResult() {
        return result;
    }
}