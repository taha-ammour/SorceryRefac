package org.example.game.Spells;

public class LightningSpell extends AbstractSpell {
    private final int chainCount;
    private final float chainEfficiency;

    private LightningSpell(LightningSpellBuilder builder) {
        super(builder);
        this.chainCount = builder.chainCount;
        this.chainEfficiency = builder.chainEfficiency;
    }

    public int getChainCount() {
        return chainCount;
    }

    public float getChainEfficiency() {
        return chainEfficiency;
    }

    @Override
    protected void performCast() {
        System.out.println("Casting Lightning Spell (Player: " + playerId + ") at level " + level +
                " dealing " + damage + " damage with " + chainCount + " chain targets at " +
                (chainEfficiency * 100) + "% efficiency. Energy cost: " + energyCost +
                ", Cooldown: " + cooldown + "s");
    }

    @Override
    public AbstractSpell upgrade() {
        // Check max level
        if (level >= maxLevel) {
            throw new IllegalStateException("Spell is already at max level");
        }

        // Create a new builder with increased attributes
        return new LightningSpellBuilder()
                .playerId(playerId)
                .level(level + 1)
                .buyCost(buyCost)
                .energyCost(energyCost + 7)
                .damage(damage + 12)
                .cooldown(cooldown)
                .chainCount(level % 2 == 0 ? chainCount + 1 : chainCount)
                .chainEfficiency(Math.min(0.9f, chainEfficiency + 0.05f))
                .build();
    }

    public static class LightningSpellBuilder extends AbstractSpellBuilder<LightningSpellBuilder> {
        private int chainCount = 1;
        private float chainEfficiency = 0.5f;

        public LightningSpellBuilder() {
            buyCost = 200;
            energyCost = 30;
            damage = 40.0;
            cooldown = 2.5;
            maxLevel = 5;
        }

        public LightningSpellBuilder chainCount(int chainCount) {
            this.chainCount = chainCount;
            return this;
        }

        public LightningSpellBuilder chainEfficiency(float chainEfficiency) {
            this.chainEfficiency = chainEfficiency;
            return this;
        }

        @Override
        protected LightningSpellBuilder self() {
            return this;
        }

        @Override
        public LightningSpell build() {
            return new LightningSpell(this);
        }
    }
}
