// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.cisco.ios;

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
import org.joval.intf.juniper.system.IJunosSession;
import org.joval.intf.juniper.system.ISupportInformation;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.io.PerishableReader;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * Provides Cisco IOS Global OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class GlobalAdapter implements IAdapter {
    IIosSession session;
    String showConfig;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IIosSession) {
	    this.session = (IIosSession)session;
	    if (session instanceof IJunosSession) {
		showConfig = ISupportInformation.GLOBAL;
	    } else {
		showConfig = ITechSupport.GLOBAL;
	    }
	    classes.add(GlobalObject.class);
	}
	return classes;
    }

    private static final String BUILDING = "Building configuration...";
    private static final String CURRENT = "Current configuration";

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws CollectException, OvalException {
	GlobalObject gObj = (GlobalObject)rc.getObject();
	EntityObjectStringType globalCommand = gObj.getGlobalCommand();
	OperationEnumeration op = globalCommand.getOperation();

	List<String> lines = null;
	Collection<String> commands = new Vector<String>();
	try {
	    lines = session.getTechSupport().getData(showConfig);

	    if (globalCommand.isSetVarRef()) {
		commands.addAll(rc.resolve(globalCommand.getVarRef()));
	    } else {
		commands.add((String)globalCommand.getValue());
	    }
	} catch (ResolveException e) {
	    throw new OvalException(e);
	} catch (NoSuchElementException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_TECH_SHOW, showConfig));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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
	}

	Collection<JAXBElement<? extends ItemType>> wrappedItems = new Vector<JAXBElement<? extends ItemType>>();
	for (GlobalItem item : items) {
	    wrappedItems.add(Factories.sc.ios.createGlobalItem(item));
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
