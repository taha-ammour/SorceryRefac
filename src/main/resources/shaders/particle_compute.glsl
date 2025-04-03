#version 430 core

layout (local_size_x = 256, local_size_y = 1, local_size_z = 1) in;

struct Particle {
    vec3 pos;
    vec3 vel;
    float life;
    float size;
    vec4 color;
};

layout (std430, binding = 0) buffer Particles {
    Particle particles[];
};

// Core uniforms
uniform float u_deltaTime;
uniform float u_time;
uniform float u_baseLife;
uniform float u_baseSize;
uniform float u_spawnThreshold;
uniform vec3 u_direction;
uniform float u_explosionForce;
uniform vec3 u_gravity;
uniform float u_damping;

// Additional control parameters
uniform vec3 u_emitterPosition = vec3(0.0, 0.0, 0.0);
uniform float u_emitterRadius = 1.0;
uniform float u_rotationSpeed = 0.0;
uniform float u_spiralFactor = 0.0;
uniform float u_turbulence = 0.0;
uniform float u_sizeOverLifetime = 1.0;
uniform float u_colorVariation = 1.0;
uniform int u_effectType = 0;  // 0=fountain, 1=explosion, 2=vortex, 3=spiral, 4=rain

// Hash function for better randomization
float hash(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float hash3D(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

// 3D noise function for turbulence
vec3 noise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    return mix(
    mix(
    mix(vec3(hash3D(i)), vec3(hash3D(i + vec3(1.0, 0.0, 0.0))), f.x),
    mix(vec3(hash3D(i + vec3(0.0, 1.0, 0.0))), vec3(hash3D(i + vec3(1.0, 1.0, 0.0))), f.x),
    f.y
    ),
    mix(
    mix(vec3(hash3D(i + vec3(0.0, 0.0, 1.0))), vec3(hash3D(i + vec3(1.0, 0.0, 1.0))), f.x),
    mix(vec3(hash3D(i + vec3(0.0, 1.0, 1.0))), vec3(hash3D(i + vec3(1.0, 1.0, 1.0))), f.x),
    f.y
    ),
    f.z
    ) * 2.0 - 1.0;
}

// Generate unique random values for a particle
vec4 randomValues(uint index) {
    float a = hash(float(index) * 12.9898 + u_time * 0.1);
    float b = hash(float(index) * 78.233 + u_time * 0.15);
    float c = hash(float(index) * 56.78 + u_time * 0.2);
    float d = hash(float(index) * 90.12 + u_time * 0.25);
    return vec4(a, b, c, d);
}

// Initialize a new particle
void initParticle(inout Particle p, uint index) {
    vec4 rand = randomValues(index);

    // Position within emitter radius
    float angle = rand.x * 2.0 * 3.14159;
    float radius = rand.y * u_emitterRadius;

    // Different initialization based on effect type
    if (u_effectType == 0) { // Fountain
        p.pos = u_emitterPosition + vec3(cos(angle) * radius, 0.0, sin(angle) * radius);
        p.vel = vec3(cos(angle) * rand.z, 3.0 + rand.w * 2.0, sin(angle) * rand.z) * u_explosionForce * 0.2;
    }
    else if (u_effectType == 1) { // Explosion
        p.pos = u_emitterPosition;
        vec3 dir = normalize(vec3(rand.x * 2.0 - 1.0, rand.y * 2.0 - 1.0, rand.z * 2.0 - 1.0));
        p.vel = dir * u_explosionForce;
    }
    else if (u_effectType == 2) { // Vortex
        p.pos = u_emitterPosition + vec3(cos(angle) * radius, rand.z * u_emitterRadius, sin(angle) * radius);
        p.vel = vec3(cos(angle), 0.1, sin(angle)) * u_explosionForce * 0.3;
    }
    else if (u_effectType == 3) { // Spiral
        p.pos = u_emitterPosition;
        p.vel = vec3(cos(angle), 0.5 + rand.z, sin(angle)) * u_explosionForce * 0.3;
    }
    else if (u_effectType == 4) { // Rain
        p.pos = u_emitterPosition + vec3((rand.x * 2.0 - 1.0) * 100.0, 50.0, (rand.y * 2.0 - 1.0) * 100.0);
        p.vel = vec3(0.0, -5.0 - rand.z * 5.0, 0.0);
    }

    // Add directional influence
    p.vel += u_direction * u_explosionForce * rand.w;

    // Life, size and color
    p.life = u_baseLife * (0.8 + rand.x * 0.4);
    p.size = u_baseSize * (0.8 + rand.y * 0.4);

    // Color with variation
    if (u_colorVariation > 0.5) {
        // Full color variation
        p.color = vec4(rand.x, rand.y, rand.z, 1.0);
    } else {
        // White with slight variation
        float colorVar = u_colorVariation * 2.0;
        p.color = vec4(
        1.0 - rand.x * colorVar * 0.2,
        1.0 - rand.y * colorVar * 0.2,
        1.0 - rand.z * colorVar * 0.2,
        1.0
        );
    }
}

void main() {
    uint index = gl_GlobalInvocationID.x;
    if(index >= particles.length()) return;

    Particle p = particles[index];

    // Life cycle management
    if (p.life > 0.0) {
        // Update living particle
        p.life -= u_deltaTime;

        // Apply forces
        p.vel += u_gravity * u_deltaTime;

        // Apply damping
        p.vel *= (1.0 - u_damping * u_deltaTime);

        // Apply turbulence
        if (u_turbulence > 0.0) {
            vec3 noiseForce = noise3D(p.pos * 0.1 + vec3(u_time * 0.2)) * u_turbulence;
            p.vel += noiseForce * u_deltaTime * 5.0;
        }

        // Apply rotational forces for effects
        if (u_rotationSpeed > 0.0) {
            float dist = length(p.pos.xz - u_emitterPosition.xz);
            vec3 toCenter = normalize(vec3(u_emitterPosition.x - p.pos.x, 0.0, u_emitterPosition.z - p.pos.z));
            vec3 perpendicular = vec3(-toCenter.z, 0.0, toCenter.x);
            p.vel += perpendicular * u_rotationSpeed * dist * u_deltaTime;
        }

        // Apply spiral force
        if (u_spiralFactor > 0.0) {
            float t = 1.0 - (p.life / u_baseLife);
            p.vel.y += u_spiralFactor * sin(t * 10.0) * u_deltaTime;
        }

        // Update position
        p.pos += p.vel * u_deltaTime;

        // Size over lifetime
        if (u_sizeOverLifetime != 1.0) {
            float lifeFactor = p.life / u_baseLife;
            if (u_sizeOverLifetime < 1.0) {
                // Shrink over lifetime
                p.size *= (1.0 - (1.0 - lifeFactor) * (1.0 - u_sizeOverLifetime) * u_deltaTime * 2.0);
            } else {
                // Grow over lifetime
                p.size *= (1.0 + (1.0 - lifeFactor) * (u_sizeOverLifetime - 1.0) * u_deltaTime * 0.5);
            }
        }

        // Alpha fadeout
        p.color.a = clamp(p.life / u_baseLife, 0.0, 1.0);

        // Simple bounds culling (wider than before)
        if (p.pos.x < -2000.0 || p.pos.x > 2000.0 ||
        p.pos.y < -2000.0 || p.pos.y > 2000.0 ||
        p.pos.z < -2000.0 || p.pos.z > 2000.0) {
            p.life = 0.0;
        }
    }
    else {
        // Spawn new particle with optimized probability calculation
        // This ensures a more even distribution of spawns per frame
        float spawnChance = u_spawnThreshold * 256.0 * 10.0; // Scale for better control
        if (hash(float(index) + u_time) < spawnChance) {
            initParticle(p, index);
        }
    }

    particles[index] = p;
}