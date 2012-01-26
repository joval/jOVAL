// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.aix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.aix.FixObject;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.systemcharacteristics.aix.EntityItemFixInstallationStatusType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.aix.FixItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.NotCollectableException;
import org.joval.oval.TestException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * Retrieves items for AIX fileset_objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FixAdapter implements IAdapter {
    private IUnixSession session;

    public FixAdapter(IUnixSession session) {
	this.session = session;
    }

    // Implement IAdapter

    private static Class[] objectClasses = {FixObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public boolean connect() {
	return session != null;
    }

    public void disconnect() {}

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws NotCollectableException {
	FixObject fObj = (FixObject)rc.getObject();
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	switch(fObj.getAparNumber().getOperation()) {
	  case EQUALS:
	    try {
		items.add(JOVALSystem.factories.sc.aix.createFixItem(getItem((String)fObj.getAparNumber().getValue())));
	    } catch (Exception e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default: {
	    String s = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, fObj.getAparNumber().getOperation());
	    throw new NotCollectableException(s);
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

	FixItem item = JOVALSystem.factories.sc.aix.createFixItem();

	EntityItemStringType aparNumber = JOVALSystem.factories.sc.core.createEntityItemStringType();
	aparNumber.setValue(apar);
	item.setAparNumber(aparNumber);

	List<String> lines = SafeCLI.multiLine("/usr/sbin/instfix -iavk " + apar, session, IUnixSession.Timeout.M);
	Iterator<String> iter = lines.iterator();

	String line = null;
	while(iter.hasNext()) {
	    line = iter.next();
	    if (line.startsWith(abstractLine)) {
		EntityItemStringType _abstract = JOVALSystem.factories.sc.core.createEntityItemStringType();
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
		    EntityItemStringType symptom = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    symptom.setValue(sb.toString());
		    item.setSymptom(symptom);
		}
	    }
	}

	if (line == null) {
	    //
	    // No stdout output means the fix is not installed.
	    //
	    item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
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
		    JOVALSystem.factories.sc.aix.createEntityItemFixInstallationStatusType();
		installationStatus.setValue(status);
		item.setInstallationStatus(installationStatus);
	    }
	}

	return item;
    }
}
