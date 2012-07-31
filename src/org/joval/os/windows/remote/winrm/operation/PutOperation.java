// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import java.io.IOException;
import javax.xml.bind.JAXBException;

import org.xmlsoap.ws.transfer.AnyXmlType;
import org.xmlsoap.ws.transfer.AnyXmlOptionalType;
import org.xmlsoap.ws.transfer.ObjectFactory;

import org.joval.os.windows.remote.winrm.IPort;
import org.joval.os.windows.remote.winrm.WSMFault;

public class PutOperation extends BaseOperation<AnyXmlType, AnyXmlOptionalType> {
    public PutOperation(AnyXmlType input) {
	super("http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", input);
    }

    @Override
    public AnyXmlOptionalType dispatch(IPort port) throws IOException, JAXBException, WSMFault {
	Object obj = dispatch0(port);
	if (obj instanceof AnyXmlOptionalType) {
	    return (AnyXmlOptionalType)obj;
	} else {
	    AnyXmlOptionalType any = Factories.TRANSFER.createAnyXmlOptionalType();
	    if (obj != null) {
		any.setAny(obj);
	    }
	    return any;
	}
    }
}
