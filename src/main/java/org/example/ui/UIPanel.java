package org.example.ui;

import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

public class UIPanel extends UIComponent {
    public enum Layout {
        NONE,
        VERTICAL,
        HORIZONTAL
    }

    private List<UIComponent> children;
    private Layout layoutMode = Layout.NONE;
    private float spacing = 5.0f; // Default spacing between children

    public UIPanel(float x, float y, float width, float height) {
        super(x, y, width, height);
        children = new ArrayList<>();
    }

    public void setLayout(Layout layoutMode, float spacing) {
        this.layoutMode = layoutMode;
        this.spacing = spacing;
        layoutDirty = true;
    }

    public void addComponent(UIComponent component) {
        children.add(component);
        layoutDirty = true;
    }

    public void removeComponent(UIComponent component) {
        children.remove(component);
        layoutDirty = true;
    }

    /**
     * Get the number of child components
     * @return The number of components in this panel
     */
    public int getComponentCount() {
        return children.size();
    }

    /**
     * Remove all child components
     */
    public void removeAllComponents() {
        children.clear();
        layoutDirty = true;
    }

    /**
     * Remove a component at a specific index
     * @param index The index of the component to remove
     */
    public void removeComponentAt(int index) {
        if (index >= 0 && index < children.size()) {
            children.remove(index);
            layoutDirty = true;
        }
    }

    /**
     * Get a component at a specific index
     * @param index The index of the component to get
     * @return The component at the specified index
     */
    public UIComponent getComponentAt(int index) {
        if (index >= 0 && index < children.size()) {
            return children.get(index);
        }
        return null;
    }

    /**
     * Update layout positions if dirty.
     */
    private void updateLayout() {
        if (!layoutDirty) return;
        float offsetX = paddingLeft;
        float offsetY = paddingTop;

        for (UIComponent comp : children) {
            // Position relative to panel's (x, y)
            if (layoutMode == Layout.VERTICAL) {
                comp.setPosition(x + offsetX, y + offsetY);
                offsetY += comp.height + spacing;
            } else if (layoutMode == Layout.HORIZONTAL) {
                comp.setPosition(x + offsetX, y + offsetY);
                offsetX += comp.width + spacing;
            } else {
            }
        }
        layoutDirty = false;
    }

    @Override
    public void update(float deltaTime) {
        if (!visible) return;
        updateLayout();
        for (UIComponent comp : children) {
            comp.update(deltaTime);
        }
    }

    @Override
    public void render(Matrix4f viewProj) {
        if (!visible) return;
        // Optionally render a panel background here.
        for (UIComponent comp : children) {
            comp.render(viewProj);
        }
    }
}