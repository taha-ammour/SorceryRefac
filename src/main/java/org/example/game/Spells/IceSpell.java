package org.example.game.Spells;

public class IceSpell extends AbstractSpell {
    private final float slowFactor;
    private final float slowDuration;

    private IceSpell(IceSpellBuilder builder) {
        super(builder);
        this.slowFactor = builder.slowFactor;
        this.slowDuration = builder.slowDuration;
    }

    public float getSlowFactor() {
        return slowFactor;
    }

    public float getSlowDuration() {
        return slowDuration;
    }

    @Override
    protected void performCast() {
        System.out.println("Casting Ice Spell (Player: " + playerId + ") at level " + level +
                " dealing " + damage + " damage with " + (slowFactor * 100) + "% slow for " +
                slowDuration + " seconds. Energy cost: " + energyCost +
                ", Cooldown: " + cooldown + "s");
    }

    @Override
    public AbstractSpell upgrade() {
        // Check max level
        if (level >= maxLevel) {
            throw new IllegalStateException("Spell is already at max level");
        }

        // Create a new builder with increased attributes
        return new IceSpellBuilder()
                .playerId(playerId)
                .level(level + 1)
                .buyCost(buyCost)
                .energyCost(energyCost + 4)
                .damage(damage + 8)
                .cooldown(Math.max(0.8, cooldown - 0.1))
                .slowFactor(slowFactor + 0.05f)
                .slowDuration(slowDuration + 0.2f)
                .build();
    }

    public static class IceSpellBuilder extends AbstractSpellBuilder<IceSpellBuilder> {
        private float slowFactor = 0.2f;
        private float slowDuration = 1.5f;

        public IceSpellBuilder() {
            buyCost = 180;
            energyCost = 25;
            damage = 30.0;
            cooldown = 2.0;
            maxLevel = 5;
        }

        public IceSpellBuilder slowFactor(float slowFactor) {
            this.slowFactor = slowFactor;
            return this;
        }

        public IceSpellBuilder slowDuration(float slowDuration) {
            this.slowDuration = slowDuration;
            return this;
        }

        @Override
        protected IceSpellBuilder self() {
            return this;
        }

        @Override
        public IceSpell build() {
            return new IceSpell(this);
        }
    }
}
