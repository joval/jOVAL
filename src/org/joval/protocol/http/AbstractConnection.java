// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.protocol.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.net.ssl.SSLSocketFactory;

import org.joval.util.RFC822;

/**
 * An HTTP 1.1 connection implementation that re-uses a single socket connection.  This is useful when a single TCP connection
 * is needed to communicate repeatedly with a particular URL, for example, when performing NTLM authentication negotiation.
 *
 * Thanks to James Marshall for his concise discussion of HTTP/1.1:
 * @see http://www.jmarshall.com/easy/http/#http1.1c2
 *
 * @author David A. Solin
 * @version %I% %G%
 */
abstract class AbstractConnection extends HttpURLConnection {
    static final byte[] CRLF = {'\r', '\n'};

    /**
     * Set a property of a multi-valued map.
     */
    static void setMapProperty(String key, String value, Map<String, List<String>> map) {
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
     * Add a key/value pair to a multi-valued map. If the value is comma-delimited, its values are parsed and added to
     * the map accordingly.
     */
    static void addMapProperties(KVP pair, Map<String, List<String>> map) {
	for (String val : pair.value().split(", ")) {
	    addMapProperty(pair.key(), val, map);
	}
    }

    /**
     * Add a property to a multi-valued map.
     */
    static void addMapProperty(String key, String value, Map<String, List<String>>map) {
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
     * Reads a line as a key-value-pair, or returns null when CRLF is reached.
     */
    static KVP readKVP(InputStream in) throws IOException {
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
		    return null;
		} else if (sb.length() > 0) {
		    return new KVP(sb.toString());
		}
		// fall-thru
	      default:
		cr = false;
		sb.append((char)ch);
		break;
	    }
	}
	throw new ProtocolException("Failed to parse header from " + sb.toString());
    }

    /**
     * Container for a Key-Value Pair.
     */
    static class KVP {
	private String key, value;

	KVP(String header) throws IllegalArgumentException {
	    int ptr = header.indexOf(": ");
	    if (ptr == -1) {
		key = "";
		value = header;
	    } else {
		key = header.substring(0,ptr);
		value = header.substring(ptr+2);
	    }
	}

	KVP(String key, String value) {
	    this.key = key;
	    this.value = value;
	}

	KVP(Map.Entry<String, List<String>> entry) {
	    key = entry.getKey();
	    StringBuffer sb = new StringBuffer();
	    for (String s : entry.getValue()) {
		if (sb.length() > 0) {
		    sb.append(", ");
		}
		sb.append(s);
	    }
	    value = sb.toString();
	}

	@Override
	public String toString() {
	    if (key.length() == 0) {
		return value;
	    } else {
		return new StringBuffer(key).append(": ").append(value).toString();
	    }
	}

	String key() {
	    return key;
	}

	String value() {
	    return value;
	}
    }

    // Instance

    Map<String, List<String>> headerFields, requestProperties;
    List<KVP> orderedHeaderFields;
    int contentLength;
    String contentType, contentEncoding;
    long expiration, date, lastModified;
    InputStream responseData;

    /**
     * Initialize with the URL.
     */
    AbstractConnection(URL url) {
	super(url);
	initialize();
    }

    /**
     * Initialize all fields to their default states.
     */
    void initialize() {
	//
	// initialize inherited fields
	//
	connected = false;
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
	// initialize inherited fields
	//
	orderedHeaderFields = null;
	headerFields = null;
	requestProperties = new HashMap<String, List<String>>();
	responseData = null;
	contentLength = 0;
	contentType = null;
	contentEncoding = null;
	expiration = 0;
	date = 0;
	lastModified = 0;
    }

    /**
     * Perform the request/response exchange; implemented by subclasses.
     *
     * Subclasses must set all the protected fields, including the responseData InputStream, which is returned by both
     * getInputStream and getErrorStream, as appropriate according to the value of responseCode.
     */
    abstract void getResponse() throws IOException;

    // Overrides for HttpURLConnection

    @Override
    public int getContentLength() {
	try {
	    getResponse();
	} catch (IOException e) {
	}
	return contentLength;
    }

    @Override
    public String getContentType() {
	try {
	    getResponse();
	} catch (IOException e) {
	}
	return contentType;
    }

    @Override
    public String getContentEncoding() {
	try {
	    getResponse();
	} catch (IOException e) {
	}
	return contentEncoding;
    }

    @Override
    public long getExpiration() {
	try {
	    getResponse();
	} catch (IOException e) {
	}
	return expiration;
    }

    @Override
    public long getDate() {
	try {
	    getResponse();
	} catch (IOException e) {
	}
	return date;
    }

    @Override
    public long getLastModified() {
	try {
	    getResponse();
	} catch (IOException e) {
	}
	return lastModified;
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
	try {
	    getResponse();
	} catch (IOException e) {
	}
	return headerFields;
    }

    @Override
    public String getHeaderField(String header) {
	for (Map.Entry<String, List<String>> entry : getHeaderFields().entrySet()) {
	    if (header.equalsIgnoreCase(entry.getKey())) {
		return entry.getValue().get(0);
	    }
	}
	return null;
    }

    @Override
    public int getHeaderFieldInt(String header, int def) {
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
	String s = getHeaderField(header);
	if (s != null) {
	    try {
		return RFC822.valueOf(s);
	    } catch (IllegalArgumentException e) {
	    }
	}
	return def;
    }

    @Override
    public String getHeaderFieldKey(int index) {
	try {
	    getResponse();
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
	    getResponse();
	} catch (IOException e) {
	}
	if (orderedHeaderFields.size() > index) {
	    return orderedHeaderFields.get(index).value();
	} else {
	    return null;
	}
    }

    /**
     * Closing the resulting stream will automatically reset this connection.
     */
    @Override
    public InputStream getInputStream() throws IOException {
	try {
	    getResponse();
	} catch (IOException e) {
	}
	if (responseCode == HTTP_OK) {
	    return responseData;
	} else {
	    throw new IOException("Response error: " + responseCode);
	}
    }

    /**
     * Closing this stream will automatically reset this connection.
     */
    @Override
    public InputStream getErrorStream() {
	try {
	    getResponse();
	} catch (IOException e) {
	}
	if (responseCode == HTTP_OK) {
	    return null;
	} else {
	    return responseData;
	}
    }

    @Override
    public int getResponseCode() throws IOException {
	try {
	    getResponse();
	} catch (IOException e) {
	}
	return responseCode;
    }

    @Override
    public String getResponseMessage() throws IOException {
	try {
	    getResponse();
	} catch (IOException e) {
	}
	return responseMessage;
    }
}
