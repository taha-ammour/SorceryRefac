package org.example.engine.ecs.components;

import org.example.engine.ecs.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Component for particle effects
 */
public class ParticleEffectComponent extends Component {
    private int effectType = 0; // 0=fountain, 1=explosion, 2=vortex, 3=spiral, 4=rain, etc.
    private float baseLife = 5.0f;
    private float baseSize = 2.0f;
    private float spawnRate = 0.005f;
    private int maxParticles = 500;
    private int color = 0xFFFFFF;
    private boolean isPlaying = true;

    public ParticleEffectComponent() {
    }

    public ParticleEffectComponent(int effectType) {
        this.effectType = effectType;
    }

    public int getEffectType() {
        return effectType;
    }

    public void setEffectType(int effectType) {
        this.effectType = effectType;
    }

    public float getBaseLife() {
        return baseLife;
    }

    public void setBaseLife(float baseLife) {
        this.baseLife = baseLife;
    }

    public float getBaseSize() {
        return baseSize;
    }

    public void setBaseSize(float baseSize) {
        this.baseSize = baseSize;
    }

    public float getSpawnRate() {
        return spawnRate;
    }

    public void setSpawnRate(float spawnRate) {
        this.spawnRate = spawnRate;
    }

    public int getMaxParticles() {
        return maxParticles;
    }

    public void setMaxParticles(int maxParticles) {
        this.maxParticles = maxParticles;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public void play() {
        isPlaying = true;
    }

    public void stop() {
        isPlaying = false;
    }

    public void burst(int particleCount) {
        // This would be implemented in the ParticleSystem
        // For now, it's just a marker for burst effect
    }
}

