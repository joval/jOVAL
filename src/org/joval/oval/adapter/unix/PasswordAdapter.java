// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.unix;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

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

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException, CollectException {
	if (!initialized) {
	    loadPasswords();
	}

	if (error != null) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(error);
	    rc.addMessage(msg);
	}

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	PasswordObject pObj = (PasswordObject)rc.getObject();
	EntityObjectStringType usernameType = pObj.getUsername();
	try {
	    List<String> usernames = new Vector<String>();
	    if (usernameType.isSetVarRef()) {
		usernames.addAll(rc.resolve(usernameType.getVarRef()));
	    } else {
		usernames.add((String)usernameType.getValue());
	    }

	    for (String username : usernames) {
		OperationEnumeration op = usernameType.getOperation();
		switch(op) {
		  case EQUALS:
		    if (passwordMap.containsKey(username)) {
			items.add(Factories.sc.unix.createPasswordItem(passwordMap.get(username)));
		    }
		    break;

		  case NOT_EQUAL:
		    for (String s : passwordMap.keySet()) {
			if (!s.equals(username)) {
			    items.add(Factories.sc.unix.createPasswordItem(passwordMap.get(s)));
			}
		    }
		    break;

		  case PATTERN_MATCH: {
		    Pattern p = Pattern.compile(username);
		    for (String s : passwordMap.keySet()) {
			if (p.matcher(s).find()) {
			    items.add(Factories.sc.unix.createPasswordItem(passwordMap.get(s)));
			}
		    }
		    break;
		  }

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
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
	    for (String line : SafeCLI.multiLine("cat /etc/passwd", session, IUnixSession.Timeout.S)) {
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

		    EntityItemIntType lastLogin = Factories.sc.core.createEntityItemIntType();
		    lastLogin.setStatus(StatusEnumeration.NOT_COLLECTED);
		    lastLogin.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    item.setLastLogin(lastLogin);

		    passwordMap.put(usernameString, item);
		} else {
		    session.getLogger().warn(JOVALMsg.ERROR_PASSWD_LINE, line);
		}
	    }
	} catch (Exception e) {
	    error = e.getMessage();
	    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	initialized = true;
    }
}
