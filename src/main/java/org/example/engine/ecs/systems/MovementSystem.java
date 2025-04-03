package org.example.engine.ecs.systems;

import org.example.engine.ecs.Entity;
import org.example.engine.ecs.System;
import org.example.engine.ecs.components.MovementComponent;
import org.example.engine.ecs.components.TransformComponent;
import org.joml.Vector3f; /**
 * System that handles movement physics
 */
public class MovementSystem extends System {

    public MovementSystem() {
        // Medium priority to process after input but before rendering
        super(20, TransformComponent.class, MovementComponent.class);
    }

    @Override
    public void processEntity(Entity entity, float deltaTime) {
        TransformComponent transformComponent = entity.getComponent(TransformComponent.class);
        MovementComponent movementComponent = entity.getComponent(MovementComponent.class);

        // Only process movement for moving entities
        if (movementComponent.isMoving()) {
            float moveAmount = movementComponent.getMoveSpeed() * deltaTime;
            Vector3f position = transformComponent.getPosition();

            // Move based on direction flags
            if (movementComponent.isMoveUp()) {
                position.y -= moveAmount;
            }
            if (movementComponent.isMoveDown()) {
                position.y += moveAmount;
            }
            if (movementComponent.isMoveLeft()) {
                position.x -= moveAmount;
            }
            if (movementComponent.isMoveRight()) {
                position.x += moveAmount;
            }

            // Optional: Handle collision if CollisionComponent is present
            if (movementComponent.isCollisionEnabled()) {
                // This would be handled by a separate collision system
                // For now, we'll just update the position directly
            }
        }
    }
}
