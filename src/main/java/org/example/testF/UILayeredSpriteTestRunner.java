package org.example.testF;

import org.example.engine.*;
import org.example.ui.*;

/**
 * Test runner for UILayeredSprite functionality
 */
public class UILayeredSpriteTestRunner {
    public static void main(String[] args) {
        System.out.println("Starting UILayeredSprite Test");

        // Create and initialize the engine
        Engine engine = new Engine();
        engine.init(800, 600, "UILayeredSprite Test");

        // Create a test scene
        Scene testScene = new Scene();

        // Set up sprite resources
        SpriteManager spriteManager = new SpriteManager();
        EntityRegistry.registerEntities(spriteManager);
        EntityRegistry.registerUi(spriteManager);
        EntityRegistry.registerTiles(spriteManager);

        // Create a test instance with the created sprites and add to scene
        UILayeredSpriteTest testObject = new UILayeredSpriteTest(spriteManager, engine.getInput());
        testScene.addGameObject(testObject);

        // Add instructions text
        FontSheet fontSheet = new FontSheet();
        fontSheet.setScale(2.0f);
        Shader fontShader = Shader.loadFromFiles("/shaders/sprite.vs.glsl", "/shaders/Fontsh.fs.glsl");

        // Test instructions
        String instructionsText =
                "UILayeredSprite Test\n" +
                        "----------------------\n" +
                        "Press SPACE to toggle character animation\n" +
                        "Press R to toggle button rotation\n\n" +
                        "The character sprite demonstrates multiple layers:\n" +
                        "- Body layer (animated)\n" +
                        "- Hat layer (positioned relative to body)\n" +
                        "- Weapon layer (separate z-index)\n\n" +
                        "The button demonstrates how UILayeredSprite\n" +
                        "can be used for UI elements with background,\n" +
                        "icon, and text layers.";

        BatchedFontObject instructions = new BatchedFontObject(
                fontSheet, instructionsText, 400, 200, 10, 0, 0xFFFFFF, 1.0f, fontShader);
        testScene.addGameObject(instructions);

        // Set active scene and run
        engine.setActiveScene(testScene);
        engine.run();
    }
}