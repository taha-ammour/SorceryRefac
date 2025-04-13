package org.example.engine;

import org.example.game.SpellEntity;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * A simple particle emitter that creates and manages a collection of particles.
 * Can be used for effects like fire, smoke, sparkles, or ambient effects.
 */
public class SimpleParticleEmitter extends GameObject implements ZOrderProvider {
    private SpriteManager spriteManager;
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    // Emitter properties
    private Vector3f position = new Vector3f(0, 0, 0);
    private float spawnRate = 10f; // particles per second
    private float spawnTimer = 0f;
    private float maxParticles = 100;
    private boolean active = true;

    // Particle properties
    private float particleLifeMin = 1.0f;
    private float particleLifeMax = 2.0f;
    private float particleSizeMin = 2.0f;
    private float particleSizeMax = 5.0f;
    private float velocityXMin = -10f;
    private float velocityXMax = 10f;
    private float velocityYMin = -20f;
    private float velocityYMax = -5f;
    private float gravity = 9.8f;
    private int[] spriteIds = {158, 159, 160, 161}; // Default fire sprites
    private int baseColor = 0xFFFFFF;

    // Effect types
    public enum EffectType {
        FIRE, SMOKE, SPARKLE, MAGIC, CUSTOM
    }

    /**
     * Creates a simple particle emitter with default settings.
     * @param spriteManager The sprite manager to create particle sprites
     */
    public SimpleParticleEmitter(SpriteManager spriteManager) {
        this.spriteManager = spriteManager;
    }

    /**
     * Create a preset effect type
     * @param spriteManager The sprite manager
     * @param effectType The type of effect to create
     * @param x X position
     * @param y Y position
     * @return Configured particle emitter
     */
    public static SimpleParticleEmitter createEffect(SpriteManager spriteManager, EffectType effectType, float x, float y) {
        SimpleParticleEmitter emitter = new SimpleParticleEmitter(spriteManager);
        emitter.setPosition(x, y, 10);

        switch (effectType) {
            case FIRE:
                emitter.setSpawnRate(20f);
                emitter.setParticleLife(0.5f, 1.5f);
                emitter.setParticleSize(3.0f, 6.0f);
                emitter.setVelocityX(-5f, 5f);
                emitter.setVelocityY(-30f, -10f);
                emitter.setGravity(-5f); // Negative gravity for rising effect
                emitter.setBaseColor(0xFF5500); // Orange for fire
                emitter.setSpriteIds(new int[]{158, 159, 160, 161}); // Fire sprites
                break;

            case SMOKE:
                emitter.setSpawnRate(8f);
                emitter.setParticleLife(2.0f, 4.0f);
                emitter.setParticleSize(3.0f, 8.0f);
                emitter.setVelocityX(-3f, 3f);
                emitter.setVelocityY(-15f, -5f);
                emitter.setGravity(-1f); // Very slight upward drift
                emitter.setBaseColor(0x888888); // Gray for smoke
                // Use the same sprites but they'll be colored gray
                break;

            case SPARKLE:
                emitter.setSpawnRate(15f);
                emitter.setParticleLife(0.3f, 1.0f);
                emitter.setParticleSize(1.0f, 2.0f);
                emitter.setVelocityX(-20f, 20f);
                emitter.setVelocityY(-20f, 20f);
                emitter.setGravity(5f); // Light gravity
                emitter.setBaseColor(0xFFFF00); // Yellow for sparkles
                emitter.setSpriteIds(new int[]{150, 151, 152, 153}); // Particle point sprites
                break;

            case MAGIC:
                emitter.setSpawnRate(12f);
                emitter.setParticleLife(1.0f, 2.0f);
                emitter.setParticleSize(2.0f, 4.0f);
                emitter.setVelocityX(-10f, 10f);
                emitter.setVelocityY(-10f, 10f);
                emitter.setGravity(0f); // No gravity for floating effect
                emitter.setBaseColor(0x00AAFF); // Blue for magic
                emitter.setSpriteIds(new int[]{221, 222, 223, 224}); // Magic-like sprites
                break;

            case CUSTOM:
                // Use default values, caller will customize
                break;
        }

        return emitter;
    }

