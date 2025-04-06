package org.example.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Sprite extends GameObject implements ZOrderProvider  {
    private final Texture texture;
    private final float u0, v0, u1, v1;
    private final float width, height;

    private int vaoId, vboId, eboId;
    private static Shader shader;

    // Transform parameters
    private float x;
    private float y;
    private float z = 0.0f; // Leave z as requested.
    private float rotation = 0.0f;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private Material material;
    private boolean FlipX = false;
    private boolean FlipY = false;

    // Instead of a single light, we use a list of lights.
    private static List<Light> globalLights = new ArrayList<>();

    private int color = 0xFFFFFF;
    private float alpha = 1.0f;

    // Uniform locations
    private int u_MVPLoc;
    private int u_ModelLoc;
    private int u_TextureLoc;
    private int u_PaletteLoc;
    private int u_ColorLoc;
    private int u_AmbientColorLoc;
    private int u_ViewPosLoc;
    private int u_specularLoc;
    private int u_shininessLoc;

    private static int u_flip_x_Loc;
    private static int u_flip_y_Loc;
    private static int u_texCoordsLoc;

    // Added: Cache for light uniform locations
    private static boolean uniformLocationsCached = false;
    private static final int MAX_LIGHTS = 10; // Adjust based on your needs
    private static int[] lightPositionLoc = new int[MAX_LIGHTS];
    private static int[] lightColorLoc = new int[MAX_LIGHTS];
    private static int[] lightDirectionLoc = new int[MAX_LIGHTS];
    private static int[] lightIntensityLoc = new int[MAX_LIGHTS];
    private static int[] lightConstantLoc = new int[MAX_LIGHTS];
    private static int[] lightLinearLoc = new int[MAX_LIGHTS];
    private static int[] lightQuadraticLoc = new int[MAX_LIGHTS];
    private static int[] lightCutoffLoc = new int[MAX_LIGHTS];
    private static int[] lightOuterCutoffLoc = new int[MAX_LIGHTS];
    private static int[] lightTypeLoc = new int[MAX_LIGHTS];
    private static int lightCountLoc;

    // Local copy for the palette (4 colors * 3 channels)
    private final float[] paletteFloats = new float[12];

    // Local view position (declared only once)
    private final Vector3f viewPos = new Vector3f();

    private static final Map<String, GeometryData> geometryCache = new HashMap<>();

    public boolean isFlipY() {
        return FlipY;
    }

    public void setFlipY(boolean flipY) {
        FlipY = flipY;
    }

    public boolean isFlipX() {
        return FlipX;
    }

    public void setFlipX(boolean flipX) {
        FlipX = flipX;
    }

    private static class GeometryData {
        int vaoId, vboId, eboId;
    }

    public Sprite(Texture texture, float u0, float v0, float u1, float v1,
                  float width, float height) {
        this.texture = texture;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        this.width = width;
        this.height = height;

        material = new Material();
        material.setDiffuse(texture);
        material.setAmbient(new Vector3f(0.1f, 0.1f, 0.1f));
        material.setSpecular(new Vector3f(0.0f, 0.0f, 0.0f));
        material.setShininess(1.0f);

        createShader();
        setupBuffers();
    }

    public Sprite(Sprite original) {
        // Copy the texture and UV coordinates
        this.texture = original.texture;
        this.u0 = original.getU0();
        this.v0 = original.getV0();
        this.u1 = original.getU1();
        this.v1 = original.getV1();
        this.width = original.width;
        this.height = original.height;

        // Copy material properties
        this.material = new Material();
        this.material.setDiffuse(original.material.getDiffuse());
        this.material.setAmbient(new Vector3f(original.material.getAmbient()));
        this.material.setSpecular(new Vector3f(original.material.getSpecular()));
        this.material.setShininess(original.material.getShininess());

        // Initialize other properties
        this.x = original.x;
        this.y = original.y;
        this.z = original.z;
        this.rotation = original.rotation;
        this.scaleX = original.scaleX;
        this.scaleY = original.scaleY;
        this.color = original.color;
        this.alpha = original.alpha;
        this.FlipX = original.isFlipX();
        this.FlipY = original.isFlipY();

        // Copy palette
        System.arraycopy(original.paletteFloats, 0, this.paletteFloats, 0, this.paletteFloats.length);

        // Recreate shader and buffers
        createShader();
        setupBuffers();
    }

    private void createShader() {
        if (shader == null) {
            shader = Shader.loadFromFiles("/shaders/sprite.vs.glsl", "/shaders/sprite.fs.glsl");
            initU_locShader();

            // Cache uniform locations
            cacheUniformLocations();
            glUseProgram(0);
        }
        else {
            // Just get the uniform locations from the existing shader
            initU_locShader();
            glUseProgram(0);
        }

    }

    private void initU_locShader() {
        shader.use();
        u_MVPLoc = shader.getUniformLocation("u_MVP");
        u_ModelLoc = shader.getUniformLocation("u_Model");
        u_TextureLoc = shader.getUniformLocation("u_Texture");
        u_PaletteLoc = shader.getUniformLocation("u_Palette[0]");
        u_ColorLoc = shader.getUniformLocation("u_Color");
        u_AmbientColorLoc = shader.getUniformLocation("u_AmbientColor");
        u_ViewPosLoc = shader.getUniformLocation("u_ViewPos");
        u_specularLoc = shader.getUniformLocation("u_Specular");
        u_shininessLoc = shader.getUniformLocation("u_Shininess");
        u_flip_x_Loc = shader.getUniformLocation("u_flipX");
        u_flip_y_Loc = shader.getUniformLocation("u_flipY");
        u_texCoordsLoc = shader.getUniformLocation("u_texCoords");

    }

    private void cacheUniformLocations() {
        if (!uniformLocationsCached) {
            shader.use();

            // Cache light uniform locations
            lightCountLoc = shader.getUniformLocation("lightCount");

            for (int i = 0; i < MAX_LIGHTS; i++) {
                String prefix = "lights[" + i + "].";
                lightPositionLoc[i] = shader.getUniformLocation(prefix + "position");
                lightColorLoc[i] = shader.getUniformLocation(prefix + "color");
                lightDirectionLoc[i] = shader.getUniformLocation(prefix + "direction");
                lightIntensityLoc[i] = shader.getUniformLocation(prefix + "intensity");
                lightConstantLoc[i] = shader.getUniformLocation(prefix + "constant");
                lightLinearLoc[i] = shader.getUniformLocation(prefix + "linear");
                lightQuadraticLoc[i] = shader.getUniformLocation(prefix + "quadratic");
                lightCutoffLoc[i] = shader.getUniformLocation(prefix + "cutoff");
                lightOuterCutoffLoc[i] = shader.getUniformLocation(prefix + "outerCutoff");
                lightTypeLoc[i] = shader.getUniformLocation(prefix + "type");
            }

            uniformLocationsCached = true;
            glUseProgram(0);
        }
    }

    private void setupBuffers() {
        String geometryKey = width + "x" + height + ":" + u0 + "," + v0 + "," + u1 + "," + v1;

        if (geometryCache.containsKey(geometryKey)) {
            GeometryData data = geometryCache.get(geometryKey);
            vaoId = data.vaoId;
            vboId = data.vboId;
            eboId = data.eboId;
            return;
        }

        float[] vertices = {
                //  X,     Y,     Z,     U,   V,     Nx,   Ny,   Nz
                0.0f,   0.0f,   0.0f,   u0,  v0,    0.0f, 0.0f, 1.0f,
                width,  0.0f,   0.0f,   u1,  v0,    0.0f, 0.0f, 1.0f,
                width,  height, 0.0f,   u1,  v1,    0.0f, 0.0f, 1.0f,
                0.0f,   height, 0.0f,   u0,  v1,    0.0f, 0.0f, 1.0f
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

        // Position attribute (location 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        // Texture coordinates attribute (location 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 8 * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 8 * Float.BYTES, 5L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        GeometryData data = new GeometryData();
        data.vaoId = vaoId;
        data.vboId = vboId;
        data.eboId = eboId;
        geometryCache.put(geometryKey, data);
    }

    @Override
    public void update(float deltaTime) {
        // Update per–sprite logic; for example, update view position from the camera.
        setViewPos(Camera.getCamcenter());
    }

    @Override
    public void render(Matrix4f viewProjectionMatrix) {
        Matrix4f model = new Matrix4f()
                .translate(x, y, 0)
                .rotateZ(rotation)
                .scale(scaleX, scaleY, 1.0f);
        Matrix4f mvp = new Matrix4f(viewProjectionMatrix).mul(model);

        shader.use();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Set MVP matrix.
            FloatBuffer mvpBuf = stack.mallocFloat(16);
            mvp.get(mvpBuf);
            glUniformMatrix4fv(u_MVPLoc, false, mvpBuf);

            // Set model matrix.
            FloatBuffer modelBuf = stack.mallocFloat(16);
            model.get(modelBuf);
            glUniformMatrix4fv(u_ModelLoc, false, modelBuf);
        }

        // Set overall tint color.
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        glUniform4f(u_ColorLoc, r, g, b, alpha);

        // Set material and view uniforms.
        Vector3f amb = material.getAmbient();
        glUniform3f(u_AmbientColorLoc, amb.x, amb.y, amb.z);
        glUniform3f(u_specularLoc, material.getSpecular().x, material.getSpecular().y, material.getSpecular().z);
        glUniform1f(u_shininessLoc, material.getShininess());
        glUniform3f(u_ViewPosLoc, viewPos.x, viewPos.y, viewPos.z);
        glUniform1i(u_flip_x_Loc, FlipX? 1:0);
        glUniform1i(u_flip_y_Loc, FlipY? 1:0);
        glUniform4f(u_texCoordsLoc, u0, v0, u1, v1);

        // Update lighting uniforms by iterating over all global lights - OPTIMIZED VERSION
        int lightIndex = 0;
        for (Light light : globalLights) {
            if (lightIndex >= MAX_LIGHTS) break;

            Vector3f position = light.getPosition();
            Vector3f color = light.getColor();
            Vector3f direction = light.getDirection();

            glUniform3f(lightPositionLoc[lightIndex], position.x, position.y, position.z);
            glUniform3f(lightColorLoc[lightIndex], color.x, color.y, color.z);
            glUniform3f(lightDirectionLoc[lightIndex], direction.x, direction.y, direction.z);
            glUniform1f(lightIntensityLoc[lightIndex], light.getIntensity());
            glUniform1f(lightConstantLoc[lightIndex], light.getConstant());
            glUniform1f(lightLinearLoc[lightIndex], light.getLinear());
            glUniform1f(lightQuadraticLoc[lightIndex], light.getQuadratic());
            glUniform1f(lightCutoffLoc[lightIndex], light.getCutoff());
            glUniform1f(lightOuterCutoffLoc[lightIndex], light.getOuterCutoff());
            glUniform1i(lightTypeLoc[lightIndex], light.getType());

            lightIndex++;
        }
        glUniform1i(lightCountLoc, lightIndex);

        // Set palette uniform (the palette should have 4 colors, 3 floats each).
        glUniform3fv(u_PaletteLoc, paletteFloats);

        // Bind the texture.
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture.getId());
        glUniform1i(u_TextureLoc, 0);

        // Draw the sprite.
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

    public static void cleanupAll() {
        for (GeometryData data : geometryCache.values()) {
            glDeleteVertexArrays(data.vaoId);
            glDeleteBuffers(data.vboId);
            glDeleteBuffers(data.eboId);
        }
        geometryCache.clear();

        if (shader != null) {
            shader.delete();
            shader = null;
            uniformLocationsCached = false;
        }
    }

    // Static method to update the global list of lights.
    public static void setGlobalLights(List<Light> lights) {
        globalLights = lights;
    }

    // Utility setters.
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void setRotation(float angle) { this.rotation = angle; }
    public void setScale(float sx, float sy) { this.scaleX = sx; this.scaleY = sy; }
    public void setZ(float z) { this.z = z; }
    public void setColor(int color, float alpha) { this.color = color; this.alpha = alpha; }

    // Set the palette from an array of 4 three-digit strings.
    public void setPaletteFromCodes(String[] paletteCodes) {
        if (paletteCodes == null || paletteCodes.length != 4) {
            throw new IllegalArgumentException("Expected exactly 4 palette codes.");
        }
        for (int i = 0; i < 4; i++) {
            String code = paletteCodes[i].trim();
            if (code.length() != 3) {
                throw new IllegalArgumentException("Palette code at index " + i + " must be exactly 3 characters long.");
            }
            paletteFloats[i * 3 + 0] = (code.charAt(0) - '0') / 5.0f;
            paletteFloats[i * 3 + 1] = (code.charAt(1) - '0') / 5.0f;
            paletteFloats[i * 3 + 2] = (code.charAt(2) - '0') / 5.0f;
        }
        shader.use();
        FloatBuffer buf = BufferUtils.createFloatBuffer(12);
        buf.put(paletteFloats).flip();
        glUniform3fv(u_PaletteLoc, buf);
        glUseProgram(0);
    }

    public Shader getShader() {
        return shader;
    }

    /**
     * Updates the local view position without reassigning the final Vector3f.
     *
     * @param vp The new view position.
     */
    public void setViewPos(Vector3f vp) {
        viewPos.set(vp);
    }

    @Override
    public float getZ() {
        return z;
    }

    public static void clearGlobalLights() {
        globalLights.clear();
    }

    public static void addGlobalLight(Light light) {
        for (Light existingLight : globalLights) {
            if (existingLight == light) {
                return;
            }
        }
        globalLights.add(light);
    }

    public static int getLightCount() {

        int lightIndex = 0;
        for (Light light : globalLights) {
            if (lightIndex >= MAX_LIGHTS) break;

            lightIndex++;
        }
        return lightIndex;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getU0() { return u0; }
    public float getU1() { return u1; }
    public float getV0() { return v0; }
    public float getV1() { return v1; }
}