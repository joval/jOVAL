// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.apple.system;

import com.dd.plist.NSObject;

import org.joval.intf.system.IBaseSession;

/**
 * A representation of an iOS (offline/plist) session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IiOSSession extends IBaseSession {
    /**
     * Retrieves plist data for the device.
     */
    NSObject getConfigurationProfile();
}
