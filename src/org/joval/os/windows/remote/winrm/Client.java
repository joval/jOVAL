// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.math.BigInteger;
import java.security.PrivilegedActionException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.security.auth.login.LoginException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import org.ietf.jgss.GSSException;

import jcifs.Config;
import jcifs.http.NtlmHttpURLConnection;
import net.sourceforge.spnego.SpnegoHttpURLConnection;

import com.microsoft.wsman.config.ClientAuthType;
import org.dmtf.wsman.MaxEnvelopeSizeType;
import org.dmtf.wsman.SelectorSetType;
import org.dmtf.wsman.SelectorType;
import org.xmlsoap.ws.addressing.AttributedURI;
import org.xmlsoap.ws.addressing.EndpointReferenceType;
import org.xmlsoap.ws.enumeration.Enumerate;
import org.xmlsoap.ws.enumeration.EnumerateResponse;
import org.xmlsoap.ws.transfer.AnyXmlOptionalType;
import org.xmlsoap.ws.transfer.AnyXmlType;
import org.w3c.soap.envelope.Body;
import org.w3c.soap.envelope.Envelope;
import org.w3c.soap.envelope.Header;

import org.joval.os.windows.remote.winrm.IMessage;
import org.joval.os.windows.remote.winrm.IOperation;
import org.joval.os.windows.remote.winrm.message.EnumerateMessage;
import org.joval.os.windows.remote.winrm.message.OptionalXmlMessage;
import org.joval.os.windows.remote.winrm.operation.EnumerateOperation;
import org.joval.os.windows.remote.winrm.operation.GetOperation;
import org.joval.util.Base64;

