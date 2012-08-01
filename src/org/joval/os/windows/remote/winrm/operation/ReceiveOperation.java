// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import com.microsoft.wsman.shell.Receive;
import com.microsoft.wsman.shell.ReceiveResponse;

public class ReceiveOperation extends BaseOperation<Receive, ReceiveResponse> {
    public ReceiveOperation(Receive input) {
	super("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Receive", input);
    }
}
