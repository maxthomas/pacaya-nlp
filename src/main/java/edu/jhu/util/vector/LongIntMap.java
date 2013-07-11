package edu.jhu.util.vector;

import java.util.Iterator;

public interface LongIntMap extends Iterable<LongIntEntry> {

    public abstract void clear();

    // TODO: rename to containsKey.
    public abstract boolean contains(long idx);

    public abstract int get(long idx);

    public abstract int getWithDefault(long idx, int defaultVal);

    public abstract void remove(long idx);

    public abstract void put(long idx, int val);

    public abstract Iterator<LongIntEntry> iterator();

    public abstract int size();

    /**
     * Returns the indices.
     */
    public abstract long[] getIndices();

    /**
     * Returns the values.
     */
    public abstract int[] getValues();

}