// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.protocol.http;

import java.io.ByteArrayInputStream;
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
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
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
import java.util.StringTokenizer;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.joval.util.Base64;

/**
 * An HTTP 1.1 connection implementation that re-uses a single socket connection.
 */
public class HttpSocketConnection extends HttpURLConnection {
    public static final byte[] CRLF = {'\r', '\n'};
static boolean debug = false;

    private static final int MAX_REDIRECTS = 20;

    private Socket socket;
    private Proxy proxy;
    private String host; // host:port
    private Map<String, List<String>> headerFields, requestProperties;
    private List<KVP> orderedHeaderFields;
    private int outputLength, contentLength;
    private String contentType, contentEncoding;
    private long expiration, date, lastModified;
    private boolean didRequest;
    private ByteArrayOutputStream buffer;

    /**
     * Create a direct connection.
     */
    public HttpSocketConnection(URL url) {
	this(url, null);
    }

    /**
     * Create a connection through a proxy.
     */
    public HttpSocketConnection(URL url, Proxy proxy) throws IllegalArgumentException {
	super(url);
	if (proxy != null) {
	    switch(proxy.type()) {
	      case HTTP:
		this.proxy = proxy;
		break;
	      case SOCKS:
		throw new IllegalArgumentException("Illegal proxy type: SOCKS");
	      case DIRECT:
	      default:
		this.proxy = null;
		break;
	    }
	}
	StringBuffer sb = new StringBuffer(url.getHost());
	if (url.getPort() == -1) {
	    sb.append(":80");
	} else {
	    sb.append(":").append(Integer.toString(url.getPort()));
	}
	host = sb.toString();
	connected = false;
	reset();
    }

    /**
     * Reset the connection to a pristine state.
     */
    public void reset() {
	//
	// reset inherited fields
	//
	chunkLength = -1;
	fixedContentLength = -1;
	method = "GET";
	responseCode = -1;
	responseMessage = null;
	allowUserInteraction = getDefaultAllowUserInteraction();
	doInput = true;
	doOutput = false;
	ifModifiedSince = 0;
	useCaches = getDefaultUseCaches();

	//
	// reset private fields
	//
	orderedHeaderFields = null;
	headerFields = null;
	requestProperties = new HashMap<String, List<String>>();
	setRequestProperty("User-Agent", "jOVAL HTTP Client");
	buffer = null;
	outputLength = 0;
	contentLength = 0;
	contentType = null;
	contentEncoding = null;
	expiration = 0;
	date = 0;
	lastModified = 0;
	didRequest = false;
    }

    // Overrides for HttpURLConnection

    @Override
    public void connect() throws IOException {
	if (!connected) {
	    if (proxy == null) {
		socket = new Socket(url.getHost(), url.getPort());
	    } else {
		socket = new Socket();
		socket.connect(proxy.address());
	    }
	    connected = true;
	}
    }

    @Override
    public int getContentLength() {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	return contentLength;
    }

    @Override
    public String getContentType() {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	return contentType;
    }

    @Override
    public String getContentEncoding() {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	return contentEncoding;
    }

    @Override
    public long getExpiration() {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	return expiration;
    }

    @Override
    public long getDate() {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	return date;
    }

    @Override
    public long getLastModified() {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	return lastModified;
    }

    @Override
    public String getHeaderField(String header) {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	List<String> values = headerFields.get(header);
	if (values == null) {
	    return null;
	} else {
	    return values.get(0);
	}
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
	if (headerFields == null) {
	    try {
		doRequest();
	    } catch (IOException e) {
	    }
	}
	return headerFields;
    }

    @Override
    public int getHeaderFieldInt(String header, int def) {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	String s = getHeaderField(header);
	if (s != null) {
	    try {
		return Integer.parseInt(s);
	    } catch (NumberFormatException e) {
	    }
	}
	return def;
    }

    @Override
    public long getHeaderFieldDate(String header, long def) {
	try {
	    doRequest();
	} catch (IOException e) {
	}
//DAS: TBD - attempt all the possible date formats
	return 0;
    }

    @Override
    public String getHeaderFieldKey(int index) {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	if (orderedHeaderFields.size() > index) {
	    return orderedHeaderFields.get(index).key();
	} else {
	    return null;
	}
    }

    @Override
    public String getHeaderField(int index) {
	try {
	    doRequest();
	} catch (IOException ex) {
	}
	if (orderedHeaderFields.size() > index) {
	    return orderedHeaderFields.get(index).value();
	} else {
	    return null;
	}
    }

