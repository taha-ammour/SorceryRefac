package org.example.engine.ecs.components;

import org.example.engine.Sprite;
import org.example.engine.ecs.Component; /**
 * Component that stores render data for a sprite
 */
public class SpriteComponent extends Component {
    private Sprite sprite;
    private int color = 0xFFFFFF;
    private float alpha = 1.0f;
    private String[] palette;

    public SpriteComponent(Sprite sprite) {
        this.sprite = sprite;
    }

    public Sprite getSprite() {
        return sprite;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public String[] getPalette() {
        return palette;
    }

    public void setPalette(String[] palette) {
        this.palette = palette;
        if (sprite != null && palette != null) {
            sprite.setPaletteFromCodes(palette);
        }
    }
}
