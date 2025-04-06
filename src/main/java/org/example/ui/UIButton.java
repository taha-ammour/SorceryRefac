//package org.example.ui;
//
//import org.example.engine.FontSheet;
//import org.example.engine.Input;
//import org.example.engine.Shader;
//import org.example.engine.SpriteManager;
//import org.joml.Matrix4f;
//
//import static org.lwjgl.glfw.GLFW.*;
//
///**
// * A button component that combines a sprite background with text
// * Has proper state management for normal, hover, and pressed states
// */
//public class UIButton extends UIComponent {
//    // States
//    public enum ButtonState {
//        NORMAL,
//        HOVER,
//        PRESSED,
//        DISABLED
//    }
//
//    private ButtonState currentState = ButtonState.NORMAL;
//    private UISprite backgroundSprite;
//    private UIText labelText;
//    private final Input input;
//
//    // State sprites
//    private String normalSpriteName;
//    private String hoverSpriteName;
//    private String pressedSpriteName;
//    private String disabledSpriteName;
//
//    // Colors for different states
//    private int normalTextColor = 0xFFFFFF;
//    private int hoverTextColor = 0xFFFFFF;
//    private int pressedTextColor = 0xCCCCCC;
//    private int disabledTextColor = 0x999999;
//
//    // Click handling
//    private boolean wasPressed = false;
//    private ButtonClickListener clickListener;
//
//    /**
//     * Create a new button with a background sprite and text label
//     */
//    public UIButton(float x, float y, float width, float height,
//                    String normalSpriteName, String text,
//                    FontSheet fontSheet, Shader fontShader,
//                    SpriteManager spriteManager, Input input) {
//        super(x, y, width, height);
//        this.input = input;
//        this.normalSpriteName = normalSpriteName;
//
//        // Create background sprite
//        backgroundSprite = new UISprite(x, y, width, height, normalSpriteName, spriteManager);
//
//        // Create label text
//        labelText = new UIText(fontSheet, fontShader, text, x, y);
//
//        // Center the text on the button by default
//        centerText();
//    }
//
//    /**
//     * Center the text on the button
//     */
//    public void centerText() {
//        float textX = x + (width - labelText.getTextWidth()) / 2;
//        float textY = y + (height - labelText.getTextHeight()) / 2 + labelText.getTextHeight() - 4;
//        labelText.setPosition(textX, textY);
//    }
//
//    /**
//     * Set the text of the button
//     */
//    public void setText(String text) {
//        labelText.setText(text);
//        centerText();
//    }
//
//    /**
//     * Set the sprite names for different button states
//     */
//    public void setStateSprites(String normalSprite, String hoverSprite,
//                                String pressedSprite, String disabledSprite) {
//        this.normalSpriteName = normalSprite;
//        this.hoverSpriteName = hoverSprite != null ? hoverSprite : normalSprite;
//        this.pressedSpriteName = pressedSprite != null ? pressedSprite : normalSprite;
//        this.disabledSpriteName = disabledSprite != null ? disabledSprite : normalSprite;
//
//        // Update current sprite based on state
//        updateVisuals();
//    }
//
//    /**
//     * Set the text colors for different button states
//     */
//    public void setStateColors(int normalColor, int hoverColor, int pressedColor, int disabledColor) {
//        this.normalTextColor = normalColor;
//        this.hoverTextColor = hoverColor;
//        this.pressedTextColor = pressedColor;
//        this.disabledTextColor = disabledColor;
//
//        // Update current color based on state
//        updateVisuals();
//    }
//
//    /**
//     * Sets the enabled state of the button
//     */
//    public void setEnabled(boolean enabled) {
//        if (!enabled) {
//            currentState = ButtonState.DISABLED;
//        } else if (currentState == ButtonState.DISABLED) {
//            currentState = ButtonState.NORMAL;
//        }
//        updateVisuals();
//    }
//
//    /**
//     * Update the visual appearance based on current state
//     */
//    private void updateVisuals() {
//        String spriteName;
//        int textColor;
//
//        switch (currentState) {
//            case HOVER:
//                spriteName = hoverSpriteName;
//                textColor = hoverTextColor;
//                break;
//            case PRESSED:
//                spriteName = pressedSpriteName;
//                textColor = pressedTextColor;
//                break;
//            case DISABLED:
//                spriteName = disabledSpriteName;
//                textColor = disabledTextColor;
//                break;
//            case NORMAL:
//            default:
//                spriteName = normalSpriteName;
//                textColor = normalTextColor;
//                break;
//        }
//
//        // Update sprite if needed
//        backgroundSprite.getSprite().getSprite(spriteName);
//
//        // Update text color
//        labelText.setColor(textColor);
//    }
//
//    /**
//     * Check if the mouse is over the button
//     */
//    private boolean isMouseOver() {
//        float mouseX = input.getMouseX();
//        float mouseY = input.getMouseY();
//
//        return mouseX >= x && mouseX <= x + width &&
//                mouseY >= y && mouseY <= y + height;
//    }
//
//    /**
//     * Set a click listener for the button
//     */
//    public void setClickListener(ButtonClickListener listener) {
//        this.clickListener = listener;
//    }
//
//    @Override
//    public void update(float deltaTime) {
//        if (!visible) return;
//
//        // Update children first
//        backgroundSprite.update(deltaTime);
//        labelText.update(deltaTime);
//
//        // Don't process input if disabled
//        if (currentState == ButtonState.DISABLED) {
//            return;
//        }
//
//        boolean mouseOver = isMouseOver();
//        boolean mousePressed = input.isMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT);
//
//        // Determine the new state
//        ButtonState newState;
//
//        if (mouseOver) {
//            if (mousePressed) {
//                newState = ButtonState.PRESSED;
//                wasPressed = true;
//            } else {
//                if (wasPressed) {
//                    // Mouse was released while over the button - trigger click
//                    if (clickListener != null) {
//                        clickListener.onClick(this);
//                    }
//                    wasPressed = false;
//                }
//                newState = ButtonState.HOVER;
//            }
//        } else {
//            newState = ButtonState.NORMAL;
//            // Reset wasPressed if mouse moved away
//            if (!mousePressed) {
//                wasPressed = false;
//            }
//        }
//
//        // Update state if changed
//        if (newState != currentState) {
//            currentState = newState;
//            updateVisuals();
//        }
//    }
//
//    @Override
//    public void render(Matrix4f viewProj) {
//        if (!visible) return;
//
//        // Render background sprite first
//        backgroundSprite.render(viewProj);
//
//        // Then render the text on top
//        labelText.render(viewProj);
//    }
//
//    @Override
//    public void setPosition(float x, float y) {
//        float offsetX = x - this.x;
//        float offsetY = y - this.y;
//
//        this.x = x;
//        this.y = y;
//
//        // Move the child components
//        backgroundSprite.setPosition(x, y);
//        labelText.setPosition(labelText.getX() + offsetX, labelText.getY() + offsetY);
//
//        layoutDirty = true;
//    }
//
//    @Override
//    public void setSize(float width, float height) {
//        this.width = width;
//        this.height = height;
//
//        // Update the background sprite size
//        backgroundSprite.setSize(width, height);
//
//        // Recenter the text
//        centerText();
//
//        layoutDirty = true;
//    }
//
//    /**
//     * Interface for button click callbacks
//     */
//    public interface ButtonClickListener {
//        void onClick(UIButton button);
//    }
//}