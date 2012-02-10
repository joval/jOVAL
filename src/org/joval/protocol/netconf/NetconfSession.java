// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.protocol.netconf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.vngx.jsch.ChannelExec;
import org.vngx.jsch.ChannelSubsystem;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.Session;
import org.vngx.jsch.exception.JSchException;

import org.joval.io.PerishableReader;
import org.joval.ssh.system.SshSession;

/**
 * Basic implementation of RFC 4741, to fetch router configurations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class NetconfSession {
    private static final String SUBSYS	= "netconf";
    private static final String MARKER	= "]]>]]>";
    private static final int ID_GET_CONFIG	= 101;
    private static final int ID_CLOSE_SESSION	= 102;

    private DocumentBuilderFactory factory;
    private SshSession ssh;

    public NetconfSession(SshSession ssh) {
	factory = DocumentBuilderFactory.newInstance();
	this.ssh = ssh;
    }

    public void getConfig(long timeout) {
	try {
	    ssh.connect();
	    Session session = ssh.getJschSession();
	    ChannelSubsystem channel = (ChannelSubsystem)session.openChannel(ChannelType.SUBSYSTEM);
	    channel.setSubsystem(SUBSYS);
	    channel.connect();
	    InputStream in = channel.getInputStream();
	    OutputStream out = channel.getOutputStream();

	    MessageSender sender = new MessageSender(out);
	    sender.start();

	    PerishableReader reader = PerishableReader.newInstance(in, timeout);
	    String data = null;

	    while((data = reader.readUntil(MARKER)) != null) {
		data = data.trim();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Element elt = builder.parse(new ByteArrayInputStream(data.getBytes())).getDocumentElement();
		String tagName = elt.getTagName();

		//
		// Process server hello
		//
		if (tagName.equals("hello")) {
		    String sessionId = null;
		    NodeList children = elt.getChildNodes();
		    for (int i=0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeName().equals("session-id")) {
			    sessionId = child.getTextContent();
			}
		    }
		    System.out.println("Session ID: " + sessionId);

		//
		// Process RPC response messages
		//
		} else if (tagName.equals("rpc-reply")) {
		    switch(Integer.parseInt(elt.getAttribute("message-id"))) {
		      case ID_GET_CONFIG:
			System.out.println(data);
			break;

		      case ID_CLOSE_SESSION:
			ssh.disconnect();
			break;
		    }
		}
	    }

	    sender.join();
	} catch (ParserConfigurationException e) {
	    throw new RuntimeException(e);
	} catch (JSchException e) {
	    e.printStackTrace();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	} catch (NumberFormatException e) {
	    e.printStackTrace();
	} catch (SAXException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    // Private

    class MessageSender implements Runnable {
	OutputStream out;
	Thread t;

	MessageSender(OutputStream out) {
	    this.out = out;
	}

	public void start() {
	    t = new Thread(this);
	    t.start();
	}

	public void join() throws InterruptedException {
	    t.join();
	}

	public void run() {
	    try {
		//
		// Say Hello
		//
		System.out.println("Writing hello");
		StringBuffer msg = new StringBuffer();
		msg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		msg.append("<hello><capabilities>");
		msg.append("<capability>urn:ietf:params:netconf:base:1.0</capability>");
		msg.append("</capabilities></hello>");
		msg.append(MARKER);
		out.write(msg.toString().getBytes());
		out.flush();

		//
		// Request the running config
		//
		System.out.println("Writing get-config");
		msg = new StringBuffer();
		msg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		msg.append("<rpc message-id=\"").append(Integer.toString(ID_GET_CONFIG)).append("\">");
		msg.append("<get-config><source><running/></source></get-config>");
		msg.append("</rpc>");
		msg.append(MARKER);
		out.write(msg.toString().getBytes());
		out.flush();

		//
		// Close the session
		//
		System.out.println("Writing close-message");
		msg = new StringBuffer();
		msg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		msg.append("<rpc message-id=\"").append(Integer.toString(ID_CLOSE_SESSION)).append("\">");
		msg.append("<close-session/>");
		msg.append("</rpc>");
		msg.append(MARKER);
		out.write(msg.toString().getBytes());
		out.flush();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }
}
