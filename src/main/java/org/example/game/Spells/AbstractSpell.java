package org.example.game.Spells;

public abstract class AbstractSpell implements Spell {
    protected final String playerId;
    protected final int level;
    protected final int buyCost;
    protected final int energyCost;
    protected final double damage;
    protected final double cooldown;
    protected final int maxLevel;

    // Protected constructor for subclasses
    protected AbstractSpell(AbstractSpellBuilder<?> builder) {
        this.playerId = builder.playerId;
        this.level = builder.level;
        this.buyCost = builder.buyCost;
        this.energyCost = builder.energyCost;
        this.damage = builder.damage;
        this.cooldown = builder.cooldown;
        this.maxLevel = builder.maxLevel;
    }

    @Override
    public void cast() {
        validateCast();
        performCast();
    }

    // Validate before casting
    protected void validateCast() {
        if (level <= 0) {
            throw new IllegalStateException("Spell is not initialized");
        }
    }

    // Abstract method to be implemented by specific spell types
    protected abstract void performCast();

    @Override
    public abstract AbstractSpell upgrade();

    @Override
    public int getBuyCost() {
        return buyCost;
    }

    @Override
    public int getUpgradeCost() {
        return calculateUpgradeCost();
    }

    @Override
    public int getEnergyCost() {
        return energyCost;
    }

    @Override
    public double getDamage() {
        return damage;
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    public String getPlayerId() {
        return playerId;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    // Protected method to calculate upgrade cost, can be overridden
    protected int calculateUpgradeCost() {
        return buyCost / 2 + (level * 25);
    }

    // Builder pattern for flexible spell creation
    public static abstract class AbstractSpellBuilder<T extends AbstractSpellBuilder<T>> {
        protected String playerId;
        protected int level = 1;
        protected int buyCost = 100;
        protected int energyCost = 10;
        protected double damage = 10.0;
        protected double cooldown = 1.0;
        protected int maxLevel = 10;

        public T playerId(String playerId) {
            this.playerId = playerId;
            return self();
        }

        public T level(int level) {
            this.level = level;
            return self();
        }

        public T buyCost(int buyCost) {
            this.buyCost = buyCost;
            return self();
        }

        public T energyCost(int energyCost) {
            this.energyCost = energyCost;
            return self();
        }

        public T damage(double damage) {
            this.damage = damage;
            return self();
        }

        public T cooldown(double cooldown) {
            this.cooldown = cooldown;
            return self();
        }

        public T maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return self();
        }

        // Abstract method to be implemented by subclass builders
        protected abstract T self();

        // Abstract method to create the specific spell
        public abstract AbstractSpell build();
    }
}
