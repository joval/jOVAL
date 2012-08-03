// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wsmv.operation;

import javax.xml.bind.JAXBElement;

import com.microsoft.wsman.shell.Signal;
import com.microsoft.wsman.shell.SignalResponse;

/**
 * Signal operation implementation class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SignalOperation extends BaseOperation<JAXBElement<Signal>, SignalResponse> {
    public SignalOperation(Signal input) {
	super("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/Signal", Factories.SHELL.createSignal(input));
    }
}
