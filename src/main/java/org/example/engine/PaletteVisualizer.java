package org.example.engine;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class PaletteVisualizer extends GameObject {
    private final List<PreviewCell> cells = new ArrayList<>();
    private final SpriteManager spriteManager;
    private final String baseSpriteName;
    private final int cellWidth, cellHeight, columns;

    // For mouse interaction:
    private int hoveredIndex = -1;
    private int selectedIndex = -1;
    private final int previewScale = 3;
    private Input input;

    // FBO fields for baking the grid
    private int fbo;
    private int fboTexture;
    private int gridWidth, gridHeight;
    private boolean fboDirty = true;

    // Cached BatchedFontObject for drawing text (tooltip/labels)
    private BatchedFontObject tooltipFont = null;
    private String lastTooltip = "";

    public PaletteVisualizer(Input input, SpriteManager spriteManager, String baseSpriteName, int cellWidth, int cellHeight, int columns) {
        this.spriteManager = spriteManager;
        this.baseSpriteName = baseSpriteName;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.columns = columns;
        this.input = input;
        precomputeCells();
        computeGridSize();
        createFBO();
        // Bake grid to FBO with an orthographic projection matching grid size.
        Matrix4f ortho = new Matrix4f().ortho2D(0, gridWidth, gridHeight, 0);
        updateFBO(ortho);
    }

    /**
     * Precomputes all 6^4 = 1296 palette combinations.
     */
    private void precomputeCells() {
        String[] digits = new String[6];
        for (int i = 0; i < 6; i++) {
            digits[i] = "" + i + i + i;
        }
        int index = 0;
        for (int a = 0; a < 6; a++) {
            for (int b = 0; b < 6; b++) {
                for (int c = 0; c < 6; c++) {
                    for (int d = 0; d < 6; d++) {
                        String[] palette = new String[]{digits[a], digits[b], digits[c], digits[d]};
                        Sprite sprite = spriteManager.getSprite(baseSpriteName);
                        sprite.setPaletteFromCodes(palette);
                        int col = index % columns;
                        int row = index / columns;
                        float posX = 20 + col * (cellWidth + 2);
                        float posY = 20 + row * (cellHeight + 2);
                        sprite.setPosition(posX, posY);
                        sprite.setZ(0);
                        cells.add(new PreviewCell(index, palette, sprite, posX, posY, cellWidth, cellHeight));
                        index++;
                    }
                }
            }
        }
        System.out.println("Precomputed " + index + " palette cells.");
    }

    private void computeGridSize() {
        int rows = (cells.size() + columns - 1) / columns;
        gridWidth = 20 + columns * (cellWidth + 2);
        gridHeight = 20 + rows * (cellHeight + 2);
    }

    private void createFBO() {
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        fboTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fboTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, gridWidth, gridHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTexture, 0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("ERROR: Framebuffer not complete!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        fboDirty = true;
    }

    /**
     * Renders all palette cells into the FBO.
     */
    private void updateFBO(Matrix4f orthoProj) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, gridWidth, gridHeight);
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
        for (PreviewCell cell : cells) {
            cell.sprite.render(orthoProj);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        fboDirty = false;
    }

    @Override
    public void update(float deltaTime) {
        float mx = input.getMouseX();
        float my = input.getMouseY();
        hoveredIndex = -1;
        for (PreviewCell cell : cells) {
            if (cell.contains(mx, my)) {
                hoveredIndex = cell.index;
                if (input.isMouseButtonDown(0)) {
                    selectedIndex = cell.index;
                }
                break;
            }
        }
    }

    @Override
    public void render(Matrix4f viewProj) {
        // If FBO is dirty, update it.
        if (fboDirty) {
            Matrix4f ortho = new Matrix4f().ortho2D(0, gridWidth, gridHeight, 0);
            updateFBO(ortho);
        }
        // Instead of drawing a quad, we blit the FBO directly to the default framebuffer.
        // Set the read framebuffer to our FBO and the draw framebuffer to 0.
        glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        // Adjust destination rectangle as desired. Here, we copy the FBO content starting at (0, 0).
        glBlitFramebuffer(0, 0, gridWidth, gridHeight, 0, 0, gridWidth, gridHeight, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

        // Draw tooltip for hovered cell.
        if (hoveredIndex != -1) {
            PreviewCell cell = cells.get(hoveredIndex);
            String tooltip = String.format("[%s, %s, %s, %s]", cell.palette[0], cell.palette[1], cell.palette[2], cell.palette[3]);
            drawText(tooltip, input.getMouseX() + 10, input.getMouseY() - 10, viewProj);
        }

        // Draw large preview of selected palette.
        if (selectedIndex != -1) {
            PreviewCell cell = cells.get(selectedIndex);
            Sprite previewSprite = spriteManager.getSprite(baseSpriteName);
            previewSprite.setPaletteFromCodes(cell.palette);
            previewSprite.setPosition(1024 - cellWidth * previewScale - 20, 20);
            previewSprite.setZ(0);
            float origScaleX = previewSprite.getScaleX();
            float origScaleY = previewSprite.getScaleY();
            previewSprite.setScale(origScaleX * previewScale, origScaleY * previewScale);
            previewSprite.render(viewProj);
            drawText("Selected: " + String.join(", ", cell.palette), 1024 - cellWidth * previewScale - 20, 20 + cellHeight * previewScale + 10, viewProj);
            previewSprite.setScale(origScaleX, origScaleY);
        }
    }

    @Override
    public void cleanup() {
        glDeleteFramebuffers(fbo);
        glDeleteTextures(fboTexture);
        if (tooltipFont != null) {
            tooltipFont.cleanup();
        }
    }

    /**
     * Draws text using a cached BatchedFontObject.
     * This object is recreated only if the text changes.
     */
    private void drawText(String text, float x, float y, Matrix4f viewProj) {
        if (tooltipFont == null || !text.equals(lastTooltip)) {
            if (tooltipFont != null) {
                tooltipFont.cleanup();
            }
            FontSheet fontSheet = new FontSheet();
            Shader fontShader = Shader.loadFromFiles("/shaders/sprite_instanced.vs.glsl", "/shaders/Fontsh.fs.glsl");
            tooltipFont = new BatchedFontObject(fontSheet, text, x, y, 0.0f, 0, 0xFFFFFF, 1.0f, fontShader);
            lastTooltip = text;
        } else {
            tooltipFont.setPosition(x, y, 0);
        }
        tooltipFont.render(viewProj);
    }

    private static class PreviewCell {
        int index;
        String[] palette;
        Sprite sprite;
        float x, y, width, height;

        PreviewCell(int index, String[] palette, Sprite sprite, float x, float y, float width, float height) {
            this.index = index;
            this.palette = palette;
            this.sprite = sprite;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean contains(float mx, float my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }
    }
}
