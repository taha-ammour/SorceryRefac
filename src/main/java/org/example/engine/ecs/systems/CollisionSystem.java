package org.example.engine.ecs.systems;

import org.example.engine.ecs.Entity;
import org.example.engine.ecs.System;
import org.example.engine.ecs.components.CollisionComponent;
import org.example.engine.ecs.components.TransformComponent;

/**
 * System that handles collision detection and resolution
 */
public class CollisionSystem extends System {
    private final java.util.List<Entity> collidableEntities = new java.util.ArrayList<>();

    public CollisionSystem() {
        // Run after movement but before rendering
        super(30, TransformComponent.class, CollisionComponent.class);
    }

    @Override
    public void begin(float deltaTime) {
        // Clear the list of collidable entities
        collidableEntities.clear();
    }

    @Override
    public void processEntity(Entity entity, float deltaTime) {
        // First pass: collect all collidable entities
        collidableEntities.add(entity);
    }

    @Override
    public void end(float deltaTime) {
        // Second pass: check for collisions between all pairs
        for (int i = 0; i < collidableEntities.size(); i++) {
            Entity entityA = collidableEntities.get(i);
            TransformComponent transformA = entityA.getComponent(TransformComponent.class);
            CollisionComponent collisionA = entityA.getComponent(CollisionComponent.class);

            for (int j = i + 1; j < collidableEntities.size(); j++) {
                Entity entityB = collidableEntities.get(j);
                TransformComponent transformB = entityB.getComponent(TransformComponent.class);
                CollisionComponent collisionB = entityB.getComponent(CollisionComponent.class);

                // Check if these entities should collide based on layers
                boolean shouldCheck = false;
                for (String layerA : collisionA.getCollidesWith()) {
                    if (layerA.equals(collisionB.getCollisionLayer())) {
                        shouldCheck = true;
                        break;
                    }
                }

                if (!shouldCheck) {
                    continue;
                }

                // Check if they collide
                if (collisionA.collidesWith(collisionB, transformB, transformA)) {
                    // Handle collision based on trigger status
                    if (collisionA.isTrigger() || collisionB.isTrigger()) {
                        // Handle trigger collision (no physics resolution)
                        handleTriggerCollision(entityA, entityB);
                    } else {
                        // Handle physical collision
                        resolveCollision(entityA, entityB, transformA, transformB, collisionA, collisionB);
                    }
                }
            }
        }
    }

    private void handleTriggerCollision(Entity triggerEntity, Entity otherEntity) {
        // In a full implementation, you might notify both entities of the collision
        // or trigger special behavior based on entity types
    }

    private void resolveCollision(Entity entityA, Entity entityB,
                                  TransformComponent transformA, TransformComponent transformB,
                                  CollisionComponent collisionA, CollisionComponent collisionB) {
        // Simple collision resolution: push entities apart
        // This is a very basic implementation

        float aLeft = transformA.getPosition().x;
        float aRight = aLeft + collisionA.getWidth();
        float aTop = transformA.getPosition().y;
        float aBottom = aTop + collisionA.getHeight();

        float bLeft = transformB.getPosition().x;
        float bRight = bLeft + collisionB.getWidth();
        float bTop = transformB.getPosition().y;
        float bBottom = bTop + collisionB.getHeight();

        // Calculate overlap distances
        float overlapX = Math.min(aRight - bLeft, bRight - aLeft);
        float overlapY = Math.min(aBottom - bTop, bBottom - aTop);

        // Resolve along the axis with the smallest overlap
        if (overlapX < overlapY) {
            // Resolve horizontally
            if (aLeft < bLeft) {
                transformA.getPosition().x -= overlapX / 2;
                transformB.getPosition().x += overlapX / 2;
            } else {
                transformA.getPosition().x += overlapX / 2;
                transformB.getPosition().x -= overlapX / 2;
            }
        } else {
            // Resolve vertically
            if (aTop < bTop) {
                transformA.getPosition().y -= overlapY / 2;
                transformB.getPosition().y += overlapY / 2;
            } else {
                transformA.getPosition().y += overlapY / 2;
                transformB.getPosition().y -= overlapY / 2;
            }
        }
    }
}
