// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm;

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
    private static final String SOAP	= "http://www.w3.org/2003/05/soap-envelope";
    private static final String ADDR	= "http://schemas.xmlsoap.org/ws/2004/08/addressing";
    private static final String WSMAN	= "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd";
    private static final String MSFAULT	= "http://schemas.microsoft.com/wbem/wsman/1/wsmanfault";

    private Code code;
    private WSMCode subcode;
    private List<Reasontext> reasons;

    /**
     * SOAP fault code enumeration
     */
    public enum Code {
	ENCODING	(SOAP, "DataEncodingUnknown"),
	UNDERSTAND	(SOAP, "MustUnderstand"),
	RECEIVER	(SOAP, "Receiver"),
	SENDER		(SOAP, "Sender"),
	VERSION		(SOAP, "VersionMismatch");

	private String uri, name;

	private Code(String uri, String name) {
	    this.uri = uri;
	    this.name = name;
	}

	static Code getCode(QName base) throws IllegalArgumentException {
	    for (Code code : Code.values()) {
		if (code.uri.equals(base.getNamespaceURI()) && code.name.equals(base.getLocalPart())) {
		    return code;
		}
	    }
	    throw new IllegalArgumentException(base.toString());
	}
    }

    /**
     * WS-Management fault code enumeration
     */
    public enum WSMCode {
	ACCESS		(WSMAN, "AccessDenied"),
	EXISTS		(WSMAN, "AlreadyExists"),
	CONCURRENCY	(WSMAN, "Concurrency"),
	REFUSED		(WSMAN, "DeliveryRefused"),
	ENCLIMIT	(WSMAN, "EncodingLimit"),
	INTERNAL	(WSMAN, "InternalError"),
	DIALECT		(WSMAN, "FragmentDialectNotSupported"),
	BOOKMARK	(WSMAN, "InvalidBookmark"),
	OPTIONS		(WSMAN, "InvalidOptions"),
	PARAMS		(WSMAN, "InvalidParameter"),
	SELECTORS	(WSMAN, "InvalidSelectors"),
	ACK		(WSMAN, "NoAck"),
	QUOTA		(WSMAN, "QuotaLimit"),
	SCHEMA		(WSMAN, "SchemaValidationError"),
	TIMEOUT		(WSMAN, "TimedOut"),
	FEATURE		(WSMAN, "UnsupportedFeature"),
	HEADER_INVALID	(ADDR, "InvalidMessageInformationHeader"),
	HEADER_MISSING	(ADDR, "MessageInformationHeaderRequired"),
	UNREACHABLE	(ADDR, "DestinationUnreachable"),
	NOT_SUPPORTED	(ADDR, "ActionNotSupported"),
	UNAVAILABLE	(ADDR, "EndpointUnavailable");

	private String uri, name;

	private WSMCode(String uri, String name) {
	    this.uri = uri;
	    this.name = name;
	}

	static WSMCode getCode(QName base) throws IllegalArgumentException {
	    for (WSMCode code : WSMCode.values()) {
		if (code.uri.equals(base.getNamespaceURI()) && code.name.equals(base.getLocalPart())) {
		    return code;
		}
	    }
	    throw new IllegalArgumentException(base.toString());
	}
    }

    public WSMFault(Fault fault) {
	code = Code.getCode(fault.getCode().getValue());
	if (fault.getCode().isSetSubcode()) {
	    subcode = WSMCode.getCode(fault.getCode().getSubcode().getValue());
	}

	reasons = fault.getReason().getText();
    }

    public String getMessage() {
	StringBuffer sb = new StringBuffer(code.toString());
	sb.append("[").append(subcode).append("]: ");
	sb.append(reasons.get(0).getValue());
	return sb.toString();
    }
}
