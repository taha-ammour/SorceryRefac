package org.example.engine.ecs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap; /**
 * Main ECS manager that coordinates entities and systems
 */
public class ECSManager {
    private final Map<UUID, Entity> entities = new ConcurrentHashMap<>();
    private final List<System> systems = new ArrayList<>();
    private final Map<String, Entity> entitiesByName = new HashMap<>();

    private static ECSManager instance;

    private ECSManager() {
        // Private constructor for singleton
    }

    public static ECSManager getInstance() {
        if (instance == null) {
            instance = new ECSManager();
        }
        return instance;
    }

    /**
     * Add an entity to the manager
     * @param entity The entity to add
     * @return The added entity
     */
    public Entity addEntity(Entity entity) {
        entities.put(entity.getId(), entity);

        if (entity.getName() != null && !entity.getName().isEmpty()) {
            entitiesByName.put(entity.getName(), entity);
        }

        return entity;
    }

    /**
     * Create and add a new entity
     * @return The new entity
     */
    public Entity createEntity() {
        Entity entity = new Entity();
        return addEntity(entity);
    }

    /**
     * Create and add a new named entity
     * @param name Entity name
     * @return The new entity
     */
    public Entity createEntity(String name) {
        Entity entity = new Entity(name);
        return addEntity(entity);
    }

    /**
     * Remove an entity from the manager
     * @param entityId The ID of the entity to remove
     */
    public void removeEntity(UUID entityId) {
        Entity entity = entities.remove(entityId);
        if (entity != null && entity.getName() != null) {
            entitiesByName.remove(entity.getName());
        }
    }

    /**
     * Get an entity by ID
     * @param entityId The ID of the entity to get
     * @return The entity or null if not found
     */
    public Entity getEntity(UUID entityId) {
        return entities.get(entityId);
    }

    /**
     * Get an entity by name
     * @param name The name of the entity to get
     * @return The entity or null if not found
     */
    public Entity getEntityByName(String name) {
        return entitiesByName.get(name);
    }

    /**
     * Get all entities in the manager
     * @return List of all entities
     */
    public List<Entity> getAllEntities() {
        return new ArrayList<>(entities.values());
    }

    /**
     * Add a system to the manager
     * @param system The system to add
     */
    public void addSystem(System system) {
        systems.add(system);
        // Sort systems by priority
        systems.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
    }

    /**
     * Remove a system from the manager
     * @param systemClass The class of the system to remove
     * @param <T> System type
     * @return True if the system was removed
     */
    public <T extends System> boolean removeSystem(Class<T> systemClass) {
        return systems.removeIf(system -> system.getClass().equals(systemClass));
    }

    /**
     * Get a system by class
     * @param systemClass The class of the system to get
     * @param <T> System type
     * @return The system or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends System> T getSystem(Class<T> systemClass) {
        for (System system : systems) {
            if (system.getClass().equals(systemClass)) {
                return (T) system;
            }
        }
        return null;
    }

    /**
     * Update all systems and process matching entities
     * @param deltaTime Time since last frame
     */
    public void update(float deltaTime) {
        for (System system : systems) {
            if (system.isEnabled()) {
                system.begin(deltaTime);

                for (Entity entity : entities.values()) {
                    if (system.matches(entity)) {
                        system.processEntity(entity, deltaTime);
                    }
                }

                system.end(deltaTime);
            }
        }
    }

    /**
     * Clear all entities and systems
     */
    public void clear() {
        entities.clear();
        entitiesByName.clear();
        systems.clear();
    }
}