    @Override
    public Object getContent() {
	throw new UnsupportedOperationException("getContent");
    }

    @Override
    public Object getContent(Class[] classes) throws IOException {
	throw new UnsupportedOperationException("getContent");
    }

    @Override
    public Permission getPermission() throws IOException {
	return new java.net.SocketPermission(host, "connect");
    }

    @Override
    public InputStream getInputStream() throws IOException {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	if (buffer == null) {
	    return null;
	} else {
	    return new HSBufferStream(buffer.toByteArray());
	}
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
	try {
	    connect();
	} catch (IOException ex) {
	}
	if (buffer == null) {
	    buffer = new ByteArrayOutputStream();
	}
	return buffer;
    }

    @Override
    public String toString() {
	return "Connection to " + url.toString();
    }

    @Override
    public void setRequestMethod(String method) {
	this.method = method;
    }

    @Override
    public void setDoOutput(boolean doOutput) {
	this.doOutput = doOutput;
    }

    @Override
    public void setDoInput(boolean doInput) {
	this.doInput = doInput;
    }

    @Override
    public void setAllowUserInteraction(boolean allowUserInteraction) {
	this.allowUserInteraction = allowUserInteraction;
    }

    @Override
    public void setIfModifiedSince(long ifModifiedSince) {
	this.ifModifiedSince = ifModifiedSince;
    }

    @Override
    public void setUseCaches(boolean useCaches) {
	this.useCaches = useCaches;
    }

    @Override
    public void setRequestProperty(String key, String value) {
	setMapProperty(key, value, requestProperties);
    }

    @Override
    public void addRequestProperty(String key, String value) {
	addMapProperty(key, value, requestProperties);
    }

