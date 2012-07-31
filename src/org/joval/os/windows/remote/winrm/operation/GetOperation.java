// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import java.io.IOException;
import javax.xml.bind.JAXBException;

import org.xmlsoap.ws.transfer.AnyXmlOptionalType;
import org.xmlsoap.ws.transfer.AnyXmlType;
import org.xmlsoap.ws.transfer.ObjectFactory;
import org.dmtf.wsman.SelectorSetType;

import org.joval.os.windows.remote.winrm.IPort;
import org.joval.os.windows.remote.winrm.WSMFault;

public class GetOperation extends BaseOperation<AnyXmlOptionalType, AnyXmlType> {
    public GetOperation(AnyXmlOptionalType input) {
	super("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", input);
    }

    public void addSelectorSet(SelectorSetType selectors) {
	headers.add(Factories.WSMAN.createSelectorSet(selectors));
    }

    @Override
    public AnyXmlType dispatch(IPort port) throws IOException, JAXBException, WSMFault {
	Object obj = dispatch0(port);
	if (obj instanceof AnyXmlType) {
	    return (AnyXmlType)obj;
	} else {
	    AnyXmlType any = Factories.TRANSFER.createAnyXmlType();
	    any.setAny(obj);
	    return any;
	}
    }
}
