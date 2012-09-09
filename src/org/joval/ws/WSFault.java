// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ws;

import javax.xml.bind.JAXBElement;

import com.microsoft.wsman.fault.WSManFaultType;
import org.w3c.soap.envelope.Detail;
import org.w3c.soap.envelope.Fault;

/**
 * A Web-Service (SOAP) Fault-derived exception class
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WSFault extends Exception {
    Fault fault;

    public WSFault(Fault fault) {
	super();
	this.fault = fault;
    }

    public Fault getFault() {
	return fault;
    }

    /**
     * Extract the (required) detail message from the fault.
     */
    @Override
    public String getMessage() {
	Detail detail = fault.getDetail();
	JAXBElement elt = (JAXBElement)detail.getAny().get(0);
	Object obj = elt.getValue();
	if (obj instanceof String) {
	    return (String)obj;
	} else if (obj instanceof WSManFaultType) {
	    WSManFaultType wsmft = (WSManFaultType)obj;
	    return (String)wsmft.getMessage().getContent().get(0);
	} else {
	    return obj.toString();
	}
    }
}
