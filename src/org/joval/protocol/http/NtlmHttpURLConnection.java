// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.protocol.http;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.Key;
import java.security.MessageDigest;
import java.security.Permission;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.joval.util.Base64;
import org.joval.util.StringTools;

import org.microsoft.security.ntlm.NtlmAuthenticator;
import org.microsoft.security.ntlm.NtlmAuthenticator.NtlmVersion;
import org.microsoft.security.ntlm.NtlmAuthenticator.ConnectionType;
import org.microsoft.security.ntlm.NtlmSession;
import org.microsoft.security.ntlm.impl.NtlmChallengeMessage;
import org.microsoft.security.ntlm.impl.NtlmRoutines;

/**
 * Based on jcifs.http.NtlmHttpURLConnection, but uses a superior NTLM implementation.
 */
public class NtlmHttpURLConnection extends HttpURLConnection {
    private static final int MAX_REDIRECTS = 20;

    private HttpURLConnection connection;
    private Map requestProperties;
    private Map headerFields;
    private ByteArrayOutputStream cachedOutput;
    private String authProperty;
    private String authMethod;
    private boolean handshakeComplete;
    private NtlmSession session;
    private URL url;

    public NtlmHttpURLConnection(HttpURLConnection connection) {
	super(connection.getURL());
	this.connection = connection;
	requestProperties = new HashMap();
	url = getURL();
	String user = url.getUserInfo();
	String pass = null;
	String host = null;
	String domain = null;
	if (user != null) {
	    user = URLDecoder.decode(user);
	    int ptr = user.indexOf(':');
	    if (ptr != -1) {
		pass = user.substring(ptr+1);
		user = user.substring(0,ptr);
	    }
	    try {
		host = InetAddress.getLocalHost().getHostName();
	    } catch (UnknownHostException e) {
	    }
	    ptr = user.indexOf('\\');
	    if (ptr == -1) {
		domain = host;
	    } else {
		domain = user.substring(0,ptr);
		user = user.substring(ptr+1);
	    }
	}
	NtlmAuthenticator.NtlmVersion ntlmv2 = NtlmAuthenticator.NtlmVersion.ntlmv2;
	NtlmAuthenticator.ConnectionType type = NtlmAuthenticator.ConnectionType.connectionOriented;
	NtlmAuthenticator auth = new NtlmAuthenticator(ntlmv2, type, host, domain, user, pass);
	session = auth.createSession();
    }

    public void connect() throws IOException {
	if (connected) return;
	connection.connect();
	connected = true;
    }

    private void handshake() throws IOException {
	if (!handshakeComplete) {
	    doHandshake();
	    handshakeComplete = true;
	}
    }

    public URL getURL() {
	return connection.getURL();
    }

