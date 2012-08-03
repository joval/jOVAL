// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wsmv.operation;

import org.xmlsoap.ws.enumeration.Enumerate;
import org.xmlsoap.ws.enumeration.EnumerateResponse;

/**
 * Enumerate operation implementation class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class EnumerateOperation extends BaseOperation<Enumerate, EnumerateResponse> {
    public EnumerateOperation(Enumerate input) {
	super("http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate", input);
    }
}
