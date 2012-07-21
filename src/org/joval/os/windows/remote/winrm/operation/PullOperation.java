// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import java.util.Collections;
import java.util.List;

import org.joval.os.windows.remote.winrm.IMessage;
import org.joval.os.windows.remote.winrm.IOperation;
import org.joval.os.windows.remote.winrm.message.PullMessage;
import org.joval.os.windows.remote.winrm.message.PullResponseMessage;

public class PullOperation implements IOperation<PullResponseMessage> {
    private PullMessage input;
    private PullResponseMessage output;

    public PullOperation(PullMessage input) {
	this.input = input;
    }

    // Implement IOperation

    public IMessage getInput() {
	return input;
    }

    public void setOutput(PullResponseMessage output) {
	this.output = output;
    }

    public PullResponseMessage getOutput() {
	return output;
    }

    public getSOAPAction() {
	return "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull";
    }

    public List<Object> getHeaders() {
	@SuppressWarnings("unchecked")
	List<Object> empty = (List<Object>)Collections.EMPTY_LIST;
	return empty;
    }
}
