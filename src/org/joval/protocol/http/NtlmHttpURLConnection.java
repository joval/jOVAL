// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.protocol.http;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joval.util.Base64;

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
    private Map<String, List<String>> headerFields, requestProperties;
    private ByteArrayOutputStream cachedOutput;
    private String authProperty;
    private String authMethod;
    private boolean handshakeComplete;
    private NtlmSession session;
    private URL url;

    public NtlmHttpURLConnection(HttpURLConnection connection) {
	super(connection.getURL());
	this.connection = connection;
	requestProperties = new HashMap<String, List<String>>();
	url = getURL();
	String user = url.getUserInfo();
	String pass = null;
	String host = null;
	String domain = null;
	if (user != null) {
	    try {
		user = URLDecoder.decode(user, "UTF-8");
	    } catch (UnsupportedEncodingException e) {
	    }
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

    @Override
    public void connect() throws IOException {
	if (connected) return;
	connection.connect();
	connected = true;
    }

    @Override
    public URL getURL() {
	return connection.getURL();
    }

    @Override
    public int getContentLength() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getContentLength();
    }

    @Override
    public String getContentType() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getContentType();
    }

    @Override
    public String getContentEncoding() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getContentEncoding();
    }

    @Override
    public long getExpiration() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getExpiration();
    }

    @Override
    public long getDate() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getDate();
    }

    @Override
    public long getLastModified() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getLastModified();
    }

    @Override
    public String getHeaderField(String header) {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getHeaderField(header);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
	if (headerFields != null) {
	    return headerFields;
	} else {
	    try {
		handshake();
	    } catch (IOException e) {
	    }
	    return getHeaderFields0();
	}
    }

    @Override
    public int getHeaderFieldInt(String header, int def) {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getHeaderFieldInt(header, def);
    }

    @Override
    public long getHeaderFieldDate(String header, long def) {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getHeaderFieldDate(header, def);
    }

    @Override
    public String getHeaderFieldKey(int index) {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getHeaderFieldKey(index);
    }

    @Override
    public String getHeaderField(int index) {
	try {
	    handshake();
	} catch (IOException ex) {
	}
	return connection.getHeaderField(index);
    }

    @Override
    public Object getContent() throws IOException {
	try {
	    handshake();
	} catch (IOException ex) {
	}
	return connection.getContent();
    }

    @Override
    public Object getContent(Class[] classes) throws IOException {
	try {
	    handshake();
	} catch (IOException ex) {	
	}
	return connection.getContent(classes);
    }

    @Override
    public Permission getPermission() throws IOException {
	return connection.getPermission();
    }

    @Override
    public InputStream getInputStream() throws IOException {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
	try {
	    connect();
	} catch (IOException ex) {
	}
	OutputStream output = connection.getOutputStream();
	cachedOutput = new ByteArrayOutputStream();
	return new CacheStream(output, cachedOutput);
    }

    @Override
    public String toString() {
	return connection.toString();
    }

    @Override
    public void setDoInput(boolean doInput) {
	connection.setDoInput(doInput);
	this.doInput = doInput;
    }

    @Override
    public boolean getDoInput() {
	return connection.getDoInput();
    }

    @Override
    public void setDoOutput(boolean doOutput) {
	connection.setDoOutput(doOutput);
	this.doOutput = doOutput;
    }

    @Override
    public boolean getDoOutput() {
	return connection.getDoOutput();
    }

    @Override
    public void setAllowUserInteraction(boolean allowUserInteraction) {
	connection.setAllowUserInteraction(allowUserInteraction);
	this.allowUserInteraction = allowUserInteraction;
    }

    @Override
    public boolean getAllowUserInteraction() {
	return connection.getAllowUserInteraction();
    }

    @Override
    public void setUseCaches(boolean useCaches) {
	connection.setUseCaches(useCaches);
	this.useCaches = useCaches;
    }

    @Override
    public boolean getUseCaches() {
	return connection.getUseCaches();
    }

    @Override
    public void setIfModifiedSince(long ifModifiedSince) {
	connection.setIfModifiedSince(ifModifiedSince);
	this.ifModifiedSince = ifModifiedSince;
    }

    @Override
    public long getIfModifiedSince() {
	return connection.getIfModifiedSince();
    }

    @Override
    public boolean getDefaultUseCaches() {
	return connection.getDefaultUseCaches();
    }

    @Override
    public void setDefaultUseCaches(boolean defaultUseCaches) {
	connection.setDefaultUseCaches(defaultUseCaches);
    }

    @Override
    public void setRequestProperty(String key, String value) {
	if (key == null) throw new NullPointerException();
	List<String> values = new ArrayList<String>();
	values.add(value);
	boolean found = false;
	for (Map.Entry<String, List<String>> entry : requestProperties.entrySet()) {
	    if (key.equalsIgnoreCase(entry.getKey())) {
		entry.setValue(values);
		found = true;
		break;
	    }
	}
	if (!found) {
	    requestProperties.put(key, values);
	}
	connection.setRequestProperty(key, value);
    }

    @Override
    public void addRequestProperty(String key, String value) {
	if (key == null) {
	    throw new NullPointerException();
	}
	List<String> values = null;
	for (Map.Entry<String, List<String>> entry : requestProperties.entrySet()) {
	    if (key.equalsIgnoreCase(entry.getKey())) {
		values = entry.getValue();
		values.add(value);
		break;
	    }
	}
	if (values == null) {
	    values = new ArrayList<String>();
	    values.add(value);
	    requestProperties.put(key, values);
	}
	StringBuffer buffer = new StringBuffer();
	for (String s : values) {
	    if (buffer.length() > 0) {
		buffer.append(", ");
	    }
	    buffer.append(s);
	}
	connection.setRequestProperty(key, buffer.toString());
    }

    @Override
    public String getRequestProperty(String key) {
	return connection.getRequestProperty(key);
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
	Map<String, List<String>> map = new HashMap<String, List<String>>();
	for (Map.Entry<String, List<String>> entry : requestProperties.entrySet()) {
	    map.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
	}
	return Collections.unmodifiableMap(map);
    }

    @Override
    public void setInstanceFollowRedirects(boolean instanceFollowRedirects) {
	connection.setInstanceFollowRedirects(instanceFollowRedirects);
    }

    @Override
    public boolean getInstanceFollowRedirects() {
	return connection.getInstanceFollowRedirects();
    }

    @Override
    public void setRequestMethod(String requestMethod) throws ProtocolException {
	connection.setRequestMethod(requestMethod);
	this.method = requestMethod;
    }

    @Override
    public String getRequestMethod() {
	return connection.getRequestMethod();
    }

    @Override
    public int getResponseCode() throws IOException {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getResponseCode();
    }

    @Override
    public String getResponseMessage() throws IOException {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getResponseMessage();
    }

    @Override
    public void disconnect() {
	connection.disconnect();
	handshakeComplete = false;
	connected = false;
    }

    @Override
    public boolean usingProxy() {
	return connection.usingProxy();
    }

    @Override
    public InputStream getErrorStream() {
	try {
	    handshake();
	} catch (IOException e) {
	}
	return connection.getErrorStream();
    }

    @Override
    public void setChunkedStreamingMode(int chunkSize) {
	connection.setChunkedStreamingMode(chunkSize);
    }

    @Override
    public void setFixedLengthStreamingMode(int contentLength) {
	connection.setFixedLengthStreamingMode(contentLength);
    }

    // Private

    private Map<String, List<String>> getHeaderFields0() {
	if (headerFields != null) {
	    return headerFields;
	}
	Map<String, List<String>> map = new HashMap<String, List<String>>();
	String key = connection.getHeaderFieldKey(0);
	String value = connection.getHeaderField(0);
	for (int i=1; key != null || value != null; i++) {
	    List<String> values = map.get(key);
	    if (values == null) {
		values = new ArrayList<String>();
		map.put(key, values);
	    }
	    values.add(value);
	    key = connection.getHeaderFieldKey(i);
	    value = connection.getHeaderField(i);
	}
	for (Map.Entry<String, List<String>> entry : map.entrySet()) {
	    entry.setValue(Collections.unmodifiableList(entry.getValue()));
	}
	return (headerFields = Collections.unmodifiableMap(map));
    }

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

    private void handshake() throws IOException {
	if (!handshakeComplete) {
	    doHandshake();
	    handshakeComplete = true;
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
	    errorStream.close();
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
	List<String> methods = getHeaderFields0().get(authHeader);
	if (methods == null) return null;
	for (String method : methods) {
	    if (method.startsWith("NTLM")) {
		if (method.length() == 4) {
		    authMethod = "NTLM";
		    break;
		}
		if (method.indexOf(' ') != 4) continue;
		authMethod = "NTLM";
		authorization = method.substring(5).trim();
		break;
	    } else if (method.startsWith("Negotiate")) {
		if (method.length() == 9) {
		    authMethod = "Negotiate";
		    break;
		}
		if (method.indexOf(' ') != 9) continue;
		authMethod = "Negotiate";
		authorization = method.substring(10).trim();
		break;
	    }
	}
	if (authMethod == null) return null;
	byte[] message = null;
	if (authorization != null) {
	    // read type 2
	    message = Base64.decode(authorization);
	    session.processChallengeMessage(message);
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
	if (connection instanceof HttpSocketConnection) {
	    ((HttpSocketConnection)connection).reset();
	} else {
	    connection = (HttpURLConnection)connection.getURL().openConnection();
	}
	connection.setRequestMethod(method);
	headerFields = null;
	for (Map.Entry<String, List<String>> property : requestProperties.entrySet()) {
	    String key = property.getKey();
	    StringBuffer value = new StringBuffer();
	    for (String val : property.getValue()) {
		if (value.length() > 0) {
		    value.append(", ");
		}
		value.append(val);
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
