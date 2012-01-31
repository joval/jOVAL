// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.protocol.tftp;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Stream handler class for TFTP URLs.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Handler extends URLStreamHandler {
    protected URLConnection openConnection(URL url) {
	return new TftpURLConnection(url);
    }

    protected int getDefaultPort() {
	return TftpURLConnection.DEFAULT_PORT;
    }
}
