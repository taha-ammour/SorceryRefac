#version 330 core

in vec2 TexCoord;
out vec4 FragColor;

uniform sampler2D u_Texture;
uniform vec3 u_Palette[4];
uniform vec4 u_Color;

void main() {
    vec4 c = texture(u_Texture, TexCoord);
    if (c.a < 0.0001)
    discard;

    // Determine palette index based on the red channel value.
    uint i;
    if (c.r >= (0xA0 / 255.0))
    i = 3u;
    else if (c.r >= (0x70 / 255.0))
    i = 2u;
    else if (c.r >= (0x40 / 255.0))
    i = 1u;
    else
    i = 0u;

    // Use the palette array directly.
    vec3 col = u_Palette[int(i)];
    FragColor = vec4(col, c.a) * u_Color;
}
