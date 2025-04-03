package org.example.engine.ecs;

/**
 * System that processes entities with specific component requirements
 */
public abstract class System {
    // The component types this system processes
    private final Class<? extends Component>[] componentTypes;
    // Priority - lower numbers run first
    private final int priority;
    private boolean isEnabled = true;

    @SafeVarargs
    public System(int priority, Class<? extends Component>... componentTypes) {
        this.priority = priority;
        this.componentTypes = componentTypes;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    /**
     * Check if an entity matches the component requirements for this system
     * @param entity The entity to check
     * @return True if the entity has all required components
     */
    public boolean matches(Entity entity) {
        if (!entity.isActive()) {
            return false;
        }

        return entity.hasAllComponents(componentTypes);
    }

    /**
     * Process a single entity
     * @param entity The entity to process
     * @param deltaTime Time since last frame
     */
    public abstract void processEntity(Entity entity, float deltaTime);

    /**
     * Called before processing all entities
     * @param deltaTime Time since last frame
     */
    public void begin(float deltaTime) {
        // Override in subclasses if needed
    }

    /**
     * Called after processing all entities
     * @param deltaTime Time since last frame
     */
    public void end(float deltaTime) {
        // Override in subclasses if needed
    }
}
