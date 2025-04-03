#version 430 core
layout (location = 0) in vec2 aPos;       // Unit quad positions (from -0.5 to 0.5)
layout (location = 1) in vec2 aTexCoord;  // Texture coordinates

uniform mat4 u_VP;
uniform float u_time;
uniform int u_billboardType = 0; // 0=camera-facing, 1=velocity-aligned, 2=fixed

out vec2 TexCoord;
out vec4 vColor;
out float vLife;
out float vNormalizedLife;

struct Particle {
    vec3 pos;
    vec3 vel;
    float life;
    float size;
    vec4 color;
};

layout(std430, binding = 0) buffer Particles {
    Particle particles[];
};

// Extract camera right and up vectors from VP matrix
vec3 getCameraRight(mat4 vpMat) {
    return normalize(vec3(vpMat[0][0], vpMat[1][0], vpMat[2][0]));
}

vec3 getCameraUp(mat4 vpMat) {
    return normalize(vec3(vpMat[0][1], vpMat[1][1], vpMat[2][1]));
}

void main() {
    uint id = gl_InstanceID;
    Particle p = particles[id];

    // Only process living particles
    if (p.life <= 0.0) {
        gl_Position = vec4(0, 0, -1000, 1); // Move off-screen
        vColor = vec4(0);
        return;
    }

    // Billboarding based on type
    vec3 right, up;

    if (u_billboardType == 0) {
        // Camera-facing billboard
        right = getCameraRight(u_VP);
        up = getCameraUp(u_VP);
    }
    else if (u_billboardType == 1 && length(p.vel) > 0.01) {
        // Velocity-aligned
        vec3 velDir = normalize(p.vel);
        up = velDir;
        right = normalize(cross(velDir, vec3(0, 1, 0)));
        if (length(right) < 0.01) {
            right = normalize(cross(velDir, vec3(1, 0, 0)));
        }
        vec3 newUp = cross(right, velDir);
        up = normalize(newUp);
    }
    else {
        // Fixed orientation
        right = vec3(1, 0, 0);
        up = vec3(0, 1, 0);
    }

    // Apply a slight wobble effect to some particles
    float wobble = sin(u_time * 5.0 + float(id) * 0.1) * 0.05;
    right = normalize(right + vec3(wobble));
    up = normalize(up + vec3(wobble));

    // Apply size and calculate world position
    vec3 vertexPos = p.pos + (right * aPos.x + up * aPos.y) * p.size;
    gl_Position = u_VP * vec4(vertexPos, 1.0);

    // Pass data to fragment shader
    TexCoord = aTexCoord;
    vColor = p.color;
    vLife = p.life;
    vNormalizedLife = clamp(p.life / 5.0, 0.0, 1.0); // Assuming 5.0 is baseLife
}