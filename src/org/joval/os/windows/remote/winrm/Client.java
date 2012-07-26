// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Proxy;
import java.net.URL;
import java.util.Properties;
import javax.security.auth.login.LoginException;
import javax.xml.bind.JAXBException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;

import org.dmtf.wsman.SelectorSetType;
import org.dmtf.wsman.SelectorType;
import org.xmlsoap.ws.enumeration.Enumerate;
import org.xmlsoap.ws.enumeration.EnumerateResponse;
import org.xmlsoap.ws.transfer.AnyXmlOptionalType;
import org.xmlsoap.ws.transfer.AnyXmlType;

import org.joval.os.windows.remote.winrm.operation.EnumerateOperation;
import org.joval.os.windows.remote.winrm.operation.GetOperation;
import org.joval.os.windows.remote.winrm.operation.PutOperation;

/**
 * A WinRM client.  To use it, you must first do this on the target machine:
 *
 *   winrm set winrm/config/service @{AllowUnencrypted="true"}
 *   winrm set winrm/config/service/auth @{Basic="true"}
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Client {
    public static final String URL_PREFIX = "wsman";

    public static final int HTTP_PORT	= 5985;
    public static final int HTTPS_PORT	= 5986;

    public static void main(String[] argv) {
	String host = argv[0];
	String user = argv[1];
	String pass = argv[2];

	StringBuffer targetSpec = new StringBuffer("http://");
	targetSpec.append(host);
	if (host.indexOf(":") == -1) {
	    targetSpec.append(":");
	    targetSpec.append(Integer.toString(HTTP_PORT));
	}
	targetSpec.append("/");
	targetSpec.append(URL_PREFIX);
	String url = targetSpec.toString();

	try {
	    IPort port = new WSMPort(url, user, pass);

	    AnyXmlOptionalType arg = new AnyXmlOptionalType();
	    String processor = "http://schemas.microsoft.com/wbem/wsman/1/wmi/root/cimv2/Win32_Processor";
	    SelectorSetType set = WSMPort.Factories.WSMAN.createSelectorSetType();
	    SelectorType sel = WSMPort.Factories.WSMAN.createSelectorType();
	    sel.setName("DeviceID");
	    sel.getContent().add("CPU0");
	    set.getSelector().add(sel);
	    GetOperation getOperation = new GetOperation(arg);
	    getOperation.addResourceURI(processor);
	    getOperation.addSelectorSet(set);
	    DOMSource ds = new DOMSource((Element)getOperation.dispatch(port));
	    StreamResult sr = new StreamResult(new OutputStreamWriter(System.out, "utf-8"));
	    TransformerFactory tf = TransformerFactory.newInstance();
	    tf.setAttribute("indent-number", 4);
	    Transformer trans = tf.newTransformer();
	    trans.setOutputProperty(OutputKeys.INDENT,"yes");
	    trans.transform(ds, sr);

	    EnumerateOperation enumerateOperation = new EnumerateOperation(new Enumerate());
	    enumerateOperation.addResourceURI(processor);
	    EnumerateResponse er = enumerateOperation.dispatch(port);
	    for (Object elt : er.getEnumerationContext().getContent()) {
		System.out.println(elt.getClass().getName() + ": " + elt.toString());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

// unused

    private IPort port;

    public Client(IPort port) {
	this.port = port;
    }

    /**
     * Since the Get operation is allowed to return anything, the object return type is dependent on the resource URI.
     * If the returned XML is known to the JAXB context, an object of the corresponding type will be returned. Otherwise,
     * a DOM Element instance will be returned.
     */
    public AnyXmlType get(AnyXmlOptionalType opt, String uri, SelectorSetType sel)
		throws IOException, JAXBException, WSMFault {

	GetOperation op = new GetOperation(opt);
	if (uri != null) {
	    op.addResourceURI(uri);
	}
	if (sel != null) {
	    op.addSelectorSet(sel);
	}
	return op.dispatch(port);
    }

    public AnyXmlOptionalType put(AnyXmlType any, String uri) throws IOException, JAXBException, WSMFault {
	PutOperation op = new PutOperation(any);
	if (uri != null) {
	    op.addResourceURI(uri);
	}
	return op.dispatch(port);
    }

    public EnumerateResponse enumerate(Enumerate e, String uri) throws IOException, JAXBException, WSMFault {
	EnumerateOperation op = new EnumerateOperation(e);
	if (uri != null) {
	    op.addResourceURI(uri);
	}
	return op.dispatch(port);
    }
}
