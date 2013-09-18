// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.aix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.aix.InterimFixObject;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.aix.EntityItemInterimFixStateType;
import scap.oval.systemcharacteristics.aix.InterimFixItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves items for AIX interimfix_objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class InterimFixAdapter implements IAdapter {
    private IUnixSession session;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.AIX) {
	    this.session = (IUnixSession)session;
	    classes.add(InterimFixObject.class);
	} else {
	    notapplicable.add(InterimFixObject.class);
	}
	return classes;
    }

    public Collection<InterimFixItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	InterimFixObject iObj = (InterimFixObject)obj;
	try {
	    Collection<InterimFixItem> items = new ArrayList<InterimFixItem>();
	    switch(iObj.getVuid().getOperation()) {
	      case EQUALS:
		items.add(getItem((String)iObj.getVuid().getValue(), rc));
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, iObj.getVuid().getOperation());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	    return items;
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new CollectException(e, FlagEnumeration.ERROR);
	}
    }

    // Private

    private InterimFixItem getItem(String vuid, IRequestContext rc) throws Exception {
	InterimFixItem item = Factories.sc.aix.createInterimFixItem();

	EntityItemStringType vuidType = Factories.sc.core.createEntityItemStringType();
	vuidType.setValue(vuid);
	item.setVuid(vuidType);

	StringBuffer cmd = new StringBuffer("/usr/sbin/emgr -l -u ");
	cmd.append(SafeCLI.checkArgument(vuid, session));
	List<String> lines = SafeCLI.multiLine(cmd.toString(), session, IUnixSession.Timeout.M);
	for (int i=0; i < lines.size(); i++) {
	    String line = lines.get(i);
	    if (line.startsWith("===")) {
		StringTokenizer tok = new StringTokenizer(lines.get(i+1));
		if (tok.countTokens() > 5) {
		    String id = tok.nextToken();
		    String state = tok.nextToken().toUpperCase();
		    String label = tok.nextToken();
		    String date = tok.nextToken();
		    String time = tok.nextToken();
		    String summary = tok.nextToken("\\n");

		    EntityItemStringType labelType = Factories.sc.core.createEntityItemStringType();
		    labelType.setValue(label);
		    item.setLabel(labelType);

		    EntityItemStringType abstractType = Factories.sc.core.createEntityItemStringType();
		    abstractType.setValue(summary);
		    item.setAbstract(abstractType);

		    EntityItemInterimFixStateType stateType = Factories.sc.aix.createEntityItemInterimFixStateType();
		    if ("S".equals(state)) {
			stateType.setValue("STABLE");
		    } else if ("M".equals(state)) {
			stateType.setValue("MOUNTED");
		    } else if ("U".equals(state)) {
			stateType.setValue("UNMOUNTED");
		    } else if ("B".equals(state)) {
			stateType.setValue("BROKEN");
		    } else if ("I".equals(state)) {
			stateType.setValue("INSTALLING");
		    } else if ("Q".equals(state)) {
			stateType.setValue("REBOOT_REQUIRED");
		    } else if ("R".equals(state)) {
			stateType.setValue("REMOVING");
		    } else {
			stateType.setStatus(StatusEnumeration.ERROR);

			String s = JOVALMsg.getMessage(JOVALMsg.ERROR_AIX_EMGR_STATE, vuid, state);
			session.getLogger().warn(s);
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(s);
			rc.addMessage(msg);
		    }
		    item.setState(stateType);
		    return item;
		}
	    }
	}

	//
	// If we've made it this far, there is no such VUID.
	//
	item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	return item;
    }
}
