package org.example.engine.ecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a game entity with a collection of components
 */
public class Entity {
    private final UUID id;
    private final Map<Class<? extends Component>, Component> components = new ConcurrentHashMap<>();
    private boolean isActive = true;
    private String name;

    public Entity() {
        this.id = UUID.randomUUID();
    }

    public Entity(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Add a component to this entity
     * @param component The component to add
     * @param <T> Component type
     * @return This entity (for chaining)
     */
    public <T extends Component> Entity addComponent(T component) {
        component.setEntityId(id);
        components.put(component.getClass(), component);
        component.onAdd();
        return this;
    }

    /**
     * Remove a component from this entity
     * @param componentClass The component class to remove
     * @param <T> Component type
     * @return True if the component was removed, false if it wasn't found
     */
    public <T extends Component> boolean removeComponent(Class<T> componentClass) {
        Component component = components.remove(componentClass);
        if (component != null) {
            component.onRemove();
            return true;
        }
        return false;
    }

    /**
     * Get a component by type
     * @param componentClass The component class to get
     * @param <T> Component type
     * @return The component or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> componentClass) {
        return (T) components.get(componentClass);
    }

    /**
     * Check if this entity has a specific component
     * @param componentClass The component class to check
     * @return True if the entity has the component
     */
    public boolean hasComponent(Class<? extends Component> componentClass) {
        return components.containsKey(componentClass);
    }

    /**
     * Check if this entity has all the specified components
     * @param componentClasses The component classes to check
     * @return True if the entity has all components
     */
    public boolean hasAllComponents(Class<? extends Component>... componentClasses) {
        for (Class<? extends Component> componentClass : componentClasses) {
            if (!components.containsKey(componentClass)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all components in this entity
     * @return List of all components
     */
    public List<Component> getAllComponents() {
        return new ArrayList<>(components.values());
    }
}

