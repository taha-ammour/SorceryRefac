package org.example.engine.ecs.systems;

import org.example.engine.Input;
import org.example.engine.ecs.Entity;
import org.example.engine.ecs.System;
import org.example.engine.ecs.components.MovementComponent;
import org.example.engine.ecs.components.NetworkComponent;
import org.example.engine.ecs.components.PlayerComponent;

import static org.lwjgl.glfw.GLFW.*;

/**
 * System that handles player input for local player entities
 */
public class PlayerInputSystem extends System {
    private final Input input;

    public PlayerInputSystem(Input input) {
        // Highest priority (lowest number) to handle input before physics
        super(10, PlayerComponent.class, MovementComponent.class);
        this.input = input;
    }

    @Override
    public void processEntity(Entity entity, float deltaTime) {
        PlayerComponent playerComponent = entity.getComponent(PlayerComponent.class);
        MovementComponent movementComponent = entity.getComponent(MovementComponent.class);

        // Only process input for the local player
        if (playerComponent.isLocalPlayer() && playerComponent.isAlive()) {
            // Update movement flags based on input
            movementComponent.setMoveUp(input.isKeyDown(GLFW_KEY_W) || input.isKeyDown(GLFW_KEY_UP));
            movementComponent.setMoveDown(input.isKeyDown(GLFW_KEY_S) || input.isKeyDown(GLFW_KEY_DOWN));
            movementComponent.setMoveLeft(input.isKeyDown(GLFW_KEY_A) || input.isKeyDown(GLFW_KEY_LEFT));
            movementComponent.setMoveRight(input.isKeyDown(GLFW_KEY_D) || input.isKeyDown(GLFW_KEY_RIGHT));

            // Set direction based on input priority
            if (movementComponent.isMoveUp()) {
                movementComponent.setDirection(1); // Up
            } else if (movementComponent.isMoveDown()) {
                movementComponent.setDirection(0); // Down
            } else if (movementComponent.isMoveRight()) {
                movementComponent.setDirection(2); // Right
            } else if (movementComponent.isMoveLeft()) {
                movementComponent.setDirection(3); // Left
            }

            // Mark network component for sync if present
            NetworkComponent networkComponent = entity.getComponent(NetworkComponent.class);
            if (networkComponent != null && movementComponent.isMoving()) {
                networkComponent.markForSync();
            }
        }
    }
}
