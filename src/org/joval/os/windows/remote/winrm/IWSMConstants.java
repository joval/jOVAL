// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;

/**
 * WS-Management constants.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IWSMConstants {
    String URL_PREFIX = "wsman";

    String SHELL_URI = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/cmd";

    int HTTP_PORT   = 5985;
    int HTTPS_PORT  = 5986;

    /**
     * URI for anonymous role addressing
     */
    String REPLY_TO = "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous";

    /**
     * QName for "mustUnderstand" attributes
     */
    QName MUST_UNDERSTAND = new QName("http://www.w3.org/2003/05/soap-envelope", "mustUnderstand");

    /**
     * QName for xsi:nil
     */
    QName NIL = new QName("http://www.w3.org/2001/XMLSchema-instance", "nil");

    String XMLNS = "http://www.w3.org/2001/XMLSchema";

    /**
     * JAXB ObjectFactories
     */
    class Factories {
	public static final org.dmtf.wsman.ObjectFactory		WSMAN;
	public static final org.xmlsoap.ws.addressing.ObjectFactory	ADDRESS;
	public static final org.xmlsoap.ws.enumeration.ObjectFactory	ENUMERATION;
	public static final org.xmlsoap.ws.eventing.ObjectFactory	EVENTING;
	public static final org.xmlsoap.ws.transfer.ObjectFactory	TRANSFER;
	public static final org.w3c.soap.envelope.ObjectFactory		SOAP;
	public static final org.w3c.ws.addressing.ObjectFactory		WSADDRESS;
	public static final com.microsoft.wsman.config.ObjectFactory	WSMC;
	public static final com.microsoft.wsman.shell.ObjectFactory	SHELL;

	public static final DatatypeFactory XMLDT;

	static {
	    WSMAN	= new org.dmtf.wsman.ObjectFactory();
	    ADDRESS	= new org.xmlsoap.ws.addressing.ObjectFactory();
	    ENUMERATION	= new org.xmlsoap.ws.enumeration.ObjectFactory();
	    EVENTING	= new org.xmlsoap.ws.eventing.ObjectFactory();
	    TRANSFER	= new org.xmlsoap.ws.transfer.ObjectFactory();
	    SOAP	= new org.w3c.soap.envelope.ObjectFactory();
	    WSADDRESS	= new org.w3c.ws.addressing.ObjectFactory();
	    WSMC	= new com.microsoft.wsman.config.ObjectFactory();
	    SHELL	= new com.microsoft.wsman.shell.ObjectFactory();

	    try {
	        XMLDT = DatatypeFactory.newInstance();
	    } catch (DatatypeConfigurationException e) {
	        throw new RuntimeException(e);
	    }
	}
    }
}
