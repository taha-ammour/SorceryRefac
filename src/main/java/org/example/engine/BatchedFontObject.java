package org.example.engine;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class BatchedFontObject extends GameObject {
    private final FontSheet fontSheet;
    private String text; // May include control codes

    // World-space transform parameters
    private float x, y, z;
    private float angle;    // rotation in radians
    private float scaleX, scaleY;

    // Formatting and default color (0xRRGGBB) and alpha
    private int flags;
    private int defaultColor;
    private float alpha;

    // OpenGL handles for the batched geometry
    private int vao;
    private int vbo;
    private int vertexCount;

    // Shader (this one uses a uniform u_Color for overall tint)
    private final Shader shader;

    // We'll use the default palette from FontSheet (control codes for palette remain unchanged)
    private final String[] controlPalette;

    // A batch holds a segment of vertices that share the same uniform color.
    private static class Batch {
        int start;   // starting vertex index
        int count;   // number of vertices in this batch
        int color;   // color for this batch (0xRRGGBB)
    }
    private List<Batch> batches;

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
        this.angle = 0.0f;
        this.scaleX = 1.0f;
        this.scaleY = 1.0f;
        this.controlPalette = fontSheet.getColorPalette(); // e.g. {"555", "555", "555", "555"}
        precomputeMesh();
    }

    /**
     * Precomputes the vertex data for the entire text string.
     * Vertex layout per vertex: [x, y, z, u, v].
     * Also creates a list of batches—each batch records the starting vertex,
     * count, and color (as determined by control codes) for that segment.
     */
    private void precomputeMesh() {
        float scale = fontSheet.getScale();
        final int glyphWidth = fontSheet.getGlyphWidth();
        final int glyphHeight = fontSheet.getGlyphHeight();
        final float sw = glyphWidth * scale;
        final float sh = glyphHeight * scale;

        // We'll generate geometry in local space starting at (0,0)
        float cursorX = 0;
        float cursorY = 0;
        int currentFlags = flags;
        int currentColor = defaultColor;

        // List to store batches.
        batches = new ArrayList<>();
        Batch currentBatch = new Batch();
        currentBatch.start = 0;
        currentBatch.count = 0;
        currentBatch.color = currentColor;

        // First pass: Determine total glyph count and split into batches.
        int glyphCount = 0;
        for (int i = 0; i < text.length(); ) {
            char c = text.charAt(i);
            if (c == '$' && i + 3 < text.length()) {
                if (text.charAt(i + 1) == '$') {
                    // Literal '$'
                    glyphCount++;
                    currentBatch.count += 6;
                    i += 2;
                    cursorX += sw;
                } else {
                    String code = text.substring(i + 1, i + 4);
                    // If code matches a color change, flush current batch and update currentColor.
                    if (code.matches("[0-5]{3}")) {
                        if (currentBatch.count > 0) {
                            batches.add(currentBatch);
                            currentBatch = new Batch();
                            currentBatch.start = glyphCount * 6;
                        }
                        int r = code.charAt(0) - '0';
                        int g = code.charAt(1) - '0';
                        int b = code.charAt(2) - '0';
                        r *= 51; g *= 51; b *= 51;
                        currentColor = (r << 16) | (g << 8) | b;
                        currentBatch.color = currentColor;
                    } else {
                        // Process other control codes normally.
                        switch (code) {
                            case "ITA": currentFlags |= FontSheet.FONT_ITALIC; break;
                            case "REG": currentFlags &= ~FontSheet.FONT_ITALIC; break;
                            case "SIN": currentFlags &= ~FontSheet.FONT_DOUBLED; break;
                            case "DBL": currentFlags |= FontSheet.FONT_DOUBLED; break;
                            case "RES": currentFlags &= ~(FontSheet.FONT_ITALIC | FontSheet.FONT_DOUBLED); break;
                            case "CTX": currentFlags |= FontSheet.FONT_CENTER_X; break;
                            case "PCT": /* Will render literal '%' below */ break;
                        }
                    }
                    i += 4;
                    continue;
                }
            } else if (c == '\n') {
                cursorY += sh + 4;
                cursorX = 0;
                i++;
            } else if (Character.isWhitespace(c)) {
                cursorX += sw;
                i++;
            } else {
                glyphCount++;
                currentBatch.count += 6;
                cursorX += sw;
                i++;
            }
        }
        // Add final batch if it has any vertices.
        if (currentBatch.count > 0) {
            batches.add(currentBatch);
        }

        vertexCount = glyphCount * 6;

        // Second pass: Build vertex buffer.
        // Reset cursor and formatting.
        cursorX = 0;
        cursorY = 0;
        currentFlags = flags;
        currentColor = defaultColor;
        // We'll not rebuild the batches here—they are already computed.
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexCount * 5);

        for (int i = 0; i < text.length(); ) {
            char c = text.charAt(i);
            if (c == '$' && i + 3 < text.length()) {
                if (text.charAt(i + 1) == '$') {
                    c = '$';
                    i += 2;
                } else {
                    String code = text.substring(i + 1, i + 4);
                    // Process control code.
                    if (code.matches("[0-5]{3}")) {
                        // Color change: already handled in batch splitting.
                    } else {
                        switch (code) {
                            case "ITA": currentFlags |= FontSheet.FONT_ITALIC; break;
                            case "REG": currentFlags &= ~FontSheet.FONT_ITALIC; break;
                            case "SIN": currentFlags &= ~FontSheet.FONT_DOUBLED; break;
                            case "DBL": currentFlags |= FontSheet.FONT_DOUBLED; break;
                            case "RES": currentFlags &= ~(FontSheet.FONT_ITALIC | FontSheet.FONT_DOUBLED); break;
                            case "CTX": currentFlags |= FontSheet.FONT_CENTER_X; break;
                            case "PCT": c = '%'; break;
                        }
                    }
                    i += 4;
                    continue;
                }
            } else if (c == '\n') {
                cursorY += sh + 4;
                cursorX = 0;
                i++;
                continue;
            } else if (Character.isWhitespace(c)) {
                cursorX += sw;
                i++;
                continue;
            }
            // For a visible glyph, get UV coordinates.
            GlyphQuad quad = fontSheet.getGlyphQuad(c, currentFlags);
            float t_u1 = quad.u1, t_u2 = quad.u2;
            float t_v1 = quad.v1, t_v2 = quad.v2;
            float[] verts = {
                    // Triangle 1
                    cursorX,         cursorY,         0.0f, t_u1, t_v1,
                    cursorX,         cursorY + sh,    0.0f, t_u1, t_v2,
                    cursorX + sw,    cursorY + sh,    0.0f, t_u2, t_v2,
                    // Triangle 2
                    cursorX,         cursorY,         0.0f, t_u1, t_v1,
                    cursorX + sw,    cursorY + sh,    0.0f, t_u2, t_v2,
                    cursorX + sw,    cursorY,         0.0f, t_u2, t_v1,
            };
            buffer.put(verts);
            cursorX += sw;
            i++;
        }
        buffer.flip();

        if (vao == 0) {
            vao = glGenVertexArrays();
        }
        glBindVertexArray(vao);
        if (vbo == 0) {
            vbo = glGenBuffers();
        }
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        int stride = 5 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    @Override
    public void update(float deltaTime) {
        // For static text, no per-frame update is needed unless text changes.
    }

    @Override
    public void render(Matrix4f viewProj) {
        // Build a model matrix for world-space positioning.
        Matrix4f model = new Matrix4f()
                .translate(x, y, z)
                .rotateZ(angle)
                .scale(scaleX, scaleY, 1.0f);
        Matrix4f mvp = new Matrix4f(viewProj).mul(model);
        shader.use();
        FloatBuffer mvpBuf = BufferUtils.createFloatBuffer(16);
        mvp.get(mvpBuf);
        shader.setUniformMatrix4fv("u_MVP", mvpBuf);
        int modelLoc = shader.getUniformLocation("u_Model");
        if (modelLoc != -1) {
            FloatBuffer modelBuf = BufferUtils.createFloatBuffer(16);
            model.get(modelBuf);
            glUniformMatrix4fv(modelLoc, false, modelBuf);
        }
        // Set a global color uniform.
        float r = ((defaultColor >> 16) & 0xFF) / 255.0f;
        float g = ((defaultColor >> 8) & 0xFF) / 255.0f;
        float b = (defaultColor & 0xFF) / 255.0f;
        int colorLoc = shader.getUniformLocation("u_Color");
        if (colorLoc != -1) {
            // We set u_Color to white so that the per-batch color (via u_Color) takes effect.
            glUniform4f(colorLoc, 1.0f, 1.0f, 1.0f, alpha);
        }
        // Set palette uniform (here we force index 0 to be white).
        int paletteLoc = shader.getUniformLocation("u_Palette");
        if (paletteLoc != -1) {
            float[] palette = {
                    1.0f, 1.0f, 1.0f,
                    0.7f, 0.7f, 0.7f,
                    0.9f, 0.9f, 0.9f,
                    1.0f, 1.0f, 1.0f
            };
            glUniform3fv(paletteLoc, palette);
        }
        // Bind the font atlas texture.
        fontSheet.getFontAtlas().bindTexture();
        glBindVertexArray(vao);
        // Now, draw by iterating through each batch.
        int vertexOffset = 0;
        for (Batch batch : batches) {
            // Set the uniform color for this batch.
            float br = ((batch.color >> 16) & 0xFF) / 255.0f;
            float bg = ((batch.color >> 8) & 0xFF) / 255.0f;
            float bb = (batch.color & 0xFF) / 255.0f;
            // Multiply by the global tint stored in u_Color (here we assume u_Color is white in our shader)
            glUniform4f(colorLoc, br, bg, bb, alpha);
            glDrawArrays(GL_TRIANGLES, batch.start, batch.count);
            vertexOffset += batch.count;
        }
        glBindVertexArray(0);
        glUseProgram(0);
    }

    @Override
    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }

    // Dynamic setters.
    public void setText(String newText) {
        if (!this.text.equals(newText)) {
            this.text = newText;
            precomputeMesh();
        }
    }

    public String getText() {
        return text;
    }

    public void setPosition(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    public void setRotation(float angle) { this.angle = angle; }
    public void setScale(float sx, float sy) { this.scaleX = sx; this.scaleY = sy; }
    public void setColor(int color, float alpha) { this.defaultColor = color; this.alpha = alpha; }
}
