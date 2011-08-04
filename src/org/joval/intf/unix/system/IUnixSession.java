// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.unix.system;

import org.joval.intf.system.ISession;
import org.joval.unix.UnixFlavor;

/**
 * A representation of a Unix command-line session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IUnixSession extends ISession {
    UnixFlavor getFlavor();
}
