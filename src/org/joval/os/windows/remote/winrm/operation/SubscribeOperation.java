// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import java.util.Collections;
import java.util.List;

import org.joval.os.windows.remote.winrm.IMessage;
import org.joval.os.windows.remote.winrm.IOperation;
import org.joval.os.windows.remote.winrm.message.SubscribeMessage;
import org.joval.os.windows.remote.winrm.message.SubscribeResponseMessage;

public class SubscribeOperation implements IOperation<SubscribeResponseMessage> {
    private SubscribeMessage input;
    private SubscribeResponseMessage output;

    public SubscribeOperation(SubscribeMessage input) {
	this.input = input;
    }

    // Implement IOperation

    public IMessage getInput() {
	return input;
    }

    public void setOutput(SubscribeResponseMessage output) {
	this.output = output;
    }

    public SubscribeResponseMessage getOutput() {
	return output;
    }

    public String getSOAPAction() {
	return "http://schemas.xmlsoap.org/ws/2004/08/eventing/Subscribe";
    }

    public List<Object> getHeaders() {
	@SuppressWarnings("unchecked")
	List<Object> empty = (List<Object>)Collections.EMPTY_LIST;
	return empty;
    }
}
