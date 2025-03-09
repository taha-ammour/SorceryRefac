package org.example.engine;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Manages a collection of GameObjects.
 */
public class Scene {
    private final List<GameObject> gameObjects = new ArrayList<>();

    public void addGameObject(GameObject obj) {
        gameObjects.add(obj);
    }

    public void removeGameObject(GameObject obj) {
        gameObjects.remove(obj);
    }

    public void update(float deltaTime) {
        for (GameObject obj : gameObjects) {
            obj.update(deltaTime);
        }
    }

    public void render(Matrix4f viewProjectionMatrix) {
        Collections.sort(gameObjects, Comparator.comparingDouble(obj ->
                obj instanceof Sprite ? ((Sprite) obj).getZ() : 0.0));

        for (GameObject obj : gameObjects) {
            obj.render(viewProjectionMatrix);
        }
    }

    public void cleanup() {
        for (GameObject obj : gameObjects) {
            obj.cleanup();
        }
    }
}
