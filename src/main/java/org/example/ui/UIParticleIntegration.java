package org.example.ui;

import org.example.engine.*;
import org.joml.Matrix4f;

/**
 * Integrates particle effects with UI components
 */
public class UIParticleIntegration {

    /**
     * Creates a particle effect for a UI button
     * @param button The UI button to enhance
     * @param spriteManager The sprite manager
     * @param scene The scene to add the emitter to
     * @return The created particle emitter
     */
    public static SimpleParticleEmitter addButtonParticles(UIComponent button, SpriteManager spriteManager, Scene scene) {
        // Create a sparkle effect for the button
        SimpleParticleEmitter emitter = SimpleParticleEmitter.createEffect(
                spriteManager,
                SimpleParticleEmitter.EffectType.SPARKLE,
                button.x + button.width/2,
                button.y + button.height/2);

        // Customize for button
        emitter.setSpawnRate(5f);
        emitter.setParticleLife(0.3f, 0.8f);
        emitter.setParticleSize(0.5f, 1.0f);
        emitter.setVelocityX(-5f, 5f);
        emitter.setVelocityY(-5f, 5f);
        emitter.setGravity(0f);
        emitter.setBaseColor(0xFFFF00); // Gold sparkles
        emitter.setMaxParticles(20);

        // Add to scene
        scene.addGameObject(emitter);

        // The emitter will need to be updated with the button's position if it moves

        return emitter;
    }

    /**
     * Creates a particle effect for a UI panel
     * @param panel The UI panel to enhance
     * @param spriteManager The sprite manager
     * @param scene The scene to add the emitter to
     * @return The created particle emitter
     */
    public static SimpleParticleEmitter addPanelBorderEffect(UIPanel panel, SpriteManager spriteManager, Scene scene) {
        // Create a magic effect for the panel borders
        SimpleParticleEmitter emitter = SimpleParticleEmitter.createEffect(
                spriteManager,
                SimpleParticleEmitter.EffectType.MAGIC,
                panel.x, panel.y);

        // Customize for panel border
        emitter.setSpawnRate(10f);
        emitter.setParticleLife(1.0f, 2.0f);
        emitter.setParticleSize(1.0f, 1.5f);
        emitter.setVelocityX(10f, 20f); // Move along border
        emitter.setVelocityY(0f, 0f);   // Stay at same height
        emitter.setGravity(0f);
        emitter.setBaseColor(0x00AAFF); // Blue glow
        emitter.setMaxParticles(40);

        // Add to scene
        scene.addGameObject(emitter);

        return emitter;
    }

    /**
     * Creates a particle effect for a UI text component that appears when the text changes
     * @param text The UI text to enhance
     * @param spriteManager The sprite manager
     * @param scene The scene to add the emitter to
     * @return The created particle emitter
     */
    public static SimpleParticleEmitter addTextChangeEffect(UIText text, SpriteManager spriteManager, Scene scene) {
        // Create a particle emitter
        SimpleParticleEmitter emitter = SimpleParticleEmitter.createEffect(
                spriteManager,
                SimpleParticleEmitter.EffectType.SPARKLE,
                text.x, text.y);

        // Customize for text
        emitter.setSpawnRate(0f); // Don't spawn continuously
        emitter.setParticleLife(0.5f, 1.0f);
        emitter.setParticleSize(0.8f, 1.2f);
        emitter.setVelocityX(-10f, 10f);
        emitter.setVelocityY(-10f, 10f);
        emitter.setGravity(0f);
        emitter.setBaseColor(0xFFFFFF); // White sparkles
        emitter.setMaxParticles(30);

        // Add to scene
        scene.addGameObject(emitter);

        // Initially inactive
        emitter.setActive(false);

        return emitter;
    }

    /**
     * UI Particle Button - A custom button component with particle effects
     */
    public static class UIParticleButton extends UIComponent {
        private SimpleParticleEmitter hoverEmitter;
        private SimpleParticleEmitter clickEmitter;
        private SpriteManager spriteManager;
        private Scene scene;
        private UISprite backgroundSprite;
        private UIText labelText;
        private String text;
        private Input input;
        private boolean isHovered = false;
        private boolean isPressed = false;
        private boolean wasHovered = false;

        public UIParticleButton(float x, float y, float width, float height,
                                String spriteName, String text,
                                FontSheet fontSheet, Shader fontShader,
                                SpriteManager spriteManager, Input input, Scene scene) {
            super(x, y, width, height);
            this.spriteManager = spriteManager;
            this.scene = scene;
            this.text = text;
            this.input = input;

            // Create background sprite
            this.backgroundSprite = new UISprite(x, y, width, height, spriteName, spriteManager);

            // Create label text
            this.labelText = new UIText(fontSheet, fontShader, text, x + width/2 - 20, y + height/2);

            // Create emitters but don't activate them yet
            setupEmitters();
        }

