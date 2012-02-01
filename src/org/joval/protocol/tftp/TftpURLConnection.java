// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.protocol.tftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.cal10n.LocLogger;

import org.apache.commons.net.tftp.TFTP;
import org.apache.commons.net.tftp.TFTPClient;

import org.joval.intf.util.ILoggable;
import org.joval.io.StreamTool;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * URLConnection subclass implementing RFC783 (TFTP protocol). Only supports read requests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TftpURLConnection extends URLConnection implements Runnable, ILoggable {
    // Relevant protected fields are:
    //
    // boolean connected
    // boolean doInput
    // boolean doOutput
    // URL url

    private TFTPClient client;
    private LocLogger logger;
    private int timeout = 5000;
    private InetAddress host;
    private int port;
    private PipedInputStream in;
    private PipedOutputStream out;

    public TftpURLConnection(URL url) {
	super(url);
	client = new TFTPClient();
	logger = JOVALSystem.getLogger();
    }

    // URLConnection overrides

    public void connect() throws IOException {
	host = InetAddress.getByName(url.getHost());
	if (url.getPort() == -1) {
	    port = TFTP.DEFAULT_PORT;
	} else {
	    port = url.getPort();
	}
	client.open();
	connected = true;
    }

    public InputStream getInputStream() throws IOException {
	if (!connected) {
	    connect();
	}
	out = new PipedOutputStream();
	in = new PipedInputStream(out);
	new Thread(this, "TFTP Client").start();
	return in;
    }

    public void setConnectTimeout(int timeout) {
	this.timeout = timeout;
    }

    // Implement Runnable

    public void run() {
	try {
	    if (0 == client.receiveFile(url.getPath(), TFTP.OCTET_MODE, out, host, port)) {
		logger.warn(JOVALMsg.ERROR_TFTP, url.getPath());
	    }
	} catch (IOException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    try {
		out.close();
	    } catch (IOException e) {
		logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    connected = false;
	}
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }
}
