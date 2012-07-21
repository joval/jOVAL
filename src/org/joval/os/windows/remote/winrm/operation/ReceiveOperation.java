// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import java.util.Collections;
import java.util.List;

import org.joval.os.windows.remote.winrm.IMessage;
import org.joval.os.windows.remote.winrm.IOperation;
import org.joval.os.windows.remote.winrm.message.ReceiveMessage;
import org.joval.os.windows.remote.winrm.message.ReceiveResponseMessage;

public class ReceiveOperation implements IOperation<ReceiveResponseMessage> {
    private ReceiveMessage input;
    private ReceiveResponseMessage output;

    public ReceiveOperation(ReceiveMessage input) {
	this.input = input;
    }

    // Implement IOperation

    public IMessage getInput() {
	return input;
    }

    public void setOutput(ReceiveResponseMessage output) {
	this.output = output;
    }

    public ReceiveResponseMessage getOutput() {
	return output;
    }

    public String getSOAPAction() {
	return "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Receive";
    }

    public List<Object> getHeaders() {
	@SuppressWarnings("unchecked")
	List<Object> empty = (List<Object>)Collections.EMPTY_LIST;
	return empty;
    }
}
