package com.vaultpack.data.components;

import lombok.Getter;

/**
 * Base implementation of DataComponent with common functionality.
 * Handles dirty tracking and provides default implementations.
 */
public abstract class BaseDataComponent implements DataComponent {

    @Getter
    private final String id;

    private boolean dirty = false;

    protected BaseDataComponent(String id) {
        this.id = id;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markClean() {
        this.dirty = false;
    }

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * Convenience method to mark dirty and return a value.
     * Useful for setter methods.
     *
     * @param value The value to return
     * @param <T>   The value type
     * @return The value
     */
    protected <T> T dirty(T value) {
        markDirty();
        return value;
    }
}
