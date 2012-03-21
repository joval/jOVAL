// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.oval;

import java.io.File;

/**
 * Interface defining an OVAL Definition Filter.  The filter lets the engine know which tests it should evaluate, and which
 * it should skip.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IDefinitionFilter {
    /**
     * Returns true to indicate that the definition with the corresponding ID should be evaluated.
     */
    public boolean accept(String id);

    /**
     * Add a definition to the filter.
     */
    public void addDefinition(String id);

    public int size();

    public void writeXML(File f);
}
