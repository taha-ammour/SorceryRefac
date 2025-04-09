package org.example.engine;

import org.example.GameWorld;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import org.example.ui.*;
import org.example.ui.UIManagerGameObject;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Manages window creation, main loop, and engine lifecycle.
 */
public class Engine {
    private long window;
    private Scene activeScene;
    private Input input;
    private Renderer renderer;
    private Camera camera;
    private boolean running = false;
    private UIManagerGameObject uiGameObject;
    private static org.example.GameWorld gameWorld;

    public void init(int width, int height, String title) {
        // Set up error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW for OpenGL 3.3 core profile
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Create the window
        window = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        // Center window on primary monitor
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidmode != null) {
            glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
        }

        // Create input handler
        input = new Input(window);

        // Make context current and enable v-sync
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        // Create OpenGL capabilities
        GL.createCapabilities();

        // Create a simple 2D orthographic camera
        camera = new Camera(width, height);

        glfwSetFramebufferSizeCallback(window, (win, newWidth, newHeight) -> {
            // Update the OpenGL viewport
            glViewport(0, 0, newWidth, newHeight);
            // Update the camera's viewport size
            camera.setViewportSize(newWidth, newHeight);
            if (uiGameObject != null) {
                uiGameObject.setWindowSize(newWidth, newHeight);
            }
        });

        // Initialize renderer
        renderer = new Renderer();

        // Set the initial viewport (in case the window is already a certain size)
        glViewport(0, 0, width, height);

        // Default clear color
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        camera = new Camera(width, height);

        // Create default scene (you can swap with setActiveScene later)
        activeScene = new Scene();

        UIManager uiManager = new UIManager();
        uiGameObject = new UIManagerGameObject(uiManager, width, height);
        activeScene.addGameObject(uiGameObject);
    }

    public void run() {
        double lastTime = glfwGetTime();
        double lastFPSUpdate = lastTime;
        int frames = 0;
        running = true;

        while (!glfwWindowShouldClose(window) && running) {
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;
            frames++;

            // Update FPS in title every 1 second
            if (currentTime - lastFPSUpdate >= 1.0) {
                glfwSetWindowTitle(window, "Engine - FPS: " + frames);
                frames = 0;
                lastFPSUpdate = currentTime;
            }

            // Poll for window events
            glfwPollEvents();

            // Update input
            input.update();
            Sprite.clearGlobalLights();

            // Update camera if needed
            if (camera != null && !camera.isFollowing()) {
                handleCameraMovement(deltaTime);
            }


            // Update scene
            activeScene.update(deltaTime);
            if (gameWorld != null) {
                gameWorld.update(deltaTime);
            }

            camera.update();

            // Render
            renderer.clear();
            Matrix4f vpMatrix = camera.getViewProjectionMatrix();
            activeScene.render(vpMatrix);

            glfwSwapBuffers(window);
        }
        cleanup();
    }

    public void setActiveScene(Scene scene) {
        if (activeScene != null) {
            activeScene.cleanup();
        }
        activeScene = scene;
    }

    private void cleanup() {
        if (activeScene != null) {
            activeScene.cleanup();
        }
        ResourceManager.cleanup();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public long getWindow() {
        return window;
    }

    public Camera getCamera() {
        return camera;
    }

    private void handleCameraMovement(float deltaTime) {
        // Pick a speed in "pixels per second"
        float moveSpeed = 200.0f;
        if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)){
            moveSpeed += 1250.0f;
        }
        // WASD or arrow keys for panning:
        if (input.isKeyDown(GLFW_KEY_W) || input.isKeyDown(GLFW_KEY_UP)) {
            camera.move(0, -moveSpeed * deltaTime);
        }
        if (input.isKeyDown(GLFW_KEY_S) || input.isKeyDown(GLFW_KEY_DOWN)) {
            camera.move(0, moveSpeed * deltaTime);
        }
        if (input.isKeyDown(GLFW_KEY_A) || input.isKeyDown(GLFW_KEY_LEFT)) {
            camera.move(-moveSpeed * deltaTime, 0);
        }
        if (input.isKeyDown(GLFW_KEY_D) || input.isKeyDown(GLFW_KEY_RIGHT)) {
            camera.move(moveSpeed * deltaTime, 0);
        }

        // Optional zoom in/out with Q/E
        float zoomDelta = 1.0f; // how fast we zoom
        if (input.isKeyDown(GLFW_KEY_Q)) {
            camera.setZoom(camera.getZoom() + zoomDelta * deltaTime);
        }
        if (input.isKeyDown(GLFW_KEY_E)) {
            camera.setZoom(camera.getZoom() - zoomDelta * deltaTime);
        }

        // Optional rotation with Z/X (example):
        float rotationSpeed = 1.5f; // radians per second
        if (input.isKeyDown(GLFW_KEY_Z)) {
            camera.setRotation(camera.getRotation() - rotationSpeed * deltaTime);
        }
        if (input.isKeyDown(GLFW_KEY_X)) {
            camera.setRotation(camera.getRotation() + rotationSpeed * deltaTime);
        }

        if (input.isKeyDown(GLFW_KEY_SPACE) && input.isKeyDown(GLFW_KEY_LEFT_CONTROL)){
            camera.setRotation(0.0f);
            camera.setPosition(0.0f,0.0f);
            camera.setZoom(1.0f);
        }
    }

    public UIManagerGameObject getUIManagerGameObject() {
        return uiGameObject;
    }

    public Input getInput() {
        return input;
    }
    public void setGameWorld(org.example.GameWorld gameWorld) {
        Engine.gameWorld = gameWorld;
    }

    public static GameWorld getGameWorld() {
        return gameWorld;
    }
}
