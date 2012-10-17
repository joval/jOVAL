// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.cisco.ios;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.ios.SnmpObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.ios.SnmpItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.io.PerishableReader;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * Provides Cisco IOS SNMP OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SnmpAdapter implements IAdapter {
    IIosSession session;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IIosSession) {
	    this.session = (IIosSession)session;
	    classes.add(SnmpObject.class);
	}
	return classes;
    }

    public Collection<SnmpItem> getItems(ObjectType obj, IRequestContext rc) {
	Collection<SnmpItem> items = new Vector<SnmpItem>();
	try {
	    for (String line : session.getTechSupport().getLines(ITechSupport.GLOBAL)) {
		if (line.toLowerCase().startsWith("snmp-server community ")) {
		    StringTokenizer tok = new StringTokenizer(line);
		    String name = null;
		    String number = null;
		    int numTokens = tok.countTokens();
		    if (numTokens >= 3) {
			SnmpItem item = Factories.sc.ios.createSnmpItem();

			tok.nextToken(); // "snmp-server"
			tok.nextToken(); // "community"

			EntityItemStringType communityName = Factories.sc.core.createEntityItemStringType();
			communityName.setValue(tok.nextToken());
			item.setCommunityName(communityName);

			if (numTokens > 3) {
			    String access = null;
			    String temp = tok.nextToken();
			    if (temp.equals("view")) {
				String viewName = tok.nextToken();
				temp = null;
			    }
			    //
			    // Get the final argument from the command
			    //
			    while(tok.hasMoreTokens()) {
				temp = tok.nextToken();
			    }
			    if (temp != null) {
				int i = 0;
				try {
				    i = Integer.parseInt(temp);
				} catch (NumberFormatException e) {
				    // not a valid access list
				}
				//
				// Valid values are between 1 and 99, inclusive.  See:
				// http://www.cisco.com/en/US/docs/ios/12_1/configfun/command/reference/frd3001.html#wp1022436
				//
				if (0 < i && i < 100) {
				    EntityItemStringType accessList = Factories.sc.core.createEntityItemStringType();
				    accessList.setValue(temp);
				    item.setAccessList(accessList);
				}
			    }
			}

			items.add(item);
		    } else {
			session.getLogger().warn(JOVALMsg.ERROR_IOS_SNMP, line);
		    }
		}
	    }
	} catch (NoSuchElementException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_TECH_SHOW, ITechSupport.GLOBAL));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }
}
