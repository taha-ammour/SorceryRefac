package org.example.engine.collision;

public class CollisionPair {
    private final Class<? extends Collider> typeA;
    private final Class<? extends Collider> typeB;

    public CollisionPair(Class<? extends Collider> typeA, Class<? extends Collider> typeB) {
        this.typeA = typeA;
        this.typeB = typeB;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        CollisionPair pair = (CollisionPair) obj;
        return typeA.equals(pair.typeA) && typeB.equals(pair.typeB);
    }

    @Override
    public int hashCode() {
        return 31 * typeA.hashCode() + typeB.hashCode();
    }
}