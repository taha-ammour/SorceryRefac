package org.example.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL43.*;

public class ParticleSystem extends GameObject {
    // Particle data layout: pos (vec3), vel (vec3), life (float), size (float), color (vec4)
    private static final int FLOATS_PER_PARTICLE = 12;

    // Reduce particle count for better performance
    private final int maxParticles;
    public static final int DEFAULT_MAX_PARTICLES = 500; // Reduced from 2 million

    private final int ssbo; // Shader Storage Buffer Object holding particle data
    private final ComputeShader computeShader;
    private final Shader renderShader;

    // VAO/VBO for drawing a unit quad (billboard)
    private int quadVAO, quadVBO, quadEBO;

    private final Random random = new Random();
    private Matrix4f vpMatrix;

    // Base control uniforms
    private float baseLife = 5.0f;
    private float baseSize = 2.0f;
    private float spawnThreshold = 0.0005f;  // Adjusted for better spawn rate control
    private float explosionForce = 50.0f;
    private Vector3f direction = new Vector3f(0.0f, 0.0f, 0.0f);
    private Vector3f gravity = new Vector3f(0.0f, -9.81f, 0.0f);
    private float damping = 0.1f;

    // Additional control uniforms for effects
    private Vector3f emitterPosition = new Vector3f(0.0f, 0.0f, 0.0f);
    private float emitterRadius = 1.0f;
    private float rotationSpeed = 0.0f;
    private float spiralFactor = 0.0f;
    private float turbulence = 0.0f;
    private float sizeOverLifetime = 1.0f;
    private float colorVariation = 1.0f;
    private int effectType = 0; // 0=fountain, 1=explosion, 2=vortex, 3=spiral, 4=rain

    // Visual control
    private int particleTexture = 0;
    private boolean useTexture = false;
    private int particleShape = 0; // 0=square, 1=circle, 2=ring, 3=star
    private float glow = 0.0f;
    private float fadeEdge = 0.1f;
    private int billboardType = 0; // 0=camera-facing, 1=velocity-aligned, 2=fixed

    // Performance monitoring
    private long lastFrameTime = 0;
    private float frameTime = 0;
    private int frameCount = 0;
    private float avgFrameTime = 0;

    // Constructors
    public ParticleSystem() {
        this(DEFAULT_MAX_PARTICLES);
    }