    /**
     * Creates a new particle with randomized properties.
     */
    private void spawnParticle() {
        if (particles.size() >= maxParticles) {
            return;
        }

        try {
            // Choose a random sprite ID from our options
            int spriteId = spriteIds[random.nextInt(spriteIds.length)];

            // Create a new sprite
            Sprite sprite = createParticleSprite(spriteId);

            if (sprite != null) {
                // Randomize particle properties
                float life = particleLifeMin + random.nextFloat() * (particleLifeMax - particleLifeMin);
                float size = particleSizeMin + random.nextFloat() * (particleSizeMax - particleSizeMin);
                float velX = velocityXMin + random.nextFloat() * (velocityXMax - velocityXMin);
                float velY = velocityYMin + random.nextFloat() * (velocityYMax - velocityYMin);

                // Apply size and position
                sprite.setScale(size, size);
                sprite.setPosition(position.x, position.y);
                sprite.setZ(position.z);

                // Apply a slight color variation
                float colorVariation = 0.2f;
                int r = Math.min(255, Math.max(0, ((baseColor >> 16) & 0xFF) + (int)(random.nextFloat() * colorVariation * 255 - colorVariation * 255 / 2)));
                int g = Math.min(255, Math.max(0, ((baseColor >> 8) & 0xFF) + (int)(random.nextFloat() * colorVariation * 255 - colorVariation * 255 / 2)));
                int b = Math.min(255, Math.max(0, ((baseColor) & 0xFF) + (int)(random.nextFloat() * colorVariation * 255 - colorVariation * 255 / 2)));
                int color = (r << 16) | (g << 8) | b;

                sprite.setColor(color, 1.0f);

                // Create and add the particle
                Particle particle = new Particle(sprite, life, new Vector2f(velX, velY));
                particles.add(particle);
            }
        } catch (Exception e) {
            System.err.println("Error spawning particle: " + e.getMessage());
        }
    }

    /**
     * Create a new sprite for a particle
     */
    private Sprite createParticleSprite(int spriteId) {
        try {
            // Get the template sprite
            Sprite templateSprite = spriteManager.getSprite(spriteId);

            if (templateSprite == null) {
                // Fallback to a common sprite
                int[] fallbackIds = {158, 159, 160, 161};
                for (int fallbackId : fallbackIds) {
                    templateSprite = spriteManager.getSprite(fallbackId);
                    if (templateSprite != null) break;
                }

                if (templateSprite == null) {
                    return null;
                }
            }

            // Create a fresh copy of the sprite
            return new Sprite(templateSprite);
        } catch (Exception e) {
            System.err.println("Error creating particle sprite: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!active) return;

        // Spawn new particles based on spawn rate
        spawnTimer += deltaTime;
        float spawnInterval = 1.0f / spawnRate;

        while (spawnTimer >= spawnInterval) {
            spawnParticle();
            spawnTimer -= spawnInterval;
        }

        // Update existing particles
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();

            // Update lifetime
            particle.life -= deltaTime;
            if (particle.life <= 0) {
                iterator.remove();
                continue;
            }

            // Update velocity with gravity
            particle.velocity.y += gravity * deltaTime;

            // Update position
            float x = particle.sprite.getPosition().x + particle.velocity.x * deltaTime;
            float y = particle.sprite.getPosition().y + particle.velocity.y * deltaTime;
            particle.sprite.setPosition(x, y);

            // Update alpha based on remaining life percentage
            float lifeRatio = particle.life / particle.initialLife;
            particle.sprite.setColor(particle.sprite.getColor(), lifeRatio);

            // Update scale for a growing/shrinking effect
            float scaleMultiplier = 0.8f + 0.4f * (float)Math.sin(lifeRatio * Math.PI);
            particle.sprite.setScale(
                    particle.initialSize * scaleMultiplier,
                    particle.initialSize * scaleMultiplier
            );
        }
    }

    @Override
    public void render(Matrix4f viewProj) {
        if (!active) return;

        // Render all particles
        for (Particle particle : particles) {
            particle.sprite.render(viewProj);
        }
    }

    @Override
    public void cleanup() {
        // Clean up resources
        particles.clear();
    }

    @Override
    public float getZ() {
        return position.z;
    }

    /**
     * Burst effect - emit many particles at once
     * @param count Number of particles to emit
     */
    public void burst(int count) {
        for (int i = 0; i < count; i++) {
            spawnParticle();
        }
    }

    // Getters and setters

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setSpawnRate(float spawnRate) {
        this.spawnRate = spawnRate;
    }

    public float getSpawnRate() {
        return spawnRate;
    }

    public void setMaxParticles(float maxParticles) {
        this.maxParticles = maxParticles;
    }

    public float getMaxParticles() {
        return maxParticles;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void setParticleLife(float min, float max) {
        this.particleLifeMin = min;
        this.particleLifeMax = max;
    }

    public void setParticleSize(float min, float max) {
        this.particleSizeMin = min;
        this.particleSizeMax = max;
    }

    public void setVelocityX(float min, float max) {
        this.velocityXMin = min;
        this.velocityXMax = max;
    }

    public void setVelocityY(float min, float max) {
        this.velocityYMin = min;
        this.velocityYMax = max;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public float getGravity() {
        return gravity;
    }

    public void setSpriteIds(int[] spriteIds) {
        this.spriteIds = spriteIds;
    }

    public int[] getSpriteIds() {
        return spriteIds;
    }

    public void setBaseColor(int color) {
        this.baseColor = color;
    }

    public int getBaseColor() {
        return baseColor;
    }

    /**
     * Inner class for a single particle
     */
    private class Particle {
        Sprite sprite;
        float life;
        float initialLife;
        float initialSize;
        Vector2f velocity;
        int color;

        public Particle(Sprite sprite, float life, Vector2f velocity) {
            this.sprite = sprite;
            this.life = life;
            this.initialLife = life;
            this.initialSize = sprite.getScaleX();
            this.velocity = velocity;
            this.color = sprite.getColor();
        }
    }
}