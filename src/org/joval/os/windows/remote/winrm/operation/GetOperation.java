// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import org.xmlsoap.ws.transfer.AnyXmlOptionalType;
import org.xmlsoap.ws.transfer.AnyXmlType;
import org.dmtf.wsman.SelectorSetType;

public class GetOperation extends BaseOperation<AnyXmlOptionalType, AnyXmlType> {
    public GetOperation(AnyXmlOptionalType input) {
	super("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", input);
    }

    public void addSelectorSet(SelectorSetType selectors) {
	headers.add(FACTORY.createSelectorSet(selectors));
    }
}
