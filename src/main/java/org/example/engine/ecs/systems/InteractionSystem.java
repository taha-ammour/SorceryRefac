package org.example.engine.ecs.systems;

import org.example.engine.Input;
import org.example.engine.ecs.ECSManager;
import org.example.engine.ecs.Entity;
import org.example.engine.ecs.System;
import org.example.engine.ecs.components.*;
import org.example.engine.utils.Logger;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;

/**
 * System that handles interaction with interactable entities
 */
public class InteractionSystem extends System {
    private final Input input;
    private Entity playerEntity = null;
    private final ECSManager ecsManager;
    private float interactionDistance = 64.0f; // Default interaction distance

    public InteractionSystem(Input input) {
        // Run after input but before movement
        super(15, InteractableComponent.class, TransformComponent.class);
        this.input = input;
        this.ecsManager = ECSManager.getInstance();
    }

    @Override
    public void begin(float deltaTime) {
        // Find the local player entity at the start of each frame
        for (Entity entity : ecsManager.getAllEntities()) {
            PlayerComponent playerComponent = entity.getComponent(PlayerComponent.class);
            if (playerComponent != null && playerComponent.isLocalPlayer()) {
                playerEntity = entity;
                break;
            }
        }
    }

    @Override
    public void processEntity(Entity entity, float deltaTime) {
        // Skip if player not found or no interaction key pressed
        if (playerEntity == null || !input.isKeyJustPressed(GLFW_KEY_E)) {
            return;
        }

        InteractableComponent interactable = entity.getComponent(InteractableComponent.class);
        TransformComponent interactableTransform = entity.getComponent(TransformComponent.class);
        TransformComponent playerTransform = playerEntity.getComponent(TransformComponent.class);

        // Check if the player is close enough to interact
        if (playerTransform == null || !isInInteractionRange(playerTransform, interactableTransform, interactable)) {
            return;
        }

        // Handle the interaction
        if (interactable.isInteractable()) {
            String interactionType = interactable.getInteractionType();
            String interactionData = interactable.getInteractionData();

            Logger.debug("Interacting with: " + interactionType + " - Data: " + interactionData);

            switch (interactionType) {
                case "dialog":
                    showDialog(entity, interactionData);
                    break;

                case "chest":
                    openChest(entity, interactionData);
                    break;

                case "door":
                    toggleDoor(entity, interactionData);
                    break;

                case "pickup":
                    pickupItem(entity, interactionData);
                    break;

                case "lever":
                    toggleLever(entity, interactionData);
                    break;

                case "npc":
                    talkToNPC(entity, interactionData);
                    break;

                case "sign":
                    readSign(entity, interactionData);
                    break;

                default:
                    genericInteraction(entity, interactionData);
                    break;
            }

            // Play interaction sound if entity has an audio component
            AudioComponent audioComponent = entity.getComponent(AudioComponent.class);
            if (audioComponent != null) {
                // Play the interaction sound
                // This would be handled by the AudioSystem
            }
        }
    }

    private boolean isInInteractionRange(TransformComponent playerTransform,
                                         TransformComponent interactableTransform,
                                         InteractableComponent interactable) {
        // Calculate distance between player and interactable
        Vector3f playerPos = playerTransform.getPosition();
        Vector3f interactablePos = interactableTransform.getPosition();

        float dx = playerPos.x - interactablePos.x;
        float dy = playerPos.y - interactablePos.y;
        float distanceSquared = dx * dx + dy * dy;

        // Check if within interaction radius
        float radius = interactable.getInteractionRadius();
        return distanceSquared <= (radius * radius);
    }

    // Methods to handle different interaction types

    private void showDialog(Entity entity, String dialogText) {
        // In a real implementation, this would trigger the UI system to show a dialog box
        Logger.info("[DIALOG] " + dialogText);
    }

