package net.stacksmashing.sechat.db;

import android.support.annotation.Nullable;

import java.io.Serializable;

abstract class Entity implements Cloneable, Serializable {
    @Nullable
    private Entity parentEntity = null;

    @Nullable
    Entity getParentEntity() {
        return parentEntity;
    }

    void setParentEntity(@Nullable Entity entity) {
        this.parentEntity = entity;
    }

    @Override
    public Entity clone() {
        try {
            return (Entity) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    <T extends Entity> T cloneWithParent() {
        @SuppressWarnings("unchecked")
        T result = (T) clone();
        result.setParentEntity(this);
        return result;
    }
}
