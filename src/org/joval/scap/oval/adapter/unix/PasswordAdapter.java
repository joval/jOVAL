// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.unix.PasswordObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.unix.PasswordItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.os.unix.macos.DsclTool;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * Collects items for unix:password_objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PasswordAdapter implements IAdapter {
    private IUnixSession session;
    private Hashtable<String, PasswordItem> passwordMap;
    private Hashtable<String, Exception> errors;
    private List<String> errorMessages;
    private boolean initialized;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    passwordMap = new Hashtable<String, PasswordItem>();
	    errors = new Hashtable<String, Exception>();
	    errorMessages = new Vector<String>();
	    initialized = false;
	    classes.add(PasswordObject.class);
	}
	return classes;
    }

    public Collection<PasswordItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (errorMessages.size() > 0) {
	    for (String error : errorMessages) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(error);
		rc.addMessage(msg);
	    }
	}

	Collection<PasswordItem> items = new Vector<PasswordItem>();
	PasswordObject pObj = (PasswordObject)obj;
	EntityObjectStringType usernameType = pObj.getUsername();
	try {
	    String username = (String)usernameType.getValue();
	    OperationEnumeration op = usernameType.getOperation();
	    switch(op) {
	      case EQUALS:
		items.add(getPasswordItem(username, rc));
		break;

	      case NOT_EQUAL:
		for (String s : getUsernames()) {
		    if (!s.equals(username)) {
			items.add(getPasswordItem(s, rc));
		    }
		}
		break;

	      case PATTERN_MATCH: {
		Pattern p = Pattern.compile(username);
		for (String s : getUsernames()) {
		    if (p.matcher(s).find()) {
			items.add(getPasswordItem(s, rc));
		    }
		}
		break;
	      }

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (PatternSyntaxException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (NoSuchElementException e) {
	    // Ignore
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	}
	return items;
    }

    // Internal

    private Collection<String> getUsernames() {
	if (!initialized) {
	    loadPasswords();
	}
	return passwordMap.keySet();
    }

    private PasswordItem getPasswordItem(String username, IRequestContext rc) throws Exception {
	if (!initialized) {
	    loadPasswords();
	}
	if (passwordMap.containsKey(username)) {
	    // Log any non-fatal errors for this username
	    if (errors.containsKey(username)) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(errors.get(username).getMessage());
		rc.addMessage(msg);
	    }
	    return passwordMap.get(username);
	} else if (errors.containsKey(username)) {
	    throw errors.get(username);
	} else {
	    throw new NoSuchElementException(username);
	}
    }

    private void loadPasswords() {
	try {
	    List<String> lines = null;
	    switch(session.getFlavor()) {
	      //
	      // On Mac, use dscl to get the user list, and the id command to make a fake /etc/passwd file.
	      //
	      case MACOSX: {
		lines = new Vector<String>();
		for (String username : new DsclTool(session).getUsers()) {
		    String line = SafeCLI.exec("id -P " + username, session, IUnixSession.Timeout.S);
		    List<String> tokens = StringTools.toList(StringTools.tokenize(line, ":"));
		    //
		    // Strip tokens 4-6, whose meanings are unknown...
		    //
		    if (tokens.size() == 10) {
			StringBuffer sb = new StringBuffer();
			sb.append(tokens.get(0)).append(":");
			sb.append(tokens.get(1)).append(":");
			sb.append(tokens.get(2)).append(":");
			sb.append(tokens.get(3)).append(":");
			sb.append(tokens.get(7)).append(":");
			sb.append(tokens.get(8)).append(":");
			sb.append(tokens.get(9));
			lines.add(sb.toString());
		    } else {
			lines.add(line);
		    }
		}
		break;
	      }

	      //
	      // On Linux and Solaris, use getent to interact with NIS, LDAP or just the /etc/passwd file, as appropriate.
	      //
	      case LINUX:
	      case SOLARIS:
		lines = SafeCLI.multiLine("getent passwd", session, IUnixSession.Timeout.S);
		break;

	      //
	      // By default, read in the contents of /etc/passwd ... which is famously world-readable.
	      //
	      default:
		try {
		    IFile passwd = session.getFilesystem().getFile("/etc/passwd");
		    BufferedReader reader = new BufferedReader(new InputStreamReader(passwd.getInputStream()));
		    lines = new Vector<String>();
		    String line = null;
		    while ((line = reader.readLine()) != null) {
			lines.add(line);
		    }
		} catch (IOException e) {
		    session.getLogger().warn(JOVALMsg.ERROR_IO, "/etc/passwd", e.getMessage());
		    errorMessages.add(e.getMessage());
		}
		break;
	    }

	    //
	    // Create the basic PasswordItems
	    //
	    for (String line : lines) {
		if (line.startsWith("#") || line.trim().length() == 0) {
		    continue;
		}
		List<String> tokens = StringTools.toList(StringTools.tokenize(line, ":", false));
		if (tokens.size() == 7) {
		    int i=0;
		    PasswordItem item = Factories.sc.unix.createPasswordItem();
		    EntityItemStringType username = Factories.sc.core.createEntityItemStringType();
		    String usernameString = tokens.get(i++);
		    username.setValue(usernameString);
		    item.setUsername(username);

		    EntityItemStringType password = Factories.sc.core.createEntityItemStringType();
		    String passwordString = tokens.get(i++); // x
		    password.setValue(passwordString);
		    item.setPassword(password);

		    EntityItemIntType userId = Factories.sc.core.createEntityItemIntType();
		    userId.setValue(tokens.get(i++));
		    userId.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    item.setUserId(userId);

		    EntityItemIntType groupId = Factories.sc.core.createEntityItemIntType();
		    groupId.setValue(tokens.get(i++));
		    groupId.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    item.setGroupId(groupId);

		    EntityItemStringType gcos = Factories.sc.core.createEntityItemStringType();
		    gcos.setValue(tokens.get(i++));
		    item.setGcos(gcos);

		    EntityItemStringType homeDir = Factories.sc.core.createEntityItemStringType();
		    homeDir.setValue(tokens.get(i++));
		    item.setHomeDir(homeDir);

		    EntityItemStringType loginShell = Factories.sc.core.createEntityItemStringType();
		    loginShell.setValue(tokens.get(i++));
		    item.setLoginShell(loginShell);

		    passwordMap.put(usernameString, item);
		} else {
		    session.getLogger().warn(JOVALMsg.ERROR_PASSWD_LINE, line);
		}
	    }

	    //
	    // For each user, collect the last login time.
	    //
	    long systime = System.currentTimeMillis();
	    try {
		systime = session.getTime();
	    } catch (Exception e) {
		errorMessages.add(e.getMessage());
		session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    for (String username : passwordMap.keySet()) {
		setLastLogin(username, systime);
	    }
	} catch (Exception e) {
	    errorMessages.add(e.getMessage());
	    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	initialized = true;
    }

    /**
     * Set the last_login entity for the item associated with this user, using the supplied system time as a reference.
     */
    private void setLastLogin(String username, long systime) {
	EntityItemIntType lastLogin = Factories.sc.core.createEntityItemIntType();
	lastLogin.setDatatype(SimpleDatatypeEnumeration.INT.value());
	lastLogin.setStatus(StatusEnumeration.DOES_NOT_EXIST); // default, in case it's not found/handled below
	try {
	    switch(session.getFlavor()) {
	      case MACOSX:
	      case SOLARIS:
		Date now = new Date(systime);
		for (String line : SafeCLI.multiLine("last -1 " + username, session, IUnixSession.Timeout.M)) {
		    if (line.startsWith(username)) {
			StringTokenizer tok = new StringTokenizer(line);
			if (tok.countTokens() > 4) {
			    tok.nextToken(); // username
			    tok.nextToken(); // tty
			    tok.nextToken(); // host:port
			    String rest = tok.nextToken("\n").trim();
			    int ptr = rest.indexOf("-");
			    if (ptr == -1) {
				ptr = rest.indexOf("still");
			    }
			    if (ptr > 0) {
				SimpleDateFormat sdf = null;
				String datestr = rest.substring(0, ptr).trim();
				boolean adjust = true;
				switch(datestr.length()) {
				  case 12: // hack, in case the host was not listed!
				    sdf = new SimpleDateFormat("MMM dd HH:mm");
				    break;
				  case 16:
				    sdf = new SimpleDateFormat("EEE MMM dd HH:mm");
				    break;
				  case 24:
				    sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
				    adjust = false;
				    break;
				  default:
				    throw new ParseException(datestr, 0);
				}
				if (sdf != null) {
				    long secs = sdf.parse(datestr).getTime()/1000L;
				    if (adjust) {
					//
					// DAS: leap year handling is TBD
					//
					long realnowsecs = now.getTime()/1000L;
					long nowsecs = sdf.parse(sdf.format(now)).getTime()/1000L;
					long adjustedSecs = 0;
					if (secs > nowsecs) {
					    // last login happened last year
					    adjustedSecs = realnowsecs - 31536000 + secs;
					} else {
					    adjustedSecs = realnowsecs - nowsecs + secs;
					}
					lastLogin.setValue(Long.toString(adjustedSecs));
				    } else {
					lastLogin.setValue(Long.toString(secs));
				    }
				    lastLogin.setStatus(StatusEnumeration.EXISTS);
				    break;
				}
			    }
			}
		    }
		}
		break;
 
	      case LINUX: {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
		for (String line : SafeCLI.multiLine("last -n 1 -F " + username, session, IUnixSession.Timeout.M)) {
		    if (line.startsWith(username)) {
			StringTokenizer tok = new StringTokenizer(line);
			if (tok.countTokens() > 4) {
			    tok.nextToken(); // username
			    tok.nextToken(); // tty
			    tok.nextToken(); // host:port
			    String rest = tok.nextToken("\n").trim();
			    if (rest.length() > 24) {
				long secs = sdf.parse(rest.substring(0, 24)).getTime()/1000L;
				lastLogin.setValue(Long.toString(secs));
				lastLogin.setStatus(StatusEnumeration.EXISTS);
				break;
			    }
			}
		    }
		}
		break;
	      }

	      case AIX: {
		String command = "lsuser -a time_last_login " + username;
		for (String line : SafeCLI.multiLine(command, session, IUnixSession.Timeout.S)) {
		    if (line.startsWith(username)) {
			int ptr = line.indexOf("=");
			if (ptr > 0) {
			    String tm = line.substring(ptr+1);
			    lastLogin.setValue(tm);
			    lastLogin.setStatus(StatusEnumeration.EXISTS);
			    break;
			}
		    }
		}
		break;
	      }

	      default:
		lastLogin.setStatus(StatusEnumeration.NOT_COLLECTED);
		break;
	    }
	} catch (Exception e) {
	    lastLogin.setStatus(StatusEnumeration.ERROR);
	    errors.put(username, e);
	}
	passwordMap.get(username).setLastLogin(lastLogin);
    }
}
