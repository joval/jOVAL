// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wsmv.operation;

import javax.xml.bind.JAXBElement;

import com.microsoft.wsman.shell.Send;
import com.microsoft.wsman.shell.SendResponse;

/**
 * Send operation implementation class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SendOperation extends BaseOperation<JAXBElement<Send>, SendResponse> {
    public SendOperation(Send input) {
	super("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Send", Factories.SHELL.createSend(input));
    }
}
