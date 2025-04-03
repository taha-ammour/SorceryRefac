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

    // If glyph is black, it picks palette index 0, etc.
    uint i;
    if (c.r >= (0xA0 / 255.0))
    i = 3u;
    else if (c.r >= (0x70 / 255.0))
    i = 2u;
    else if (c.r >= (0x40 / 255.0))
    i = 1u;
    else
    i = 0u;

    // For black glyph, you probably want the palette index to be bright:
    vec3 baseColor = u_Palette[int(i)];

    // Multiply by user color.
    FragColor = vec4(baseColor, c.a) * u_Color;
}
