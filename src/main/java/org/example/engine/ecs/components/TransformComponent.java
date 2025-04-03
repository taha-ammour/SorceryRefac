package org.example.engine.ecs.components;


import org.example.engine.ecs.Component;
import org.example.engine.Animation;
import org.example.engine.Light;
import org.example.engine.Sprite;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Component that stores transform data (position, rotation, scale)
 */
public class TransformComponent extends Component {
    private Vector3f position = new Vector3f(0, 0, 0);
    private float rotation = 0.0f;
    private Vector2f scale = new Vector2f(1.0f, 1.0f);

    public TransformComponent() {
    }

    public TransformComponent(float x, float y, float z) {
        position.set(x, y, z);
    }

    public TransformComponent(Vector3f position) {
        this.position.set(position);
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public Vector2f getScale() {
        return scale;
    }

    public void setScale(float scaleX, float scaleY) {
        scale.set(scaleX, scaleY);
    }

    public void setScale(Vector2f scale) {
        this.scale.set(scale);
    }
}

