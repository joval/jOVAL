// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import oval.schemas.common.MessageType;

/**
 * The interface for the argument supplied to an adapter getItems request, which provides a facility for adding messages
 * related to the request.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IRequestContext {
    /**
     * Associate a message (like an error) with the request object.
     */
    public void addMessage(MessageType msg);
}
