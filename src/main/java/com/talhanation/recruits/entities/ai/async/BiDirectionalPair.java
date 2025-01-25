package com.talhanation.recruits.entities.ai.async;

public record BiDirectionalPair<L, R>(L left, R right) {

    @Override
    public int hashCode() {
        return left.hashCode() ^ right.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BiDirectionalPair<?, ?> pair) {
            return this.hashCode() == pair.hashCode();
        }

        return false;
    }
}
