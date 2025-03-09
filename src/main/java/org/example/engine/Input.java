package org.example.engine;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Basic keyboard input handling.
 */
public class Input {
    private final long window;
    private final boolean[] keys = new boolean[GLFW_KEY_LAST];

    public Input(long window) {
        this.window = window;
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key >= 0 && key < keys.length) {
                keys[key] = action != GLFW_RELEASE;
            }
        });
    }

    public void update() {
        // For more advanced input, handle mouse, etc. here.
    }

    public boolean isKeyDown(int key) {
        return (key >= 0 && key < keys.length) && keys[key];
    }
}