        private void setupEmitters() {
            // Hover effect
            hoverEmitter = SimpleParticleEmitter.createEffect(
                    spriteManager,
                    SimpleParticleEmitter.EffectType.SPARKLE,
                    x + width/2,
                    y + height/2);

            hoverEmitter.setSpawnRate(15f);
            hoverEmitter.setParticleLife(0.3f, 0.6f);
            hoverEmitter.setParticleSize(0.5f, 1.0f);
            hoverEmitter.setVelocityX(-5f, 5f);
            hoverEmitter.setVelocityY(-5f, 5f);
            hoverEmitter.setGravity(0f);
            hoverEmitter.setBaseColor(0xFFFF00); // Gold sparkles
            hoverEmitter.setMaxParticles(20);
            hoverEmitter.setActive(false);

            scene.addGameObject(hoverEmitter);

            // Click effect
            clickEmitter = SimpleParticleEmitter.createEffect(
                    spriteManager,
                    SimpleParticleEmitter.EffectType.SPARKLE,
                    x + width/2,
                    y + height/2);

            clickEmitter.setSpawnRate(0f); // Burst only
            clickEmitter.setParticleLife(0.5f, 1.0f);
            clickEmitter.setParticleSize(1.0f, 2.0f);
            clickEmitter.setVelocityX(-20f, 20f);
            clickEmitter.setVelocityY(-20f, 20f);
            clickEmitter.setGravity(0f);
            clickEmitter.setBaseColor(0xFFFFFF); // White sparkles
            clickEmitter.setMaxParticles(30);
            clickEmitter.setActive(false);

            scene.addGameObject(clickEmitter);
        }

        @Override
        public void update(float deltaTime) {
            // Update child components
            backgroundSprite.update(deltaTime);
            labelText.update(deltaTime);

            // Get current position
            float centerX = x + width/2;
            float centerY = y + height/2;

            // Update emitter positions
            hoverEmitter.setPosition(centerX, centerY, 1.0f);
            clickEmitter.setPosition(centerX, centerY, 1.0f);

            // Check mouse state
            float mouseX = input.getMouseX();
            float mouseY = input.getMouseY();

            isHovered = mouseX >= x && mouseX <= x + width &&
                    mouseY >= y && mouseY <= y + height;

            boolean mousePressed = input.isMouseButtonDown(0); // Left mouse button

            // Handle hover effect
            if (isHovered && !wasHovered) {
                // Just started hovering
                hoverEmitter.setActive(true);
            } else if (!isHovered && wasHovered) {
                // Just stopped hovering
                hoverEmitter.setActive(false);
            }

            // Handle click effect
            if (isHovered && mousePressed && !isPressed) {
                // Just pressed button
                isPressed = true;
                clickEmitter.setActive(true);
                clickEmitter.burst(20);
                clickEmitter.setActive(false);
            } else if (!mousePressed) {
                isPressed = false;
            }

            wasHovered = isHovered;
        }

        @Override
        public void render(Matrix4f viewProj) {
            if (!visible) return;

            // Render background first
            backgroundSprite.render(viewProj);

            // Then render text
            labelText.render(viewProj);
        }

        public void setText(String text) {
            this.text = text;
            this.labelText.setText(text);
        }
    }

    /**
     * Animated text with particle effects
     */
    public static class UIParticleText extends UIText {
        private String oldText = "";
        private SimpleParticleEmitter textEmitter;
        private SpriteManager spriteManager;
        private Scene scene;

        public UIParticleText(FontSheet fontSheet, Shader fontShader, String text,
                              float x, float y, SpriteManager spriteManager, Scene scene) {
            super(fontSheet, fontShader, text, x, y);
            this.spriteManager = spriteManager;
            this.scene = scene;
            this.oldText = text;

            setupEmitter();
        }

        private void setupEmitter() {
            textEmitter = SimpleParticleEmitter.createEffect(
                    spriteManager,
                    SimpleParticleEmitter.EffectType.SPARKLE,
                    x, y);

            textEmitter.setSpawnRate(0f); // Burst on text change
            textEmitter.setParticleLife(0.5f, 1.0f);
            textEmitter.setParticleSize(0.8f, 1.2f);
            textEmitter.setVelocityX(-10f, 10f);
            textEmitter.setVelocityY(-10f, 10f);
            textEmitter.setGravity(0f);
            textEmitter.setBaseColor(0xFFFFFF); // White sparkles
            textEmitter.setMaxParticles(30);
            textEmitter.setActive(false);

            scene.addGameObject(textEmitter);
        }

        @Override
        public void setText(String newText) {
            if (!newText.equals(oldText)) {
                // Text changed, trigger particle effect
                super.setText(newText);

                textEmitter.setPosition(x, y, 1.0f);
                textEmitter.setActive(true);
                textEmitter.burst(20);
                textEmitter.setActive(false);

                oldText = newText;
            } else {
                super.setText(newText);
            }
        }

        @Override
        public void update(float deltaTime) {
            super.update(deltaTime);

            // Update emitter position if text position changes
            textEmitter.setPosition(x, y, 1.0f);
        }
    }
}