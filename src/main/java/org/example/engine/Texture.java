package org.example.engine;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

/**
 * Loads an OpenGL texture from a classpath resource and automatically detects up to 4
 * representative grayscale values (where R=G=B) from the image.
 */
public class Texture {
    private final int textureId;
    private final int width;
    private final int height;

    // Stores exactly 4 reference grayscale values (each as a float[3] in [0..1]).
    private final float[][] detectedGrays = new float[4][3];

    /**
     * Loads a texture without applying a color key.
     * @param resourcePath the resource path (e.g. "/textures/entities.png")
     */
    public Texture(String resourcePath) {
        this(resourcePath, false, 0xFFFFFF);
    }

    /**
     * Loads a texture and optionally applies a color key.
     * @param resourcePath the resource path (e.g. "/textures/entities.png")
     * @param applyColorKey if true, pixels matching the key color will have alpha set to 0.
     * @param colorKey the RGB color to treat as transparent (e.g. 0xFF00FF for pink)
     */
    public Texture(String resourcePath, boolean applyColorKey, int colorKey) {
        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Flip image vertically so that top-left of image becomes bottom-left in OpenGL.
        STBImage.stbi_set_flip_vertically_on_load(false);

        ByteBuffer imageBuffer;
        try {
            imageBuffer = ioResourceToByteBuffer(resourcePath, 8 * 1024);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture file: " + resourcePath, e);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w  = stack.mallocInt(1);
            IntBuffer h  = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            // Force RGBA (4 channels)
            ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, w, h, comp, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load texture file: " + resourcePath
                        + "\n" + STBImage.stbi_failure_reason());
            }
            width = w.get();
            height = h.get();

            // Optionally apply color keying
            if (applyColorKey) {
                applyColorKey(image, width, height, colorKey);
            }

            // Automatically detect up to 4 representative grayscale values.
            detectReferenceGrays(image, width, height);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);

            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

            // For exact matching, use nearest filtering:
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

            // Set wrapping mode
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            STBImage.stbi_image_free(image);
        }
    }

    /**
     * Scans the image data for unique grayscale values (R==G==B).
     * If more than 4 unique values are found, picks 4 representative values: minimum, maximum, and two intermediate values.
     */
    private void detectReferenceGrays(ByteBuffer image, int width, int height) {
        int numPixels = width * height;
        Set<Integer> uniqueGrays = new LinkedHashSet<>();
        for (int i = 0; i < numPixels; i++) {
            int index = i * 4;
            int r = image.get(index) & 0xFF;
            int g = image.get(index + 1) & 0xFF;
            int b = image.get(index + 2) & 0xFF;
            if (r == g && g == b) {
                uniqueGrays.add(r);
            }
        }

        List<Integer> sortedGrays = new ArrayList<>(uniqueGrays);
        Collections.sort(sortedGrays);

        // If fewer than 4 unique grays, pad with zeros.
        int n = sortedGrays.size();
        if (n == 0) {
            // If no grayscale values are found, default to black.
            for (int i = 0; i < 4; i++) {
                detectedGrays[i][0] = 0f;
                detectedGrays[i][1] = 0f;
                detectedGrays[i][2] = 0f;
            }
            return;
        }
        // If exactly 4, use them directly.
        if (n == 4) {
            for (int i = 0; i < 4; i++) {
                float val = sortedGrays.get(i) / 255.0f;
                detectedGrays[i][0] = val;
                detectedGrays[i][1] = val;
                detectedGrays[i][2] = val;
            }
            return;
        }
        // If more than 4, sample at indices: 0, (n-1)/3, 2*(n-1)/3, n-1.
        int index0 = 0;
        int index1 = (n - 1) / 3;
        int index2 = 2 * (n - 1) / 3;
        int index3 = n - 1;
        int[] indices = {index0, index1, index2, index3};
        for (int i = 0; i < 4; i++) {
            float val = sortedGrays.get(indices[i]) / 255.0f;
            detectedGrays[i][0] = val;
            detectedGrays[i][1] = val;
            detectedGrays[i][2] = val;
        }
    }

    private void applyColorKey(ByteBuffer image, int width, int height, int colorKey) {
        int numPixels = width * height;
        for (int i = 0; i < numPixels; i++) {
            int index = i * 4;
            int r = image.get(index) & 0xFF;
            int g = image.get(index + 1) & 0xFF;
            int b = image.get(index + 2) & 0xFF;
            if (r == ((colorKey >> 16) & 0xFF) &&
                    g == ((colorKey >> 8) & 0xFF) &&
                    b == (colorKey & 0xFF)) {
                image.put(index + 3, (byte) 0);
            }
        }
    }

    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        try (InputStream source = Texture.class.getResourceAsStream(resource)) {
            if (source == null)
                throw new IOException("Resource not found: " + resource);
            try (ReadableByteChannel rbc = Channels.newChannel(source)) {
                buffer = ByteBuffer.allocateDirect(bufferSize);
                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1)
                        break;
                    if (buffer.remaining() == 0)
                        buffer = resizeBuffer(buffer, buffer.capacity() * 2);
                }
                buffer.flip();
            }
        }
        return buffer;
    }

    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

    public void bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    public int getId() {
        return textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Returns the 4 detected reference grayscale values as an array of 4 float[3] arrays.
     */
    public float[][] getDetectedGrays() {
        System.out.println(Arrays.deepToString(detectedGrays)); return detectedGrays;
    }
}
