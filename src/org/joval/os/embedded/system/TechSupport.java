// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.embedded.system;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.system.ISession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Executes and stores the result from the "show tech-support" command.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TechSupport implements ITechSupport {
    private Hashtable<String, List<String>> data;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    public TechSupport(ISession session) throws Exception {
	long readTimeout = JOVALSystem.getLongProperty(JOVALSystem.PROP_IOS_READ_TIMEOUT);

	String heading = null;
	List<String> body = null;

	for (String line : SafeCLI.multiLine("show tech-support", session, readTimeout)) {
	    if (isHeading(line)) {
		if (heading != null) {
		    data.put(heading, body);
		}
		heading = getHeading(line);
		body = new Vector<String>();
	    }
	    if (heading == null) {
		session.getLogger().warn(JOVALMsg.ERROR_IOS_TECH_ORPHAN, line);
	    } else {
		body.add(line);
	    }
	}
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

    public List<String> getData(String heading) throws NoSuchElementException {
	List<String> list = data.get(heading);
	if (list == null) {
	    throw new NoSuchElementException(heading);
	} else {
	    return list;
	}
    }

    // Private

    private static final int DASHLEN = DASHES.length();

    private boolean isHeading(String line) {
	return line.startsWith(DASHES) && line.endsWith(DASHES) && line.length() > (2*DASHLEN);
    }

    private String getHeading(String line) {
	return line.substring(DASHLEN, line.length() - DASHLEN).trim();
    }
}
