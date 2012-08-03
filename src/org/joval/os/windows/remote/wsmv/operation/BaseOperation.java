// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wsmv.operation;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;

import org.xmlsoap.ws.addressing.EndpointReferenceType;
import org.dmtf.wsman.AttributableDuration;
import org.dmtf.wsman.AttributableURI;
import org.dmtf.wsman.OptionSet;
import org.dmtf.wsman.SelectorSetType;

import org.joval.intf.windows.wsmv.IWSMVConstants;
import org.joval.intf.ws.IOperation;
import org.joval.intf.ws.IPort;
import org.joval.ws.WSFault;

/**
 * Base class for all WSMV operations.
 *
 * @author David A. Solin
 * @version %I%, %G%
 */
abstract class BaseOperation<I, O> implements IOperation<I, O>, IWSMVConstants {
    String action;
    List<Object> headers;
    AttributableDuration duration = null;
    I input;

    public BaseOperation(String action, I input) {
	this.action = action;
	this.input = input;
	headers = new Vector<Object>();
    }


    public void addOptionSet(OptionSet options) {
	headers.add(options);
    }

    public void addSelectorSet(SelectorSetType selectors) {
	headers.add(Factories.WSMAN.createSelectorSet(selectors));
    }

    // Implement IOperation

    public void setTimeout(long millis) {
	if (duration == null) {
	    duration = Factories.WSMAN.createAttributableDuration();
	}
	duration.setValue(Factories.XMLDT.newDuration(millis));
    }

    public void addResourceURI(String str) {
	AttributableURI uri = Factories.WSMAN.createAttributableURI();
	uri.setValue(str);
	uri.getOtherAttributes().put(MUST_UNDERSTAND, "true");
	headers.add(Factories.WSMAN.createResourceURI(uri));
    }

    public O dispatch(IPort port) throws IOException, JAXBException, WSFault {
	@SuppressWarnings("unchecked")
        O result = (O)dispatch0(port);
	return result;
    }

    // Internal

    /**
     * The internal implementation of dispatch, which subclasses that override the typed public dispatch method can use.
     */
    final Object dispatch0(IPort port) throws IOException, JAXBException, WSFault {
	List<Object> dispatchHeaders = new Vector<Object>();
	dispatchHeaders.addAll(headers);
	if (duration != null) {
	    dispatchHeaders.add(Factories.WSMAN.createOperationTimeout(duration));
	}
	return port.dispatch(action, dispatchHeaders, input);
    }
}
