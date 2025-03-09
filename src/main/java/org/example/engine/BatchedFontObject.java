package org.example.engine;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class BatchedFontObject extends GameObject {
    private final FontSheet fontSheet;
    private final String text;
    private final float x;
    private final float y;
    private final float z;
    private float angle = 0.0f;
    private final int flags;
    private final int defaultColor;
    private final float alpha;

    // OpenGL handles for the VAO and VBO.
    private int vao;
    private int vbo;
    private int vertexCount;

    // Use your existing Shader class.
    private final Shader shader;

    public BatchedFontObject(FontSheet fontSheet, String text,
                             float x, float y, float z,
                             int flags, int defaultColor, float alpha,
                             Shader shader) {
        this.fontSheet = fontSheet;
        this.text = text;
        this.x = x;
        this.y = y;
        this.z = z;
        this.flags = flags;
        this.defaultColor = defaultColor;
        this.alpha = alpha;
        this.shader = shader;
        precomputeMesh();
    }

    /**
     * Precomputes the vertex data for the entire text string.
     * Vertex layout: [x, y, z, u, v]
     */
    private void precomputeMesh() {
        float scale = fontSheet.getScale();
        final int glyphWidth = fontSheet.getGlyphWidth();
        final int glyphHeight = fontSheet.getGlyphHeight();
        final float scaledWidth = glyphWidth * scale;
        final float scaledHeight = glyphHeight * scale;

        // Count the total number of non-whitespace glyphs (excluding newlines).
        float cursorX = x;
        float cursorY = y;
        int glyphs = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                // Move down one line.
                cursorY += (scaledHeight + 4);
                cursorX = x;
            } else if (Character.isWhitespace(c)) {
                // Just advance horizontally for spaces/tabs.
                cursorX += scaledWidth;
            } else {
                glyphs++;
                cursorX += scaledWidth;
            }
        }
        vertexCount = glyphs * 6; // 6 vertices per glyph (two triangles).

        // Each vertex = 5 floats: [pos.x, pos.y, pos.z, tex.u, tex.v].
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexCount * 5);

        // Reset cursor.
        cursorX = x;
        cursorY = y;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                // Move down one line.
                cursorY += (scaledHeight + 4);
                cursorX = x;
                continue;
            } else if (Character.isWhitespace(c)) {
                cursorX += scaledWidth;
                continue;
            }

            // Retrieve texture coords for this glyph.
            GlyphQuad quad = fontSheet.getGlyphQuad(c, flags);

            // No flipping of texture coordinates here:
            float t_u1 = quad.u1;
            float t_u2 = quad.u2;
            float t_v1 = quad.v1;
            float t_v2 = quad.v2;

            // Triangles for the glyph quad, top-left to bottom-right:
            float[] positions = {
                    // Triangle 1
                    cursorX,               cursorY,               z, t_u1, t_v1,
                    cursorX,               cursorY + scaledHeight, z, t_u1, t_v2,
                    cursorX + scaledWidth, cursorY + scaledHeight, z, t_u2, t_v2,
                    // Triangle 2
                    cursorX,               cursorY,               z, t_u1, t_v1,
                    cursorX + scaledWidth, cursorY + scaledHeight, z, t_u2, t_v2,
                    cursorX + scaledWidth, cursorY,               z, t_u2, t_v1,
            };

            buffer.put(positions);

            // Advance cursor horizontally.
            cursorX += scaledWidth;
        }

        buffer.flip();

        // Create and bind VAO/VBO.
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        int stride = 5 * Float.BYTES;
        // Position (layout = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        // TexCoord (layout = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    @Override
    public void update(float deltaTime) {
        // Static text: no per-frame updates required.
    }

    @Override
    public void render(Matrix4f viewProj) {
        // Build a model matrix with no vertical flip.
        Matrix4f model = new Matrix4f()
                .translate(x, y, z) // or .translate(x,y,z) if you prefer
                .rotateZ(angle);

        // If you actually want to position at (x,y) via the MVP:
        //   - you can either do .translate(x,y,z) here
        //   - or offset in precomputeMesh() by starting cursorX=0/cursorY=0
        //     and letting the model translate by (x,y).
        // It depends on how you prefer to handle your coordinates.
        //model.translate(x, y, 0);

        Matrix4f mvp = new Matrix4f(viewProj).mul(model);

        // Activate shader
        shader.use();

        // Set MVP
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        mvp.get(fb);
        shader.setUniformMatrix4fv("u_MVP", fb);

        // Texture unit
        shader.setUniform1i("u_Texture", 0);

        // Set color (u_Color)
        float r = ((defaultColor >> 16) & 0xFF) / 255.0f;
        float g = ((defaultColor >> 8)  & 0xFF) / 255.0f;
        float b = (defaultColor         & 0xFF) / 255.0f;
        int colorLocation = shader.getUniformLocation("u_Color");
        glUniform4f(colorLocation, r, g, b, alpha);

        // Example palette uniform
        int paletteLocation = shader.getUniformLocation("u_Palette");
        float[] palette = {
                0.2f, 0.2f, 0.2f,
                0.5f, 0.5f, 0.5f,
                0.8f, 0.8f, 0.8f,
                1.0f, 1.0f, 1.0f
        };
        glUniform3fv(paletteLocation, palette);

        // Bind the font atlas texture
        fontSheet.getFontAtlas().bindTexture();

        // Render
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);

        glUseProgram(0);
    }

    @Override
    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
