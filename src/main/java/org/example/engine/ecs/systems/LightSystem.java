package org.example.engine.ecs.systems;

import org.example.engine.ecs.Entity;
import org.example.engine.ecs.System;
import org.example.engine.ecs.components.LightComponent;
import org.example.engine.ecs.components.TransformComponent;
import org.joml.Vector3f; /**
 * System that handles light processing
 */
public class LightSystem extends System {

    public LightSystem() {
        // Medium priority
        super(60, TransformComponent.class, LightComponent.class);
    }

    @Override
    public void begin(float deltaTime) {
        // Clear global lights before processing
        org.example.engine.Sprite.clearGlobalLights();
    }

    @Override
    public void processEntity(Entity entity, float deltaTime) {
        TransformComponent transformComponent = entity.getComponent(TransformComponent.class);
        LightComponent lightComponent = entity.getComponent(LightComponent.class);

        // Create a light object from the component
        org.example.engine.Light light = new org.example.engine.Light();
        light.setPosition(new Vector3f(
                transformComponent.getPosition().x,
                transformComponent.getPosition().y,
                transformComponent.getPosition().z
        ));

        // Set light properties
        light.setType(lightComponent.getLightType());
        light.setIntensity(lightComponent.getIntensity());

        // Convert RGB color (0xRRGGBB) to Vector3f (0-1 range)
        int color = lightComponent.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        light.setColor(new Vector3f(r, g, b));

        // Add light to global lights
        org.example.engine.Sprite.addGlobalLight(light);
    }
}
