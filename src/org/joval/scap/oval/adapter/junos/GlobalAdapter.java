// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.junos;

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
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
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

    public Collection<GlobalItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	GlobalObject gObj = (GlobalObject)obj;
	EntityObjectStringType globalCommand = gObj.getGlobalCommand();
	OperationEnumeration op = globalCommand.getOperation();

	Collection<GlobalItem> items = new Vector<GlobalItem>();
	String command = (String)globalCommand.getValue();
	List<String> lines = null;
	try {
	    try {
		lines = session.getSupportInformation().getLines(ISupportInformation.GLOBAL);
	    } catch (NoSuchElementException e) {
		lines = SafeCLI.multiLine(ISupportInformation.GLOBAL, session, readTimeout);
	    }
	} catch (Exception e) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_JUNOS_SHOW, ISupportInformation.GLOBAL);
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}
	try {
	    Pattern p = null;
	    if (op == OperationEnumeration.PATTERN_MATCH) {
		p = Pattern.compile(command);
	    }
	    for (String line : lines) {
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
		    GlobalItem item = Factories.sc.junos.createGlobalItem();
		    EntityItemStringType globalCommandType = Factories.sc.core.createEntityItemStringType();
		    globalCommandType.setValue(line);
		    item.setGlobalCommand(globalCommandType);
		    items.add(item);
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
}