/**
 * A WinRM client.  To use it, you must first do this on the target machine:
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
	    Client client = new Client(url, user, pass);

	    AnyXmlOptionalType arg = new AnyXmlOptionalType();
	    
	    String process = "http://schemas.microsoft.com/wbem/wsman/1/wmi/root/cimv2/Win32_Process";
	    SelectorSetType set = client.wsmanFactory.createSelectorSetType();
	    SelectorType sel = client.wsmanFactory.createSelectorType();
	    sel.setName("CommandLine");
	    sel.getContent().add("notepad.exe");
	    set.getSelector().add(sel);
	    Object obj = client.get(arg, process, set);
	    client.marshaller.marshal(obj, System.out);

	    EnumerateResponse er = client.enumerate(new Enumerate(), process);
	    for (Object elt : er.getEnumerationContext().getContent()) {
		System.out.println(elt.toString());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private static Properties schemaProps = new Properties();
    static {
	ClassLoader cl = Thread.currentThread().getContextClassLoader();
	InputStream rsc = cl.getResourceAsStream("ws-man.properties");
	if (rsc != null) {
	    try {
		schemaProps.load(rsc);
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    /**
     * Enumeration of supported authentication schemes.
     */
    public enum AuthScheme {
	NONE, BASIC, NTLM, KERBEROS;

	static AuthScheme get(String wwwAuth, AuthScheme currentScheme) {
	    if (wwwAuth != null) {
		wwwAuth = wwwAuth.toLowerCase();
		if (wwwAuth.startsWith("negotiate")) {
		    switch(currentScheme) {
		      case NONE:
			return BASIC;
		      case BASIC:
			return NTLM;
		      case NTLM:
			return KERBEROS;
		    }
		} else {
		    return BASIC;
		}
	    }
	    return NONE;
	}
    }

    private AuthScheme scheme;
    private String url;
    private Proxy proxy;
    private String user, pass;
    private org.dmtf.wsman.ObjectFactory wsmanFactory;
    private org.xmlsoap.ws.addressing.ObjectFactory addressFactory;
    private org.w3c.soap.envelope.ObjectFactory soapFactory;
    private com.microsoft.wsman.config.ObjectFactory wsmcFactory;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    /**
     * MS-WSMV client.
     */
    public Client(String url, String user, String pass) throws JAXBException {
	this.url = url;
	this.user = user;
	this.pass = pass;
	proxy = null;
	wsmanFactory = new org.dmtf.wsman.ObjectFactory();
	addressFactory = new org.xmlsoap.ws.addressing.ObjectFactory();
	soapFactory = new org.w3c.soap.envelope.ObjectFactory();
	wsmcFactory = new com.microsoft.wsman.config.ObjectFactory();
	JAXBContext ctx = JAXBContext.newInstance(schemaProps.getProperty("ws-man.packages"));
	marshaller = ctx.createMarshaller();
	marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
	unmarshaller = ctx.createUnmarshaller();
	setAuthScheme(AuthScheme.NTLM);
    }

    public void setProxy(Proxy proxy) {
	this.proxy = proxy;
    }

    public void setAuthScheme(AuthScheme scheme) {
	System.out.println("Authentication scheme set to " + scheme);
	this.scheme = scheme;
    }

    // Implement MS WS-Management operations

    public Object get(AnyXmlOptionalType opt, String res, SelectorSetType sel) throws IOException, JAXBException {
	GetOperation op = new GetOperation(new OptionalXmlMessage(opt));
	if (res != null) {
	    op.addResourceURI(res);
	}
	if (sel != null) {
	    op.addSelectorSet(sel);
	}
	return dispatch(op);
    }

    public EnumerateResponse enumerate(Enumerate e, String res) throws IOException, JAXBException {
	EnumerateOperation op = new EnumerateOperation(new EnumerateMessage(e));
	if (res != null) {
	    op.addResourceURI(res);
	}
	return (EnumerateResponse)dispatch(op);
    }

    // Private

    private static final String REPLYTO = "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous";
    private static QName MUST_UNDERSTAND = new QName("http://www.w3.org/2003/05/soap-envelope", "mustUnderstand");

    /**
     * Serialize to SOAP, and de-serialize the reply.
     */
    private Object dispatch(IOperation operation) throws IOException, JAXBException {
	Header header = soapFactory.createHeader();

	AttributedURI to = addressFactory.createAttributedURI();
	to.setValue(url);
	to.getOtherAttributes().put(MUST_UNDERSTAND, "true");
	header.getAny().add(addressFactory.createTo(to));

	EndpointReferenceType endpointRef = addressFactory.createEndpointReferenceType();
	AttributedURI address = addressFactory.createAttributedURI();
	address.setValue(REPLYTO);
	address.getOtherAttributes().put(MUST_UNDERSTAND, "true");
	endpointRef.setAddress(address);
	header.getAny().add(addressFactory.createReplyTo(endpointRef));

	AttributedURI action = addressFactory.createAttributedURI();
	action.setValue(operation.getSOAPAction());
	action.getOtherAttributes().put(MUST_UNDERSTAND, "true");
	header.getAny().add(addressFactory.createAction(action));

	AttributedURI messageId = addressFactory.createAttributedURI();
	messageId.setValue("uuid:" + UUID.randomUUID().toString());
	header.getAny().add(addressFactory.createMessageID(messageId));

	for (Object obj : operation.getHeaders()) {
	    header.getAny().add(obj);
	}

	MaxEnvelopeSizeType size = wsmanFactory.createMaxEnvelopeSizeType();
	size.setValue(BigInteger.valueOf(51200));
	size.getOtherAttributes().put(MUST_UNDERSTAND, "true");
	header.getAny().add(wsmanFactory.createMaxEnvelopeSize(size));

	ClientAuthType authType = wsmcFactory.createClientAuthType();
	authType.setBasic(true);

	Body body = soapFactory.createBody();
	if (operation.getInput().getBody() != null) {
	    body.getAny().add(operation.getInput().getBody());
	}

	Envelope request = soapFactory.createEnvelope();
	request.setHeader(header);
	request.setBody(body);

int attempt = 0;
	URL u = new URL(url);
	Object result = null;
	HttpURLConnection conn = null;
	SpnegoHttpURLConnection spnego = null;
	boolean retry = false;
	do {
	    try {
		if (conn!=null) {
		    conn.disconnect();
		}

		switch(scheme) {
		  case NONE:
		    if (proxy == null) {
			conn = (HttpURLConnection)u.openConnection();
		    } else {
			conn = (HttpURLConnection)u.openConnection(proxy);
		    }
		    break;

		  case KERBEROS:
		    System.setProperty("java.security.krb5.conf", "krb5.conf");
		    System.setProperty("sun.security.krb5.debug", "true");
		    System.setProperty("java.security.auth.login.config", "login.conf");
		    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
		    spnego = new SpnegoHttpURLConnection("com.sun.security.jgss.krb5.initiate", user, pass);
		    if (proxy == null) {
			conn = spnego.connect(u);
		    } else {
			conn = spnego.connect(u, proxy);
		    }
		    break;

		  case NTLM:
		    Config.setProperty("http.auth.ntlm.domain", "localhost");
		    Config.setProperty("jcifs.smb.client.domain", "localhost");
		    Config.setProperty("jcifs.smb.client.username", user);
		    Config.setProperty("jcifs.smb.client.password", pass);
		    if (proxy == null) {
			conn = new NtlmHttpURLConnection((HttpURLConnection)u.openConnection());
		    } else {
			conn = new NtlmHttpURLConnection((HttpURLConnection)u.openConnection(proxy));
		    }
		    break;

		  case BASIC:
		    if (proxy == null) {
			conn = (HttpURLConnection)u.openConnection();
		    } else {
			conn = (HttpURLConnection)u.openConnection(proxy);
		    }
		    conn.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes((user + ":" + pass).getBytes()));
		    break;
		}

		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/soap+xml;charset=UTF-8");
		conn.setRequestProperty("SOAPAction", operation.getSOAPAction());
		conn.setRequestProperty("Accept", "*/*");

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		marshaller.marshal(soapFactory.createEnvelope(request), buffer);
		byte[] bytes = buffer.toByteArray();
		conn.setFixedLengthStreamingMode(bytes.length);

System.out.println("Connecting " + url);
		conn.connect();
		OutputStream out = conn.getOutputStream();
		out.write(bytes);
		out.flush();

		int code = conn.getResponseCode();
		switch(code) {
		  case HttpURLConnection.HTTP_OK:
//System.out.println("------SOAP BEGIN------");
//System.out.println(new String(bytes));
//System.out.println("------SOAP END------");
		    result = getSOAPBodyContents(conn.getInputStream());
		    retry = false;
		    break;

		  case HttpURLConnection.HTTP_UNAUTHORIZED:
		    retry = !retry; // flip bit
		    debug(conn);
		    if (retry) {
//
// DAS: this needs to be re-worked, to select from all the available WWW-Authenticate headers
//
			setAuthScheme(AuthScheme.get(conn.getHeaderField("WWW-Authenticate"), scheme));
		    }
		    break;

		  case HttpURLConnection.HTTP_BAD_REQUEST:
		  case HttpURLConnection.HTTP_INTERNAL_ERROR:
		    result = getSOAPBodyContents(conn.getErrorStream());
		    retry = false;
		    break;

		  default:
		    System.out.println("Bad response: " + code);
		    debug(conn);
		    retry = false;
		    break;
		}
	    } catch (GSSException e) {
		retry = false;
		e.printStackTrace();
	    } catch (LoginException e) {
		retry = false;
		e.printStackTrace();
	    } catch (PrivilegedActionException e) {
		retry = false;
		e.printStackTrace();
	    } finally {
		if (conn != null) {
		    conn.disconnect();
		}
		if (spnego != null) {
		    spnego.disconnect();
		}
	    }
	} while(retry);

	return result;
    }

    /**
     * Read a SOAP envelope and return the unmarshalled object contents of the body.
     */
    private Object getSOAPBodyContents(InputStream in) throws JAXBException, IOException {
	Object result = unmarshaller.unmarshal(in);
	if (result instanceof JAXBElement) {
	    JAXBElement elt = (JAXBElement)result;
	    if (elt.getValue() instanceof Envelope) {
		List<Object> list = ((Envelope)elt.getValue()).getBody().getAny();
		switch(list.size()) {
		  case 0:
		    return null;
		  case 1:
		    return list.get(0);
		  default:
		    return list;
		}
	    } else {
		System.out.println("Unsupported element contents: " + elt.getValue().getClass().getName());
	    }
	} else {
	    System.out.println("Unsupported class: " + result.getClass().getName());
	}
	return null;
    }

    /**
     * Spit out information about a non-HTTP_OK reply.
     */
    private void debug(HttpURLConnection conn) throws IOException {
	int len = conn.getHeaderFields().size();
	if (len > 0) {
	    System.out.println("HTTP Headers:");
	    System.out.println("  " + conn.getHeaderField(0));
	    for (int i=1; i < len; i++) {
		System.out.println("  " + conn.getHeaderFieldKey(i) + ": " + conn.getHeaderField(i));
	    }
	}
	if (conn.getContentLength() > 0) {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
	    String line = null;
	    while((line = reader.readLine()) != null) {
		System.out.println(line);
	    }
	}
    }
}
