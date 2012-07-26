// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.xmlsoap.ws.addressing.EndpointReferenceType;
import org.dmtf.wsman.AttributableURI;
import org.dmtf.wsman.ObjectFactory;

import org.joval.os.windows.remote.winrm.IOperation;
import org.joval.os.windows.remote.winrm.IPort;
import org.joval.os.windows.remote.winrm.WSMFault;

/**
 * Base class for all WS-Management operations.
 *
 * @author David A. Solin
 * @version %I%, %G%
 */
abstract class BaseOperation<I, O> implements IOperation<I, O> {
    static QName MUST_UNDERSTAND = new QName("http://www.w3.org/2003/05/soap-envelope", "mustUnderstand");
    static ObjectFactory FACTORY = new ObjectFactory();

    String action;
    List<Object> headers;
    I input;

    public BaseOperation(String action, I input) {
	this.action = action;
	this.input = input;
	headers = new Vector<Object>();
    }

    // Implement IOperation (sparsely)

    public void addResourceURI(String str) {
	AttributableURI uri = FACTORY.createAttributableURI();
	uri.setValue(str);
	uri.getOtherAttributes().put(MUST_UNDERSTAND, "true");
	headers.add(FACTORY.createResourceURI(uri));
    }

    public O dispatch(IPort port) throws IOException, JAXBException, WSMFault {
	@SuppressWarnings("unchecked")
        O result = (O)port.dispatch(action, headers, input);
	return result;
    }
}
