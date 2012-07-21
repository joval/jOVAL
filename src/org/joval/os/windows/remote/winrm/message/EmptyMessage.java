// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.message;

import org.joval.os.windows.remote.winrm.IMessage;

public class EmptyMessage implements IMessage <Object> {
    public EmptyMessage() {
    }

    // Implement IMessage

    public Object getBody() {
	return null;
    }
}
