package org.example.engine.ecs.systems;



import org.example.engine.ecs.Entity;
import org.example.engine.ecs.System;
import org.example.engine.ecs.components.*;
import org.example.engine.Sprite;
import org.joml.Matrix4f;

/**
 * System that handles rendering sprites
 */
public class RenderSystem extends System {
    private final Matrix4f viewProjectionMatrix;

    public RenderSystem(Matrix4f viewProjectionMatrix) {
        // Set a lower priority (higher number) to ensure physics, input, etc. run first
        super(100, TransformComponent.class, SpriteComponent.class);
        this.viewProjectionMatrix = viewProjectionMatrix;
    }

    @Override
    public void processEntity(Entity entity, float deltaTime) {
        TransformComponent transformComponent = entity.getComponent(TransformComponent.class);
        SpriteComponent spriteComponent = entity.getComponent(SpriteComponent.class);

        Sprite sprite = spriteComponent.getSprite();
        if (sprite != null) {
            // Set sprite properties based on components
            sprite.setPosition(transformComponent.getPosition().x, transformComponent.getPosition().y);
            sprite.setZ(transformComponent.getPosition().z);
            sprite.setRotation(transformComponent.getRotation());
            sprite.setScale(transformComponent.getScale().x, transformComponent.getScale().y);
            sprite.setColor(spriteComponent.getColor(), spriteComponent.getAlpha());

            // Apply palette if set
            if (spriteComponent.getPalette() != null) {
                sprite.setPaletteFromCodes(spriteComponent.getPalette());
            }

            // Render the sprite
            sprite.render(viewProjectionMatrix);
        }
    }
}


