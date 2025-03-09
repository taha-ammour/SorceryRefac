package org.example.engine;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Sprite that automatically sets u_RefGray from the Texture's detected grayscale values.
 * The fragment shader uses a nearest-color approach to map each grayscale to a palette color.
 */
public class Sprite extends GameObject {
    private final Texture texture;
    private final float u0, v0, u1, v1;
    private final float width, height;

    private int vaoId, vboId, eboId;
    private Shader shader;

    // Transform
    private float x;
    private float y;
    private static float z = 0.0f;
    private float rotation = 0.0f;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;

    private int color = 0xFFFFFF;
    private float alpha = 1.0f;

    // Uniform locations
    private int u_MVPLoc;
    private int u_TextureLoc;
    private int u_PaletteLoc;
    private int u_ColorLoc;

    // Local copy for the palette
    private final float[] paletteFloats = new float[12]; // 4 * 3

    public Sprite(Texture texture, float u0, float v0, float u1, float v1,
                  float width, float height) {
        this.texture = texture;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        this.width = width;
        this.height = height;

        createShader();
        setupBuffers();


    }

    private void createShader() {
        shader = Shader.loadFromFiles("/shaders/sprite.vs.glsl", "/shaders/sprite.fs.glsl");
        shader.use();
        u_MVPLoc     = shader.getUniformLocation("u_MVP");
        u_TextureLoc = shader.getUniformLocation("u_Texture");
        u_PaletteLoc = shader.getUniformLocation("u_Palette[0]");
        u_ColorLoc = shader.getUniformLocation("u_Color");
        glUseProgram(0);
    }

    private void setupBuffers() {
        float[] vertices = {
                //  X,     Y,     Z,     U,   V
                0.0f,   0.0f,   0.0f,   u0,  v0,
                width,  0.0f,   0.0f,   u1,  v0,
                width,  height, 0.0f,   u1,  v1,
                0.0f,   height, 0.0f,   u0,  v1
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // VBO
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer fb = BufferUtils.createFloatBuffer(vertices.length);
        fb.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        // EBO
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        java.nio.IntBuffer ib = BufferUtils.createIntBuffer(indices.length);
        ib.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        // Position attribute (0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        // Texture coords (1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }



    @Override
    public void update(float deltaTime) {
        // Optional sprite logic
    }

    @Override
    public void render(Matrix4f viewProjectionMatrix) {
        Matrix4f model = new Matrix4f()
                .translate(x, y, 0)
                .rotateZ(rotation)
                .scale(scaleX, scaleY, 1.0f);
        Matrix4f mvp = new Matrix4f(viewProjectionMatrix).mul(model);

        shader.use();

        // MVP
        FloatBuffer matBuf = BufferUtils.createFloatBuffer(16);
        mvp.get(matBuf);
        glUniformMatrix4fv(u_MVPLoc, false, matBuf);

        // Set per-sprite color uniform.
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        glUniform4f(u_ColorLoc, r, g, b, alpha);

        // Texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture.getId());
        glUniform1i(u_TextureLoc, 0);

        // Draw
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        glUseProgram(0);
    }

    @Override
    public void cleanup() {
        glDeleteVertexArrays(vaoId);
        glDeleteBuffers(vboId);
        glDeleteBuffers(eboId);
        shader.delete();
    }

    // Transform setters
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    public void setRotation(float angle) {
        this.rotation = angle;
    }

    public void setScale(float sx, float sy) {
        this.scaleX = sx;
        this.scaleY = sy;
    }

    public void setZ(float z) {
        this.z = z;
    }
    /**
     * Sets the color and alpha for this sprite.
     *
     * @param color An integer in 0xRRGGBB format.
     * @param alpha A float in the range [0.0, 1.0] representing opacity.
     */
    public void setColor(int color, float alpha) {
        this.color = color;
        this.alpha = alpha;
    }


    // Palette setter
    public void setPalette(float[][] palette) {
        if (palette == null || palette.length != 4)
            throw new IllegalArgumentException("Need exactly 4 palette colors");
        for (int i = 0; i < 4; i++) {
            paletteFloats[i*3+0] = palette[i][0];
            paletteFloats[i*3+1] = palette[i][1];
            paletteFloats[i*3+2] = palette[i][2];
        }
        shader.use();
        FloatBuffer buf = BufferUtils.createFloatBuffer(12);
        buf.put(paletteFloats).flip();
        glUniform3fv(u_PaletteLoc, buf);
        glUseProgram(0);
    }

    /**
     * Sets the palette for this sprite using an array of 4 three-digit strings.
     * Each string must be exactly 3 characters long (digits '0' to '5').
     * For example, "012" is converted to (0/5, 1/5, 2/5).
     *
     * @param paletteCodes An array of 4 palette code strings.
     */
    public void setPaletteFromCodes(String[] paletteCodes) {
        if (paletteCodes == null || paletteCodes.length != 4) {
            throw new IllegalArgumentException("Expected exactly 4 palette codes.");
        }
        float[] newPalette = new float[12]; // 4 colors * 3 channels
        for (int i = 0; i < 4; i++) {
            String code = paletteCodes[i];
            if (code == null) {
                throw new IllegalArgumentException("Palette code at index " + i + " is null.");
            }
            code = code.trim();  // Remove any leading/trailing whitespace
            if (code.length() != 3) {
                throw new IllegalArgumentException("Palette code at index " + i + " must be exactly 3 characters long.");
            }
            newPalette[i * 3 + 0] = (code.charAt(0) - '0') / 5.0f;
            newPalette[i * 3 + 1] = (code.charAt(1) - '0') / 5.0f;
            newPalette[i * 3 + 2] = (code.charAt(2) - '0') / 5.0f;
        }
        shader.use();
        FloatBuffer paletteBuffer = BufferUtils.createFloatBuffer(12);
        paletteBuffer.put(newPalette).flip();
        glUniform3fv(u_PaletteLoc, paletteBuffer);
        glUseProgram(0);
    }


    public double getZ() {
        return z;
    }

    public float getU0() { return  u0; }
    public float getU1() { return u1; }
    public float getV0() { return v0; }
    public float getV1() { return v1; }
    public Shader getShader() { return shader; }
}
