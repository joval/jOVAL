// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.junos;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.junos.GlobalObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.junos.GlobalItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.juniper.system.IJunosSession;
import org.joval.intf.juniper.system.ISupportInformation;
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
 * Provides Juniper JUNOS Global OVAL items, by converting the hierarchical configuration into set commands.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class GlobalAdapter implements IAdapter {
    private IJunosSession session;
    private long readTimeout = 0;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IJunosSession) {
	    readTimeout = session.getProperties().getLongProperty(IJunosSession.PROP_READ_TIMEOUT);
	    this.session = (IJunosSession)session;
	    classes.add(GlobalObject.class);
	}
	return classes;
    }

    public Collection<GlobalItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException, OvalException {
	GlobalObject gObj = (GlobalObject)obj;
	EntityObjectStringType globalCommand = gObj.getGlobalCommand();
	OperationEnumeration op = globalCommand.getOperation();

	Collection<GlobalItem> items = new Vector<GlobalItem>();
	try {
	    String command = (String)globalCommand.getValue();
	    List<String> lines = null;
	    try {
		lines = session.getSupportInformation().getData(ISupportInformation.GLOBAL);
	    } catch (NoSuchElementException e) {
		try {
		    lines = SafeCLI.multiLine(ISupportInformation.GLOBAL, session, readTimeout);
		} catch (Exception e2) {
		    for (String heading : session.getSupportInformation().getHeadings()) {
			if (heading.startsWith(ISupportInformation.GLOBAL)) {
			    lines = session.getSupportInformation().getData(heading);
			}
		    }
		    if (lines == null) {
			throw e;
		    }
		}
	    }
	    for (String line : lines) {
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
		    GlobalItem item = Factories.sc.junos.createGlobalItem();
		    EntityItemStringType globalCommandType = Factories.sc.core.createEntityItemStringType();
		    globalCommandType.setValue(line);
		    item.setGlobalCommand(globalCommandType);
		    items.add(item);
		}
	    }
	} catch (NoSuchElementException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_TECH_SHOW, ISupportInformation.GLOBAL));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }
}
