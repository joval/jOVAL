// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import javax.xml.namespace.QName;

import org.xmlsoap.ws.addressing.EndpointReferenceType;
import org.xmlsoap.ws.transfer.AnyXmlType;
import org.dmtf.wsman.AttributableURI;
import org.dmtf.wsman.ObjectFactory;
import org.dmtf.wsman.SelectorSetType;

import org.joval.os.windows.remote.winrm.IMessage;
import org.joval.os.windows.remote.winrm.IOperation;
import org.joval.os.windows.remote.winrm.message.AnyXmlMessage;
import org.joval.os.windows.remote.winrm.message.OptionalXmlMessage;

public class GetOperation implements IOperation<AnyXmlMessage> {
    private static QName MUST_UNDERSTAND = new QName("http://www.w3.org/2003/05/soap-envelope", "mustUnderstand");
    private static ObjectFactory factory = new ObjectFactory();

    private OptionalXmlMessage input;
    private AnyXmlMessage output;
    private List<Object> headers;

    public GetOperation(OptionalXmlMessage input) {
	this.input = input;
	headers = new Vector<Object>();
    }

    public void addResourceURI(String str) {
	AttributableURI uri = factory.createAttributableURI();
	uri.setValue(str);
	uri.getOtherAttributes().put(MUST_UNDERSTAND, "true");
	headers.add(factory.createResourceURI(uri));
    }

    public void addSelectorSet(SelectorSetType selectors) {
	headers.add(factory.createSelectorSet(selectors));
    }

    // Implement IOperation

    public IMessage getInput() {
	return input;
    }

    public void setOutput(AnyXmlMessage output) {
	this.output = output;
    }

    public AnyXmlMessage getOutput() {
	return output;
    }

    public String getSOAPAction() {
	return "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get";
    }

    public List<Object> getHeaders() {
	return headers;
    }
}
