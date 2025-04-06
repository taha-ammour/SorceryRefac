package org.example.ui;

import org.example.GameWorld;
import org.example.engine.FontSheet;
import org.example.engine.Shader;
import org.example.engine.SpriteManager;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * UI component for displaying spell information, energy bar, and cooldowns
 */
public class SpellUI extends UIComponent {
    private final GameWorld gameWorld;
    private final UUID playerId;
    private final SpriteManager spriteManager;

    // UI elements
    private UIText energyText;
    private UISprite energyBar;
    private UISprite[] spellIcons;
    private UIText[] cooldownTexts;

    // Spell types
    private static final String[] SPELL_TYPES = {"fire", "ice", "lightning"};

    // Icon sprite names for each spell
    private static final String[] SPELL_ICONS = {
            "flame_spell_fil_ic_1",  // Fire
            "type_spell_sp_ic_1",    // Ice
            "type_spell_fl_ic_1"     // Lightning
    };

    public SpellUI(float x, float y, float width, float height,
                   GameWorld gameWorld, UUID playerId,
                   SpriteManager spriteManager, FontSheet fontSheet, Shader fontShader) {
        super(x, y, width, height);
        this.gameWorld = gameWorld;
        this.playerId = playerId;
        this.spriteManager = spriteManager;

        // Initialize UI elements
        initUI(fontSheet, fontShader);
    }

    private void initUI(FontSheet fontSheet, Shader fontShader) {
        // Create energy text
        energyText = new UIText(fontSheet, fontShader, "Energy: 100/100", x, y);

        // Create energy bar (placeholder - you would use a real UI element)
        // For now using a sprite as placeholder
        try {
            energyBar = new UISprite(x, y + 30, 100, 10, "progressbar_upward_start", spriteManager);
        } catch (Exception e) {
            System.err.println("Could not create energy bar: " + e.getMessage());
        }

        // Create spell icons and cooldown texts
        spellIcons = new UISprite[SPELL_TYPES.length];
        cooldownTexts = new UIText[SPELL_TYPES.length];

        for (int i = 0; i < SPELL_TYPES.length; i++) {
            try {
                // Create spell icon
                float iconX = x + (i * 50); // Space icons apart
                float iconY = y + 50;
                spellIcons[i] = new UISprite(iconX, iconY, 32, 32, SPELL_ICONS[i], spriteManager);

                // Create cooldown text below icon
                cooldownTexts[i] = new UIText(fontSheet, fontShader, "", iconX, iconY + 40);
            } catch (Exception e) {
                System.err.println("Could not create spell UI elements: " + e.getMessage());
            }
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!visible) return;

        // Update energy display
        float currentEnergy = gameWorld.getPlayerEnergy(playerId);
        float maxEnergy = gameWorld.getPlayerMaxEnergy();
        energyText.setText(String.format("Energy: %.0f/%.0f", currentEnergy, maxEnergy));

        // Update energy bar (placeholder implementation)
        if (energyBar != null) {
            float energyPercent = currentEnergy / maxEnergy;
            energyBar.setScale(energyPercent);
        }

        // Update cooldown texts
        for (int i = 0; i < SPELL_TYPES.length; i++) {
            if (cooldownTexts[i] != null) {
                float cooldown = gameWorld.getSpellCooldown(playerId, SPELL_TYPES[i]);
                if (cooldown > 0) {
                    cooldownTexts[i].setText(String.format("%.1fs", cooldown));
                } else {
                    cooldownTexts[i].setText("Ready");
                }
            }
        }
    }

    @Override
    public void render(Matrix4f viewProj) {
        if (!visible) return;

        // Render energy text
        energyText.render(viewProj);

        // Render energy bar
        if (energyBar != null) {
            energyBar.render(viewProj);
        }

        // Render spell icons and cooldown texts
        for (int i = 0; i < SPELL_TYPES.length; i++) {
            if (spellIcons[i] != null) {
                spellIcons[i].render(viewProj);
            }
            if (cooldownTexts[i] != null) {
                cooldownTexts[i].render(viewProj);
            }
        }
    }
}