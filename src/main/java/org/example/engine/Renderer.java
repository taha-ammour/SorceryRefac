package org.example.engine;

import static org.lwjgl.opengl.GL11.*;

/**
 * Simple renderer enabling depth and clearing buffers.
 */
public class Renderer {
    public Renderer() {
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
}
