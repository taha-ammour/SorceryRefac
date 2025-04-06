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
// * A text input field component that combines a sprite background with editable text
// * Supports text input, selection, cursor, and focus states
// */
//public class UIInputField extends UIComponent {
//    // States
//    public enum InputFieldState {
//        NORMAL,
//        FOCUSED,
//        DISABLED
//    }
//
//    private InputFieldState currentState = InputFieldState.NORMAL;
//    private UISprite backgroundSprite;
//    private UIText textDisplay;
//    private final Input input;
//    private final SpriteManager spriteManager;
//
//    // Input field properties
//    private String text = "";
//    private String placeholder = "";
//    private int cursorPosition = 0;
//    private float cursorBlinkTimer = 0;
//    private boolean cursorVisible = true;
//    private int maxLength = 256;
//
//    // Selection
//    private int selectionStart = 0;
//    private int selectionEnd = 0;
//
//    // State sprites
//    private String normalSpriteName;
//    private String focusedSpriteName;
//    private String disabledSpriteName;
//
//    // Colors for different states
//    private int textColor = 0x000000;
//    private int placeholderColor = 0x888888;
//    private int selectionColor = 0x3390FF;
//    private int cursorColor = 0x000000;
//
//    // Input handling
//    private float repeatDelay = 0.5f;
//    private float repeatRate = 0.05f;
//    private float keyHoldTime = 0;
//    private int lastKeyPressed = -1;
//    private InputValidationFunction validationFunction = null;
//
//    /**
//     * Create a new input field with a background sprite and text
//     */
//    public UIInputField(float x, float y, float width, float height,
//                        String normalSpriteName, String focusedSpriteName,
//                        FontSheet fontSheet, Shader fontShader,
//                        SpriteManager spriteManager, Input input) {
//        super(x, y, width, height);
//        this.input = input;
//        this.spriteManager = spriteManager;
//        this.normalSpriteName = normalSpriteName;
//        this.focusedSpriteName = focusedSpriteName != null ? focusedSpriteName : normalSpriteName;
//        this.disabledSpriteName = normalSpriteName;
//
//        // Create background sprite
//        backgroundSprite = new UISprite(x, y, width, height, normalSpriteName, spriteManager);
//
//        // Create text display with empty text
//        textDisplay = new UIText(fontSheet, fontShader, "", x + 5, y + height/2);
//        textDisplay.setColor(textColor);
//    }
//
//    /**
//     * Set placeholder text to show when the field is empty
//     */
//    public void setPlaceholder(String placeholder) {
//        this.placeholder = placeholder;
//    }
//
//    /**
//     * Get the current text in the input field
//     */
//    public String getText() {
//        return text;
//    }
//
//    /**
//     * Set the text programmatically
//     */
//    public void setText(String text) {
//        if (text == null) {
//            text = "";
//        }
//
//        if (validationFunction != null && !text.isEmpty()) {
//            if (!validationFunction.validate(text)) {
//                return; // Don't update if validation fails
//            }
//        }
//
//        this.text = text.length() <= maxLength ? text : text.substring(0, maxLength);
//        cursorPosition = this.text.length();
//        selectionStart = cursorPosition;
//        selectionEnd = cursorPosition;
//        updateTextDisplay();
//    }
//
//    /**
//     * Set the maximum allowed length for the input
//     */
//    public void setMaxLength(int maxLength) {
//        this.maxLength = maxLength;
//        if (text.length() > maxLength) {
//            text = text.substring(0, maxLength);
//            cursorPosition = Math.min(cursorPosition, text.length());
//            updateTextDisplay();
//        }
//    }
//
//    /**
//     * Set whether the input field is enabled
//     */
//    public void setEnabled(boolean enabled) {
//        if (!enabled) {
//            currentState = InputFieldState.DISABLED;
//        } else if (currentState == InputFieldState.DISABLED) {
//            currentState = InputFieldState.NORMAL;
//        }
//        updateVisuals();
//    }
//
//    /**
//     * Set a validation function to control what text is allowed
//     */
//    public void setValidationFunction(InputValidationFunction validationFunction) {
//        this.validationFunction = validationFunction;
//    }
//
//    /**
//     * Update the visual appearance based on current state
//     */
//    private void updateVisuals() {
//        String spriteName;
//
//        switch (currentState) {
//            case FOCUSED:
//                spriteName = focusedSpriteName;
//                break;
//            case DISABLED:
//                spriteName = disabledSpriteName;
//                break;
//            case NORMAL:
//            default:
//                spriteName = normalSpriteName;
//                break;
//        }
//
//        // Update sprite if needed
//        backgroundSprite.getSprite().getSprite(spriteName);
//    }
//
//    /**
//     * Update the text display with current text, cursor, and selection
//     */
//    private void updateTextDisplay() {
//        if (text.isEmpty()) {
//            // Show placeholder if text is empty
//            if (!placeholder.isEmpty() && currentState != InputFieldState.FOCUSED) {
//                textDisplay.setText(placeholder);
//                textDisplay.setColor(placeholderColor);
//            } else {
//                textDisplay.setText("");
//                textDisplay.setColor(textColor);
//            }
//        } else {
//            // Handle visible text with cursor and selection
//            StringBuilder displayText = new StringBuilder();
//
//            // Apply cursor or selection
//            if (currentState == InputFieldState.FOCUSED) {
//                if (selectionStart != selectionEnd) {
//                    // Has selection - format with selection markers
//                    String beforeSelection = text.substring(0, selectionStart);
//                    String selection = text.substring(selectionStart, selectionEnd);
//                    String afterSelection = text.substring(selectionEnd);
//
//                    // Use control codes for selected text (different color)
//                    displayText.append(beforeSelection);
//                    displayText.append("$310"); // Selection color code
//                    displayText.append(selection);
//                    displayText.append("$000"); // Reset to default color
//                    displayText.append(afterSelection);
//                } else {
//                    // Just cursor - insert cursor character at position if blinking is visible
//                    displayText.append(text);
//                    if (cursorVisible) {
//                        displayText.insert(cursorPosition, "|");
//                    }
//                }
//            } else {
//                // Normal text display without cursor
//                displayText.append(text);
//            }
//
//            textDisplay.setText(displayText.toString());
//            textDisplay.setColor(textColor);
//        }
//    }
//
//    /**
//     * Check if the mouse is over the input field
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
//     * Handle text input from keyboard
//     */
//    private void handleTextInput() {
//        // Check for character input
//        for (int i = 32; i < 127; i++) { // ASCII printable characters
//            if (input.isKeyJustPressed(i)) {
//                handleCharacterInput((char)i);
//                break;
//            }
//        }
//
//        // Special keys handling
//        handleSpecialKeys();
//    }
//
//    /**
//     * Handle a character being typed
//     */
//    private void handleCharacterInput(char c) {
//        if (currentState != InputFieldState.FOCUSED || text.length() >= maxLength) {
//            return;
//        }
//
//        // Delete selected text if there's a selection
//        deleteSelectedText();
//
//        // Insert character at cursor position
//        String newText = text.substring(0, cursorPosition) + c + text.substring(cursorPosition);
//
//        // Validate the new text if needed
//        if (validationFunction != null) {
//            if (!validationFunction.validate(newText)) {
//                return; // Don't update if validation fails
//            }
//        }
//
//        text = newText;
//        cursorPosition++;
//        selectionStart = cursorPosition;
//        selectionEnd = cursorPosition;
//
//        updateTextDisplay();
//    }
//
//    /**
//     * Handle special keyboard inputs (backspace, delete, arrows, etc)
//     */
//    private void handleSpecialKeys() {
//        // Special handling for certain control keys
//        if (currentState != InputFieldState.FOCUSED) {
//            return;
//        }
//
//        boolean ctrlHeld = input.isKeyDown(GLFW_KEY_LEFT_CONTROL) || input.isKeyDown(GLFW_KEY_RIGHT_CONTROL);
//
//        // Delete key handling
//        if (processKeyWithRepeat(GLFW_KEY_BACKSPACE)) {
//            if (selectionStart != selectionEnd) {
//                deleteSelectedText();
//            } else if (cursorPosition > 0) {
//                text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
//                cursorPosition--;
//                selectionStart = cursorPosition;
//                selectionEnd = cursorPosition;
//            }
//        }
//
//        if (processKeyWithRepeat(GLFW_KEY_DELETE)) {
//            if (selectionStart != selectionEnd) {
//                deleteSelectedText();
//            } else if (cursorPosition < text.length()) {
//                text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
//            }
//        }
//
//        // Cursor movement
//        if (processKeyWithRepeat(GLFW_KEY_LEFT)) {
//            if (ctrlHeld) {
//                // Move to previous word
//                int pos = findPreviousWordBoundary(cursorPosition);
//                cursorPosition = pos;
//            } else {
//                if (cursorPosition > 0) {
//                    cursorPosition--;
//                }
//            }
//
//            if (!input.isKeyDown(GLFW_KEY_LEFT_SHIFT) && !input.isKeyDown(GLFW_KEY_RIGHT_SHIFT)) {
//                selectionStart = cursorPosition;
//                selectionEnd = cursorPosition;
//            } else {
//                // Shift is held - modify selection
//                if (cursorPosition < selectionEnd && cursorPosition < selectionStart) {
//                    selectionStart = cursorPosition;
//                } else if (cursorPosition < selectionEnd) {
//                    selectionStart = cursorPosition;
//                } else {
//                    selectionEnd = cursorPosition;
//                }
//            }
//        }
//
//        if (processKeyWithRepeat(GLFW_KEY_RIGHT)) {
//            if (ctrlHeld) {
//                // Move to next word
//                int pos = findNextWordBoundary(cursorPosition);
//                cursorPosition = pos;
//            } else {
//                if (cursorPosition < text.length()) {
//                    cursorPosition++;
//                }
//            }
//
//            if (!input.isKeyDown(GLFW_KEY_LEFT_SHIFT) && !input.isKeyDown(GLFW_KEY_RIGHT_SHIFT)) {
//                selectionStart = cursorPosition;
//                selectionEnd = cursorPosition;
//            } else {
//                // Shift is held - modify selection
//                if (cursorPosition > selectionStart && cursorPosition > selectionEnd) {
//                    selectionEnd = cursorPosition;
//                } else if (cursorPosition > selectionStart) {
//                    selectionEnd = cursorPosition;
//                } else {
//                    selectionStart = cursorPosition;
//                }
//            }
//        }
//
//        // Home and End keys
//        if (input.isKeyJustPressed(GLFW_KEY_HOME)) {
//            cursorPosition = 0;
//            if (!input.isKeyDown(GLFW_KEY_LEFT_SHIFT) && !input.isKeyDown(GLFW_KEY_RIGHT_SHIFT)) {
//                selectionStart = 0;
//                selectionEnd = 0;
//            } else {
//                // Shift+Home selects to start
//                selectionStart = 0;
//                selectionEnd = Math.max(cursorPosition, selectionEnd);
//            }
//        }
//
//        if (input.isKeyJustPressed(GLFW_KEY_END)) {
//            cursorPosition = text.length();
//            if (!input.isKeyDown(GLFW_KEY_LEFT_SHIFT) && !input.isKeyDown(GLFW_KEY_RIGHT_SHIFT)) {
//                selectionStart = text.length();
//                selectionEnd = text.length();
//            } else {
//                // Shift+End selects to end
//                selectionEnd = text.length();
//                selectionStart = Math.min(cursorPosition, selectionStart);
//            }
//        }
//
//        // Select all with Ctrl+A
//        if (ctrlHeld && input.isKeyJustPressed(GLFW_KEY_A)) {
//            selectionStart = 0;
//            selectionEnd = text.length();
//            cursorPosition = selectionEnd;
//        }
//
//        // Copy with Ctrl+C (would need clipboard access)
//        // Paste with Ctrl+V (would need clipboard access)
//
//        // Cut with Ctrl+X (would need clipboard access)
//
//        // Update display after any key handling
//        updateTextDisplay();
//    }
//
//    /**
//     * Process a key with repeat rate handling
//     */
//    private boolean processKeyWithRepeat(int key) {
//        if (input.isKeyJustPressed(key)) {
//            keyHoldTime = 0;
//            lastKeyPressed = key;
//            return true;
//        } else if (input.isKeyDown(key) && key == lastKeyPressed) {
//            keyHoldTime += input.getFrameTime();
//            if (keyHoldTime > repeatDelay) {
//                float repeatTime = (keyHoldTime - repeatDelay) % repeatRate;
//                if (repeatTime < 0.016f) { // Assuming ~60fps
//                    return true;
//                }
//            }
//        } else if (!input.isKeyDown(key) && key == lastKeyPressed) {
//            lastKeyPressed = -1;
//        }
//
//        return false;
//    }
//
//    /**
//     * Delete the currently selected text
//     */
//    private void deleteSelectedText() {
//        if (selectionStart == selectionEnd) {
//            return;
//        }
//
//        // Ensure start is before end
//        int start = Math.min(selectionStart, selectionEnd);
//        int end = Math.max(selectionStart, selectionEnd);
//
//        text = text.substring(0, start) + text.substring(end);
//        cursorPosition = start;
//        selectionStart = cursorPosition;
//        selectionEnd = cursorPosition;
//    }
//
//    /**
//     * Find the previous word boundary from a position
//     */
//    private int findPreviousWordBoundary(int fromPos) {
//        if (fromPos <= 0) return 0;
//
//        // Skip spaces backwards
//        int pos = fromPos - 1;
//        while (pos > 0 && Character.isWhitespace(text.charAt(pos))) {
//            pos--;
//        }
//
//        // Go to the start of the word
//        while (pos > 0 && !Character.isWhitespace(text.charAt(pos - 1))) {
//            pos--;
//        }
//
//        return pos;
//    }
//
//    /**
//     * Find the next word boundary from a position
//     */
//    private int findNextWordBoundary(int fromPos) {
//        if (fromPos >= text.length()) return text.length();
//
//        // Skip spaces forwards
//        int pos = fromPos;
//        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
//            pos++;
//        }
//
//        // Go to the end of the word
//        while (pos < text.length() && !Character.isWhitespace(text.charAt(pos))) {
//            pos++;
//        }
//
//        return pos;
//    }
//
//    @Override
//    public void update(float deltaTime) {
//        if (!visible) return;
//
//        // Update child components
//        backgroundSprite.update(deltaTime);
//        textDisplay.update(deltaTime);
//
//        // Don't process input if disabled
//        if (currentState == InputFieldState.DISABLED) {
//            return;
//        }
//
//        // Handle mouse input for focus
//        boolean mouseOver = isMouseOver();
//        boolean mouseClicked = input.isMouseButtonJustPressed(GLFW_MOUSE_BUTTON_LEFT);
//
//        if (mouseClicked) {
//            if (mouseOver) {
//                // Clicked on this field - focus it
//                if (currentState != InputFieldState.FOCUSED) {
//                    currentState = InputFieldState.FOCUSED;
//                    updateVisuals();
//                }
//
//                // Could calculate cursor position based on click position
//                // For simplicity, just move cursor to end
//                cursorPosition = text.length();
//                selectionStart = cursorPosition;
//                selectionEnd = cursorPosition;
//                updateTextDisplay();
//            } else {
//                // Clicked elsewhere - lose focus
//                if (currentState == InputFieldState.FOCUSED) {
//                    currentState = InputFieldState.NORMAL;
//                    updateVisuals();
//                    updateTextDisplay();
//                }
//            }
//        }
//
//        // Handle text input when focused
//        if (currentState == InputFieldState.FOCUSED) {
//            handleTextInput();
//
//            // Blink cursor
//            cursorBlinkTimer += deltaTime;
//            if (cursorBlinkTimer >= 0.5f) {
//                cursorBlinkTimer = 0;
//                cursorVisible = !cursorVisible;
//                updateTextDisplay();
//            }
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
//        textDisplay.render(viewProj);
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
//        textDisplay.setPosition(textDisplay.getX() + offsetX, textDisplay.getY() + offsetY);
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
//        // Position the text in the middle of the field
//        textDisplay.setPosition(x + 5, y + height/2);
//
//        layoutDirty = true;
//    }
//
//    /**
//     * Interface for validating input text
//     */
//    public interface InputValidationFunction {
//        boolean validate(String text);
//    }
//}