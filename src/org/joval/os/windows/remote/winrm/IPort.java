// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm;

import java.io.IOException;
import java.net.Proxy;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.bind.JAXBException;

/**
 * Interface defining a generic SOAP port, which is an address to which messages can be dispatched.
 *
 * @author David A. Solin
 * @version %I%, %G%
 */
public interface IPort {
    /**
     * URI for anonymous role addressing
     */
    String REPLY_TO = "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous";

    /**
     * QName for "mustUnderstand" attributes
     */
    QName MUST_UNDERSTAND = new QName("http://www.w3.org/2003/05/soap-envelope", "mustUnderstand");

    /**
     * Dispatch a SOAP action.
     *
     * @param action the SOAP action URI
     * @param headers any headers that should be added to the SOAP envelope headers
     * @param body contents for the SOAP envelope body.
     */
    Object dispatch(String action, List<Object> headers, Object body) throws IOException, JAXBException, WSMFault;
}
