// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.protocol.http;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.net.URL;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joval.intf.windows.identity.IWindowsCredential;
import org.joval.util.Base64;

import org.microsoft.security.ntlm.NtlmAuthenticator;
import org.microsoft.security.ntlm.NtlmAuthenticator.NtlmVersion;
import org.microsoft.security.ntlm.NtlmAuthenticator.ConnectionType;
import org.microsoft.security.ntlm.NtlmSession;
import org.microsoft.security.ntlm.impl.NtlmChallengeMessage;
import org.microsoft.security.ntlm.impl.NtlmRoutines;

/**
 * An HttpURLConnection implementation that can negotiate NTLM authentication with an HTTP proxy and/or
 * destination HTTP server, even when using streaming modes.
 *
 * @author David A. Solin
 * @version %I% %V%
 */
public class NtlmHttpURLConnection extends HttpURLConnection {
    private static NtlmAuthenticator.NtlmVersion NTLMV2	= NtlmAuthenticator.NtlmVersion.ntlmv2;
    private static NtlmAuthenticator.ConnectionType CO	= NtlmAuthenticator.ConnectionType.connectionOriented;

    private HttpSocketConnection connection;
    private NtlmSession session, proxySession;
    private String authProperty, authMethod;
    private boolean negotiated;
    private CachingOutputStream cachedOutput;

    /**
     * Create a connection to a URL.
     */
    public NtlmHttpURLConnection(URL url) {
	this(url, null);
    }

    /**
     * Create a connection to a URL, and use the specified credentials to negotiate with the destination server.
     */
    public NtlmHttpURLConnection(URL url, IWindowsCredential cred) {
	super(url);
	connection = new HttpSocketConnection(url);
	session = createSession(cred);
    }

    /**
     * This method will cause the connection to go through a proxy.
     */
    public void setProxy(Proxy proxy) {
	setProxy(proxy, null);
    }

    /**
     * Connect through a proxy, using the specified credentials to negotiate with the proxy.
     */
    public void setProxy(Proxy proxy, IWindowsCredential cred) {
	if (connected) {
	    throw new IllegalStateException("connected");
	} else {
	    connection.setProxy(proxy);
	    proxySession = createSession(cred);
	}
    }

    // HttpURLConnection overrides

    @Override
    public void connect() throws IOException {
	if (connected) return;

	//
	// Seed the initial connection with negotiate headers
	//
	if (proxySession != null && getRequestProperty("Proxy-Authorization") == null) {
	    connection.setRequestProperty("Proxy-Authorization", "Negotiate " +
		Base64.encodeBytes(proxySession.generateNegotiateMessage()));
	}
	if (session != null && getRequestProperty("Authorization") == null) {
	    connection.setRequestProperty("Authorization", "Negotiate " +
		Base64.encodeBytes(session.generateNegotiateMessage()));
	}

	connection.connect();

	//
	// Send any cached output
	//
	if (cachedOutput != null) {
	    OutputStream out = connection.getOutputStream();
	    cachedOutput.writeTo(out);
	    out.close();
	}

	connected = true;
    }

    @Override
    public void disconnect() {
	connection.disconnect();
	negotiated = false;
	connected = false;
    }

    @Override
    public int getResponseCode() throws IOException {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getResponseCode();
    }

    @Override
    public String getResponseMessage() throws IOException {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getResponseMessage();
    }

    @Override
    public int getContentLength() {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getContentLength();
    }

    @Override
    public String getContentType() {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getContentType();
    }

    @Override
    public String getContentEncoding() {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getContentEncoding();
    }

    @Override
    public long getExpiration() {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getExpiration();
    }

    @Override
    public long getDate() {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getDate();
    }

    @Override
    public long getLastModified() {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getLastModified();
    }

    @Override
    public String getHeaderField(String header) {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getHeaderField(header);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getHeaderFields();
    }

    @Override
    public int getHeaderFieldInt(String header, int def) {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getHeaderFieldInt(header, def);
    }

    @Override
    public long getHeaderFieldDate(String header, long def) {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getHeaderFieldDate(header, def);
    }

    @Override
    public String getHeaderFieldKey(int index) {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getHeaderFieldKey(index);
    }

    @Override
    public String getHeaderField(int index) {
	try {
	    negotiate();
	} catch (IOException ex) {
	}
	return connection.getHeaderField(index);
    }

