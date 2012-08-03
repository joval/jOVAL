// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wsmv.operation;

import javax.xml.bind.JAXBElement;

import com.microsoft.wsman.shell.Receive;
import com.microsoft.wsman.shell.ReceiveResponse;

/**
 * Receive operation implementation class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ReceiveOperation extends BaseOperation<JAXBElement<Receive>, ReceiveResponse> {
    public ReceiveOperation(Receive input) {
	super("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Receive", Factories.SHELL.createReceive(input));
    }
}
