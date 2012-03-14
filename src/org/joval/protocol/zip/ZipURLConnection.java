// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.protocol.zip;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * URLConnection subclass for generic ZIP files.  Only supports read requests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ZipURLConnection extends URLConnection {
    private URL innerURL;
    private URLConnection innerConnection;
    private String path;

    ZipURLConnection(URL url) throws MalformedURLException {
	super(url);
	String spec = url.toString();
	if (spec.startsWith("zip:")) {
	    int ptr = spec.indexOf("!/");
	    if (ptr > 4) {
		innerURL = new URL(spec.substring(4, ptr));
		path = spec.substring(ptr+2);
	    }
	}
	if (innerURL == null) {
	    throw new MalformedURLException(spec);
	}
    }

    // URLConnection overrides

    @Override
    public void connect() throws IOException {
	innerConnection = innerURL.openConnection();
	connected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException {
	if (!connected) {
	    connect();
	}
	ZipInputStream zin = new ZipInputStream(innerConnection.getInputStream());
	ZipEntry entry;
	while ((entry = zin.getNextEntry()) != null) {
	    if (entry.getName().equals(path)) {
		break;
	    }
	}
	if (entry == null) {
	    throw new FileNotFoundException(path);
	} else {
	    return zin;
	}
    }
}
