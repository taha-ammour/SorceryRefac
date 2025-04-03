package org.example.engine.ecs.systems;

import org.example.engine.ecs.Entity;
import org.example.engine.ecs.System;
import org.example.engine.ecs.components.MovementComponent;
import org.example.engine.ecs.components.NetworkComponent;
import org.example.engine.ecs.components.TransformComponent;

/**
 * System that handles network synchronization
 */
public class NetworkSystem extends System {
    private final com.esotericsoftware.kryonet.Client client;

    public NetworkSystem(com.esotericsoftware.kryonet.Client client) {
        // Medium-high priority
        super(40, NetworkComponent.class, TransformComponent.class);
        this.client = client;
    }

    @Override
    public void processEntity(Entity entity, float deltaTime) {
        NetworkComponent networkComponent = entity.getComponent(NetworkComponent.class);
        TransformComponent transformComponent = entity.getComponent(TransformComponent.class);

        // Update sync timer
        networkComponent.updateSyncTimer(deltaTime);

        // Check if we need to sync this entity
        if ((networkComponent.needsSync() || networkComponent.shouldSync()) && client != null && client.isConnected()) {
            // Create a position update packet
            org.example.Packets.PlayerPositionUpdate update = new org.example.Packets.PlayerPositionUpdate();
            update.playerId = networkComponent.getNetworkId().toString();
            update.x = transformComponent.getPosition().x;
            update.y = transformComponent.getPosition().y;

            // Add direction if entity has movement component
            MovementComponent movementComponent = entity.getComponent(MovementComponent.class);
            if (movementComponent != null) {
                update.direction = movementComponent.getDirection();
                update.isMoving = movementComponent.isMoving();
            }

            // Send the update packet
            client.sendUDP(update);

            // Reset sync flags and timer
            networkComponent.resetSyncTimer();
            networkComponent.clearSyncFlag();
        }
    }
}
