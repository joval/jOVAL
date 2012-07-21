// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import javax.xml.namespace.QName;

import org.dmtf.wsman.AttributableURI;
import org.dmtf.wsman.ObjectFactory;

import org.joval.os.windows.remote.winrm.IMessage;
import org.joval.os.windows.remote.winrm.IOperation;
import org.joval.os.windows.remote.winrm.message.EnumerateMessage;
import org.joval.os.windows.remote.winrm.message.EnumerateResponseMessage;

public class EnumerateOperation implements IOperation<EnumerateResponseMessage> {
    private static QName MUST_UNDERSTAND = new QName("http://www.w3.org/2003/05/soap-envelope", "mustUnderstand");
    private static ObjectFactory factory = new ObjectFactory();

    private EnumerateMessage input;
    private EnumerateResponseMessage output;
    private List<Object> headers;

    public EnumerateOperation(EnumerateMessage input) {
	this.input = input;
	headers = new Vector<Object>();
    }

    public void addResourceURI(String str) {
	AttributableURI uri = factory.createAttributableURI();
	uri.setValue(str);
	uri.getOtherAttributes().put(MUST_UNDERSTAND, "true");
	headers.add(factory.createResourceURI(uri));
    }

    // Implement IOperation

    public IMessage getInput() {
	return input;
    }

    public void setOutput(EnumerateResponseMessage output) {
	this.output = output;
    }

    public EnumerateResponseMessage getOutput() {
	return output;
    }

    public String getSOAPAction() {
	return "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate";
    }

    public List<Object> getHeaders() {
	return headers;
    }
}
