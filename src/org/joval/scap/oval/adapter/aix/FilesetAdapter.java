// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.aix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
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
import scap.oval.definitions.aix.FilesetObject;
import scap.oval.systemcharacteristics.aix.EntityItemFilesetStateType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.EntityItemVersionType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.aix.FilesetItem;

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
public class FilesetAdapter implements IAdapter {
    private IUnixSession session;

    // Implement IAdapter

    public Collection<Class> init(ISession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    classes.add(FilesetObject.class);
	}
	return classes;
    }

    public Collection<FilesetItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	FilesetObject fObj = (FilesetObject)obj;
	Collection<FilesetItem> items = new Vector<FilesetItem>();
	switch(fObj.getFlstinst().getOperation()) {
	  case EQUALS:
	    try {
		String fileset = SafeCLI.checkArgument((String)fObj.getFlstinst().getValue(), session);
		session.getLogger().trace(JOVALMsg.STATUS_AIX_FILESET, fileset);
		for (String line : SafeCLI.multiLine("lslpp -lac '" + fileset + "'", session, IUnixSession.Timeout.M)) {
		    if (!line.startsWith("#")) {
			List<String> info = StringTools.toList(StringTools.tokenize(line, ":"));
			if (info.get(0).equals("/etc/objrepos")) {
			    FilesetItem item = Factories.sc.aix.createFilesetItem();

			    EntityItemStringType flstinst = Factories.sc.core.createEntityItemStringType();
			    flstinst.setValue(info.get(1));
			    item.setFlstinst(flstinst);

			    EntityItemVersionType level = Factories.sc.core.createEntityItemVersionType();
			    level.setValue(info.get(2));
			    level.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
			    item.setLevel(level);

			    EntityItemFilesetStateType state = Factories.sc.aix.createEntityItemFilesetStateType();
			    state.setValue(info.get(4));
			    item.setState(state);

			    EntityItemStringType description = Factories.sc.core.createEntityItemStringType();
			    description.setValue(info.get(6));
			    item.setDescription(description);

			    items.add(item);
			}
		    }
		}
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default: {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, fObj.getFlstinst().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	  }
	}

	return items;
    }
}
