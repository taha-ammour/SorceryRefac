package org.example.engine.collision;

import org.example.engine.GameObject;

import java.util.ArrayList;
import java.util.List; /**
 * Compound collider made up of multiple child colliders
 */
public class CompoundCollider extends Collider {
    private final List<Collider> childColliders;

    /**
     * Creates a new compound collider
     */
    public CompoundCollider() {
        super();
        this.childColliders = new ArrayList<>();
    }

    /**
     * Adds a child collider
     */
    public void addCollider(Collider collider) {
        childColliders.add(collider);
    }

    /**
     * Removes a child collider
     */
    public void removeCollider(Collider collider) {
        childColliders.remove(collider);
    }

    /**
     * Gets all child colliders
     */
    public List<Collider> getChildColliders() {
        return new ArrayList<>(childColliders);
    }

    @Override
    public void updatePosition(GameObject gameObject) {
        // Update position for this collider
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
            position.x = objectPos.x;
            position.y = objectPos.y;
        }

        // Update all child colliders
        for (Collider child : childColliders) {
            child.updatePosition(gameObject);
        }
    }

    @Override
    public Bounds getBounds() {
        if (childColliders.isEmpty()) {
            return new Bounds(position.x, position.y, 0, 0);
        }

        // Calculate the combined bounds of all child colliders
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (Collider child : childColliders) {
            Bounds childBounds = child.getBounds();
            minX = Math.min(minX, childBounds.getX());
            minY = Math.min(minY, childBounds.getY());
            maxX = Math.max(maxX, childBounds.getX() + childBounds.getWidth());
            maxY = Math.max(maxY, childBounds.getY() + childBounds.getHeight());
        }

        return new Bounds(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public void setLayer(int layer) {
        super.setLayer(layer);

        // Propagate layer to all child colliders
        for (Collider child : childColliders) {
            child.setLayer(layer);
        }
    }

    @Override
    public void setTrigger(boolean isTrigger) {
        super.setTrigger(isTrigger);

        // Propagate trigger setting to all child colliders
        for (Collider child : childColliders) {
            child.setTrigger(isTrigger);
        }
    }

    @Override
    public void setActive(boolean isActive) {
        super.setActive(isActive);

        // Propagate active setting to all child colliders
        for (Collider child : childColliders) {
            child.setActive(isActive);
        }
    }
}
