// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.protocol.zip;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.joval.util.JOVALMsg;

/**
 * URLConnection subclass for generic ZIP files.  Only supports read requests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ZipURLStreamHandler extends URLStreamHandler {
    public ZipURLStreamHandler() {
    }

    public URLConnection openConnection(URL u) throws IOException {
        if ("zip".equals(u.getProtocol())) {
            return new ZipURLConnection(u);
        } else {
            throw new MalformedURLException(JOVALMsg.getMessage(JOVALMsg.ERROR_PROTOCOL, u.getProtocol()));
        }
    }
}
