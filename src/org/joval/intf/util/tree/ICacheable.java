package org.joval.intf.util.tree;

public interface ICacheable {
    boolean exists();
    boolean isContainer();
    boolean isLink();

    String getLinkPath() throws IllegalStateException;

    /**
     * Inform the entry of its canonical path within the cache.
     */
    void setCachePath(String path);

    boolean isAccessible();
}
