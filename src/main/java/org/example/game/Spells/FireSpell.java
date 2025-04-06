package org.example.game.Spells;


public class FireSpell extends AbstractSpell {
    private FireSpell(FireSpellBuilder builder) {
        super(builder);
    }

    @Override
    protected void performCast() {
        System.out.println("Casting Fire Spell (Player: " + playerId + ") at level " + level +
                " dealing " + damage + " damage. Energy cost: " + energyCost +
                ", Cooldown: " + cooldown + "s");
    }

    @Override
    public AbstractSpell upgrade() {
        // Check max level
        if (level >= maxLevel) {
            throw new IllegalStateException("Spell is already at max level");
        }

        // Create a new builder with increased attributes
        return new FireSpellBuilder()
                .playerId(playerId)
                .level(level + 1)
                .buyCost(buyCost)
                .energyCost(energyCost + 5)
                .damage(damage + (level % 2 == 0 ? 15 : 10))
                .cooldown(level % 2 == 0 ? Math.max(0.5, cooldown - 0.1) : cooldown)
                .build();
    }

    public static class FireSpellBuilder extends AbstractSpellBuilder<FireSpellBuilder> {
        public FireSpellBuilder() {
            buyCost = 150;
            energyCost = 20;
            damage = 50.0;
            cooldown = 1.5;
            maxLevel = 5;
        }

        @Override
        protected FireSpellBuilder self() {
            return this;
        }

        @Override
        public FireSpell build() {
            return new FireSpell(this);
        }
    }
}