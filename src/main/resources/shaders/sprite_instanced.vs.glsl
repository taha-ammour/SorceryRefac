#version 330 core
layout (location = 0) in vec2 aPos;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in vec4 aInstanceRow0;
layout (location = 3) in vec4 aInstanceRow1;
layout (location = 4) in vec4 aInstanceRow2;
layout (location = 5) in vec4 aInstanceRow3;

uniform mat4 u_MVP;

out vec2 TexCoord;

void main() {
    mat4 model = mat4(aInstanceRow0, aInstanceRow1, aInstanceRow2, aInstanceRow3);
    gl_Position = u_MVP * model * vec4(aPos, 0.0, 1.0);
    TexCoord = aTexCoord;
}
