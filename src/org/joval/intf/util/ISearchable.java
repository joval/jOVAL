// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * The tree, without the node.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISearchable {
    int NONE		= 0;
    int FOLLOW_LINKS	= 1;

    /**
     * Recursively search this for elements matching the given pattern.
     *
     * @param flags application-specific flags.
     */
    Collection<String> search(Pattern p, int flags);
}
