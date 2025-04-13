package org.example.engine;

import org.example.engine.Scene;
import org.example.engine.SimpleParticleEmitter;
import org.example.engine.SpriteManager;
import org.example.game.Player;
import org.joml.Vector3f;

/**
 * Demo helper class to quickly add particle effects to your game
 */
public class ParticleSystemDemo {

    /**
     * Creates various particle effects and adds them to the scene
     * @param scene The game scene
     * @param spriteManager The sprite manager
     */
    public static void addParticleEffectsToScene(Scene scene, SpriteManager spriteManager) {
        System.out.println("Adding particle effects to scene...");

        // Add a fire effect
        SimpleParticleEmitter fireEmitter = SimpleParticleEmitter.createEffect(
                spriteManager,
                SimpleParticleEmitter.EffectType.FIRE,
                300, 300);
        scene.addGameObject(fireEmitter);
        System.out.println("Added fire effect at (300, 300)");

        // Add a smoke effect
        SimpleParticleEmitter smokeEmitter = SimpleParticleEmitter.createEffect(
                spriteManager,
                SimpleParticleEmitter.EffectType.SMOKE,
                500, 250);
        scene.addGameObject(smokeEmitter);
        System.out.println("Added smoke effect at (500, 250)");

        // Add a sparkle effect
        SimpleParticleEmitter sparkleEmitter = SimpleParticleEmitter.createEffect(
                spriteManager,
                SimpleParticleEmitter.EffectType.SPARKLE,
                700, 300);
        scene.addGameObject(sparkleEmitter);
        System.out.println("Added sparkle effect at (700, 300)");

        // Add a magic effect
        SimpleParticleEmitter magicEmitter = SimpleParticleEmitter.createEffect(
                spriteManager,
                SimpleParticleEmitter.EffectType.MAGIC,
                400, 400);
        scene.addGameObject(magicEmitter);
        System.out.println("Added magic effect at (400, 400)");

        // Add a custom effect
        SimpleParticleEmitter customEmitter = SimpleParticleEmitter.createEffect(
                spriteManager,
                SimpleParticleEmitter.EffectType.CUSTOM,
                600, 400);

        // Customize the effect
        customEmitter.setParticleLife(0.5f, 2.0f);
        customEmitter.setParticleSize(1.0f, 3.0f);
        customEmitter.setVelocityX(-3f, 3f);
        customEmitter.setVelocityY(-15f, -5f);
        customEmitter.setGravity(0f);
        customEmitter.setBaseColor(0xFF00FF); // Purple
        customEmitter.setSpawnRate(20f);

        scene.addGameObject(customEmitter);
        System.out.println("Added custom effect at (600, 400)");
    }

    /**
     * Adds a single particle effect to the scene
     * @param scene The game scene
     * @param spriteManager The sprite manager
     * @param type The effect type
     * @param x X position
     * @param y Y position
     * @return The created emitter
     */
    public static SimpleParticleEmitter addEffect(Scene scene, SpriteManager spriteManager,
                                                  SimpleParticleEmitter.EffectType type,
                                                  float x, float y) {
        SimpleParticleEmitter emitter = SimpleParticleEmitter.createEffect(
                spriteManager, type, x, y);
        scene.addGameObject(emitter);
        System.out.println("Added " + type.name() + " effect at (" + x + ", " + y + ")");
        return emitter;
    }

