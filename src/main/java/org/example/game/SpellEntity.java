package org.example.game;

import org.example.Packets;
import org.example.engine.GameObject;
import org.example.engine.Sprite;
import org.example.engine.SpriteManager;
import org.example.engine.ZOrderProvider;
import org.example.game.Spells.AbstractSpell;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a cast spell in the game world
 */
public class SpellEntity extends GameObject implements ZOrderProvider {
    // Spell components
    private AbstractSpell spell;
    private SpriteManager spriteManager;

    // Position and state
    private Vector3f position = new Vector3f();
    private float lifeTime = 0f;
    private float maxLifeTime = 3.0f;
    private boolean active = true;
    private boolean debug = false;

    // Rendering components
    private Sprite currentSprite;
    private List<Sprite> effectSprites = new ArrayList<>();

    // Spell type and variations
    private SpellType spellType;
    private Map<String, List<Integer>> spellVariations = new HashMap<>();
    private String currentVariation;

    // Animation tracking
    private float animationTimer = 0f;
    private float animationSpeed = 0.2f;
    private int currentFrame = 0;

    // Used for glow effects and color shifting
    private float glowTimer = 0f;
    private float glowSpeed = 2.0f;
    private float scaleTimer = 0f;
    private float scaleSpeed = 3.0f;

    /**
     * Spell types with their respective sprite IDs
     */
    public enum SpellType {
        FIRE("fire", new int[]{158, 159, 160, 161}),      // Fire spell sprites
        ICE("ice", new int[]{221, 222, 221, 222}),        // Ice spell sprites
        LIGHTNING("lightning", new int[]{223, 224, 223, 224}); // Lightning spell sprites

        private final String typeName;
        private final int[] spriteIds;

        SpellType(String typeName, int[] spriteIds) {
            this.typeName = typeName;
            this.spriteIds = spriteIds;
        }

        public String getTypeName() {
            return typeName;
        }

        public int[] getSpriteIds() {
            return spriteIds;
        }
    }

    /**
     * Create a new spell entity
     */
    public SpellEntity(SpriteManager spriteManager, AbstractSpell spell, SpellType spellType, float x, float y) {
        this.spriteManager = spriteManager;
        this.spell = spell;
        this.spellType = spellType;
        this.position.x = x;
        this.position.y = y;
        this.position.z = 10.0f; // High Z value to ensure visibility

        System.out.println("Creating " + spellType.getTypeName() + " spell at " + x + "," + y);

        // Initialize spell variations
        initializeSpellVariations();

        // Set initial variation
        setVariation("impact");  // Start with impact for better visuals

        // Create additional effect sprites for enhanced visuals
        createEffectSprites();

        // Cast the spell
        if (spell != null) {
            try {
                spell.cast();
            } catch (Exception e) {
                System.err.println("Error casting spell: " + e.getMessage());
            }
        }

        this.active = true;
    }

    /**
     * Initialize the different variations of spell animations
     */
    private void initializeSpellVariations() {
        // Base variation (primary sprites)
        List<Integer> baseIds = new ArrayList<>();
        for (int id : spellType.getSpriteIds()) {
            baseIds.add(id);
        }
        spellVariations.put("base", baseIds);

        // Cast variation (when spell is being cast)
        List<Integer> castIds = new ArrayList<>();
        switch (spellType) {
            case FIRE:
                castIds.add(154);
                castIds.add(155);
                castIds.add(156);
                castIds.add(157);
                break;
            case ICE:
                castIds.add(221);
                castIds.add(222);
                castIds.add(221);
                castIds.add(222);
                break;
            case LIGHTNING:
                castIds.add(223);
                castIds.add(224);
                castIds.add(223);
                castIds.add(224);
                break;
        }
        spellVariations.put("cast", castIds);

        // Impact variation (when spell hits)
        List<Integer> impactIds;
        if (spellType == SpellType.FIRE) {
            impactIds = new ArrayList<>();
            impactIds.add(158);
            impactIds.add(159);
            impactIds.add(160);
            impactIds.add(161);
        } else {
            // For other spell types, reuse cast sprites
            impactIds = new ArrayList<>(castIds);
        }
        spellVariations.put("impact", impactIds);

        System.out.println("Initialized spell variations for " + spellType.getTypeName());
    }

