// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.aix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.aix.FixObject;
import scap.oval.systemcharacteristics.aix.EntityItemFixInstallationStatusType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.aix.FixItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves items for AIX fileset_objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FixAdapter implements IAdapter {
    private IUnixSession session;

    // Implement IAdapter

    public Collection<Class> init(ISession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    classes.add(FixObject.class);
	}
	return classes;
    }

    public Collection<FixItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	FixObject fObj = (FixObject)obj;
	Collection<FixItem> items = new Vector<FixItem>();
	switch(fObj.getAparNumber().getOperation()) {
	  case EQUALS:
	    try {
		items.add(getItem(SafeCLI.checkArgument((String)fObj.getAparNumber().getValue(), session)));
	    } catch (NoSuchElementException e) {
		// we'll return an empty list
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default: {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, fObj.getAparNumber().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	  }
	}

	return items;
    }

    // Private

    private FixItem getItem(String apar) throws Exception {
	session.getLogger().trace(JOVALMsg.STATUS_AIX_FIX, apar);

	String abstractLine = new StringBuffer(apar).append(" Abstract:").toString();
	String symptomLine = new StringBuffer(apar).append(" Symptom Text:").toString();
	String divider = "----------------------------";

	FixItem item = Factories.sc.aix.createFixItem();

	EntityItemStringType aparNumber = Factories.sc.core.createEntityItemStringType();
	aparNumber.setValue(apar);
	item.setAparNumber(aparNumber);

	List<String> lines = SafeCLI.multiLine("/usr/sbin/instfix -iavk '" + apar + "'", session, IUnixSession.Timeout.M);
	Iterator<String> iter = lines.iterator();

	String line = null;
	while(iter.hasNext()) {
	    line = iter.next();
	    if (line.startsWith(abstractLine)) {
		EntityItemStringType _abstract = Factories.sc.core.createEntityItemStringType();
		_abstract.setValue(line.substring(abstractLine.length()).trim());
		item.setAbstract(_abstract);
	    } else if (line.startsWith(symptomLine)) {
		StringBuffer sb = new StringBuffer();
		while(iter.hasNext()) {
		    line = iter.next();
		    if (line.equals(divider)) {
			break;
		    } else {
			if (sb.length() > 0) {
			    sb.append("\n");
			}
			sb.append(line);
		    }
		}
		if (sb.length() > 0) {
		    EntityItemStringType symptom = Factories.sc.core.createEntityItemStringType();
		    symptom.setValue(sb.toString());
		    item.setSymptom(symptom);
		}
	    }
	}

	if (line == null) {
	    //
	    // No stdout output means the fix is not installed.
	    //
	    throw new NoSuchElementException(apar);
	} else {
	    //
	    // Final line contains the status text
	    //
	    line = line.trim();
	    String status = null;
	    if (line.startsWith("All filesets for ")) {
		status = "ALL_INSTALLED";
	    } else if (line.startsWith("Not all filesets for ")) {
		status = "SOME_INSTALLED";
	    } else if (line.startsWith("No filesets which ")) {
		status = "NONE_INSTALLED";
	    }

	    if (status != null) {
		EntityItemFixInstallationStatusType installationStatus =
		    Factories.sc.aix.createEntityItemFixInstallationStatusType();
		installationStatus.setValue(status);
		item.setInstallationStatus(installationStatus);
	    }

	    return item;
	}
    }
}
