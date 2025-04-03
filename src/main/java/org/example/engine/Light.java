package org.example.engine;

import org.joml.Vector3f;

public class Light {
    private Vector3f position;
    private Vector3f color;
    private Vector3f direction;  // For directional/spot lights
    private float intensity;
    private float constant;
    private float linear;
    private float quadratic;
    private float cutoff;
    private float outerCutoff;
    private int type;            // 0 = Directional, 1 = Point, 2 = Spotlight

    public Light() {
        // Default: directional light coming from above
        position = new Vector3f(0.0f, 10.0f, 5.0f);
        color = new Vector3f(1.0f, 1.0f, 1.0f);
        direction = new Vector3f(0.0f, -1.0f, -0.5f).normalize(); // Normalized direction vector
        intensity = 1.0f;
        constant = 1.0f;
        linear = 0.09f;
        quadratic = 0.032f;
        cutoff = (float) Math.cos(Math.toRadians(12.5f));
        outerCutoff = (float) Math.cos(Math.toRadians(17.5f));
        type = 0; // Default to directional light
    }

    // Getters and setters:
    public Vector3f getPosition() { return position; }
    public void setPosition(Vector3f position) { this.position.set(position); }
    public Vector3f getColor() { return color; }
    public void setColor(Vector3f color) { this.color.set(color); }
    public Vector3f getDirection() { return direction; }
    public void setDirection(Vector3f direction) { this.direction.set(direction); }
    public float getIntensity() { return intensity; }
    public void setIntensity(float intensity) { this.intensity = intensity; }
    public float getConstant() { return constant; }
    public void setConstant(float constant) { this.constant = constant; }
    public float getLinear() { return linear; }
    public void setLinear(float linear) { this.linear = linear; }
    public float getQuadratic() { return quadratic; }
    public void setQuadratic(float quadratic) { this.quadratic = quadratic; }
    public float getCutoff() { return cutoff; }
    public void setCutoff(float cutoff) { this.cutoff = cutoff; }
    public float getOuterCutoff() { return outerCutoff; }
    public void setOuterCutoff(float outerCutoff) { this.outerCutoff = outerCutoff; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
}
