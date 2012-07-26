// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import org.xmlsoap.ws.transfer.AnyXmlType;
import org.xmlsoap.ws.transfer.AnyXmlOptionalType;

public class PutOperation extends BaseOperation<AnyXmlType, AnyXmlOptionalType> {
    public PutOperation(AnyXmlType input) {
	super("http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", input);
    }
}
