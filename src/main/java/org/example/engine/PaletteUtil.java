package org.example.engine;

import org.joml.Vector3f;

/**
 * Utility class for converting palette codes to color values.
 * Each palette code is a 3‑digit string (each digit 0–5) that is scaled into a value between 0 and 1.
 */
public class PaletteUtil {

    /**
     * Converts a three-digit code string to a Vector3f color.
     * For example, "012" converts to (0/5, 1/5, 2/5).
     *
     * @param code A 3‑digit string.
     * @return A Vector3f containing the scaled RGB values.
     */
    public static Vector3f codeToColor(String code) {
        if (code == null || code.length() != 3) {
            throw new IllegalArgumentException("Code must be a 3-digit string.");
        }
        float r = (code.charAt(0) - '0') / 5.0f;
        float g = (code.charAt(1) - '0') / 5.0f;
        float b = (code.charAt(2) - '0') / 5.0f;
        return new Vector3f(r, g, b);
    }

    /**
     * Converts an array of 4 three-digit code strings to a flat float array representing the palette.
     * The output will have 12 elements (4 colors * 3 channels).
     *
     * @param codes An array of 4 palette code strings.
     * @return A float array of length 12.
     */
    public static float[] codesToPalette(String[] codes) {
        if (codes == null || codes.length != 4) {
            throw new IllegalArgumentException("There must be exactly 4 codes.");
        }
        float[] palette = new float[12];
        for (int i = 0; i < 4; i++) {
            Vector3f color = codeToColor(codes[i]);
            palette[i * 3 + 0] = color.x;
            palette[i * 3 + 1] = color.y;
            palette[i * 3 + 2] = color.z;
        }
        return palette;
    }
}
