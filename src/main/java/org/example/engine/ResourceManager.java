package org.example.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages shared Shader objects, etc.
 */
public class ResourceManager {
    private static final Map<String, Shader> shaders = new HashMap<>();

    public static Shader loadShader(String name, String vertSrc, String fragSrc) {
        Shader shader = new Shader(vertSrc, fragSrc);
        shaders.put(name, shader);
        return shader;
    }

    public static Shader getShader(String name) {
        return shaders.get(name);
    }

    public static void cleanup() {
        for (Shader shader : shaders.values()) {
            shader.delete();
        }
        shaders.clear();
    }
}
