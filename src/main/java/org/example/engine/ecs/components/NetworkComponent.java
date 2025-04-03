package org.example.engine.ecs.components;

import org.example.engine.ecs.Component;

import java.util.UUID; /**
 * Component for network synchronization
 */
public class NetworkComponent extends Component {
    private UUID networkId;
    private float syncTimer = 0f;
    private final float syncInterval = 0.05f; // 20 updates per second
    private boolean needsSync = false;

    public NetworkComponent() {
        this.networkId = UUID.randomUUID();
    }

    public NetworkComponent(UUID networkId) {
        this.networkId = networkId;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    public float getSyncTimer() {
        return syncTimer;
    }

    public void updateSyncTimer(float deltaTime) {
        syncTimer += deltaTime;
    }

    public boolean shouldSync() {
        return syncTimer >= syncInterval;
    }

    public void resetSyncTimer() {
        syncTimer = 0f;
    }

    public boolean needsSync() {
        return needsSync;
    }

    public void markForSync() {
        needsSync = true;
    }

    public void clearSyncFlag() {
        needsSync = false;
    }
}