    public int getContentLength() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getContentLength();
    }

    public String getContentType() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getContentType();
    }

    public String getContentEncoding() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getContentEncoding();
    }

    public long getExpiration() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getExpiration();
    }

    public long getDate() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getDate();
    }

    public long getLastModified() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getLastModified();
    }

    public String getHeaderField(String header) {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getHeaderField(header);
    }

    private Map getHeaderFields0() {
	if (headerFields != null) {
	    return headerFields;
	}
	Map map = new HashMap();
	String key = connection.getHeaderFieldKey(0);
	String value = connection.getHeaderField(0);
	for (int i = 1; key != null || value != null; i++) {
	    List values = (List) map.get(key);
	    if (values == null) {
		values = new ArrayList();
		map.put(key, values);
	    }
	    values.add(value);
	    key = connection.getHeaderFieldKey(i);
	    value = connection.getHeaderField(i);
	}
	Iterator entries = map.entrySet().iterator();
	while (entries.hasNext()) {
	    Map.Entry entry = (Map.Entry) entries.next();
	    entry.setValue(Collections.unmodifiableList((List)entry.getValue()));
	}
	return (headerFields = Collections.unmodifiableMap(map));
    }

    public Map getHeaderFields() {
	if (headerFields != null) return headerFields;
	try {
	    handshake();
	} catch (IOException e) {
	}
	return getHeaderFields0();
    }

    public int getHeaderFieldInt(String header, int def) {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getHeaderFieldInt(header, def);
    }

    public long getHeaderFieldDate(String header, long def) {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getHeaderFieldDate(header, def);
    }

    public String getHeaderFieldKey(int index) {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getHeaderFieldKey(index);
    }

    public String getHeaderField(int index) {
	try {
	    handshake();
	} catch (IOException ex) {
	}
	return connection.getHeaderField(index);
    }

    public Object getContent() throws IOException {
	try {
	    handshake();
	} catch (IOException ex) {
	}
	return connection.getContent();
    }

    public Object getContent(Class[] classes) throws IOException {
	try {
	    handshake();
	} catch (IOException ex) {	
	}
	return connection.getContent(classes);
    }

    public Permission getPermission() throws IOException {
	return connection.getPermission();
    }

    public InputStream getInputStream() throws IOException {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
	try {
	    connect();
	} catch (IOException ex) {
	}
	OutputStream output = connection.getOutputStream();
	cachedOutput = new ByteArrayOutputStream();
	return new CacheStream(output, cachedOutput);
    }

    public String toString() {
	return connection.toString();
    }

    public void setDoInput(boolean doInput) {
	connection.setDoInput(doInput);
	this.doInput = doInput;
    }

    public boolean getDoInput() {
	return connection.getDoInput();
    }

    public void setDoOutput(boolean doOutput) {
	connection.setDoOutput(doOutput);
	this.doOutput = doOutput;
    }

    public boolean getDoOutput() {
	return connection.getDoOutput();
    }

    public void setAllowUserInteraction(boolean allowUserInteraction) {
	connection.setAllowUserInteraction(allowUserInteraction);
	this.allowUserInteraction = allowUserInteraction;
    }

    public boolean getAllowUserInteraction() {
	return connection.getAllowUserInteraction();
    }

    public void setUseCaches(boolean useCaches) {
	connection.setUseCaches(useCaches);
	this.useCaches = useCaches;
    }

    public boolean getUseCaches() {
	return connection.getUseCaches();
    }

    public void setIfModifiedSince(long ifModifiedSince) {
	connection.setIfModifiedSince(ifModifiedSince);
	this.ifModifiedSince = ifModifiedSince;
    }

    public long getIfModifiedSince() {
	return connection.getIfModifiedSince();
    }

    public boolean getDefaultUseCaches() {
	return connection.getDefaultUseCaches();
    }

    public void setDefaultUseCaches(boolean defaultUseCaches) {
	connection.setDefaultUseCaches(defaultUseCaches);
    }

    public void setRequestProperty(String key, String value) {
	if (key == null) throw new NullPointerException();
	List values = new ArrayList();
	values.add(value);
	boolean found = false;
	Iterator entries = requestProperties.entrySet().iterator();
	while (entries.hasNext()) {
	    Map.Entry entry = (Map.Entry) entries.next();
	    if (key.equalsIgnoreCase((String) entry.getKey())) {
		entry.setValue(values);
		found = true;
		break;
	    }
	}
	if (!found) requestProperties.put(key, values);
	connection.setRequestProperty(key, value);
    }

    public void addRequestProperty(String key, String value) {
	if (key == null) throw new NullPointerException();
	List values = null;
	Iterator entries = requestProperties.entrySet().iterator();
	while (entries.hasNext()) {
	    Map.Entry entry = (Map.Entry) entries.next();
	    if (key.equalsIgnoreCase((String) entry.getKey())) {
		values = (List) entry.getValue();
		values.add(value);
		break;
	    }
	}
	if (values == null) {
	    values = new ArrayList();
	    values.add(value);
	    requestProperties.put(key, values);
	}
	// 1.3-compatible.
	StringBuffer buffer = new StringBuffer();
	Iterator propertyValues = values.iterator();
	while (propertyValues.hasNext()) {
	    buffer.append(propertyValues.next());
	    if (propertyValues.hasNext()) {
		buffer.append(", ");
	    }
	}
	connection.setRequestProperty(key, buffer.toString());
    }

    public String getRequestProperty(String key) {
	return connection.getRequestProperty(key);
    }

    public Map getRequestProperties() {
	Map map = new HashMap();
	Iterator entries = requestProperties.entrySet().iterator();
	while (entries.hasNext()) {
	    Map.Entry entry = (Map.Entry) entries.next();
	    map.put(entry.getKey(), Collections.unmodifiableList((List) entry.getValue()));
	}
	return Collections.unmodifiableMap(map);
    }

    public void setInstanceFollowRedirects(boolean instanceFollowRedirects) {
	connection.setInstanceFollowRedirects(instanceFollowRedirects);
    }

    public boolean getInstanceFollowRedirects() {
	return connection.getInstanceFollowRedirects();
    }

    public void setRequestMethod(String requestMethod) throws ProtocolException {
	connection.setRequestMethod(requestMethod);
	this.method = requestMethod;
    }

    public String getRequestMethod() {
	return connection.getRequestMethod();
    }

    public int getResponseCode() throws IOException {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getResponseCode();
    }

    public String getResponseMessage() throws IOException {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getResponseMessage();
    }

    public void disconnect() {
	connection.disconnect();
	handshakeComplete = false;
	connected = false;
    }

    public boolean usingProxy() {
	return connection.usingProxy();
    }

    public InputStream getErrorStream() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getErrorStream();
    }

    // Private

    private int parseResponseCode() throws IOException {
	try {
	    String response = connection.getHeaderField(0);
	    int index = response.indexOf(' ');
	    while (response.charAt(index) == ' ') index++;
	    return Integer.parseInt(response.substring(index, index + 3));
	} catch (Exception ex) {
	    throw new IOException(ex.getMessage());
	}
    }

    private void doHandshake() throws IOException {
	connect();
	try {
	    int response = parseResponseCode();
	    if (response != HTTP_UNAUTHORIZED && response != HTTP_PROXY_AUTH) {
		return;
	    }
	    byte[] type1 = attemptNegotiation(response);
	    if (type1 == null) return;
	    int attempt = 0;
	    while (attempt < MAX_REDIRECTS) {
		connection.setRequestProperty(authProperty, authMethod + ' ' + Base64.encodeBytes(type1));
		connection.setFixedLengthStreamingMode(0);
		connection.connect(); // send type 1
		response = parseResponseCode();
		if (response != HTTP_UNAUTHORIZED && response != HTTP_PROXY_AUTH) {
		    return;
		}
		byte[] type3 = attemptNegotiation(response);
		if (type3 == null) {
		    return;
		}
		connection.setRequestProperty(authProperty, authMethod + ' ' + Base64.encodeBytes(type3));
		if (cachedOutput != null) {
		    connection.setFixedLengthStreamingMode(cachedOutput.size());
		} else {
		    connection.setFixedLengthStreamingMode(0);
		}
		connection.connect(); // send type 3
		if (cachedOutput != null && doOutput) {
		    OutputStream output = connection.getOutputStream();
		    cachedOutput.writeTo(output);
		    output.flush();
		}
		response = parseResponseCode();
		if (response != HTTP_UNAUTHORIZED && response != HTTP_PROXY_AUTH) {
		    return;
		}
		attempt++;
		if (allowUserInteraction && attempt < MAX_REDIRECTS) {
		    reconnect();
		} else {
		    break;
		}
	    }
	} finally {
	    cachedOutput = null;
	}
    }

    private byte[] attemptNegotiation(int response) throws IOException {
	authProperty = null;
	authMethod = null;
	InputStream errorStream = connection.getErrorStream();
	if (errorStream != null && errorStream.available() != 0) {
	    int count;
	    byte[] buf = new byte[1024];
	    while ((count = errorStream.read(buf, 0, 1024)) != -1);
	}
	String authHeader;
	if (response == HTTP_UNAUTHORIZED) {
	    authHeader = "WWW-Authenticate";
	    authProperty = "Authorization";
	} else {
	    authHeader = "Proxy-Authenticate";
	    authProperty = "Proxy-Authorization";
	}
	String authorization = null;
	List methods = (List)getHeaderFields0().get(authHeader);
	if (methods == null) return null;
	Iterator iterator = methods.iterator();
	while (iterator.hasNext()) {
	    String currentAuthMethod = (String) iterator.next();
	    if (currentAuthMethod.startsWith("NTLM")) {
		if (currentAuthMethod.length() == 4) {
		    authMethod = "NTLM";
		    break;
		}
		if (currentAuthMethod.indexOf(' ') != 4) continue;
		authMethod = "NTLM";
		authorization = currentAuthMethod.substring(5).trim();
		break;
	    } else if (currentAuthMethod.startsWith("Negotiate")) {
		if (currentAuthMethod.length() == 9) {
		    authMethod = "Negotiate";
		    break;
		}
		if (currentAuthMethod.indexOf(' ') != 9) continue;
		authMethod = "Negotiate";
		authorization = currentAuthMethod.substring(10).trim();
		break;
	    }
	}
	if (authMethod == null) return null;
	byte[] message = null;
	if (authorization != null) {
	    // read type 2
	    message = Base64.decode(authorization);
//DAS
	    NtlmChallengeMessage challenge = new NtlmChallengeMessage(message);
	    challenge.setTargetInfoPair(NtlmRoutines.MsvAvFlags, new byte[] {
					(byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00});
	    challenge.setTargetInfoPair(NtlmRoutines.MsAvRestrictions, new byte[] {
					(byte)0x30, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
					(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
					(byte)0x00, (byte)0x30, (byte)0x00, (byte)0x00, (byte)0x37, (byte)0xef,
					(byte)0x99, (byte)0x63, (byte)0xdf, (byte)0xd0, (byte)0xf6, (byte)0xba,
					(byte)0xb1, (byte)0x3e, (byte)0xa8, (byte)0xe0, (byte)0xa9, (byte)0xa0,
					(byte)0xb9, (byte)0x53, (byte)0x2c, (byte)0xf2, (byte)0x02, (byte)0x7e,
					(byte)0x6d, (byte)0x52, (byte)0x06, (byte)0xff, (byte)0xc5, (byte)0xbb,
					(byte)0xcb, (byte)0x05, (byte)0x3b, (byte)0xd3, (byte)0x1d, (byte)0xc2});
	    challenge.setTargetInfoPair(NtlmRoutines.MsvChannelBindings, new byte[16]); // empty
	    String targetName = url.getProtocol().toUpperCase() + "/" + url.getHost().toUpperCase();
	    challenge.setTargetInfoPair(NtlmRoutines.MsvAvTargetName, targetName.getBytes("UTF-16LE"));

	    session.processChallengeMessage(challenge.getMessageData());
	}
	reconnect();
	if (message == null) {
	    // type 1
	    message = session.generateNegotiateMessage();
	} else {
	    // type 3
	    message = session.generateAuthenticateMessage();
	}
	return message;
    }

    private void reconnect() throws IOException {
	connection = (HttpURLConnection) connection.getURL().openConnection();
	connection.setRequestMethod(method);
	headerFields = null;
	Iterator properties = requestProperties.entrySet().iterator();
	while (properties.hasNext()) {
	    Map.Entry property = (Map.Entry) properties.next();
	    String key = (String) property.getKey();
	    StringBuffer value = new StringBuffer();
	    Iterator values = ((List) property.getValue()).iterator();
	    while (values.hasNext()) {
		value.append(values.next());
		if (values.hasNext()) value.append(", ");
	    }
	    connection.setRequestProperty(key, value.toString());
	}
	connection.setAllowUserInteraction(allowUserInteraction);
	connection.setDoInput(doInput);
	connection.setDoOutput(doOutput);
	connection.setIfModifiedSince(ifModifiedSince);
	connection.setUseCaches(useCaches);
    }

    private static class CacheStream extends OutputStream {
	private final OutputStream stream;
	private final OutputStream collector;

	public CacheStream(OutputStream stream, OutputStream collector) {
	    this.stream = stream;
	    this.collector = collector;
	}

	public void close() throws IOException {
	    stream.close();
	    collector.close();
	}

	public void flush() throws IOException {
	    stream.flush();
	    collector.flush();
	}

	public void write(byte[] b) throws IOException {
	    stream.write(b);
	    collector.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
	    stream.write(b, off, len);
	    collector.write(b, off, len);
	}

	public void write(int b) throws IOException {
	    stream.write(b);
	    collector.write(b);
	}
    }
}
