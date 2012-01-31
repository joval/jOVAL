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
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.cal10n.LocLogger;

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
    static final short OP_WRQ	= 1;
    static final short OP_RRQ	= 2;
    static final short OP_DATA	= 3;
    static final short OP_ACK	= 4;
    static final short OP_ERROR	= 5;

    static final short ERROR_UNDEFINED		= 0;
    static final short ERROR_FILE_NOT_FOUND	= 1;
    static final short ERROR_ACCESS_VIOLATION	= 2;
    static final short ERROR_DISK_FULL		= 3;
    static final short ERROR_ILLEGAL_OPERATION	= 4;
    static final short ERROR_UNKNOWN_ID		= 5;
    static final short ERROR_FILE_EXISTS	= 6;
    static final short ERROR_NO_SUCH_USER	= 7;

    static final int DEFAULT_PORT = 69;

    enum Mode {
	ASCII("netascii"),
	OCTET("octet"),
	MAIL("mail");

	String value;

	Mode(String value) {
	    this.value = value;
	}

	byte[] getBytes() {
	    return value.getBytes();
	}

	String value() {
	    return value;
	}
    }

    static final int DATA_BLOCK_LEN = 512;
    static final int DATA_PACKET_LEN = DATA_BLOCK_LEN + 4;

    // Relevant protected fields are:
    //
    // boolean connected
    // boolean doInput
    // boolean doOutput
    // URL url

    private LocLogger logger;
    private InetAddress destination;
    private int port;
    private DatagramSocket sock;
    private int timeout = 5000;
    private PipedInputStream in;
    private PipedOutputStream out;

    TftpURLConnection(URL url) {
	super(url);
	logger = JOVALSystem.getLogger();
    }

    // URLConnection overrides

    public void connect() throws IOException {
	destination = InetAddress.getByName(url.getHost());
	if (url.getPort() == -1) {
	    port = DEFAULT_PORT;
	} else {
	    port = url.getPort();
	}
	sock = new DatagramSocket();
	sock.setSoTimeout(timeout);
	sock.connect(destination, port);
	connected = true;
    }

    public InputStream getInputStream() throws IOException {
	if (!connected) {
	    connect();
	}
	TftpPacket request = new TftpPacket(OP_RRQ, url.getPath(), Mode.OCTET);
System.out.println("DAS path: " + url.getPath());
request.debugPrint(System.out);
	byte[] buff = request.getBytes();
	DatagramPacket packet = new DatagramPacket(buff, buff.length, destination, port);
	sock.send(packet);
	in = new PipedInputStream();
	out = new PipedOutputStream(in);
	new Thread(this).start();
	return in;
    }

    public void setConnectTimeout(int timeout) {
	this.timeout = timeout;
    }

    // Implement Runnable

    public void run() {
	try {
	    while(true) {
		//
		// Read the next data packet from the socket
		//
		byte[] buff = new byte[DATA_PACKET_LEN];
		DatagramPacket data = new DatagramPacket(buff, DATA_PACKET_LEN);
		sock.receive(data);
		short dataLen = (short)data.getLength();
		TftpPacket dataPacket = new TftpPacket(buff, dataLen);
System.out.println("DAS got blockNum " + dataPacket.blockNum);
		out.write(buff, 0, dataLen);

		//
		// Write the ack packet
		//
		TftpPacket ackPacket = new TftpPacket(OP_ACK, dataPacket.blockNum);
		buff = ackPacket.getBytes();
		DatagramPacket ack = new DatagramPacket(buff, buff.length, destination, port);
		sock.send(ack);
		if (dataLen < DATA_BLOCK_LEN) {
		    break;
		}
	    }
	} catch (IOException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    try {
		sock.close();
		out.close();
		connected = false;
	    } catch (IOException e) {
		logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Private

    class TftpPacket {
	short opcode, blockNum, errCode; // 2 bytes
	String fname, errMsg;
	Mode mode;
	byte[] data;

	/**
	 * Create a RREQ or WREQ packet.
	 */
	TftpPacket(short opcode, String fname, Mode mode) {
	    this.opcode = opcode;
	    this.fname = fname;
	    this.mode = mode;
	}

	/**
	 * Create a DATA packet.
	 */
	TftpPacket(byte[] buff, int len) {
	    int i=0;
	    opcode = (short)(((buff[i++] & 0xFF) >> 8) | (buff[i++] & 0xFF));
	    blockNum = (short)(((buff[i++] & 0xFF) >> 8) | (buff[i++] & 0xFF));
	    data = new byte[len - i];
	    System.arraycopy(buff, i, data, 0, data.length);
	}

	/**
	 * Create an ACK packet.
	 */
	TftpPacket(short opcode, short blockNum) {
	    this.opcode = opcode;
	    this.blockNum = blockNum;
	}

	byte[] getBytes() {
	    int len = 0;
	    byte[] buff = new byte[1024];

	    switch(opcode) {
	      case OP_WRQ:
	      case OP_RRQ: {
		buff[len++] = (byte)((opcode >> 8) & 0xFF);
		buff[len++] = (byte)(opcode & 0xFF);
		byte[] temp = fname.getBytes();
		System.arraycopy(temp, 0, buff, len, temp.length);
		len += temp.length;
		buff[len++] = 0;
		temp = mode.getBytes();
		System.arraycopy(temp, 0, buff, len, temp.length);
		len += temp.length;
		buff[len++] = 0;
		break;
	      }

	      case OP_DATA:
		buff[len++] = (byte)((opcode >> 8) & 0xFF);
		buff[len++] = (byte)(opcode & 0xFF);
		buff[len++] = (byte)((blockNum >> 8) & 0xFF);
		buff[len++] = (byte)(blockNum & 0xFF);
		System.arraycopy(data, 0, buff, len, data.length);
		len += data.length;
		break;

	      case OP_ACK:
		buff[len++] = (byte)((opcode >> 8) & 0xFF);
		buff[len++] = (byte)(opcode & 0xFF);
		buff[len++] = (byte)((blockNum >> 8) & 0xFF);
		buff[len++] = (byte)(blockNum & 0xFF);
		break;

	      case OP_ERROR: {
		buff[len++] = (byte)((opcode >> 8) & 0xFF);
		buff[len++] = (byte)(opcode & 0xFF);
		buff[len++] = (byte)((errCode >> 8) & 0xFF);
		buff[len++] = (byte)(errCode & 0xFF);
		byte[] temp = errMsg.getBytes();
		System.arraycopy(temp, 0, buff, len, temp.length);
		len += temp.length;
		buff[len++] = 0;
		break;
	      }
	    }

	    byte[] b = new byte[len];
	    System.arraycopy(buff, 0, b, 0, len);
	    return b;
	}

	void debugPrint(PrintStream out) {
	    out.println("--begin TFTP packet--");
	    StreamTool.hexDump(getBytes(), System.out);
	    out.println("--end TFTP packet--");
	}
    }
}
