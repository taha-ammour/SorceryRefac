package org.example.ui;

import org.example.engine.GameObject;
import org.joml.Matrix4f;

public class UIManagerGameObject extends GameObject {
    private final UIManager uiManager;
    private int windowWidth;
    private int windowHeight;

    public UIManagerGameObject(UIManager uiManager, int windowWidth, int windowHeight) {
        this.uiManager = uiManager;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
    }

    public void setWindowSize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        uiManager.onResize(width, height);
    }

    @Override
    public void update(float deltaTime) {
        uiManager.update(deltaTime);
    }

    @Override
    public void render(Matrix4f viewProjectionMatrix) {
        Matrix4f uiProjection = new Matrix4f().setOrtho2D(0, windowWidth, windowHeight, 0);
        uiManager.render(uiProjection);
    }

    @Override
    public void cleanup() {
        // Clean up UI resources if necessary.
    }

    public UIManager getUiManager() {
        return uiManager;
    }
}
