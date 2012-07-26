// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import org.dmtf.wsman.Receive;
import org.dmtf.wsman.ReceiveResponse;

public class ReceiveOperation extends BaseOperation<Receive, ReceiveResponse> {
    public ReceiveOperation(Receive input) {
	super("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Receive", input);
    }
}