    @Override
    public Object getContent() throws IOException {
	try {
	    negotiate();
	} catch (IOException ex) {
	}
	return connection.getContent();
    }

    @Override
    public Object getContent(Class[] classes) throws IOException {
	try {
	    negotiate();
	} catch (IOException ex) {	
	}
	return connection.getContent(classes);
    }

    @Override
    public InputStream getInputStream() throws IOException {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getInputStream();
    }

// DAS: TBD - only send cached output on the final attempt.
    @Override
    public OutputStream getOutputStream() throws IOException {
	if (cachedOutput == null) {
	    cachedOutput = new CachingOutputStream(connection.getOutputStream());
	}
	return cachedOutput;
    }

    @Override
    public InputStream getErrorStream() {
	try {
	    negotiate();
	} catch (IOException e) {
	}
	return connection.getErrorStream();
    }

    @Override
    public String toString() {
	return connection.toString();
    }

    @Override
    public Permission getPermission() throws IOException {
	return connection.getPermission();
    }

    @Override
    public boolean usingProxy() {
	return connection.usingProxy();
    }

    @Override
    public boolean getDoInput() {
	return connection.getDoInput();
    }

    @Override
    public boolean getDoOutput() {
	return connection.getDoOutput();
    }

    @Override
    public boolean getAllowUserInteraction() {
	return connection.getAllowUserInteraction();
    }

    @Override
    public boolean getUseCaches() {
	return connection.getUseCaches();
    }

