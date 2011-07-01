// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

/**
 * Interface definition for a path redirector. A path redirector should be employed with any IFilesystem implementation
 * that relies on opaque path redirection, for example, if implementing on Windows where 32-bit WOW redirection should be
 * emulated (where file paths under %SystemRoot%\System32 are redirected to %SystemRoot%\SysWOW64).
 *
 * Use of this interface is up to the writer of an IFilesystem implementation.  It is employed by the CachingFilesystem
 * base class and its subclass(es).
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IPathRedirector {
    /**
     * Return the path to which path should be redirected.  Return the original path if unchanged.
     */
    public String getRedirect(String path);
}
