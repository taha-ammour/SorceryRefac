package org.example.engine.collision;

import java.util.HashSet;
import java.util.Set;

public class CollisionLayer {
    private final String name;
    private final int id;
    private final Set<Integer> collidingLayers;

    public CollisionLayer(String name, int id) {
        this.name = name;
        this.id = id;
        this.collidingLayers = new HashSet<>();
        // By default, a layer collides with itself
        this.collidingLayers.add(id);
    }

    /**
     * Gets the name of the layer
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the ID of the layer
     */
    public int getId() {
        return id;
    }

    /**
     * Adds a layer that this layer can collide with
     */
    public void addCollidingLayer(int layerId) {
        collidingLayers.add(layerId);
    }

    /**
     * Removes a layer that this layer can collide with
     */
    public void removeCollidingLayer(int layerId) {
        collidingLayers.remove(layerId);
    }

    /**
     * Checks if this layer can collide with another layer
     */
    public boolean canCollideWith(int layerId) {
        return collidingLayers.contains(layerId);
    }
}