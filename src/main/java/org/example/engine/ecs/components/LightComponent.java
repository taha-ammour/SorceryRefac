package org.example.engine.ecs.components;

import org.example.engine.ecs.Component; /**
 * Component for light sources
 */
public class LightComponent extends Component {
    private float intensity = 1.0f;
    private float radius = 100.0f;
    private int color = 0xFFFFFF;
    private int lightType = 1; // 0=directional, 1=point, 2=spotlight

    public LightComponent() {
    }

    public LightComponent(int color, float intensity, float radius) {
        this.color = color;
        this.intensity = intensity;
        this.radius = radius;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getLightType() {
        return lightType;
    }

    public void setLightType(int lightType) {
        this.lightType = lightType;
    }
}
