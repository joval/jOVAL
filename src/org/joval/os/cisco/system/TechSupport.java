// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.cisco.system;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.cisco.system.ITechSupport;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * A structure that stores the result from the "show tech-support" command.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TechSupport implements ITechSupport {
    private static final String CRLF = "\r\n";

    private Hashtable<String, List<String>> data = new Hashtable<String, List<String>>();
    private LocLogger logger;

    /**
     * Load tech-support information from a stream source.
     */
    public TechSupport(InputStream in) throws IOException {
	logger = JOVALMsg.getLogger();
	BufferedReader br = null;
	try {
	    br = new BufferedReader(new InputStreamReader(in));
	    List<String> lines = new Vector<String>();
	    String line = null;
	    while ((line = br.readLine()) != null) {
		lines.add(line);
	    }
	    load(lines);
	} finally {
	    if (br != null) {
		try {
		    br.close();
		} catch (IOException e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION, e));
		}
	    }
	}
    }

    /**
     * Gather tech-support information from the session.
     */
    public TechSupport(IIosSession session) throws Exception {
	logger = session.getLogger();
	long readTimeout = session.getProperties().getLongProperty(IIosSession.PROP_READ_TIMEOUT);
	load(SafeCLI.multiLine("show tech-support", session, readTimeout));
    }

    // Implement ITechSupport

    public Collection<String> getShowSubcommands() {
	HashSet<String> subcommands = new HashSet<String>();
	for (String heading : getHeadings()) {
	    if (heading.toLowerCase().startsWith("show")) {
		subcommands.add(heading);
	    }
	}
	return subcommands;
    }

    public Collection<String> getHeadings() {
	return data.keySet();
    }

    public List<String> getLines(String heading) throws NoSuchElementException {
	List<String> list = data.get(heading);
	if (list == null) {
	    throw new NoSuchElementException(heading);
	} else {
	    return list;
	}
    }

    public String getData(String heading) throws NoSuchElementException {
	StringBuffer sb = new StringBuffer();
	for (String line : getLines(heading)) {
	    if (sb.length() > 0) {
		sb.append(CRLF);
	    }
	    sb.append(line);
	}
	String s = sb.toString();
	if (s.endsWith(CRLF)) {
	    return s;
	} else {
	    return sb.append(CRLF).toString();
	}
    }

    // Private

    /**
     * Populate the structure from a sequential list of lines.
     */
    protected void load(List<String> lines) {
	String heading = null;
	List<String> body = null;
	for (String line : lines) {
	    if (isHeading(line)) {
		if (heading != null) {
		    data.put(heading, body);
		}
		heading = getHeading(line);
		body = new Vector<String>();
	    } else if (heading == null) {
		if (line.length() > 0) {
		    logger.debug(JOVALMsg.ERROR_IOS_TECH_ORPHAN, line);
		}
	    } else {
		if (body.size() == 0 && line.length() == 0) {
		    // skip empty lines under header
		} else {
		    body.add(line);
		}
	    }
	}
	if (heading != null && body.size() > 0) {
	    data.put(heading, body);
	}
    }

    private static final int DASHLEN = DASHES.length();

    private boolean isHeading(String line) {
	return line.startsWith(DASHES) && line.endsWith(DASHES) && line.length() > (2*DASHLEN);
    }

    private String getHeading(String line) {
	return line.substring(DASHLEN, line.length() - DASHLEN).trim();
    }
}
