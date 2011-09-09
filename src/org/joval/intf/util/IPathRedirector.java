// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util;

/**
 * Interface definition for a path redirector. A path redirector is a means of providing an alternate view of a tree or
 * tree-like resource.  It is used primarily for emulation of the 32-bit redirection of the registry and filesystem on
 * 64-bit Windows.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IPathRedirector {
    /**
     * Return the path to which path should be redirected.  Returns null if unchanged.
     */
    public String getRedirect(String path);
}
