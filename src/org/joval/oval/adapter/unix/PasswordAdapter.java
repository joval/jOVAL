// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.unix;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
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

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.os.unix.macos.DsclTool;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
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
    private String error;
    private boolean initialized;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    passwordMap = new Hashtable<String, PasswordItem>();
	    error = null;
	    initialized = false;
	    classes.add(PasswordObject.class);
	}
	return classes;
    }

    public Collection<PasswordItem> getItems(ObjectType obj, IRequestContext rc) throws OvalException, CollectException {
	if (!initialized) {
	    loadPasswords();
	}

	if (error != null) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(error);
	    rc.addMessage(msg);
	}

	Collection<PasswordItem> items = new Vector<PasswordItem>();
	PasswordObject pObj = (PasswordObject)obj;
	EntityObjectStringType usernameType = pObj.getUsername();
	try {
	    String username = (String)usernameType.getValue();
	    OperationEnumeration op = usernameType.getOperation();
	    switch(op) {
	      case EQUALS:
		if (passwordMap.containsKey(username)) {
		    items.add(passwordMap.get(username));
		}
		break;

	      case NOT_EQUAL:
		for (String s : passwordMap.keySet()) {
		    if (!s.equals(username)) {
			items.add(passwordMap.get(s));
		    }
		}
		break;

	      case PATTERN_MATCH: {
		Pattern p = Pattern.compile(username);
		for (String s : passwordMap.keySet()) {
		    if (p.matcher(s).find()) {
			items.add(passwordMap.get(s));
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
	}
	return items;
    }

    // Internal

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
	      // By default, cat the contents of /etc/passwd, to leverage elevated privileges if necessary.
	      //
	      default:
		lines = SafeCLI.multiLine("cat /etc/passwd", session, IUnixSession.Timeout.S);
		break;
	    }

	    //
	    // Create the basic PasswordItems
	    //
	    for (String line : lines) {
		if (line.startsWith("#")) {
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
	    for (String username : passwordMap.keySet()) {
		PasswordItem item = passwordMap.get(username);
		EntityItemIntType lastLogin = Factories.sc.core.createEntityItemIntType();
		lastLogin.setDatatype(SimpleDatatypeEnumeration.INT.value());
		switch(session.getFlavor()) {
		  case MACOSX:
		  case SOLARIS:
		    Date now = new Date(session.getTime());
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
				      default: {
	    				MessageType msg = Factories.common.createMessageType();
	    				msg.setLevel(MessageLevelEnumeration.WARNING);
	    				msg.setValue("Unknown date string format: " + datestr);
	    				item.getMessage().add(msg);
					break;
				      }
				    }
				    if (sdf != null) {
					try {
					    long secs = sdf.parse(datestr).getTime()/1000L;
					    if (adjust) {
						//
						// DAS: doesn't handle leap years
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
					} catch (Exception e) {
					    lastLogin.setStatus(StatusEnumeration.ERROR);
	    				    MessageType msg = Factories.common.createMessageType();
	    				    msg.setLevel(MessageLevelEnumeration.ERROR);
	    				    msg.setValue(e.getMessage());
	    				    item.getMessage().add(msg);
					}
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
				    try {
					long secs = sdf.parse(rest.substring(0, 24)).getTime()/1000L;
					lastLogin.setValue(Long.toString(secs));
					lastLogin.setStatus(StatusEnumeration.EXISTS);
				    } catch (Exception e) {
					lastLogin.setStatus(StatusEnumeration.ERROR);
	    				MessageType msg = Factories.common.createMessageType();
	    				msg.setLevel(MessageLevelEnumeration.ERROR);
	    				msg.setValue(e.getMessage());
	    				item.getMessage().add(msg);
				    }
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
		if (!lastLogin.isSetStatus()) {
		    //
		    // User has never logged in.
		    //
		    lastLogin.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
		item.setLastLogin(lastLogin);
	    }
	} catch (Exception e) {
	    error = e.getMessage();
	    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	initialized = true;
    }
}
