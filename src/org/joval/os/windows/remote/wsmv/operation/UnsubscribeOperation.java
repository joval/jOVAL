// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wsmv.operation;

import org.xmlsoap.ws.eventing.Unsubscribe;

/**
 * Unsubscribe operation implementation class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnsubscribeOperation extends BaseOperation<Unsubscribe, Object> {
    public UnsubscribeOperation(Unsubscribe input) {
	super("http://schemas.xmlsoap.org/ws/2004/08/eventing/Unsubscribe", input);
    }
}
