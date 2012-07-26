// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import org.dmtf.wsman.Signal;
import org.dmtf.wsman.SignalResponse;

public class SignalOperation extends BaseOperation<Signal, SignalResponse> {
    public SignalOperation(SignalMessage input) {
	super("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Signal", input);
    }
}
