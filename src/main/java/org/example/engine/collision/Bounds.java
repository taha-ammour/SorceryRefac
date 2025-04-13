package org.example.engine.collision;

public class Bounds {
    private float x, y, width, height;

    public Bounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    /**
     * Checks if this bounds intersects with another bounds
     */
    public boolean intersects(Bounds other) {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y;
    }

    /**
     * Checks if this bounds contains a point
     */
    public boolean contains(float pointX, float pointY) {
        return pointX >= x && pointX <= x + width &&
                pointY >= y && pointY <= y + height;
    }
}