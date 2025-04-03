#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in vec3 aNormal;  // Add this line

uniform mat4 u_MVP;
uniform mat4 u_Model;

out vec2 TexCoord;
out vec3 FragPos;
out vec3 Normal;

void main() {
    vec4 worldPos = u_Model * vec4(aPos, 1.0);
    FragPos = worldPos.xyz;
    Normal = mat3(transpose(inverse(u_Model))) * aNormal;
    TexCoord = aTexCoord;
    gl_Position = u_MVP * vec4(aPos, 1.0);
}
