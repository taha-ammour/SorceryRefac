package org.example.engine.ecs;

import java.util.UUID;
/**
 * Base class for all entity components
 */
public abstract class Component {
    protected UUID entityId;

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public UUID getEntityId() {
        return entityId;
    }

    /**
     * Called when the component is added to an entity
     */
    public void onAdd() {
        // Override in subclasses if needed
    }

    /**
     * Called when the component is removed from an entity
     */
    public void onRemove() {
        // Override in subclasses if needed
    }
}
