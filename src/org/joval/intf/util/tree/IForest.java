// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util.tree;

import java.util.Collection;
import java.util.regex.Pattern;

import org.joval.intf.util.ILoggable;

/**
 * Representation of a node on a tree.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IForest extends ILoggable {
    /**
     * @returns the tree that was displaced, if any
     */
    public ITree addTree(ITree tree);

    public ITreeBuilder getTreeBuilder(String name);

    public ITree getTree(String name);

    /**
     * Get all the root ITrees in the forest.
     */
    public Collection<ITree> getTrees();

    /**
     * Get all the ITrees in the forest whose paths match the specified Pattern.
     */
    public Collection<String> search(Pattern p);
}
