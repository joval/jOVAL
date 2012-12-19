// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.macos;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.macos.Pwpolicy59Object;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.macos.Pwpolicy59Item;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.os.unix.macos.DsclTool;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.CollectException;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * Retrieves Pwpolicy59Items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Pwpolicy59Adapter implements IAdapter {
    private IUnixSession session;
    private DsclTool dscl;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    if (((IUnixSession)session).getFlavor() == IUnixSession.Flavor.MACOSX) {
		this.session = (IUnixSession)session;
		this.dscl = new DsclTool(this.session);
		classes.add(Pwpolicy59Object.class);
	    }
	}
	return classes;
    }

    public Collection<Pwpolicy59Item> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	Collection<Pwpolicy59Item> items = new Vector<Pwpolicy59Item>();
	Pwpolicy59Object pObj = (Pwpolicy59Object)obj;
	String value = (String)pObj.getTargetUser().getValue();
	OperationEnumeration op = pObj.getTargetUser().getOperation();
	switch(op) {
	  case EQUALS:
	    if (dscl.getUsers().contains(value)) {
		items.add(getItem(pObj, value));
	    }
	    break;

	  case NOT_EQUAL:
	    for (String username : dscl.getUsers()) {
		if (!value.equals(username)) {
		    items.add(getItem(pObj, username));
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    for (String username : dscl.getUsers(Pattern.compile(value))) {
		items.add(getItem(pObj, username));
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	return items;
    }

    private Pwpolicy59Item getItem(Pwpolicy59Object pObj, String targetUser) throws CollectException {
	Pwpolicy59Item item = Factories.sc.macos.createPwpolicy59Item();
	EntityItemStringType targetUserType = Factories.sc.core.createEntityItemStringType();
	targetUserType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	targetUserType.setValue(targetUser);
	item.setTargetUser(targetUserType);

	StringBuffer sb = new StringBuffer("pwpolicy -getpolicy -u ").append(targetUser);
	if (!pObj.getUsername().isNil()) {
	    OperationEnumeration op = pObj.getUsername().getValue().getOperation();
	    if (op == OperationEnumeration.EQUALS) {
		String value = SafeCLI.checkArgument((String)pObj.getUsername().getValue().getValue(), session);
		sb.append(" -a '").append(value).append("'");
		EntityItemStringType username = Factories.sc.core.createEntityItemStringType();
		username.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		username.setValue(value);
		item.setUsername(Factories.sc.macos.createPwpolicy59ItemUsername(username));
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}
	if (!pObj.getUserpass().isNil()) {
	    OperationEnumeration op = pObj.getUserpass().getValue().getOperation();
	    if (op == OperationEnumeration.EQUALS) {
		String value = SafeCLI.checkArgument((String)pObj.getUserpass().getValue().getValue(), session);
		sb.append(" -p '").append(value).append("'");
		EntityItemStringType userpass = Factories.sc.core.createEntityItemStringType();
		userpass.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		userpass.setValue(value);
		item.setUserpass(Factories.sc.macos.createPwpolicy59ItemUserpass(userpass));
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}
	if (!pObj.getDirectoryNode().isNil()) {
	    OperationEnumeration op = pObj.getDirectoryNode().getValue().getOperation();
	    if (op == OperationEnumeration.EQUALS) {
		String value = SafeCLI.checkArgument((String)pObj.getDirectoryNode().getValue().getValue(), session);
		sb.append(" -n '").append(value).append("'");
		EntityItemStringType directoryNode = Factories.sc.core.createEntityItemStringType();
		directoryNode.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		directoryNode.setValue(value);
		item.setDirectoryNode(Factories.sc.macos.createPwpolicy59ItemDirectoryNode(directoryNode));
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}

	try {
	    Hashtable<String, String> policies = new Hashtable<String, String>();
	    for (String line : SafeCLI.multiLine(sb.toString(), session, IUnixSession.Timeout.S)) {
		if (line.indexOf("=") > 0) {
		    for (String pair : StringTools.toList(StringTools.tokenize(line, " "))) {
			int ptr = pair.indexOf("=");
			if (ptr != -1) {
			    String key = pair.substring(0,ptr).trim();
			    String val = pair.substring(ptr+1).trim();
			    policies.put(key, val);
			}
		    }
		}
	    }

	    if (policies.containsKey("canModifyPasswordforSelf")) {
		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		type.setValue(policies.get("canModifyPasswordforSelf"));
		item.setCanModifyPasswordforSelf(type);
	    }

	    if (policies.containsKey("expirationDateGMT")) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		type.setValue(policies.get("expirationDateGMT"));
		item.setExpirationDateGMT(type);
	    }

	    if (policies.containsKey("hardExpireDateGMT")) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		type.setValue(policies.get("hardExpireDateGMT"));
		item.setHardExpireDateGMT(type);
	    }

	    if (policies.containsKey("maxChars")) {
		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
		type.setValue(policies.get("maxChars"));
		item.setMaxChars(type);
	    }

	    if (policies.containsKey("maxFailedLoginAttempts")) {
		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
		type.setValue(policies.get("maxFailedLoginAttempts"));
		item.setMaxFailedLoginAttempts(type);
	    }

	    if (policies.containsKey("maxMinutesOfNonUse")) {
		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
		type.setValue(policies.get("maxMinutesOfNonUse"));
		item.setMaxMinutesOfNonUse(type);
	    }

	    if (policies.containsKey("maxMinutesUntilChangePassword")) {
		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
		type.setValue(policies.get("maxMinutesUntilChangePassword"));
		item.setMaxMinutesUntilChangePassword(type);
	    }

	    if (policies.containsKey("maxMinutesUntilDisabled")) {
		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
		type.setValue(policies.get("maxMinutesUntilDisabled"));
		item.setMaxMinutesUntilDisabled(type);
	    }

	    if (policies.containsKey("minChars")) {
		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
		type.setValue(policies.get("minChars"));
		item.setMinChars(type);
	    }

	    if (policies.containsKey("minMinutesUntilChangePassword")) {
		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
		type.setValue(policies.get("minMinutesUntilChangePassword"));
		item.setMinMinutesUntilChangePassword(type);
	    }

	    if (policies.containsKey("minutesUntilFailedLoginReset")) {
		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
		type.setValue(policies.get("minutesUntilFailedLoginReset"));
		item.setMinutesUntilFailedLoginReset(type);
	    }

	    if (policies.containsKey("newPasswordRequired")) {
		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		type.setValue(policies.get("newPasswordRequired"));
		item.setNewPasswordRequired(type);
	    }

	    if (policies.containsKey("notGuessablePattern")) {
		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		type.setValue(policies.get("notGuessablePattern"));
		item.setNotGuessablePattern(type);
	    }

	    if (policies.containsKey("passwordCannotBeName")) {
		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		type.setValue(policies.get("passwordCannotBeName"));
		item.setPasswordCannotBeName(type);
	    }

	    if (policies.containsKey("requiresAlpha")) {
		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		type.setValue(policies.get("requiresAlpha"));
		item.setRequiresAlpha(type);
	    }

	    if (policies.containsKey("requiresMixedCase")) {
		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		type.setValue(policies.get("requiresMixedCase"));
		item.setRequiresMixedCase(type);
	    }

	    if (policies.containsKey("requiresNumeric")) {
		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		type.setValue(policies.get("requiresNumeric"));
		item.setRequiresNumeric(type);
	    }

	    if (policies.containsKey("requiresSymbol")) {
		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		type.setValue(policies.get("requiresSymbol"));
		item.setRequiresSymbol(type);
	    }

	    if (policies.containsKey("usingExpirationDate")) {
		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		type.setValue(policies.get("usingExpirationDate"));
		item.setUsingExpirationDate(type);
	    }

	    if (policies.containsKey("usingHardExpirationDate")) {
		EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
		type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		type.setValue(policies.get("usingHardExpirationDate"));
		item.setUsingHardExpirationDate(type);
	    }

	    if (policies.containsKey("usingHistory")) {
		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
		type.setValue(policies.get("usingHistory"));
		item.setUsingHistory(type);
	    }

	    item.setStatus(StatusEnumeration.EXISTS);
	} catch (Exception e) {
	    item.setStatus(StatusEnumeration.ERROR);
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    item.getMessage().add(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return item;
    }
}