    @Override
    public boolean getInstanceFollowRedirects() {
	return connection.getInstanceFollowRedirects();
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
    public String getRequestProperty(String key) {
	return connection.getRequestProperty(key);
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
	return connection.getRequestProperties();
    }

    @Override
    public void setRequestProperty(String key, String value) {
	connection.setRequestProperty(key, value);
    }

    @Override
    public void addRequestProperty(String key, String value) {
	connection.addRequestProperty(key, value);
    }

    @Override
    public void setDefaultUseCaches(boolean defaultUseCaches) {
	connection.setDefaultUseCaches(defaultUseCaches);
    }

    @Override
    public void setInstanceFollowRedirects(boolean instanceFollowRedirects) {
	connection.setInstanceFollowRedirects(instanceFollowRedirects);
    }

    @Override
    public void setChunkedStreamingMode(int chunkSize) {
	connection.setChunkedStreamingMode(chunkSize);
    }

    @Override
    public void setFixedLengthStreamingMode(int contentLength) {
	connection.setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public void setDoInput(boolean doInput) {
	connection.setDoInput(doInput);
	this.doInput = doInput;
    }

    @Override
    public void setDoOutput(boolean doOutput) {
	connection.setDoOutput(doOutput);
	this.doOutput = doOutput;
    }

    @Override
    public void setAllowUserInteraction(boolean allowUserInteraction) {
	this.allowUserInteraction = allowUserInteraction;
	connection.setAllowUserInteraction(allowUserInteraction);
    }

    @Override
    public void setUseCaches(boolean useCaches) {
	this.useCaches = useCaches;
	connection.setUseCaches(useCaches);
    }

    @Override
    public void setIfModifiedSince(long ifModifiedSince) {
	this.ifModifiedSince = ifModifiedSince;
	connection.setIfModifiedSince(ifModifiedSince);
    }

    @Override
    public void setRequestMethod(String requestMethod) throws ProtocolException {
	this.method = requestMethod;
	connection.setRequestMethod(requestMethod);
    }

    // Private

    /**
     *
     */
    private void negotiate() throws IOException {
	if (negotiated) return;
	try {
	    for (int attempt=1; attempt < 3; attempt++) {
		connect();
		int response = connection.getResponseCode();
		switch(response) {
		  case HTTP_UNAUTHORIZED:
		    if (session == null) {
			return;
		    } else {
			AuthenticateData auth = new AuthenticateData(connection.getHeaderField("WWW-Authenticate"));
			if (auth.isNegotiate()) {
			    reset();
			    setRequestProperty("Authorization", auth.createAuthenticateHeader(session));
			} else {
			    return;
			}
		    }
		    break;

		  case HTTP_PROXY_AUTH:
		    if (proxySession == null) {
			return;
		    } else {
			AuthenticateData auth = new AuthenticateData(connection.getHeaderField("Proxy-Authenticate"));
			if (auth.isNegotiate()) {
			    reset();
			    setRequestProperty("Proxy-Authorization", auth.createAuthenticateHeader(proxySession));
			} else {
			    return;
			}
		    }
		    break;

		  default:
		    return;
		}
	    }
	} finally {
	    cachedOutput = null;
	    negotiated = true;
	}
    }

    /**
     * Prepare the connection for another attempt.
     */
    private void reset() throws IOException {
	drain(connection.getErrorStream());
	Map<String, List<String>> map = connection.getRequestProperties();
	connection.reset();
	connected = false;

	for (Map.Entry<String, List<String>> entry : map.entrySet()) {
	    List<String> values = entry.getValue();
	    connection.setRequestProperty(entry.getKey(), values.get(0));
	    for (int i=1; i < values.size(); i++) {
		connection.addRequestProperty(entry.getKey(), values.get(i));
	    }
	}
	connection.setRequestMethod(method);
	connection.setAllowUserInteraction(allowUserInteraction);
	connection.setDoInput(doInput);
	connection.setDoOutput(doOutput);
	connection.setIfModifiedSince(ifModifiedSince);
	connection.setUseCaches(useCaches);
	if (cachedOutput == null) {
	    setFixedLengthStreamingMode(0);
	} else {
	    setFixedLengthStreamingMode(cachedOutput.size());
	}
    }

    /**
     * Create an NtlmSession using the supplied credentials.
     */
    private NtlmSession createSession(IWindowsCredential cred) {
	if (cred == null) {
	    return null;
	}
	String host = null;
	try {
	    host = InetAddress.getLocalHost().getHostName();
	} catch (UnknownHostException e) {
	}
	String domain = cred.getDomain();
	String user = cred.getUsername();
	String pass = cred.getPassword();
	NtlmAuthenticator auth = new NtlmAuthenticator(NTLMV2, CO, host, domain, user, pass);
	return auth.createSession();
    }

    private void drain(InputStream stream) throws IOException {
	if (stream != null && stream.available() != 0) {
	    int count;
	    byte[] buf = new byte[1024];
	    while ((count = stream.read(buf, 0, 1024)) != -1);
	    stream.close();
	}
    }

    class AuthenticateData {
	String type;
	byte[] type2;

	AuthenticateData(String header) throws IOException {
	    int ptr = header.indexOf(" ");
	    if (ptr == -1) {
		type = header;
		type2 = null;
	    } else {
		type = header.substring(0,ptr);
		type2 = Base64.decode(header.substring(ptr+1));
	    }
	}

	boolean isNegotiate() {
	    return type2 != null;
	}

	String getType() {
	    return type;
	}

	String createNegotiateHeader(NtlmSession session) {
	    StringBuffer header = new StringBuffer(type);
	    header.append(" ");
	    header.append(Base64.encodeBytes(session.generateNegotiateMessage()));
	    return header.toString();
	}

	String createAuthenticateHeader(NtlmSession session) {
	    if (type2 == null) {
		throw new IllegalStateException("no challenge");
	    }
	    session.processChallengeMessage(type2);
	    StringBuffer header = new StringBuffer(type);
	    header.append(" ");
	    header.append(Base64.encodeBytes(session.generateAuthenticateMessage()));
	    return header.toString();
	}
    }

    class CachingOutputStream extends OutputStream {
	private OutputStream destination;
	private ByteArrayOutputStream cache;

	CachingOutputStream(OutputStream destination) {
	    this.destination = destination;
	    cache = new ByteArrayOutputStream();
	}

	void writeTo(OutputStream out) throws IOException {
	    cache.writeTo(out);
	}

	int size() {
	    return cache.size();
	}

	// OutputStream Overrides

	@Override
	public void write(int ch) throws IOException {
	    cache.write(ch);
	    destination.write(ch);
	}

	@Override
	public void write(byte[] b) throws IOException {
	    write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int offset, int len) throws IOException {
	    cache.write(b, offset, len);
	    destination.write(b, offset, len);
	}

	@Override
	public void close() throws IOException {
	    cache.close();
	    destination.close();
	}

	@Override
	public void flush() throws IOException {
	    destination.flush();
	}
    }
}
