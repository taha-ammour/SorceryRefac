package org.example.engine.ecs.systems;

import org.example.engine.ecs.Entity;
import org.example.engine.ecs.System;
import org.example.engine.ecs.components.AnimationComponent;
import org.example.engine.ecs.components.SpriteComponent;

/**
 * System that handles animation updates
 */
public class AnimationSystem extends System {

    public AnimationSystem() {
        // High priority to update animations before rendering
        super(50, AnimationComponent.class, SpriteComponent.class);
    }

    @Override
    public void processEntity(Entity entity, float deltaTime) {
        AnimationComponent animationComponent = entity.getComponent(AnimationComponent.class);
        SpriteComponent spriteComponent = entity.getComponent(SpriteComponent.class);

        // Update animation if playing
        if (animationComponent.isPlaying()) {
            animationComponent.update(deltaTime);

            // Get the current sprite frame and update the sprite component
            String frameName = animationComponent.getCurrentFrameName();
            if (frameName != null && spriteComponent.getSprite() != null) {
                // In a real implementation, you'd update the sprite here
                // This might involve getting a new sprite from the sprite manager
                // For now, we assume the sprite component already has the right sprite
            }
        }
    }
}
