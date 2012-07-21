// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.message;

import org.dmtf.wsman.SendResponse;

package org.joval.os.windows.remote.winrm.IMessage;

public class SendResponseMessage implements IMessage<SendResponse> {
    private SendResponse body;

    public SendResponseMessage(SendResponse body) {
	this.body = body;
    }

    // Implement IMessage

    public SendResponse getBody() {
	return body;
    }
}
