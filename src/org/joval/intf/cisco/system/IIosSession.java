// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.cisco.system;

import org.joval.intf.system.IBaseSession;

/**
 * A representation of a Unix command-line session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IIosSession extends IBaseSession {
    ITechSupport getTechSupport();
}
