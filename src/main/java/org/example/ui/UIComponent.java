package org.example.ui;

import org.joml.Matrix4f;

public abstract class UIComponent {
    protected float x, y;           // Position relative to parent (or screen if top-level)
    protected float width, height;  // Size of the component
    protected boolean visible = true;

    // Anchoring values (0=left/top, 0.5=center, 1=right/bottom)
    protected float anchorX = 0, anchorY = 0;

    // Padding (left, top, right, bottom)
    protected float paddingLeft = 0, paddingTop = 0, paddingRight = 0, paddingBottom = 0;

    // Flag to mark if layout should be recalculated
    protected boolean layoutDirty = true;

    public UIComponent(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Update component state (animations, input, etc.)
     */
    public abstract void update(float deltaTime);

    /**
     * Render the component using the provided view-projection matrix.
     */
    public abstract void render(Matrix4f viewProj);

    /**
     * Called when the UI should recalc layout (e.g., window resized).
     */
    public void onResize(float newWidth, float newHeight) {
        layoutDirty = true;
    }

    // Layout helpers.
    public void setAnchor(float anchorX, float anchorY) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        layoutDirty = true;
    }

    public void setPadding(float left, float top, float right, float bottom) {
        this.paddingLeft = left;
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        layoutDirty = true;
    }

    // Setters.
    public void setPosition(float x, float y) { this.x = x; this.y = y; layoutDirty = true; }
    public void setSize(float width, float height) { this.width = width; this.height = height; layoutDirty = true; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public float getRenderingPriority() {
        return 0.0f; // Default implementation
    }
}
