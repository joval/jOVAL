// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import org.dmtf.wsman.Send;
import org.dmtf.wsman.SendResponse;

public class SendOperation implements IOperation<Send, SendResponse> {
    public SendOperation(Send input) {
	super("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Send", input);
    }
}
