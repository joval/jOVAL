// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.cisco.ios;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
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
import org.joval.io.PerishableReader;
import org.joval.oval.NotCollectableException;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Provides Cisco IOS Global OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class GlobalAdapter implements IAdapter {
    IIosSession session;

    public GlobalAdapter(IIosSession session) {
	this.session = session;
    }

    // Implement IAdapter

    private static Class[] objectClasses = {GlobalObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public boolean connect() {
	return session != null;
    }

    public void disconnect() {
    }

    private static final String BUILDING = "Building configuration...";
    private static final String CURRENT = "Current configuration";

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc)
	     throws NotCollectableException, OvalException {

	GlobalObject gObj = (GlobalObject)rc.getObject();
	EntityObjectStringType globalCommand = gObj.getGlobalCommand();
	OperationEnumeration op = globalCommand.getOperation();

	List<String> lines = null;
	Collection<String> commands = new Vector<String>();
	try {
	    lines = session.getTechSupport().getData(ITechSupport.GLOBAL);

	    if (globalCommand.isSetVarRef()) {
		commands.addAll(rc.resolve(globalCommand.getVarRef()));
	    } else {
		commands.add((String)globalCommand.getValue());
	    }
	} catch (ResolveException e) {
	    throw new OvalException(e);
	} catch (NoSuchElementException e) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_IOS_TECH_SHOW, ITechSupport.GLOBAL));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	Collection<GlobalItem> items = new Vector<GlobalItem>();
	for (String command : commands) {
	    for (String line : lines) {
		if (isGlobalCommand(line)) {
		    boolean add = false;

		    switch(op) {
		      case EQUALS:
			add = line.equals(command);
			break;

		      case PATTERN_MATCH:
			add = Pattern.matches(command, line);
			break;

		      default:
			throw new NotCollectableException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
		    }

		    if (add) {
			GlobalItem item = JOVALSystem.factories.sc.ios.createGlobalItem();
			EntityItemStringType globalCommandType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			globalCommandType.setValue(line);
			item.setGlobalCommand(globalCommandType);
			items.add(item);
		    }
		}
	    }
	}

	Collection<JAXBElement<? extends ItemType>> wrappedItems = new Vector<JAXBElement<? extends ItemType>>();
	for (GlobalItem item : items) {
	    wrappedItems.add(JOVALSystem.factories.sc.ios.createGlobalItem(item));
	}
	return wrappedItems;
    }

    private boolean isGlobalCommand(String line) {
	return line.length() > 0 &&		// not an empty line
	       !line.startsWith(BUILDING) &&	// not a remark
	       !line.startsWith(CURRENT) &&	// not a remark
	       !line.startsWith("!") &&		// not a comment
	       !line.startsWith(" ");		// global context command
    }
}
