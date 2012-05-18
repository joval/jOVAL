// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.net;

import org.w3c.dom.Document;

import org.joval.intf.system.IBaseSession;

/**
 * An interface for a session that can perform NETCONF operations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface INetconf extends IBaseSession {
    /**
     * Get an XML Document containing an unfiltered get-config reply.
     */
    Document getConfig() throws Exception;
}
