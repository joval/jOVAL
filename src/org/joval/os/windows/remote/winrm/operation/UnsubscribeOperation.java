// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import org.xmlsoap.ws.eventing.Unsubscribe;
import org.xmlsoap.ws.eventing.UnsubscribeResponse;

public class UnsubscribeOperation extends BaseOperation<Unssubscribe, UnsubscribeResponse> {
    public UnsubscribeOperation(Unsubscribe input) {
	super("http://schemas.xmlsoap.org/ws/2004/08/eventing/Unsubscribe", input);
    }
}
