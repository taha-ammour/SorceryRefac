package org.example.game;

import org.example.Packets;
import org.example.engine.GameObject;
import org.example.engine.Sprite;
import org.example.engine.SpriteManager;
import org.example.engine.ZOrderProvider;
import org.example.game.Spells.AbstractSpell;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellEntity extends GameObject implements ZOrderProvider {
    // Spell logic from previous implementation
    private AbstractSpell spell;

    // Rendering components
    private Sprite currentSprite;
    private SpriteManager spriteManager;
    private float x, y;
    private float lifeTime = 0f;
    private float maxLifeTime = 3f; // Increased duration for visibility
    private boolean active = false;
    private boolean debug = true;

    // Spell type and variations
    private SpellType spellType;
    private Map<String, List<Integer>> spellVariations;
    private String currentVariation;

    // Animation tracking
    private float animationTimer = 0f;
    private float animationSpeed = 0.2f;
    private int currentFrame = 0;

    public enum SpellType {
        FIRE("fire", new int[]{218, 219}),  // Using flame_spell_fil_ic_1, flame_spell_ic_1
        ICE("ice", new int[]{221, 222}),    // Using spread_spell_ic_1, type_spell_sp_ic_1
        LIGHTNING("lightning", new int[]{223, 224}); // Using type_spell_fl_ic_1, type_spell_pa_ic_1

        private final String typeName;
        private final int[] baseSpriteIds;

        SpellType(String typeName, int[] baseSpriteIds) {
            this.typeName = typeName;
            this.baseSpriteIds = baseSpriteIds;
        }

        public String getTypeName() {
            return typeName;
        }

        public int[] getBaseSpriteIds() {
            return baseSpriteIds;
        }
    }

    public SpellEntity(SpriteManager spriteManager, AbstractSpell spell, SpellType spellType, float x, float y) {
        this.spriteManager = spriteManager;
        this.spell = spell;
        this.spellType = spellType;
        this.x = x;
        this.y = y;

        System.out.println("Creating " + spellType.getTypeName() + " spell at " + x + "," + y);

        // Initialize spell variations
        initializeSpellVariations();

        // Set initial variation
        setVariation("base");

        // Cast the spell
        spell.cast();
        this.active = true;

        // Force sprite update after creation
        updateSprite();
    }

    private void initializeSpellVariations() {
        spellVariations = new HashMap<>();

        // Add base variation from sprite type
        List<Integer> baseVariation = new ArrayList<>();
        for (int spriteId : spellType.getBaseSpriteIds()) {
            baseVariation.add(spriteId);
            System.out.println("Added base sprite ID: " + spriteId + " to variation");
        }
        spellVariations.put("base", baseVariation);

        // Add cast variations based on spell type
        List<Integer> castVariation = new ArrayList<>();
        if (spellType == SpellType.FIRE) {
            // Use pre-defined fire spell cast sprites (154-157)
            castVariation.add(154);
            castVariation.add(155);
            castVariation.add(156);
            castVariation.add(157);
        } else if (spellType == SpellType.ICE) {
            // For ice, we can use the spread spell sprites
            castVariation.add(221);
            castVariation.add(221);
            castVariation.add(222);
            castVariation.add(222);
        } else if (spellType == SpellType.LIGHTNING) {
            // For lightning, use the type spell sprites
            castVariation.add(223);
            castVariation.add(224);
            castVariation.add(223);
            castVariation.add(224);
        }
        spellVariations.put("cast", castVariation);

        // Add impact variations
        List<Integer> impactVariation = new ArrayList<>();
        if (spellType == SpellType.FIRE) {
            // Use pre-defined fire spell impact sprites (158-161)
            impactVariation.add(158);
            impactVariation.add(159);
            impactVariation.add(160);
            impactVariation.add(161);
        } else {
            // For other spells, reuse cast sprites for impact
            impactVariation.addAll(castVariation);
        }
        spellVariations.put("impact", impactVariation);
    }

    // Set current spell variation
    public void setVariation(String variationName) {
        if (!spellVariations.containsKey(variationName)) {
            System.err.println("Unknown spell variation: " + variationName);
            return;
        }

        this.currentVariation = variationName;
        this.currentFrame = 0;
        System.out.println("Set " + spellType.getTypeName() + " spell variation to: " + variationName);
        updateSprite();
    }

    private void updateSprite() {
        // Get the current variation's sprite IDs
        List<Integer> currentFrames = spellVariations.get(currentVariation);
        if (currentFrames == null || currentFrames.isEmpty()) {
            System.err.println("No frames for variation: " + currentVariation);
            return;
        }

        // Get the sprite ID for the current frame
        int spriteId = currentFrames.get(currentFrame % currentFrames.size());

        try {
            // Get the sprite by ID
            this.currentSprite = spriteManager.getSprite(spriteId);

            if (this.currentSprite != null) {
                System.out.println("Using sprite ID: " + spriteId + " for " +
                        spellType.getTypeName() + " spell, frame: " + currentFrame);

                // Set position and enhance visibility
                this.currentSprite.setPosition(x, y);
                this.currentSprite.setZ(50.0f);  // High Z-order for visibility
                this.currentSprite.setScale(3.0f, 3.0f);  // Make it larger
            } else {
                System.err.println("Failed to get sprite with ID: " + spriteId);
            }
        } catch (Exception e) {
            System.err.println("Error getting sprite: " + e.getMessage());
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
            // Get the current variation's frames
            List<Integer> currentFrames = spellVariations.get(currentVariation);
            if (currentFrames != null && !currentFrames.isEmpty()) {
                // Update frame
                currentFrame = (currentFrame + 1) % currentFrames.size();

                // Update sprite
                updateSprite();

                System.out.println("Updated " + spellType.getTypeName() +
                        " spell to frame " + currentFrame + " of " + currentFrames.size());
            }

            // Reset animation timer
            animationTimer = 0f;
        }

        // Deactivate spell after max lifetime
        if (lifeTime >= maxLifeTime) {
            System.out.println("Spell deactivated after reaching max lifetime");
            active = false;
        }
    }

    @Override
    public void render(Matrix4f viewProjectionMatrix) {
        if (!active) {
            return;
        }

        if (currentSprite == null) {
            System.err.println("Cannot render spell - sprite is null");
            return;
        }

        try {
            // Update position before rendering
            currentSprite.setPosition(x, y);

            // Render sprite
            currentSprite.render(viewProjectionMatrix);

            if (debug && Math.random() < 0.05) { // Only log occasionally to avoid spam
                System.out.println("Rendering " + spellType.getTypeName() +
                        " spell at: " + x + "," + y + ", frame: " + currentFrame);
            }
        } catch (Exception e) {
            System.err.println("Error rendering spell: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Create a copy of the spell entity
    public SpellEntity createCopy() {
        SpellEntity copy = new SpellEntity(spriteManager, spell, spellType, x, y);
        copy.currentVariation = this.currentVariation;
        copy.currentFrame = this.currentFrame;
        copy.maxLifeTime = this.maxLifeTime;
        return copy;
    }

    // Networking support
    public Packets.GameAction toGameAction() {
        Packets.GameAction action = new Packets.GameAction();
        action.actionType = getSpellActionType();
        action.x = x;
        action.y = y;
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

    // Getters
    public AbstractSpell getSpell() {
        return spell;
    }

    public SpellType getSpellType() {
        return spellType;
    }

    public boolean isActive() {
        return active;
    }

    // Additional methods for spell positioning and management
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        if (currentSprite != null) {
            currentSprite.setPosition(x, y);
        }
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setMaxLifeTime(float maxLifeTime) {
        this.maxLifeTime = maxLifeTime;
    }

    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    @Override
    public void cleanup() {
        // Free any resources if needed
        active = false;
    }

    @Override
    public float getZ() {
        return 50.0f; // Ensure spell is rendered above other elements
    }
}