// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.message;

import org.dmtf.wsman.Send;

import org.joval.os.windows.remote.winrm.IMessage;

public class SendMessage implements IMessage<Send> {
    private Send body;

    public SendMessage(Send body) {
	this.body = body;
    }

    // Implement IMessage

    public Send getBody() {
	return body;
    }
}
