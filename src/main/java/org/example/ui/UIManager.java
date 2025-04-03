package org.example.ui;

import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

public class UIManager {
    // Separate lists for different layers.
    private List<UIComponent> backgroundComponents;
    private List<UIComponent> foregroundComponents;

    public UIManager() {
        backgroundComponents = new ArrayList<>();
        foregroundComponents = new ArrayList<>();
    }

    public void addComponent(UIComponent comp, boolean isForeground) {
        if (isForeground) {
            foregroundComponents.add(comp);
        } else {
            backgroundComponents.add(comp);
        }
    }

    public void removeComponent(UIComponent comp) {
        backgroundComponents.remove(comp);
        foregroundComponents.remove(comp);
    }

    /**
     * Update all UI components.
     */
    public void update(float deltaTime) {
        for (UIComponent comp : backgroundComponents) {
            comp.update(deltaTime);
        }
        for (UIComponent comp : foregroundComponents) {
            comp.update(deltaTime);
        }
    }

    /**
     * Render all UI components.
     */
    public void render(Matrix4f viewProj) {
        for (UIComponent comp : backgroundComponents) {
            comp.render(viewProj);
        }
        for (UIComponent comp : foregroundComponents) {
            comp.render(viewProj);
        }
    }

    /**
     * Propagate a resize event to all UI components.
     */
    public void onResize(float newWidth, float newHeight) {
        for (UIComponent comp : backgroundComponents) {
            comp.onResize(newWidth, newHeight);
        }
        for (UIComponent comp : foregroundComponents) {
            comp.onResize(newWidth, newHeight);
        }
    }
}