    @Override
    public String getRequestProperty(String key) {
	if (requestProperties.containsKey(key)) {
	    List<String> values = requestProperties.get(key);
	    return values.get(0);
	}
	return null;
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
    public int getResponseCode() throws IOException {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	return responseCode;
    }

    @Override
    public String getResponseMessage() throws IOException {
	try {
	    doRequest();
	} catch (IOException e) {
	}
	return responseMessage;
    }

    @Override
    public void disconnect() {
	if (connected) {
	    try {
		socket.close();
	    } catch (IOException e) {
	    }
	    connected = false;
	}
    }

    @Override
    public boolean usingProxy() {
	return proxy != null;
    }

    @Override
    public InputStream getErrorStream() {
	try {
	    return getInputStream();
	} catch (IOException e) {
	}
	return null;
    }

    @Override
    public void setFixedLengthStreamingMode(int contentLength) {
	outputLength = contentLength;
    }

    // Private

    /**
     * Write the request and read the response over the socket.
     */
    private void doRequest() throws IOException {
	if (didRequest) return;
	connect();
	try {
	    StringBuffer req = new StringBuffer(getRequestMethod()).append(" ");
	    if (proxy != null) {
		req.append(host);
	    }
	    String path = url.getPath();
	    if (!path.startsWith("/")) {
		req.append("/");
	    }
	    req.append(path);
	    req.append(" HTTP/1.1\n");
	    setRequestProperty("Connection", "Keep-Alive");
	    setRequestProperty("Host", host);
	    if (doOutput) {
		if (buffer != null) {
		    outputLength = buffer.size();
		}
		setRequestProperty("Content-Length", Integer.toString(outputLength));
	    }
	    write(req.toString());
	    for (Map.Entry<String, List<String>> entry : requestProperties.entrySet()) {
		StringBuffer header = new StringBuffer();
		for (String s : entry.getValue()) {
		    if (header.length() > 0) {
			header.append(", ");
		    }
		    header.append(s);
		}
		header.insert(0, ": ");
		header.insert(0, entry.getKey());
		header.append("\n");
		write(header.toString());
	    }
	    write(CRLF);
	    if (buffer != null) {
		write(buffer.toByteArray());
		buffer = null;
	    }

	    orderedHeaderFields = new ArrayList<KVP>();
	    Map<String, List<String>> map = new HashMap<String, List<String>>();

if(debug)System.out.println("\nRESPONSE:");
	    InputStream in = socket.getInputStream();
	    StringBuffer sb = new StringBuffer();
	    boolean done = false;
	    boolean cr = false;
	    while(!done) {
		int ch = in.read();
		switch(ch) {
		  case -1:
		    throw new IOException("Connection was closed!");
		  case '\r':
		    if (sb.length() == 0) {
			cr = true;
		    }
		    break;
		  case '\n':
		    if (cr) {
			done = true;
		    } else if (sb.length() > 0) {
if(debug)System.out.println(sb.toString());
			if (orderedHeaderFields.size() == 0) {
			    String response = sb.toString();
			    orderedHeaderFields.add(new KVP("", response));
			    parseResponse(response);
			} else {
			    KVP pair = new KVP(sb.toString());
			    if (pair.key().equalsIgnoreCase("Content-Length")) {
				contentLength = Integer.parseInt(pair.value());
			    } else if (pair.key().equalsIgnoreCase("Content-Type")) {
				contentType = pair.value();
			    } else if (pair.key().equalsIgnoreCase("Content-Encoding")) {
				contentEncoding = pair.value();
			    }
			    orderedHeaderFields.add(pair);
			    for (String val : pair.value().split(", ")) {
				addMapProperty(pair.key(), val, map);
			    }
			}
			sb = new StringBuffer();
		    }
		    break;
		  default:
		    cr = false;
		    sb.append((char)ch);
		    break;
		}
	    }
	    headerFields = Collections.unmodifiableMap(map);
	    if (contentLength > 0) {
		buffer = new ByteArrayOutputStream(contentLength);
		byte[] bytes = new byte[contentLength];
		in.read(bytes, 0, contentLength);
		buffer.write(bytes, 0, contentLength);
	    }
	} catch (Exception e) {
	    if (debug) {
		e.printStackTrace();
	    }
	    throw e;
	} finally {
	    didRequest = true;
	    if ("Close".equalsIgnoreCase(getHeaderField("Connection"))) {
		disconnect();
	    }
	}
    }

    private void write(String s) throws IOException {
	write(s.getBytes("US-ASCII"));
    }

    private void write(byte[] bytes) throws IOException {
if (debug) System.out.write(bytes);
	socket.getOutputStream().write(bytes);
	socket.getOutputStream().flush();
    }

    /**
     * Parse the HTTP response line.
     */
    private void parseResponse(String line) throws IllegalArgumentException {
	StringTokenizer tok = new StringTokenizer(line, " ");
	if (tok.countTokens() < 2) {
	    throw new IllegalArgumentException(line);
	}
	String httpVersion = tok.nextToken();
	responseCode = Integer.parseInt(tok.nextToken());
	if (tok.hasMoreTokens()) {
	    responseMessage = tok.nextToken("\n");
	}
    }

    /**
     * Set a property of a multi-valued map.
     */
    private void setMapProperty(String key, String value, Map<String, List<String>> map) {
	List<String> values = new ArrayList<String>();
	values.add(value);
	boolean found = false;
	for (Map.Entry<String, List<String>> entry : map.entrySet()) {
	    if (key.equalsIgnoreCase(entry.getKey())) {
		entry.setValue(values);
		found = true;
		break;
	    }
	}
	if (!found) {
	    map.put(key, values);
	}
    }

    /**
     * Add a property to a multi-valued map.
     */
    private void addMapProperty(String key, String value, Map<String, List<String>>map) {
	List<String> values = null;
	for (Map.Entry<String, List<String>> entry : map.entrySet()) {
	    if (key.equalsIgnoreCase(entry.getKey())) {
		values = entry.getValue();
		values.add(value);
		break;
	    }
	}
	if (values == null) {
	    values = new ArrayList<String>();
	    values.add(value);
	    map.put(key, values);
	}
    }

    /**
     * Container for a Key-Value Pair.
     */
    class KVP {
	private String key, value;

	KVP(String header) throws IllegalArgumentException {
	    int ptr = header.indexOf(": ");
	    if (ptr == -1) {
		throw new IllegalArgumentException(header);
	    } else {
		key = header.substring(0,ptr);
		value = header.substring(ptr+2);
	    }
	}

	KVP(String key, String value) {
	    this.key = key;
	    this.value = value;
	}

	String key() {
	    return key;
	}

	String value() {
	    return value;
	}
    }

    class HSBufferStream extends ByteArrayInputStream {
	HSBufferStream(byte[] buffer) {
	    super(buffer);
	}

	@Override
	public void close() throws IOException {
	    super.close();
	    reset();
	}
    }
}
