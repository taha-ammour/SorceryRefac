package org.example.engine;

import org.joml.Matrix4f;

/**
 * Base class for all renderable and updatable entities.
 */
public abstract class GameObject {
    public abstract void update(float deltaTime);
    public abstract void render(Matrix4f viewProjectionMatrix);
    public void cleanup() {}
}
