package org.example.engine;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * A modern OpenGL Shader class for OpenGL 3.3+ (core profile) that loads shader
 * source code from external files (via the classpath), compiles, links, and caches uniform locations.
 *
 * Usage:
 * <pre>
 *     // Load shader from files (ensure these files exist in src/main/resources/shaders/)
 *     Shader shader = Shader.loadFromFiles("/shaders/sprite.vs.glsl", "/shaders/sprite.fs");
 *
 *     shader.use();
 *     // Set uniforms
 *     shader.setUniform1i("u_Texture", 0);
 *
 *     // ...rendering code...
 *
 *     shader.delete();
 * </pre>
 */
public class Shader {
    private final int programId;
    private final Map<String, Integer> uniformLocationCache = new HashMap<>();

    /**
     * Constructs a Shader program from given vertex and fragment shader source strings.
     *
     * @param vertexShaderSource   The source code for the vertex shader.
     * @param fragmentShaderSource The source code for the fragment shader.
     */
    public Shader(String vertexShaderSource, String fragmentShaderSource) {
        // Compile both shaders
        int vertexShaderId = compileShader(vertexShaderSource, GL_VERTEX_SHADER);
        int fragmentShaderId = compileShader(fragmentShaderSource, GL_FRAGMENT_SHADER);

        // Create the shader program and attach shaders
        programId = glCreateProgram();
        if (programId == 0) {
            throw new RuntimeException("Could not create shader program");
        }
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader program linking failed:\n" + glGetProgramInfoLog(programId));
        }
        // Optional: Validate program (useful during development)
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == GL_FALSE) {
            System.err.println("Warning: Shader program validation failed:\n" + glGetProgramInfoLog(programId));
        }
        // Shaders can be deleted after linking
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }

    /**
     * Static factory method to create a Shader program from external files.
     * The files are loaded from the classpath.
     *
     * @param vertexShaderPath   Path to the vertex shader file (e.g. "/shaders/sprite.vs.glsl")
     * @param fragmentShaderPath Path to the fragment shader file (e.g. "/shaders/sprite.fs")
     * @return A new Shader instance.
     */
    public static Shader loadFromFiles(String vertexShaderPath, String fragmentShaderPath) {
        String vertexSource = loadShaderSource(vertexShaderPath);
        String fragmentSource = loadShaderSource(fragmentShaderPath);
        return new Shader(vertexSource, fragmentSource);
    }

    private int compileShader(String source, int type) {
        int shaderId = glCreateShader(type);
        if (shaderId == 0) {
            throw new RuntimeException("Could not create shader of type: " + type);
        }
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String typeStr = (type == GL_VERTEX_SHADER) ? "Vertex" : "Fragment";
            throw new RuntimeException(typeStr + " shader compilation failed:\n" + glGetShaderInfoLog(shaderId));
        }
        return shaderId;
    }

    /**
     * Activates this shader program.
     */
    public void use() {
        glUseProgram(programId);
    }

    /**
     * Deletes this shader program.
     */
    public void delete() {
        glDeleteProgram(programId);
    }

    /**
     * Returns the OpenGL program ID.
     */
    public int getProgramId() {
        return programId;
    }

    /**
     * Retrieves and caches the location of a uniform variable.
     *
     * @param name The uniform name.
     * @return The location of the uniform.
     */
    public int getUniformLocation(String name) {
        if (uniformLocationCache.containsKey(name)) {
            return uniformLocationCache.get(name);
        }
        int location = glGetUniformLocation(programId, name);
        if (location == -1) {
            System.err.println("Warning: Uniform '" + name + "' not found in shader");
        }
        uniformLocationCache.put(name, location);
        return location;
    }

    // Convenience methods for setting uniform values

    public void setUniform1i(String name, int value) {
        glUniform1i(getUniformLocation(name), value);
    }

    public void setUniform1f(String name, float value) {
        glUniform1f(getUniformLocation(name), value);
    }

    /**
     * Sets a 4x4 matrix uniform.
     *
     * @param name         The uniform name.
     * @param matrixBuffer A FloatBuffer containing 16 float values.
     */
    public void setUniformMatrix4fv(String name, FloatBuffer matrixBuffer) {
        glUniformMatrix4fv(getUniformLocation(name), false, matrixBuffer);
    }

    /**
     * Loads shader source code from a file on the classpath.
     *
     * @param path The resource path (e.g. "/shaders/sprite.vs.glsl")
     * @return The shader source code as a String.
     */
    private static String loadShaderSource(String path) {
        StringBuilder source = new StringBuilder();
        try (InputStream in = Shader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Shader file not found: " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    source.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader source from " + path, e);
        }
        return source.toString();
    }

    // (Optional) Utility method for loading a resource into a ByteBuffer.
    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        try (InputStream source = Shader.class.getResourceAsStream(resource)) {
            if (source == null)
                throw new IOException("Resource not found: " + resource);
            try (ReadableByteChannel rbc = Channels.newChannel(source)) {
                buffer = ByteBuffer.allocateDirect(bufferSize);
                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1)
                        break;
                    if (buffer.remaining() == 0)
                        buffer = resizeBuffer(buffer, buffer.capacity() * 2);
                }
                buffer.flip();
            }
        }
        return buffer;
    }

    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }
}