    public ParticleSystem(int maxParticles) {
        this.maxParticles = maxParticles;

        // Create and initialize SSBO with optimized approach
        ssbo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        glBufferData(GL_SHADER_STORAGE_BUFFER, maxParticles * FLOATS_PER_PARTICLE * Float.BYTES, GL_DYNAMIC_COPY);

        // Use a smaller buffer to initialize data in chunks to avoid massive memory allocations
        final int CHUNK_SIZE = 10000;
        final int CHUNKS = Math.min(maxParticles / CHUNK_SIZE + 1, maxParticles);

        FloatBuffer data = BufferUtils.createFloatBuffer(CHUNK_SIZE * FLOATS_PER_PARTICLE);
        for (int chunk = 0; chunk < CHUNKS; chunk++) {
            int startIdx = chunk * CHUNK_SIZE;
            int count = Math.min(CHUNK_SIZE, maxParticles - startIdx);

            data.clear();
            for (int i = 0; i < count; i++) {
                // Position (vec3)
                data.put(0).put(0).put(0);
                // Velocity (vec3)
                data.put(0).put(0).put(0);
                // Life (float) – 0 means dead
                data.put(0);
                // Size (float)
                data.put(baseSize);
                // Color (vec4) – white
                data.put(1.0f).put(1.0f).put(1.0f).put(1.0f);
            }
            data.flip();

            long offset = (long) startIdx * FLOATS_PER_PARTICLE * Float.BYTES;
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, offset, data);
        }

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, ssbo);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        // Load compute shader
        String computeSource = Shader.loadShaderSource1("/shaders/particle_compute.glsl");
        computeShader = new ComputeShader(computeSource);

        // Load render shaders
        renderShader = Shader.loadFromFiles("/shaders/particle_instanced.vs.glsl",
                "/shaders/particle_instanced.fs.glsl");

        // Create unit quad geometry for instanced rendering
        createQuad();

        lastFrameTime = System.nanoTime();
    }

    private void createQuad() {
        // A unit quad centered at (0,0) with vertices from -0.5 to 0.5
        float[] vertices = {
                // positions      // texCoords
                -0.5f, -0.5f,     0.0f, 0.0f,
                0.5f, -0.5f,      1.0f, 0.0f,
                0.5f,  0.5f,      1.0f, 1.0f,
                -0.5f,  0.5f,     0.0f, 1.0f
        };
        int[] indices = {0, 1, 2, 2, 3, 0};

        quadVAO = glGenVertexArrays();
        quadVBO = glGenBuffers();
        quadEBO = glGenBuffers();

        glBindVertexArray(quadVAO);

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadEBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        int stride = 4 * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L); // position
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES); // texCoord
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    @Override
    public void update(float deltaTime) {
        // Calculate frame time for performance monitoring
        long currentTime = System.nanoTime();
        frameTime = (currentTime - lastFrameTime) / 1_000_000.0f; // Convert to milliseconds
        lastFrameTime = currentTime;

        // Update rolling average (every 60 frames)
        frameCount++;
        avgFrameTime = avgFrameTime * 0.95f + frameTime * 0.05f;
        if (frameCount >= 60) {
            frameCount = 0;
            // Optionally log or adjust parameters if performance is poor
            if (avgFrameTime > 16.0f) { // More than 16ms per frame (less than 60 FPS)
                System.out.println("Warning: Particle system performance is below 60 FPS. Consider reducing particles.");
            }
        }

        // Bind SSBO and update particles via compute shader
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, ssbo);
        computeShader.use();

        // Set core uniforms
        computeShader.setUniform1f("u_deltaTime", deltaTime);
        computeShader.setUniform1f("u_time", (float)(System.currentTimeMillis() / 1000.0));
        computeShader.setUniform1f("u_baseLife", baseLife);
        computeShader.setUniform1f("u_baseSize", baseSize);
        computeShader.setUniform1f("u_spawnThreshold", spawnThreshold);
        computeShader.setUniform3f("u_direction", direction.x, direction.y, direction.z);
        computeShader.setUniform1f("u_explosionForce", explosionForce);
        computeShader.setUniform3f("u_gravity", gravity.x, gravity.y, gravity.z);
        computeShader.setUniform1f("u_damping", damping);

        // Set effect uniforms
        computeShader.setUniform3f("u_emitterPosition", emitterPosition.x, emitterPosition.y, emitterPosition.z);
        computeShader.setUniform1f("u_emitterRadius", emitterRadius);
        computeShader.setUniform1f("u_rotationSpeed", rotationSpeed);
        computeShader.setUniform1f("u_spiralFactor", spiralFactor);
        computeShader.setUniform1f("u_turbulence", turbulence);
        computeShader.setUniform1f("u_sizeOverLifetime", sizeOverLifetime);
        computeShader.setUniform1f("u_colorVariation", colorVariation);
        computeShader.setUniform1i("u_effectType", effectType);

        // Dispatch compute shader with optimized work groups
        int workGroupSize = 256;
        int numGroups = (maxParticles + workGroupSize - 1) / workGroupSize;
        glDispatchCompute(numGroups, 1, 1);

        // Ensure compute shader writes are visible to subsequent draws
        glMemoryBarrier(GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT | GL_SHADER_STORAGE_BARRIER_BIT);
        glUseProgram(0);
    }

    @Override
    public void render(Matrix4f viewProjectionMatrix) {
        this.vpMatrix = viewProjectionMatrix;
        renderShader.use();

        // Set view-projection matrix
        FloatBuffer vpBuf = BufferUtils.createFloatBuffer(16);
        viewProjectionMatrix.get(vpBuf);
        renderShader.setUniformMatrix4fv("u_VP", vpBuf);

        // Set time uniform for animation effects
        renderShader.setUniform1f("u_time", (float)(System.currentTimeMillis() / 1000.0));

        // Set visual control uniforms
        renderShader.setUniform1i("u_particleTexture", 0); // Texture unit 0
        renderShader.setUniform1i("u_useTexture", useTexture ? 1 : 0);
        renderShader.setUniform1i("u_particleShape", particleShape);
        renderShader.setUniform1f("u_glow", glow);
        renderShader.setUniform1f("u_fadeEdge", fadeEdge);
        renderShader.setUniform1i("u_billboardType", billboardType);

        // Bind texture if used
        if (useTexture && particleTexture != 0) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, particleTexture);
        }

        // Enable blending for transparent particles
        boolean blendingWasEnabled = glIsEnabled(GL_BLEND);
        if (!blendingWasEnabled) {
            glEnable(GL_BLEND);
        }

        // Use additive blending for glow effects
        if (glow > 0.0f) {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        } else {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }

        // Draw particles
        glBindVertexArray(quadVAO);
        glDrawElementsInstanced(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0, maxParticles);
        glBindVertexArray(0);

        // Restore previous blend state
        if (!blendingWasEnabled) {
            glDisable(GL_BLEND);
        } else {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // Reset to default blending
        }

        glUseProgram(0);
    }

    @Override
    public void cleanup() {
        glDeleteBuffers(quadVBO);
        glDeleteBuffers(quadEBO);
        glDeleteBuffers(ssbo);
        glDeleteVertexArrays(quadVAO);
        computeShader.delete();
        renderShader.delete();
    }

    // Performance metrics methods
    public float getFrameTimeMs() {
        return frameTime;
    }

    public float getAverageFrameTimeMs() {
        return avgFrameTime;
    }

    public int getMaxParticles() {
        return maxParticles;
    }

    // Method to trigger a burst of particles at a specific position
    public void emitBurst(Vector3f position, float strength, int count) {
        Vector3f oldPos = new Vector3f(emitterPosition);
        float oldThreshold = spawnThreshold;
        float oldForce = explosionForce;
        int oldType = effectType;

        // Set burst parameters
        setEmitterPosition(position);
        setEffectType(1); // Explosion type
        setSpawnThreshold(count / (float)maxParticles * 10.0f); // Approximate conversion
        setExplosionForce(strength);

        // Force an update to generate particles
        update(0.016f); // ~60 FPS

        // Restore previous settings
        setEmitterPosition(oldPos);
        setSpawnThreshold(oldThreshold);
        setExplosionForce(oldForce);
        setEffectType(oldType);
    }

    // Preset effects
    public void setPresetFountain() {
        setEffectType(0);
        setGravity(new Vector3f(0.0f, -9.81f, 0.0f));
        setBaseLife(5.0f);
        setBaseSize(1.5f);
        setSpawnThreshold(0.001f);
        setExplosionForce(20.0f);
        setEmitterRadius(1.0f);
        setDamping(0.1f);
        setColorVariation(1.0f);
        setParticleShape(1); // Circle
        setBillboardType(0); // Camera-facing
        setGlow(0.3f);
    }

    public void setPresetExplosion() {
        setEffectType(1);
        setGravity(new Vector3f(0.0f, -2.0f, 0.0f));
        setBaseLife(3.0f);
        setBaseSize(2.0f);
        setSpawnThreshold(0.01f);
        setExplosionForce(100.0f);
        setEmitterRadius(0.1f);
        setDamping(0.05f);
        setColorVariation(1.0f);
        setParticleShape(0); // Square
        setBillboardType(1); // Velocity-aligned
        setGlow(0.8f);
    }

    public void setPresetVortex() {
        setEffectType(2);
        setGravity(new Vector3f(0.0f, 0.0f, 0.0f));
        setBaseLife(8.0f);
        setBaseSize(1.0f);
        setSpawnThreshold(0.002f);
        setEmitterRadius(10.0f);
        setRotationSpeed(5.0f);
        setDamping(0.02f);
        setColorVariation(0.7f);
        setParticleShape(2); // Ring
        setBillboardType(0); // Camera-facing
        setGlow(0.5f);
    }

    public void setPresetSpiral() {
        setEffectType(3);
        setGravity(new Vector3f(0.0f, 0.1f, 0.0f));
        setBaseLife(10.0f);
        setBaseSize(1.2f);
        setSpawnThreshold(0.1015f);
        // Increase explosion force for a more dramatic spiral motion.
        setExplosionForce(20.0f);  // Changed from 10.0f
        setSpiralFactor(2.0f);
        setDamping(0.01f);
        setColorVariation(0.5f);
        setParticleShape(1); // Circle
        setBillboardType(0); // Camera-facing
        setGlow(0.4f);
    }

    public void setPresetRain() {
        setEffectType(4);
        setGravity(new Vector3f(0.0f, -20.0f, 0.0f));
        setBaseLife(3.0f);
        setBaseSize(0.8f);
        setSpawnThreshold(0.005f);
        setDamping(0.0f);
        setColorVariation(0.0f);
        setParticleShape(1); // Circle
        setBillboardType(1); // Velocity-aligned
        setGlow(0.0f);
    }

    public void setPresetSnow() {
        setEffectType(4); // Use rain effect type
        setGravity(new Vector3f(0.5f, -2.0f, 0.0f));
        setBaseLife(15.0f);
        setBaseSize(0.5f);
        setSpawnThreshold(0.003f);
        setDamping(0.2f);
        setTurbulence(0.5f);
        setColorVariation(0.0f);  // White
        setParticleShape(1); // Circle
        setBillboardType(0); // Camera-facing
        setGlow(0.1f);
    }


    public void setPresetFire() {
        // Use the fountain base (effect type 0) but adjust parameters to simulate fire.
        setEffectType(0);
        // Gravity is set upward so that particles rise.
        setGravity(new Vector3f(0.0f, 3.0f, 0.0f));
        setBaseLife(2.0f);
        setBaseSize(2.5f);
        setSpawnThreshold(0.008f);
        // Increase explosion force so that fire particles shoot upward faster.
        setExplosionForce(20.0f);  // Changed from 5.0f
        setEmitterRadius(2.0f);
        setDamping(0.05f);
        setTurbulence(0.8f);
        setSizeOverLifetime(0.5f); // Shrink over lifetime
        setColorVariation(0.3f);  // Warm colors
        setParticleShape(0); // Square – good for a fiery look
        setBillboardType(0); // Camera-facing
        setGlow(1.0f); // Full glow for fire brightness
    }


    // Getters and setters for all properties
    public void setBaseLife(float life) {
        this.baseLife = Math.max(0.1f, life);
    }

    public float getBaseLife() {
        return baseLife;
    }

    public void setBaseSize(float size) {
        this.baseSize = Math.max(0.1f, size);
    }

    public float getBaseSize() {
        return baseSize;
    }

    public void setSpawnThreshold(float threshold) {
        this.spawnThreshold = Math.max(0.0f, Math.min(0.1f, threshold));
    }

    public float getSpawnThreshold() {
        return spawnThreshold;
    }

    public void setDirection(Vector3f dir) {
        this.direction.set(dir);
    }

    public Vector3f getDirection() {
        return new Vector3f(direction);
    }

    public void setExplosionForce(float force) {
        this.explosionForce = force;
    }

    public float getExplosionForce() {
        return explosionForce;
    }

    public void setGravity(Vector3f gravity) {
        this.gravity.set(gravity);
    }

    public Vector3f getGravity() {
        return new Vector3f(gravity);
    }

    public void setDamping(float damping) {
        this.damping = Math.max(0.0f, Math.min(1.0f, damping));
    }

    public float getDamping() {
        return damping;
    }

    public void setEmitterPosition(Vector3f position) {
        this.emitterPosition.set(position);
    }

    public Vector3f getEmitterPosition() {
        return new Vector3f(emitterPosition);
    }

    public void setEmitterRadius(float radius) {
        this.emitterRadius = Math.max(0.0f, radius);
    }

    public float getEmitterRadius() {
        return emitterRadius;
    }

    public void setRotationSpeed(float speed) {
        this.rotationSpeed = speed;
    }

    public float getRotationSpeed() {
        return rotationSpeed;
    }

    public void setSpiralFactor(float factor) {
        this.spiralFactor = factor;
    }

    public float getSpiralFactor() {
        return spiralFactor;
    }

    public void setTurbulence(float turbulence) {
        this.turbulence = Math.max(0.0f, turbulence);
    }

    public float getTurbulence() {
        return turbulence;
    }

    public void setSizeOverLifetime(float factor) {
        this.sizeOverLifetime = Math.max(0.1f, factor);
    }

    public float getSizeOverLifetime() {
        return sizeOverLifetime;
    }

    public void setColorVariation(float variation) {
        this.colorVariation = Math.max(0.0f, Math.min(1.0f, variation));
    }

    public float getColorVariation() {
        return colorVariation;
    }

    public void setEffectType(int type) {
        this.effectType = Math.max(0, Math.min(4, type));
    }

    public int getEffectType() {
        return effectType;
    }

    public void setParticleTexture(int texId) {
        this.particleTexture = texId;
    }

    public int getParticleTexture() {
        return particleTexture;
    }

    public void setUseTexture(boolean useTexture) {
        this.useTexture = useTexture;
    }

    public boolean isUsingTexture() {
        return useTexture;
    }

    public void setParticleShape(int shape) {
        this.particleShape = Math.max(0, Math.min(3, shape));
    }

    public int getParticleShape() {
        return particleShape;
    }

    public void setGlow(float glow) {
        this.glow = Math.max(0.0f, Math.min(1.0f, glow));
    }

    public float getGlow() {
        return glow;
    }

    public void setFadeEdge(float fade) {
        this.fadeEdge = Math.max(0.0f, Math.min(0.5f, fade));
    }

    public float getFadeEdge() {
        return fadeEdge;
    }

    public void setBillboardType(int type) {
        this.billboardType = Math.max(0, Math.min(2, type));
    }

    public int getBillboardType() {
        return billboardType;
    }
}