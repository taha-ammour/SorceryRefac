package org.example;

import org.example.engine.GameObject;
import org.joml.Matrix4f;

public class GameWorldObject extends GameObject {
    private final org.example.GameWorld gameWorld;

    public GameWorldObject(org.example.GameWorld gameWorld) {
        this.gameWorld = gameWorld;
    }

    @Override
    public void update(float deltaTime) {
        if (gameWorld != null) {
            gameWorld.update(deltaTime);
        }
    }

    @Override
    public void render(Matrix4f viewProj) {
        // Nothing to render
    }

    @Override
    public void cleanup() {
        // Nothing to clean up
    }
}