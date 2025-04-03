package org.example.engine.ecs.components;

import org.example.engine.ecs.Component; /**
 * Component for tile-based entities
 */
public class TileComponent extends Component {
    private int tileId;
    private boolean isWalkable = true;
    private boolean isInteractable = false;

    public TileComponent(int tileId) {
        this.tileId = tileId;
    }

    public int getTileId() {
        return tileId;
    }

    public void setTileId(int tileId) {
        this.tileId = tileId;
    }

    public boolean isWalkable() {
        return isWalkable;
    }

    public void setWalkable(boolean walkable) {
        isWalkable = walkable;
    }

    public boolean isInteractable() {
        return isInteractable;
    }

    public void setInteractable(boolean interactable) {
        isInteractable = interactable;
    }
}
