// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.message;

import org.xmlsoap.ws.enumeration.ReleaseResponse;

import org.joval.os.windows.remote.winrm.IMessage;

public class ReleaseResponseMessage implements IMessage<ReleaseResponse> {
    private ReleaseResponse other;

    public ReleaseResponseMessage(ReleaseResponse other) {
	this.other = other;
    }

    // Implement IMessage

    public ReleaseResponse getBody() {
	return other;
    }
}