    /**
     * Create additional effect sprites for better visuals
     */
    private void createEffectSprites() {
        try {
            // Choose appropriate effects based on spell type
            String[] effectSpriteNames;
            switch (spellType) {
                case FIRE:
                    effectSpriteNames = new String[]{"spell_cast_fire_1", "spell_cast_fire_2", "spell_cast_fire_3"};
                    break;
                case ICE:
                    effectSpriteNames = new String[]{"spell_castpartpoint_1", "spell_castpartpoint_2", "spell_castpartpoint_3"};
                    break;
                case LIGHTNING:
                    effectSpriteNames = new String[]{"spell_cast_sm_fire_1", "spell_cast_sm_fire_2", "spell_cast_sm_fire_3"};
                    break;
                default:
                    effectSpriteNames = new String[]{"spell_cast_fire_1", "spell_cast_fire_2"};
            }

            // Create sprites for each effect
            for (String spriteName : effectSpriteNames) {
                try {
                    Sprite effectSprite = spriteManager.getSprite(spriteName);
                    if (effectSprite != null) {
                        // Position at spell location
                        effectSprite.setPosition(position.x, position.y);
                        effectSprite.setZ(position.z - 0.1f); // Slightly behind main sprite

                        // Apply color based on spell type
                        applySpellTypeColor(effectSprite);

                        // Make larger for visibility
                        effectSprite.setScale(3.0f, 3.0f);

                        // Add to effects list
                        effectSprites.add(effectSprite);
                    }
                } catch (Exception e) {
                    // Skip if sprite not found
                    System.err.println("Effect sprite not found: " + spriteName);
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating effect sprites: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Apply color effects based on spell type
     */
    private void applySpellTypeColor(Sprite sprite) {
        if (sprite == null) return;

        switch (spellType) {
            case FIRE:
                sprite.setColor(0xFF5500, 1.0f); // Orange for fire
                break;
            case ICE:
                sprite.setColor(0x00AAFF, 1.0f); // Blue for ice
                break;
            case LIGHTNING:
                sprite.setColor(0xFFFF00, 1.0f); // Yellow for lightning
                break;
        }
    }

    /**
     * Set the current variation of the spell
     */
    public void setVariation(String variationName) {
        if (!spellVariations.containsKey(variationName)) {
            System.err.println("Unknown spell variation: " + variationName);
            return;
        }

        this.currentVariation = variationName;
        this.currentFrame = 0;
        updateSprite(); // Update sprite after changing variation
    }

    /**
     * Update the current sprite based on animation frame
     */
    private void updateSprite() {
        try {
            // Get the sprite IDs for the current variation
            List<Integer> frames = spellVariations.get(currentVariation);
            if (frames == null || frames.isEmpty()) {
                System.err.println("No frames for variation: " + currentVariation);
                return;
            }

            // Get sprite ID for current frame
            int spriteId = frames.get(currentFrame % frames.size());

            try {
                // Create or get sprite
                Sprite sprite = spriteManager.getSprite(spriteId);

                if (sprite != null) {
                    // Update the current sprite
                    currentSprite = sprite;

                    // Set position
                    currentSprite.setPosition(position.x, position.y);
                    currentSprite.setZ(position.z);

                    // Make it larger for visibility
                    currentSprite.setScale(3.0f, 3.0f);

                    // Apply color based on spell type
                    applySpellTypeColor(currentSprite);
                } else {
                    System.err.println("Failed to get sprite with ID: " + spriteId);
                }
            } catch (Exception e) {
                System.err.println("Error updating sprite: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error in updateSprite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!active) return;

        // Update lifetime
        lifeTime += deltaTime;

        // Update animation
        animationTimer += deltaTime;
        if (animationTimer >= animationSpeed) {
            // Move to next frame
            currentFrame = (currentFrame + 1) % spellVariations.get(currentVariation).size();

            // Update sprite for new frame
            updateSprite();

            // Reset animation timer
            animationTimer = 0f;
        }

        // Update glow and scale effects
        updateVisualEffects(deltaTime);

        // Update effect sprites positions
        for (Sprite effect : effectSprites) {
            effect.setPosition(position.x, position.y);
        }

        // Deactivate spell after max lifetime
        if (lifeTime >= maxLifeTime) {
            active = false;
        }
    }

    /**
     * Update visual effects like glow and scaling
     */
    private void updateVisualEffects(float deltaTime) {
        // Update glow timer
        glowTimer += deltaTime * glowSpeed;
        float glowFactor = 0.7f + 0.3f * (float)Math.sin(glowTimer);

        // Update scale timer
        scaleTimer += deltaTime * scaleSpeed;
        float scaleFactor = 2.5f + 0.5f * (float)Math.sin(scaleTimer);

        // Apply effects to current sprite
        if (currentSprite != null) {
            // Apply pulsing scale effect
            currentSprite.setScale(scaleFactor, scaleFactor);

            // Apply color intensity based on spell type
            switch (spellType) {
                case FIRE:
                    // Pulsing red-orange glow for fire
                    currentSprite.setColor(0xFF2000 + (int)(0x30 * glowFactor), 1.0f);
                    break;
                case ICE:
                    // Pulsing blue glow for ice
                    currentSprite.setColor(0x0040FF + (int)(0x30 * glowFactor), 1.0f);
                    break;
                case LIGHTNING:
                    // Pulsing yellow glow for lightning
                    currentSprite.setColor(0xFFCC00 + (int)(0x30 * glowFactor), 1.0f);
                    break;
            }
        }

        // Apply effects to effect sprites
        for (int i = 0; i < effectSprites.size(); i++) {
            Sprite effect = effectSprites.get(i);

            // Different scale for each effect sprite
            float effectScale = 2.0f + 0.5f * (float)Math.sin(scaleTimer + i * 0.5f);
            effect.setScale(effectScale, effectScale);

            // Different position offsets for each effect
            float offsetX = 5.0f * (float)Math.sin(glowTimer + i * 0.7f);
            float offsetY = 5.0f * (float)Math.cos(glowTimer + i * 0.7f);
            effect.setPosition(position.x + offsetX, position.y + offsetY);
        }
    }

    @Override
    public void render(Matrix4f viewProj) {
        if (!active) return;

        try {
            // Render effect sprites first (they go behind the main sprite)
            for (Sprite effect : effectSprites) {
                if (effect != null) {
                    effect.render(viewProj);
                }
            }

            // Render main sprite on top
            if (currentSprite != null) {
                currentSprite.render(viewProj);
            } else {
                System.err.println("Cannot render spell - sprite is null");
            }

        } catch (Exception e) {
            System.err.println("Error rendering spell: " + e.getMessage());
            if (debug) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Convert to network action packet
     */
    public Packets.GameAction toGameAction() {
        Packets.GameAction action = new Packets.GameAction();
        action.actionType = getSpellActionType();
        action.x = position.x;
        action.y = position.y;
        return action;
    }

    private int getSpellActionType() {
        switch(spellType) {
            case FIRE: return 1;
            case ICE: return 2;
            case LIGHTNING: return 3;
            default: return 0;
        }
    }

    // Getters and setters
    public AbstractSpell getSpell() {
        return spell;
    }

    public SpellType getSpellType() {
        return spellType;
    }

    public boolean isActive() {
        return active;
    }

    public void setPosition(float x, float y) {
        position.x = x;
        position.y = y;
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }

    public void setMaxLifeTime(float maxLifeTime) {
        this.maxLifeTime = maxLifeTime;
    }

    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void cleanup() {
        active = false;
    }

    @Override
    public float getZ() {
        return position.z;
    }
}