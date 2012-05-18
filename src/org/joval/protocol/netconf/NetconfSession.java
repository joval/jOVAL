// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.protocol.netconf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.slf4j.cal10n.LocLogger;

import org.vngx.jsch.ChannelExec;
import org.vngx.jsch.ChannelSubsystem;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.Session;
import org.vngx.jsch.exception.JSchException;

import org.joval.intf.net.INetconf;
import org.joval.intf.util.ILoggable;
import org.joval.io.PerishableReader;
import org.joval.ssh.system.SshSession;
import org.joval.util.JOVALMsg;

/**
 * Basic implementation of RFC 4741, to fetch router configurations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class NetconfSession implements ILoggable {
    private static final String SUBSYS	= "netconf";
    private static final String MARKER	= "]]>]]>";
    private static final int ID_GET_CONFIG	= 101;
    private static final int ID_CLOSE_SESSION	= 102;

    private SshSession ssh;
    private long timeout;
    private LocLogger logger;
    private DocumentBuilder builder;

    public NetconfSession(SshSession ssh, long timeout) {
	this.ssh = ssh;
	this.timeout = timeout;
	logger = ssh.getLogger();
	try {
	    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	} catch (ParserConfigurationException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    public Document getConfig() throws Exception {
	Document config = builder.newDocument();
	MessageSender sender = null;

	try {
	    ssh.connect();
	    Session session = ssh.getJschSession();
	    ChannelSubsystem channel = (ChannelSubsystem)session.openChannel(ChannelType.SUBSYSTEM);
	    channel.setSubsystem(SUBSYS);
	    channel.connect();
	    InputStream in = channel.getInputStream();
	    OutputStream out = channel.getOutputStream();

	    sender = new MessageSender(out);
	    sender.start();

	    PerishableReader reader = PerishableReader.newInstance(in, timeout);
	    String data = null;
	    while((data = reader.readUntil(MARKER)) != null) {
		data = data.trim();
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
		    logger.info(JOVALMsg.STATUS_NETCONF_SESSIONID, sessionId);

		//
		// Process RPC response messages
		//
		} else if (tagName.equals("rpc-reply")) {
		    switch(Integer.parseInt(elt.getAttribute("message-id"))) {
		      //
		      // Process the response to the get-config request.
		      //
		      case ID_GET_CONFIG: {
			// First, get the data node from the reply
			NodeList dataNodes = elt.getElementsByTagName("data");
			if (dataNodes.getLength() > 0) {
			    Node dataNode = dataNodes.item(0);
			    //
			    // Next, copy all attributes from the rpc-reply to the new data element for our Document.
			    // This will catch any namespace declarations that would otherwise be lost in the import
			    // (and cause errors).
			    //
			    Element newData = config.createElement("data");
			    NamedNodeMap attrs = elt.getAttributes();
			    for (int i=0; i < attrs.getLength(); i++) {
				Node attr = attrs.item(i);
				newData.setAttribute(attr.getNodeName(), attr.getNodeValue());
			    }
			    //
			    // Finally, import the children of the data node, and add the new data element to our Document.
			    //
			    NodeList list = dataNode.getChildNodes();
			    for (int i=0; i < list.getLength(); i++) {
				newData.appendChild(config.importNode(list.item(i), true));
			    }
			    config.appendChild(newData);
			}
			break;
		      }

		      case ID_CLOSE_SESSION:
			channel.disconnect();
			break;
		    }
		}
	    }
	} finally {
	    if (sender != null) {
		sender.join();
	    }
	}

	return config;
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Private

    class MessageSender implements Runnable {
	OutputStream out;
	Thread t = null;

	MessageSender(OutputStream out) {
	    this.out = out;
	}

	public void start() {
	    t = new Thread(this);
	    t.start();
	}

	public void join() throws InterruptedException {
	    if (t != null) {
		t.join();
	    }
	}

	public void run() {
	    try {
		//
		// Say Hello
		//
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
