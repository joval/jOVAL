// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

/**
 * Interface definition for a registry redirector. A registry redirector should be employed with any IRegistry implementation
 * that relies on opaque path redirection, for example, if implementing on Windows where 32-bit WOW redirection should be
 * emulated.  (In fact, it is specifically intended for that case).
 *
 * See <a href="http://msdn.microsoft.com/en-us/library/aa384253(v=vs.85).aspx">http://msdn.microsoft.com/en-us/library/aa384253(v=vs.85).aspx</a>
 *
 * Use of this interface is up to the writer of an IRegistry implementation.  It is employed by the BaseRegistry base class
 * and its subclass(es).
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IRegistryRedirector {
    /**
     * Indicated whether or not redirection is enabled.
     */
    public boolean isEnabled();

    /**
     * Return an original Key path from which the specified Key path could have been be redirected.
     */
    public String getOriginal(String keyPath);

    /**
     * Return the path to which the specified Key path should be redirected.  Return the original path if unchanged.
     */
    public String getRedirect(String keyPath);
}
