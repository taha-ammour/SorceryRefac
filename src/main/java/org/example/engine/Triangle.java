package org.example.engine;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Simple example of rendering a colored triangle.
 */
public class Triangle extends GameObject {
    private int vaoId;
    private int vboId;
    private Shader shader;

    public Triangle() {
        createShader();
        setupBuffers();
    }

    private void createShader() {
        String vertexShaderSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 aPos;\n" +
                "uniform mat4 u_MVP;\n" +
                "void main() {\n" +
                "    gl_Position = u_MVP * vec4(aPos, 1.0);\n" +
                "}\n";
        String fragmentShaderSource = "#version 330 core\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "    FragColor = vec4(0.2, 0.8, 0.2, 1.0);\n" +
                "}\n";
        shader = new Shader(vertexShaderSource, fragmentShaderSource);
    }

    private void setupBuffers() {
        float[] vertices = {
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                0.0f,  0.5f, 0.0f
        };

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer fb = BufferUtils.createFloatBuffer(vertices.length);
        fb.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    @Override
    public void update(float deltaTime) {
        // Example: animate
    }

    @Override
    public void render(Matrix4f viewProjectionMatrix) {
        // For a simple triangle, we just pass in the MVP = viewProjectionMatrix
        // (We have no local transformations for this object.)
        shader.use();
        int mvpLoc = glGetUniformLocation(shader.getProgramId(), "u_MVP");

        // Directly use the viewProjection as the MVP
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        viewProjectionMatrix.get(fb);
        glUniformMatrix4fv(mvpLoc, false, fb);

        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
        glUseProgram(0);
    }

    @Override
    public void cleanup() {
        glDeleteVertexArrays(vaoId);
        glDeleteBuffers(vboId);
        shader.delete();
    }
}
