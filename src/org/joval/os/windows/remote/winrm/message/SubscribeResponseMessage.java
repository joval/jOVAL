// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.message;

import org.xmlsoap.ws.eventing.SubscribeResponse;

package org.joval.os.windows.remote.winrm.IMessage;

public class SubscribeResponseMessage implements IMessage<SubscribeResponse> {
    private SubscribeResponse body;

    public SubscribeResponseMessage(SubscribeResponse body) {
	this.body = body;
    }

    // Implement IMessage

    public SubscribeResponse getBody() {
	return body;
    }
}
