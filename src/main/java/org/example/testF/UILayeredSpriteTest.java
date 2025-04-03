package org.example.testF;

import org.example.engine.*;
import org.example.ui.*;
import org.joml.Matrix4f;

/**
 * Test class for demonstrating UILayeredSprite functionality
 */
public class UILayeredSpriteTest extends GameObject {
    private SpriteManager spriteManager;
    private UIManager uiManager;
    private UILayeredSprite characterSprite;
    private UILayeredSprite buttonSprite;
    private Input input;

    // Animations for the character sprite
    private Animation idleAnimation;
    private Animation walkAnimation;
    private boolean isWalking = false;

    public UILayeredSpriteTest(SpriteManager spriteManager, Input input) {
        this.spriteManager = spriteManager;
        this.input = input;
        this.uiManager = new UIManager();

        // Create animations
        createAnimations();

        // Create a character with multiple layers
        createCharacterSprite();

        // Create an animated button example
        createButtonSprite();
    }

    private void createAnimations() {
        // Idle animation (simply alternates between two frames)
        String[] idleFrames = {"player_sprite_d", "player_sprite_u"};
        idleAnimation = new Animation(idleFrames, 0.5f, true);

        // Walk animation (cycles through 4 frames)
        String[] walkFrames = {"player_sprite_d", "player_sprite_r", "player_sprite_u", "player_sprite_rr"};
        walkAnimation = new Animation(walkFrames, 0.25f, true);
    }

    private void createCharacterSprite() {
        // Create a character with multiple layers (body, hat, weapon)
        characterSprite = new UILayeredSprite(100, 100, 32, 32, spriteManager);

        // Body layer with animation
        characterSprite.addLayer("body", idleAnimation, 0, 0, 1.0f);

        // Hat layer (static sprite positioned on top of the head)
        characterSprite.addLayer("hat", "hat_idle_1", 0, -5, 2.0f);

        // Weapon layer (positioned to the right side)
        characterSprite.addLayer("weapon", "ChestE_id_1", 0, -20, 20.0f);

        // Set custom palette for the hat
        characterSprite.setLayerPalette("hat", new String[]{"000", "500", "300", "555"});

        // Scale up the character for better visibility
        characterSprite.setScale(2.0f, 2.0f);

        // Add to UI manager
        uiManager.addComponent(characterSprite, false);
    }

    private void createButtonSprite() {
        // Create a button with background and icon layers
        buttonSprite = new UILayeredSprite(300, 100, 64, 32, spriteManager);

        // Background layer
        buttonSprite.addLayer("background", "tile_w_1", 0, 0, 0);

        // Icon layer (centered on the button)
        buttonSprite.addLayer("icon", "health_1", 24, 12, 1);

        // Text indicator (uses a static sprite for demonstration)
        buttonSprite.addLayer("text", "health_1", 40, 12, 2);

        // Scale up for better visibility
        buttonSprite.setScale(1.5f, 1.5f);

        // Add to UI manager
        uiManager.addComponent(buttonSprite, false);
    }

    @Override
    public void update(float deltaTime) {
        // Toggle walking animation on key press
        if (input.isKeyJustPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE)) {
            isWalking = !isWalking;

            if (isWalking) {
                // Switch to walk animation
                characterSprite.updateLayerAnimation("body", walkAnimation);

                // Update hat position to match walk animation
                characterSprite.setLayerOffset("hat", 0, -6);

                // Update weapon visualization for walking
                characterSprite.updateLayerSprite("weapon", "ChestE_id_2");
                characterSprite.setLayerOffset("weapon", 18, 2);
            } else {
                // Switch back to idle animation
                characterSprite.updateLayerAnimation("body", idleAnimation);

                // Reset hat position
                characterSprite.setLayerOffset("hat", 0, -8);

                // Reset weapon
                characterSprite.updateLayerSprite("weapon", "ChestE_id_1");
                characterSprite.setLayerOffset("weapon", 16, 0);
            }
        }

        // Rotate button on R key press
        if (input.isKeyJustPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_R)) {
            // Toggle rotation
            float currentRotation = buttonSprite.getRotation();
            buttonSprite.setRotation(currentRotation > 0 ? 0 : (float) Math.PI / 4);
        }

        // Update UI components
        uiManager.update(deltaTime);
    }

    @Override
    public void render(Matrix4f viewProj) {
        // Create an orthographic projection matrix for UI
        Matrix4f uiProjection = new Matrix4f().setOrtho2D(0, 800, 600, 0);

        // Render UI components
        uiManager.render(uiProjection);
    }

    @Override
    public void cleanup() {
        // Cleanup not needed for this test
    }

    /**
     * Helper method to get the UILayeredSprite access for testing
     */
    public UILayeredSprite getCharacterSprite() {
        return characterSprite;
    }

    /**
     * Helper method to get the button sprite for testing
     */
    public UILayeredSprite getButtonSprite() {
        return buttonSprite;
    }
}