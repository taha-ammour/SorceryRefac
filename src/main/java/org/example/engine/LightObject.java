package org.example.engine;

import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

public class LightObject extends GameObject {
    private final Light light;

    public LightObject(Light light) {
        this.light = light;
    }

    @Override
    public void update(float deltaTime) {
        if (light.getType() == 1) { // Only move point lights
//            float radius = 200.0f;
//            float speed = 1.0f;
//            float x = (float) Math.cos(deltaTime * speed) * radius;
//            float y = (float) Math.sin(deltaTime * speed) * radius;
//            light.getPosition().set(x, y, light.getPosition().z);

        }

        // For a single light, create a list with this light and update the global lights.
        Sprite.addGlobalLight(light);
        System.out.println("Added light type: " + light.getType() + ", position: " + light.getPosition());

    }

    @Override
    public void render(Matrix4f viewProj) {
        // Light objects typically do not render anything.

    }

    @Override
    public void cleanup() {
        // Nothing to clean up.
    }

    public Light getLight() {
        return light;
    }
}
