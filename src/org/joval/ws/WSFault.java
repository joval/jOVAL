// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ws;

import org.w3c.soap.envelope.Fault;

/**
 * A Web-Service (SOAP) Fault-derived exception class
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WSFault extends Exception {
    Fault fault;

    public WSFault(Fault fault, String message) {
	super(message);
	this.fault = fault;
    }

    public Fault getFault() {
	return fault;
    }
}