    private void openChest(Entity entity, String contents) {
        // Parse contents and add items to player inventory
        InventoryComponent playerInventory = playerEntity.getComponent(InventoryComponent.class);
        if (playerInventory != null) {
            // Simple content format: "item1:quantity1,item2:quantity2"
            String[] items = contents.split(",");
            for (String item : items) {
                String[] parts = item.split(":");
                if (parts.length == 2) {
                    String itemId = parts[0];
                    int quantity = Integer.parseInt(parts[1]);
                    playerInventory.addItem(itemId, itemId, quantity);
                    Logger.debug("Added item to inventory: " + itemId + " x" + quantity);
                }
            }
        }

        // Change the chest sprite to open state
        SpriteComponent spriteComponent = entity.getComponent(SpriteComponent.class);
        if (spriteComponent != null) {
            // Change sprite to open chest
            // This would require access to the SpriteManager
        }

        // Disable further interaction with this chest
        InteractableComponent interactable = entity.getComponent(InteractableComponent.class);
        interactable.setInteractable(false);
    }

    private void toggleDoor(Entity entity, String doorData) {
        // Toggle the door state (open/closed)
        boolean isOpen = doorData.equals("open");

        // Update door data and sprite
        SpriteComponent spriteComponent = entity.getComponent(SpriteComponent.class);
        InteractableComponent interactable = entity.getComponent(InteractableComponent.class);
        CollisionComponent collision = entity.getComponent(CollisionComponent.class);

        if (isOpen) {
            // Close the door
            interactable.setInteractionData("closed");
            Logger.debug("Door closed");
            // Enable collision
            if (collision != null) {
                collision.setTrigger(false);
            }
            // Change sprite to closed door
        } else {
            // Open the door
            interactable.setInteractionData("open");
            Logger.debug("Door opened");
            // Disable collision or set as trigger
            if (collision != null) {
                collision.setTrigger(true);
            }
            // Change sprite to open door
        }
    }

    private void pickupItem(Entity entity, String itemData) {
        // Add item to player inventory
        InventoryComponent playerInventory = playerEntity.getComponent(InventoryComponent.class);
        if (playerInventory != null) {
            // Parse item data: "itemId:itemName:quantity"
            String[] parts = itemData.split(":");
            if (parts.length >= 3) {
                String itemId = parts[0];
                String itemName = parts[1];
                int quantity = Integer.parseInt(parts[2]);
                playerInventory.addItem(itemId, itemName, quantity);
                Logger.debug("Picked up: " + itemName + " x" + quantity);
            }
        }

        // Remove the item entity from the world
        ecsManager.removeEntity(entity.getId());
    }

    private void toggleLever(Entity entity, String leverData) {
        // Toggle the lever state
        boolean isOn = leverData.equals("on");

        // Update lever data and sprite
        InteractableComponent interactable = entity.getComponent(InteractableComponent.class);
        SpriteComponent spriteComponent = entity.getComponent(SpriteComponent.class);

        if (isOn) {
            // Turn lever off
            interactable.setInteractionData("off");
            Logger.debug("Lever switched OFF");
            // Change sprite to off state
        } else {
            // Turn lever on
            interactable.setInteractionData("on");
            Logger.debug("Lever switched ON");
            // Change sprite to on state
        }

        // Trigger any connected mechanism (doors, traps, etc.)
        // This would require a way to link entities together
    }

    private void talkToNPC(Entity entity, String npcData) {
        // Parse NPC data and trigger dialogue
        // This might involve a dialogue system or simple text display
        Logger.info("Talking to NPC");

        // For now, just show the NPC's dialogue
        showDialog(entity, npcData);
    }

    private void readSign(Entity entity, String signText) {
        // Display the sign text
        Logger.info("Reading sign");
        showDialog(entity, signText);
    }

    private void genericInteraction(Entity entity, String data) {
        // Generic interaction handler for custom interaction types
        Logger.info("Generic interaction: " + data);
        showDialog(entity, "Interacting with: " + data);
    }

    // Helper method to set the interaction distance
    public void setInteractionDistance(float distance) {
        this.interactionDistance = distance;
    }
}
