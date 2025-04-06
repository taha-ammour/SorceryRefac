package org.example.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A 2D orthographic camera with position, zoom, rotation, and viewport size.
 * The coordinate system has the origin at the top-left (y increases downward).
 */
public class Camera {
    private float viewportWidth;
    private float viewportHeight;
    private final Matrix4f projectionMatrix;
    private final Matrix4f viewMatrix;
    private final Matrix4f viewProjectionMatrix;
    private final Vector3f position;
    private float zoom;
    private float rotation; // in radians
    private static Vector3f Camcen;

    // Camera following state
    private boolean isFollowing = false;
    private Vector3f targetPosition = new Vector3f();
    private float followLerp = 0.15f; // Increased for faster following

    // Debug flag
    private boolean debug = false;

    /**
     * Creates a camera with the given viewport dimensions.
     * @param viewportWidth  The width of the viewport in pixels.
     * @param viewportHeight The height of the viewport in pixels.
     */
    public Camera(float viewportWidth, float viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.position = new Vector3f(0, 0, 0);
        this.zoom = 0.5f;
        this.rotation = 0.0f;
        projectionMatrix = new Matrix4f().ortho2D(0, viewportWidth, viewportHeight, 0);
        viewMatrix = new Matrix4f();
        viewProjectionMatrix = new Matrix4f();
        update();
    }

    /**
     * Recalculates the view, projection, and view-projection matrices based on the current camera properties.
     */
    public void update() {
        // Apply follow logic if active
        if (isFollowing) {
            updateFollowing();
        }

        // Clamp zoom to prevent issues
        if (zoom < 0.1f) {
            zoom = 0.1f;
        }


        // Update the projection matrix (orthographic)
        projectionMatrix.identity().ortho2D(0, viewportWidth, viewportHeight, 0);

        // Build the view matrix:
        // First, translate the world by the negative camera position.
        // Then, apply rotation.
        // Then, apply scaling (using 1/zoom to enlarge the view when zoom < 1).
        viewMatrix.identity()
                .translate(-position.x, -position.y, 0)
                .rotateZ(rotation)
                .scale(1.0f / zoom, 1.0f / zoom, 1.0f);

        // Multiply projection and view to form the view-projection matrix.
        projectionMatrix.mul(viewMatrix, viewProjectionMatrix);

        // Update camera center for reference
        Vector3f camCenter = new Vector3f(position)
                .add(viewportWidth / (2.0f * zoom), viewportHeight / (2.0f * zoom), 0);
        Camcen = camCenter;
    }

    /**
     * Update the following logic - separate from main update for clarity
     */
    private void updateFollowing() {
        float screenHalfWidth = viewportWidth  * zoom;
        float screenHalfHeight = viewportHeight  * zoom;

        float targetX = ((targetPosition.x) - (screenHalfWidth * zoom))+16;
        float targetY = ((targetPosition.y) - (screenHalfHeight * zoom))+16;


        // Smoothly interpolate to target position
        position.x += (targetX - position.x*zoom) * followLerp;
        position.y += (targetY - position.y*zoom) * followLerp;
    }

    public static Vector3f getCamcenter() {
        return Camcen;
    }

    /**
     * Returns the combined view-projection matrix.
     * @return The view-projection matrix.
     */
    public Matrix4f getViewProjectionMatrix() {
        return viewProjectionMatrix;
    }

    /**
     * Returns the current camera position.
     * @return The camera position as a Vector3f.
     */
    public Vector3f getPosition() {
        return position;
    }

    /**
     * Sets the camera position.
     * @param x New X coordinate.
     * @param y New Y coordinate.
     */
    public void setPosition(float x, float y) {
        position.set(x, y, 0);
        update();
    }

    /**
     * Moves the camera by the given delta.
     * @param dx Delta X.
     * @param dy Delta Y.
     */
    public void move(float dx, float dy) {
        position.add(dx, dy, 0);
        update();
    }

    /**
     * Returns the current zoom factor.
     * @return The zoom factor.
     */
    public float getZoom() {
        return zoom;
    }

    /**
     * Sets the zoom factor. Must be greater than zero.
     * @param zoom The new zoom factor.
     */
    public void setZoom(float zoom) {
        if (zoom <= 0) {
            zoom = 0.1f; // Clamp to minimum instead of throwing exception
        }

        if (debug) {
            System.out.println("Setting zoom: " + zoom);
        }

        this.zoom = zoom;
        update();
    }

    /**
     * Returns the current rotation in radians.
     * @return The rotation in radians.
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * Sets the camera rotation (in radians).
     * @param rotation The rotation angle in radians.
     */
    public void setRotation(float rotation) {
        this.rotation = rotation;
        update();
    }

    /**
     * Sets a new viewport size. Useful for window resizing.
     * @param width  The new viewport width in pixels.
     * @param height The new viewport height in pixels.
     */
    public void setViewportSize(float width, float height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
        update();
    }

    public float getViewportWidth() {
        return viewportWidth;
    }

    public float getViewportHeight() {
        return viewportHeight;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    /**
     * Set camera to follow a target position
     * @param targetX Target X position
     * @param targetY Target Y position
     */
    public void follow(float targetX, float targetY) {
        isFollowing = true;
        targetPosition.set(targetX, targetY, 0);

    }

    /**
     * Stop following and return to manual control
     */
    public void stopFollowing() {
        isFollowing = false;
    }

    /**
     * Check if camera is currently in follow mode
     */
    public boolean isFollowing() {
        return isFollowing;
    }

    /**
     * Set the smoothness of camera following
     * @param lerp Value between 0-1, higher = faster following
     */
    public void setFollowSmoothness(float lerp) {
        this.followLerp = Math.max(0.01f, Math.min(1.0f, lerp));
    }

    /**
     * Enable or disable debug output
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}