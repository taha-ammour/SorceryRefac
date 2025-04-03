#version 430 core
in vec2 TexCoord;
in vec4 vColor;
in float vLife;
in float vNormalizedLife;

out vec4 FragColor;

uniform sampler2D u_particleTexture;
uniform int u_useTexture;         // 1 if texture is used, 0 for procedural
uniform float u_time;
uniform int u_particleShape = 0;  // 0=square, 1=circle, 2=ring, 3=star
uniform float u_glow = 0.0;       // 0=no glow, 1=max glow
uniform float u_fadeEdge = 0.1;   // Edge softness

// Function to create a circular particle
float makeCircle(vec2 uv) {
    float dist = length(uv - vec2(0.5));
    return smoothstep(0.5, 0.5 - u_fadeEdge, dist);
}

// Function to create a ring
float makeRing(vec2 uv) {
    float dist = length(uv - vec2(0.5));
    float outer = smoothstep(0.5, 0.5 - u_fadeEdge, dist);
    float inner = smoothstep(0.3 - u_fadeEdge, 0.3, dist);
    return outer * inner;
}

// Function to create a star
float makeStar(vec2 uv) {
    uv = uv * 2.0 - 1.0;

    // Star shape
    float angle = atan(uv.y, uv.x);
    float len = length(uv);
    float rays = abs(cos(angle * 5.0)) * 0.5 + 0.5;
    float star = smoothstep(0.5 + rays * 0.5, 0.5 + rays * 0.5 - u_fadeEdge, len);

    return star;
}

// Function to create a procedural glow
vec4 addGlow(vec4 color, float shape) {
    if (u_glow <= 0.0) return color;

    vec4 glowColor = color;
    glowColor.a *= shape * u_glow;
    return mix(color, glowColor, u_glow);
}

void main() {
    // Default texture or shape
    float shape = 1.0;

    if (u_useTexture == 1) {
        // Use provided texture
        shape = texture(u_particleTexture, TexCoord).r;
    } else {
        // Use procedural shape
        if (u_particleShape == 0) {
            // Square (with soft edges)
            vec2 uv = TexCoord;
            vec2 border = smoothstep(0.0, u_fadeEdge, uv) * smoothstep(1.0, 1.0 - u_fadeEdge, uv);
            shape = border.x * border.y;
        }
        else if (u_particleShape == 1) {
            // Circle
            shape = makeCircle(TexCoord);
        }
        else if (u_particleShape == 2) {
            // Ring
            shape = makeRing(TexCoord);
        }
        else if (u_particleShape == 3) {
            // Star
            shape = makeStar(TexCoord);
        }
    }

    // Apply base color
    vec4 finalColor = vColor;

    // Apply life-based transparency
    finalColor.a *= vNormalizedLife;

    // Apply shape
    finalColor.a *= shape;

    // Add glow effect if enabled
    finalColor = addGlow(finalColor, shape);

    // Discard fully transparent pixels for better performance
    if (finalColor.a < 0.01) discard;

    FragColor = finalColor;
}