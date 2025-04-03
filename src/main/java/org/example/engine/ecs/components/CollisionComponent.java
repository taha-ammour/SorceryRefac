package org.example.engine.ecs.components;

import org.example.engine.ecs.Component; /**
 * Component for collision detection
 */
public class CollisionComponent extends Component {
    private float width;
    private float height;
    private boolean isTrigger = false;
    private String collisionLayer = "default";
    private String[] collidesWith = {"default"};

    public CollisionComponent(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public boolean isTrigger() {
        return isTrigger;
    }

    public void setTrigger(boolean trigger) {
        isTrigger = trigger;
    }

    public String getCollisionLayer() {
        return collisionLayer;
    }

    public void setCollisionLayer(String collisionLayer) {
        this.collisionLayer = collisionLayer;
    }

    public String[] getCollidesWith() {
        return collidesWith;
    }

    public void setCollidesWith(String[] collidesWith) {
        this.collidesWith = collidesWith;
    }

    /**
     * Check if this collider collides with another collider
     * @param other The other collider
     * @param otherTransform The transform of the other entity
     * @param thisTransform The transform of this entity
     * @return True if they collide
     */
    public boolean collidesWith(CollisionComponent other, TransformComponent otherTransform, TransformComponent thisTransform) {
        // Simple AABB collision
        float thisLeft = thisTransform.getPosition().x;
        float thisRight = thisLeft + width;
        float thisTop = thisTransform.getPosition().y;
        float thisBottom = thisTop + height;

        float otherLeft = otherTransform.getPosition().x;
        float otherRight = otherLeft + other.width;
        float otherTop = otherTransform.getPosition().y;
        float otherBottom = otherTop + other.height;

        return thisRight >= otherLeft &&
                thisLeft <= otherRight &&
                thisBottom >= otherTop &&
                thisTop <= otherBottom;
    }
}
