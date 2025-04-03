package org.example.engine;

import org.example.ui.UIComponent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class Scene {
    private final List<GameObject> gameObjects = new ArrayList<>();
    private boolean needsSort = false;  // Flag to optimize sorting

    public void addGameObject(GameObject obj) {
        gameObjects.add(obj);
        needsSort = true;  // New object added, need to sort
    }

    public void removeGameObject(GameObject obj) {
        gameObjects.remove(obj);
    }

    public void update(float deltaTime) {
        List<GameObject> objectsCopy = new ArrayList<>(gameObjects);
        for (GameObject obj : objectsCopy) {
            obj.update(deltaTime);


            if (obj instanceof ZOrderProvider) {
                needsSort = true;
            }
        }
    }

    public <T extends GameObject> T getGameObject(Class<T> type) {
        for (GameObject obj : gameObjects) {
            if (type.isInstance(obj)) {
                return type.cast(obj);
            }
        }
        return null;
    }

    public <T extends GameObject> List<T> getGameObjects(Class<T> type) {
        List<T> results = new ArrayList<>();
        for (GameObject obj : gameObjects) {
            if (type.isInstance(obj)) {
                results.add(type.cast(obj));
            }
        }
        return results;
    }

    /**
     * Sort objects by z-order for proper rendering
     */
    private void sortGameObjects() {
        if (!needsSort) {
            return;
        }

        Collections.sort(gameObjects, Comparator.comparingDouble(obj -> {
            if (obj instanceof ZOrderProvider) {
                return ((ZOrderProvider) obj).getZ();
            }
            return 0.0;
        }));

        needsSort = false;
    }

    public void render(Matrix4f viewProjectionMatrix) {
        sortGameObjects();

        for (GameObject obj : gameObjects) {
            obj.render(viewProjectionMatrix);
        }
    }

    public void cleanup() {
        for (GameObject obj : gameObjects) {
            obj.cleanup();
        }
        gameObjects.clear();
    }
}