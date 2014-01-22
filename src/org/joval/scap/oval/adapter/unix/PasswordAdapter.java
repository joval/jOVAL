// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.unix.PasswordObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.unix.PasswordItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.macos.DsclTool;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for unix:password_objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PasswordAdapter implements IAdapter {
    private IUnixSession session;
    private Map<String, PasswordItem> passwordMap;
    private Map<String, String> errors;
    private List<String> errorMessages;
    private boolean initialized;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    passwordMap = new HashMap<String, PasswordItem>();
	    errors = new HashMap<String, String>();
	    errorMessages = new ArrayList<String>();
	    initialized = false;
	    classes.add(PasswordObject.class);
	} else {
	    notapplicable.add(PasswordObject.class);
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

	Collection<PasswordItem> items = new ArrayList<PasswordItem>();
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
		Pattern p = StringTools.pattern(username);
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

    /**
     * Retrieve a specific PasswordItem for a request. This decorates the request context with any error messages that
     * accumulated during the creation of the item.
     */
    private PasswordItem getPasswordItem(String username, IRequestContext rc) throws Exception {
	if (!initialized) {
	    loadPasswords();
	}
	if (passwordMap.containsKey(username)) {
	    PasswordItem item = passwordMap.get(username);
	    if (!item.isSetLastLogin()) {
		setLastLogin(item);
	    }
	    // Log any non-fatal errors for this username
	    if (errors.containsKey(username)) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(errors.get(username));
		rc.addMessage(msg);
	    }
	    return passwordMap.get(username);
	} else {
	    throw new NoSuchElementException(username);
	}
    }

    /**
     * Create the map of PasswordItems for all user accounts on the machine.
     */
    private void loadPasswords() {
	try {
	    //
	    // If possible, collect the last login times of all users at once
	    //
	    Map<String, EntityItemIntType> lastLogins = new HashMap<String, EntityItemIntType>();
	    switch(session.getFlavor()) {
	      case AIX: {
		String command = "lsuser -a time_last_login ALL";
		for (String line : SafeCLI.multiLine(command, session, IUnixSession.Timeout.S)) {
		    String username = new StringTokenizer(line).nextToken();
		    EntityItemIntType lastLogin = Factories.sc.core.createEntityItemIntType();
		    lastLogin.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    int ptr = line.indexOf("=");
		    if (ptr > 0) {
			lastLogin.setValue(line.substring(ptr+1));
			lastLogin.setStatus(StatusEnumeration.EXISTS);
		    } else {
			lastLogin.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		    }
		    lastLogins.put(username, lastLogin);
		}
		break;
	      }

	      //
	      // REMIND (DAS): Lastlog isn't recording GDM (Gnome) logins under the default configuration, but lastlog is
	      //               easier and faster to use than the 'last' command...
	      //
	      case LINUX: {
		int ptr = -1;
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
		for (String line : SafeCLI.multiLine("lastlog", session, IUnixSession.Timeout.M)) {
		    if (line.startsWith("Username")) {
			ptr = line.indexOf("Latest");
		    } else if (ptr != -1) {
			String username = line.substring(0, line.indexOf(" "));
			EntityItemIntType lastLogin = Factories.sc.core.createEntityItemIntType();
			lastLogin.setDatatype(SimpleDatatypeEnumeration.INT.value());
			if (line.toLowerCase().indexOf("never logged in") == -1) {
			    long secs = sdf.parse(line.substring(ptr)).getTime()/1000L;
			    lastLogin.setValue(Long.toString(secs));
			    lastLogin.setStatus(StatusEnumeration.EXISTS);
			} else {
			    lastLogin.setStatus(StatusEnumeration.DOES_NOT_EXIST);
			}
			lastLogins.put(username, lastLogin);
		    }
		}
	      }
	      break;
	    }

	    //
	    // Get the lines of the passwd file or generated equivalent
	    //
	    List<String> lines = null;
	    switch(session.getFlavor()) {
	      //
	      // On Mac, use dscl to get the user list, and the id command to make a fake /etc/passwd file.
	      //
	      case MACOSX: {
		lines = new ArrayList<String>();
		String cmd = "dscl localhost -list Search/Users | xargs -I{} id -P {}";
		for (String line : SafeCLI.multiLine(cmd, session, IUnixSession.Timeout.S)) {
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
		    lines = new ArrayList<String>();
		    String line = null;
		    while ((line = reader.readLine()) != null) {
			lines.add(line);
		    }
		} catch (IOException e) {
		    session.getLogger().warn(Message.ERROR_IO, "/etc/passwd", e.getMessage());
		    errorMessages.add(e.getMessage());
		}
		break;
	    }

	    //
	    // Create the PasswordItems
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

		    if (lastLogins.containsKey(usernameString)) {
			item.setLastLogin(lastLogins.get(usernameString));
		    }

		    passwordMap.put(usernameString, item);
		} else {
		    session.getLogger().warn(JOVALMsg.ERROR_PASSWD_LINE, line);
		}
	    }
	} catch (Exception e) {
	    errorMessages.add(e.getMessage());
	    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	initialized = true;
    }

    /**
     * Set the last_login entity for the item. This can be time-consuming, so it is done separately from initialization.
     */
    private void setLastLogin(PasswordItem item) {
	String username = (String)item.getUsername().getValue();
	EntityItemIntType lastLogin = Factories.sc.core.createEntityItemIntType();
	lastLogin.setDatatype(SimpleDatatypeEnumeration.INT.value());
	lastLogin.setStatus(StatusEnumeration.DOES_NOT_EXIST); // default, in case it's not found/handled below
	try {
	    switch(session.getFlavor()) {
	      case MACOSX:
	      case SOLARIS:
		Date now = new Date();
		try {
		    now = new Date(session.getTime());
		} catch (Exception e) {
		    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
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
 
	      default:
		lastLogin.setStatus(StatusEnumeration.NOT_COLLECTED);
		break;
	    }
	} catch (Exception e) {
	    lastLogin.setStatus(StatusEnumeration.ERROR);
	    errors.put(username, e.getMessage());
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	passwordMap.get(username).setLastLogin(lastLogin);
    }
}
