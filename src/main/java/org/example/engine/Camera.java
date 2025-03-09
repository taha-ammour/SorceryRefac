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

    /**
     * Creates a camera with the given viewport dimensions.
     * @param viewportWidth  The width of the viewport in pixels.
     * @param viewportHeight The height of the viewport in pixels.
     */
    public Camera(float viewportWidth, float viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.position = new Vector3f(0, 0, 0);
        this.zoom = 1.0f;
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
        // Update the projection matrix (orthographic).
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
            throw new IllegalArgumentException("Zoom must be greater than zero.");
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

    public  Matrix4f getProjectionMatrix(){
        return projectionMatrix;
    }
}
