// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ws;

import java.util.List;
import java.util.Locale;
import java.util.Vector;
import javax.xml.namespace.QName;

import org.w3c.soap.envelope.Fault;
import org.w3c.soap.envelope.Reasontext;

/**
 * A WS-Management Fault-derived exception class
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WSMFault extends Exception {
    public WSMFault(String message) {
	super(message);
    }
}
