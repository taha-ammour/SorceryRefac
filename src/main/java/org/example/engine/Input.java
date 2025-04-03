package org.example.engine;

import static org.lwjgl.glfw.GLFW.*;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;


public class Input {
    private static final int NUM_KEYS = GLFW_KEY_LAST;
    private static final int NUM_MOUSE_BUTTONS = 8;


    private static boolean[] keys = new boolean[NUM_KEYS];
    private static boolean[] keysJustPressed = new boolean[NUM_KEYS];
    private static boolean[] keysJustReleased = new boolean[NUM_KEYS];
    private static boolean[] keysLastFrame = new boolean[NUM_KEYS];


    private static boolean[] mouseButtons = new boolean[NUM_MOUSE_BUTTONS];
    private static boolean[] mouseButtonsJustPressed = new boolean[NUM_MOUSE_BUTTONS];
    private static boolean[] mouseButtonsJustReleased = new boolean[NUM_MOUSE_BUTTONS];
    private static boolean[] mouseButtonsLastFrame = new boolean[NUM_MOUSE_BUTTONS];


    private static double mouseX, mouseY;
    private static double scrollX, scrollY;

    private long window;

    public Input(long window) {
        this.window = window;


        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key >= 0 && key < NUM_KEYS) {
                keys[key] = (action != GLFW_RELEASE);
            }
        });


        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = ypos;
        });


        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button >= 0 && button < NUM_MOUSE_BUTTONS) {
                mouseButtons[button] = (action != GLFW_RELEASE);
            }
        });


        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            scrollX = xoffset;
            scrollY = yoffset;
        });
    }


    public void update() {

        for (int i = 0; i < NUM_KEYS; i++) {
            keysJustPressed[i] = keys[i] && !keysLastFrame[i];
            keysJustReleased[i] = !keys[i] && keysLastFrame[i];
            keysLastFrame[i] = keys[i];
        }


        for (int i = 0; i < NUM_MOUSE_BUTTONS; i++) {
            mouseButtonsJustPressed[i] = mouseButtons[i] && !mouseButtonsLastFrame[i];
            mouseButtonsJustReleased[i] = !mouseButtons[i] && mouseButtonsLastFrame[i];
            mouseButtonsLastFrame[i] = mouseButtons[i];
        }


        scrollX = 0;
        scrollY = 0;
    }

    // ----- Keyboard Input Methods -----

    /**
     * Check if a key is currently held down.
     * @param key GLFW key code.
     * @return true if the key is down.
     */
    public boolean isKeyDown(int key) {
        return key >= 0 && key < NUM_KEYS && keys[key];
    }

    /**
     * Check if a key was pressed this frame.
     * @param key GLFW key code.
     * @return true if the key was just pressed.
     */
    public boolean isKeyJustPressed(int key) {
        return key >= 0 && key < NUM_KEYS && keysJustPressed[key];
    }

    /**
     * Check if a key was released this frame.
     * @param key GLFW key code.
     * @return true if the key was just released.
     */
    public boolean isKeyJustReleased(int key) {
        return key >= 0 && key < NUM_KEYS && keysJustReleased[key];
    }

    // ----- Mouse Input Methods -----

    /**
     * Check if a mouse button is currently held down.
     * @param button mouse button index.
     * @return true if the button is down.
     */
    public boolean isMouseButtonDown(int button) {
        return button >= 0 && button < NUM_MOUSE_BUTTONS && mouseButtons[button];
    }

    /**
     * Check if a mouse button was pressed this frame.
     * @param button mouse button index.
     * @return true if the button was just pressed.
     */
    public boolean isMouseButtonJustPressed(int button) {
        return button >= 0 && button < NUM_MOUSE_BUTTONS && mouseButtonsJustPressed[button];
    }

    /**
     * Check if a mouse button was released this frame.
     * @param button mouse button index.
     * @return true if the button was just released.
     */
    public boolean isMouseButtonJustReleased(int button) {
        return button >= 0 && button < NUM_MOUSE_BUTTONS && mouseButtonsJustReleased[button];
    }

    /**
     * Get the current X coordinate of the mouse cursor.
     * @return mouse X position.
     */
    public float getMouseX() {
        return (float) mouseX;
    }

    /**
     * Get the current Y coordinate of the mouse cursor.
     * @return mouse Y position.
     */
    public float getMouseY() {
        return (float) mouseY;
    }

    /**
     * Get the horizontal scroll offset for the current frame.
     * @return scroll offset X.
     */
    public float getScrollX() {
        return (float) scrollX;
    }

    /**
     * Get the vertical scroll offset for the current frame.
     * @return scroll offset Y.
     */
    public float getScrollY() {
        return (float) scrollY;
    }

    // ----- Joystick / Gamepad Support Methods -----

    /**
     * Retrieves the axes values for a given joystick.
     * @param joystick the joystick ID (e.g., GLFW_JOYSTICK_1)
     * @return an array of axes values, or an empty array if joystick not present.
     */
    public static float[] getJoystickAxes(int joystick) {
        if (glfwJoystickPresent(joystick)) {
            FloatBuffer axes = glfwGetJoystickAxes(joystick);
            if (axes != null) {
                float[] axesArray = new float[axes.limit()];
                for (int i = 0; i < axes.limit(); i++) {
                    axesArray[i] = axes.get(i);
                }
                return axesArray;
            }
        }
        return new float[0];
    }

    /**
     * Retrieves the button states for a given joystick.
     * @param joystick the joystick ID (e.g., GLFW_JOYSTICK_1)
     * @return an array of button states, or an empty array if joystick not present.
     */
    public static byte[] getJoystickButtons(int joystick) {
        if (glfwJoystickPresent(joystick)) {
            ByteBuffer buttons = glfwGetJoystickButtons(joystick);
            if (buttons != null) {
                byte[] buttonsArray = new byte[buttons.limit()];
                for (int i = 0; i < buttons.limit(); i++) {
                    buttonsArray[i] = buttons.get(i);
                }
                return buttonsArray;
            }
        }
        return new byte[0];
    }

    /**
     * Retrieves the name of the joystick.
     * @param joystick the joystick ID (e.g., GLFW_JOYSTICK_1)
     * @return the name of the joystick, or null if not present.
     */
    public static String getJoystickName(int joystick) {
        if (glfwJoystickPresent(joystick)) {
            return glfwGetJoystickName(joystick);
        }
        return null;
    }
}
