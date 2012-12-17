// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.macos;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.joval.intf.unix.system.IUnixSession;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * Utility class that uses the dscl command-line utility on MacOS X.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DsclTool {
    private IUnixSession session;
    private HashSet<String> users = null;

    public DsclTool(IUnixSession session) throws IllegalArgumentException {
	IUnixSession.Flavor flavor = session.getFlavor();
	switch(flavor) {
	  case MACOSX:
	    this.session = session;
	    break;
	  default:
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, flavor));
	}
    }

    public Collection<String> getUsers() {
	if (users == null) {
	    initUsers();
	}
	return users;
    }

    public Collection<String> getUsers(Pattern p) {
	HashSet<String> filtered = new HashSet<String>();
	for (String username : getUsers()) {
	    if (p.matcher(username).find()) {
		filtered.add(username);
	    }
	}
	return filtered;
    }

    // Private

    private void initUsers() {
	try {
	    users = new HashSet<String>();
	    for (String username : SafeCLI.multiLine("dscl localhost -list Search/Users", session, IUnixSession.Timeout.S)) {
		users.add(username.trim());
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }
}
