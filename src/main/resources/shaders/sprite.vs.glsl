#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoord;

uniform mat4 u_MVP;
uniform mat4 u_Model;

out vec2 TexCoord;
out vec2 FragPos;  // World-space 2D position

void main() {
    vec4 worldPos = u_Model * vec4(aPos, 1.0);
    FragPos = worldPos.xy;
    gl_Position = u_MVP * vec4(aPos, 1.0);
    TexCoord = aTexCoord;
}
