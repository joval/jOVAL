// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.system;

import java.util.Iterator;

/**
 * A representation of a system environment.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IEnvironment {
    /**
     * Get an environment variable by name.
     */
    public String getenv(String var);

    /**
     * If s contains references to variables defined in this environment, then the returned string replaces them with their
     * values.
     */
    public String expand(String s);

    /**
     * Returns an Iterator over the names of the variables defined in this environment.
     */
    public Iterator<String> iterateVariables();
}