    /**
     * Creates a ring of particles around a center point
     * @param scene The game scene
     * @param spriteManager The sprite manager
     * @param centerX Center X position
     * @param centerY Center Y position
     * @param radius Radius of the ring
     * @param count Number of emitters in the ring
     */
    public static void createParticleRing(Scene scene, SpriteManager spriteManager,
                                          float centerX, float centerY, float radius, int count) {
        System.out.println("Creating particle ring at (" + centerX + ", " + centerY +
                ") with radius " + radius);

        for (int i = 0; i < count; i++) {
            float angle = (float) (i * 2 * Math.PI / count);
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;

            // Alternate between effect types
            SimpleParticleEmitter.EffectType type;
            switch (i % 4) {
                case 0: type = SimpleParticleEmitter.EffectType.FIRE; break;
                case 1: type = SimpleParticleEmitter.EffectType.SPARKLE; break;
                case 2: type = SimpleParticleEmitter.EffectType.MAGIC; break;
                default: type = SimpleParticleEmitter.EffectType.SMOKE; break;
            }

            SimpleParticleEmitter emitter = SimpleParticleEmitter.createEffect(
                    spriteManager, type, x, y);

            // Make these smaller and slower for the ring
            emitter.setSpawnRate(5f);
            emitter.setParticleSize(1.0f, 2.0f);
            emitter.setMaxParticles(20);

            scene.addGameObject(emitter);
        }

        System.out.println("Created ring with " + count + " emitters");
    }
    /**
     * Add a particle trail effect that follows a player
     * @param player The player to follow
     * @param scene The game scene
     * @param spriteManager The sprite manager
     * @return The created emitter
     */
    public static SimpleParticleEmitter addPlayerTrail(Player player, Scene scene, SpriteManager spriteManager) {
        // Create a magic effect for the player trail
        SimpleParticleEmitter trailEmitter = SimpleParticleEmitter.createEffect(
                spriteManager, SimpleParticleEmitter.EffectType.MAGIC,
                player.getPosition().x, player.getPosition().y);

        // Customize the trail effect
        trailEmitter.setSpawnRate(15f);
        trailEmitter.setParticleLife(0.5f, 1.0f); // Short-lived particles
        trailEmitter.setParticleSize(1.0f, 2.0f); // Small particles
        trailEmitter.setVelocityX(-5f, 5f); // Slight sideways drift
        trailEmitter.setVelocityY(5f, 10f); // Upward drift
        trailEmitter.setGravity(0f); // No gravity

        // Set color based on player color
        String playerColor = player.getColor();
        int color;

        switch (playerColor.toUpperCase()) {
            case "RED":
                color = 0xFF5500;
                break;
            case "BLUE":
                color = 0x0088FF;
                break;
            case "GREEN":
                color = 0x00FF88;
                break;
            case "YELLOW":
                color = 0xFFFF00;
                break;
            case "PINK":
                color = 0xFF00FF;
                break;
            default:
                color = 0x00AAFF; // Default color
        }

        trailEmitter.setBaseColor(color);

        // Add to scene
        scene.addGameObject(trailEmitter);

        // The trail emitter will need to be updated with the player's position
        // in your game update loop with code like:
        // trailEmitter.setPosition(player.getPosition().x, player.getPosition().y, player.getPosition().z - 0.1f);

        return trailEmitter;
    }

    /**
     * Create an environmental effect like falling rain or snow
     * @param scene The game scene
     * @param spriteManager The sprite manager
     * @param width Width of the area to cover
     * @param height Height above the visible area to start particles
     * @param isSnow True for snow, false for rain
     */
    public static void createWeatherEffect(Scene scene, SpriteManager spriteManager,
                                           float width, float height, boolean isSnow) {
        // Position the emitter above the visible area
        float emitterX = width / 2;
        float emitterY = -100; // Above the visible area

        // Create a custom effect
        SimpleParticleEmitter weatherEmitter = SimpleParticleEmitter.createEffect(
                spriteManager, SimpleParticleEmitter.EffectType.CUSTOM, emitterX, emitterY);

        if (isSnow) {
            // Snow settings
            weatherEmitter.setSpawnRate(50f);
            weatherEmitter.setParticleLife(5.0f, 10.0f);
            weatherEmitter.setParticleSize(1.0f, 2.0f);
            weatherEmitter.setVelocityX(-20f, 20f); // Horizontal drift for snow
            weatherEmitter.setVelocityY(20f, 40f); // Downward velocity
            weatherEmitter.setGravity(5f); // Light gravity
            weatherEmitter.setBaseColor(0xFFFFFF); // White for snow
            weatherEmitter.setMaxParticles(500);
        } else {
            // Rain settings
            weatherEmitter.setSpawnRate(80f);
            weatherEmitter.setParticleLife(1.0f, 2.0f);
            weatherEmitter.setParticleSize(0.5f, 1.0f);
            weatherEmitter.setVelocityX(-5f, 5f); // Slight sideways drift
            weatherEmitter.setVelocityY(100f, 150f); // Faster downward velocity
            weatherEmitter.setGravity(20f); // Higher gravity for rain
            weatherEmitter.setBaseColor(0x88AAFF); // Light blue for rain
            weatherEmitter.setMaxParticles(800);
        }

        // Make the weather cover a wide area
        weatherEmitter.setVelocityX(-width/2, width/2);

        // Add to scene
        scene.addGameObject(weatherEmitter);
    }

