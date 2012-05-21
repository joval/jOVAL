// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.cisco.ios;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.ios.GlobalObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.ios.GlobalItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.io.PerishableReader;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * Provides Cisco IOS Global OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class GlobalAdapter implements IAdapter {
    private IIosSession session;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IIosSession) {
	    this.session = (IIosSession)session;
	    classes.add(GlobalObject.class);
	}
	return classes;
    }

    private static final String BUILDING = "Building configuration...";
    private static final String CURRENT = "Current configuration";

    public Collection<GlobalItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException, OvalException {
	GlobalObject gObj = (GlobalObject)obj;
	EntityObjectStringType globalCommand = gObj.getGlobalCommand();
	OperationEnumeration op = globalCommand.getOperation();

	Collection<GlobalItem> items = new Vector<GlobalItem>();
	String command = (String)globalCommand.getValue();
	List<String> lines = null;
	try {
	    lines = session.getTechSupport().getLines(ITechSupport.GLOBAL);
	} catch (NoSuchElementException e) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_TECH_SHOW, ITechSupport.GLOBAL);
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}

	try {
	    Pattern p = null;
	    if (op == OperationEnumeration.PATTERN_MATCH) {
		p = Pattern.compile(command);
	    }
	    for (String line : lines) {
		if (isGlobalCommand(line)) {
		    boolean add = false;

		    switch(op) {
		      case EQUALS:
			add = line.equals(command);
			break;

		      case NOT_EQUAL:
			add = !line.equals(command);
			break;

		      case PATTERN_MATCH:
			add = p.matcher(line).find();
			break;

		      default:
			String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
			throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		    }

		    if (add) {
			GlobalItem item = Factories.sc.ios.createGlobalItem();
			EntityItemStringType globalCommandType = Factories.sc.core.createEntityItemStringType();
			globalCommandType.setValue(line);
			item.setGlobalCommand(globalCommandType);
			items.add(item);
		    }
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

    private boolean isGlobalCommand(String line) {
	return line.length() > 0 &&		// not an empty line
	       !line.startsWith(BUILDING) &&	// not a remark
	       !line.startsWith(CURRENT) &&	// not a remark
	       !line.startsWith("!") &&		// not a comment
	       !line.startsWith(" ");		// global context command
    }
}
