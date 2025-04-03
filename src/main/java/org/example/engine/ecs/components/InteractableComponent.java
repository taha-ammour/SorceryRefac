package org.example.engine.ecs.components;

import org.example.engine.ecs.Component; /**
 * Component for interactable objects
 */
public class InteractableComponent extends Component {
    private float interactionRadius = 32.0f;
    private boolean isInteractable = true;
    private String interactionType = "none";
    private String interactionData = "";

    public InteractableComponent() {
    }

    public InteractableComponent(String interactionType) {
        this.interactionType = interactionType;
    }

    public float getInteractionRadius() {
        return interactionRadius;
    }

    public void setInteractionRadius(float interactionRadius) {
        this.interactionRadius = interactionRadius;
    }

    public boolean isInteractable() {
        return isInteractable;
    }

    public void setInteractable(boolean interactable) {
        isInteractable = interactable;
    }

    public String getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(String interactionType) {
        this.interactionType = interactionType;
    }

    public String getInteractionData() {
        return interactionData;
    }

    public void setInteractionData(String interactionData) {
        this.interactionData = interactionData;
    }

    /**
     * Check if a point is within interaction radius
     * @param x X coordinate
     * @param y Y coordinate
     * @param transformComponent The transform of this entity
     * @return True if the point is within interaction radius
     */
    public boolean canInteractFrom(float x, float y, TransformComponent transformComponent) {
        float dx = x - transformComponent.getPosition().x;
        float dy = y - transformComponent.getPosition().y;
        float distanceSquared = dx * dx + dy * dy;
        return distanceSquared <= interactionRadius * interactionRadius;
    }
}