    /**
     * Create an explosion effect at a position
     * @param scene The game scene
     * @param spriteManager The sprite manager
     * @param x X position
     * @param y Y position
     * @param size Size of the explosion (0.5 to 2.0 is reasonable range)
     */
    public static void createExplosion(Scene scene, SpriteManager spriteManager, float x, float y, float size) {
        // Create a fire effect for the explosion
        SimpleParticleEmitter explosionEmitter = SimpleParticleEmitter.createEffect(
                spriteManager, SimpleParticleEmitter.EffectType.FIRE, x, y);

        // Configure for an explosion
        explosionEmitter.setSpawnRate(0f); // Don't spawn continuously
        explosionEmitter.setParticleLife(0.5f, 1.5f);
        explosionEmitter.setParticleSize(2.0f * size, 5.0f * size);
        explosionEmitter.setVelocityX(-50f * size, 50f * size);
        explosionEmitter.setVelocityY(-50f * size, 50f * size);
        explosionEmitter.setGravity(-2f); // Slight upward drift
        explosionEmitter.setBaseColor(0xFF5500); // Orange for fire

        // Add to scene
        scene.addGameObject(explosionEmitter);

        // Create an initial burst of particles
        explosionEmitter.burst((int)(50 * size));

        // Also add a sparkle effect for added visual interest
        SimpleParticleEmitter sparkleEmitter = SimpleParticleEmitter.createEffect(
                spriteManager, SimpleParticleEmitter.EffectType.SPARKLE, x, y);

        // Configure for sparkles
        sparkleEmitter.setSpawnRate(0f); // Don't spawn continuously
        sparkleEmitter.setParticleLife(0.3f, 1.0f);
        sparkleEmitter.setParticleSize(1.0f * size, 2.0f * size);
        sparkleEmitter.setVelocityX(-80f * size, 80f * size);
        sparkleEmitter.setVelocityY(-80f * size, 80f * size);
        sparkleEmitter.setGravity(10f); // Fall down after explosion
        sparkleEmitter.setBaseColor(0xFFFF00); // Yellow for sparkles

        // Add to scene
        scene.addGameObject(sparkleEmitter);

        // Create an initial burst of sparkles
        sparkleEmitter.burst((int)(30 * size));

        // These emitters will automatically deactivate when all particles expire
        // Or you could explicitly deactivate them after a time with:
        // explosionEmitter.setActive(false);
        // sparkleEmitter.setActive(false);
    }

    /**
     * Create healing particles around a player
     * @param player The player to heal
     * @param scene The game scene
     * @param spriteManager The sprite manager
     */
    public static void createHealingEffect(Player player, Scene scene, SpriteManager spriteManager) {
        Vector3f position = player.getPosition();

        // Create a particle emitter
        SimpleParticleEmitter healingEmitter = SimpleParticleEmitter.createEffect(
                spriteManager, SimpleParticleEmitter.EffectType.CUSTOM,
                position.x, position.y);

        // Configure for healing effect
        healingEmitter.setSpawnRate(20f);
        healingEmitter.setParticleLife(1.0f, 2.0f);
        healingEmitter.setParticleSize(1.0f, 2.0f);
        healingEmitter.setVelocityX(-10f, 10f);
        healingEmitter.setVelocityY(-20f, -5f); // Upward movement
        healingEmitter.setGravity(-5f); // Strong upward drift
        healingEmitter.setBaseColor(0x00FF88); // Green for healing
        healingEmitter.setMaxParticles(50);

        // Add to scene
        scene.addGameObject(healingEmitter);

        // This effect should automatically stop after a few seconds
        // You could implement this with a timer in your game logic:
        //
        // float healDuration = 3.0f;
        // float healTimer = 0.0f;
        //
        // And in your update method:
        // healTimer += deltaTime;
        // if (healTimer >= healDuration) {
        //     healingEmitter.setActive(false);
        // }
    }

    /**
     * Create portal effect (circular swirling particles)
     * @param scene The game scene
     * @param spriteManager The sprite manager
     * @param x X position
     * @param y Y position
     * @param color Base color for the portal (0xRRGGBB format)
     */
    public static void createPortalEffect(Scene scene, SpriteManager spriteManager,
                                          float x, float y, int color) {
        // Create a particle emitter
        SimpleParticleEmitter portalEmitter = SimpleParticleEmitter.createEffect(
                spriteManager, SimpleParticleEmitter.EffectType.MAGIC, x, y);

        // Configure for portal effect
        portalEmitter.setSpawnRate(30f);
        portalEmitter.setParticleLife(1.0f, 2.0f);
        portalEmitter.setParticleSize(2.0f, 3.0f);
        portalEmitter.setVelocityX(-5f, 5f);
        portalEmitter.setVelocityY(-5f, 5f);
        portalEmitter.setGravity(0f); // No gravity
        portalEmitter.setBaseColor(color);
        portalEmitter.setMaxParticles(100);

        // Add to scene
        scene.addGameObject(portalEmitter);

        // Create a particle ring around the portal
        ParticleSystemDemo.createParticleRing(scene, spriteManager, x, y, 30, 8);
    }
}
